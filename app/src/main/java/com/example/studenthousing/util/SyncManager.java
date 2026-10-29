package com.example.studenthousing.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.studenthousing.data.local.MessageDao;
import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.local.ReviewDao;
import com.example.studenthousing.data.local.VerificationTicketDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.remote.MessageRemote;
import com.example.studenthousing.data.remote.PostRemote;
import com.example.studenthousing.data.remote.ReviewRemote;
import com.example.studenthousing.data.remote.VerificationTicketRemote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncManager {
    private static final String TAG = "SyncManager";
    /** Timeout cho mỗi batch sync (tickets/posts/reviews/messages). */
    private static final long BATCH_TIMEOUT_SEC = 60L;

    private final Context ctx;
    private final ReviewDao reviewDao;
    private final PostDao postDao;
    private final MessageDao messageDao;
    private final VerificationTicketDao ticketDao;

    private final ReviewRemote reviewRemote;
    private final PostRemote postRemote;
    private final MessageRemote messageRemote;
    private final VerificationTicketRemote ticketRemote;
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);
    private ConnectivityManager.NetworkCallback networkCallback;

    private static SyncManager instance;

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) instance = new SyncManager(context.getApplicationContext());
        return instance;
    }

    public SyncManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.reviewDao = new ReviewDao(this.ctx);
        this.postDao = new PostDao(this.ctx);
        this.messageDao = new MessageDao(this.ctx);
        this.ticketDao = new VerificationTicketDao(this.ctx);

        this.reviewRemote = new ReviewRemote();
        this.postRemote = new PostRemote();
        this.messageRemote = new MessageRemote();
        this.ticketRemote = new VerificationTicketRemote();
    }

    public synchronized void startAutoSync() {
        if (networkCallback != null) return;
        ConnectivityManager cm = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Có mạng — bắt đầu tự động đồng bộ Firestore <-> SQLite");
                triggerImmediateSyncWork();
                new Thread(() -> syncAll()).start();
            }
        };
        try {
            cm.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception e) {
            networkCallback = null;
            Log.e(TAG, "Lỗi đăng ký NetworkCallback: " + e.getMessage());
        }
    }

    public void scheduleWorkManager() {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest periodicSync =
                    new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                            .setConstraints(constraints)
                            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                            .build();

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    "StudentHousingPeriodicSync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicSync
            );
        } catch (Exception e) {
            Log.e(TAG, "Lỗi lập lịch WorkManager: " + e.getMessage());
        }
    }

    public void triggerImmediateSyncWork() {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest oneTimeSync =
                    new OneTimeWorkRequest.Builder(SyncWorker.class)
                            .setConstraints(constraints)
                            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                            .build();

            WorkManager.getInstance(ctx).enqueueUniqueWork(
                    "StudentHousingImmediateSync",
                    ExistingWorkPolicy.KEEP,
                    oneTimeSync
            );
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trigger OneTimeWorkRequest: " + e.getMessage());
        }
    }

    /**
     * ✅ FIX: Recovery — reset các item kẹt ở SYNCING (do app bị kill giữa lúc sync)
     * về trạng thái PENDING_* tương ứng để lần sync sau xử lý lại.
     *
     * Heuristic cho Posts (vì không có cột "sync_intent" để biết create/update/delete):
     *  - postId == null  -> PENDING_CREATE (chưa từng tạo remote)
     *  - postId != null  -> PENDING_UPDATE  (đã có remote, chỉ cần update lại)
     *
     * Với Tickets / Reviews / Messages: chỉ có luồng create -> reset về PENDING_CREATE.
     *
     * Idempotent — gọi nhiều lần không sao.
     */
    public void recoverStuckSyncing() {
        final String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return;

        try {
            // ---- POSTS ----
            List<AccommodationPost> allPosts = postDao.getAll();
            if (allPosts != null) {
                for (AccommodationPost p : allPosts) {
                    if (!activeUid.equals(p.getUserId())) continue;
                    if (!Constants.SYNC_SYNCING.equals(p.getSyncStatus())) continue;

                    boolean hasRemoteId = p.getPostId() != null && !p.getPostId().isEmpty();
                    p.setSyncStatus(hasRemoteId
                            ? Constants.SYNC_PENDING_UPDATE
                            : Constants.SYNC_PENDING_CREATE);
                    postDao.upsert(p);
                    Log.w(TAG, "Recovery post " + p.getPostId()
                            + " SYNCING -> " + p.getSyncStatus());
                }
            }

            // ---- TICKETS ----
            List<VerificationTicket> pendingTickets = ticketDao.getPendingByUser(activeUid);
            if (pendingTickets != null) {
                for (VerificationTicket t : pendingTickets) {
                    if (Constants.SYNC_SYNCING.equals(t.getSyncStatus())) {
                        t.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                        ticketDao.upsert(t);
                        Log.w(TAG, "Recovery ticket " + t.getTicketId()
                                + " SYNCING -> PENDING_CREATE");
                    }
                }
            }

            // ---- REVIEWS ----
            // ReviewDao không có API lấy tất cả; dùng getPendingReviews rồi lọc SYNCING.
            // Nếu DAO chỉ trả PENDING_* thì SYNCING sẽ không lọt — khi đó cần thêm
            // method getAllByReviewer(uid) trong ReviewDao để recovery đầy đủ.
            List<Review> pendingReviews = reviewDao.getPendingReviews();
            if (pendingReviews != null) {
                for (Review r : pendingReviews) {
                    if (!activeUid.equals(r.getReviewerId())) continue;
                    if (Constants.SYNC_SYNCING.equals(r.getSyncStatus())) {
                        r.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                        reviewDao.upsert(r);
                        Log.w(TAG, "Recovery review " + r.getReviewId()
                                + " SYNCING -> PENDING_CREATE");
                    }
                }
            }

            // ---- MESSAGES ----
            List<Message> pendingMsgs = messageDao.getPendingMessages();
            if (pendingMsgs != null) {
                for (Message msg : pendingMsgs) {
                    if (!activeUid.equals(msg.getSenderId())) continue;
                    if (Constants.SYNC_SYNCING.equals(msg.getSyncStatus())) {
                        msg.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                        messageDao.upsert(msg);
                        Log.w(TAG, "Recovery message " + msg.getMessageId()
                                + " SYNCING -> PENDING_CREATE");
                    }
                }
            }

            Log.d(TAG, "recoverStuckSyncing done for uid=" + activeUid);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi recoverStuckSyncing: " + e.getMessage(), e);
        }
    }

    /**
     * Chạy toàn bộ sync. Blocking — nên gọi trên thread nền (Worker, Executor…).
     *
     * @return true nếu tất cả các bước đều thành công (hoặc không có gì để sync).
     */
    public boolean syncAll() {
        if (!isSyncing.compareAndSet(false, true)) return true;
        boolean allSuccess = true;
        try {
            // ✅ FIX: luôn recovery trước — tránh item kẹt SYNCING từ lần sync trước
            //         (app bị kill, mất mạng giữa chừng, v.v.)
            recoverStuckSyncing();

            if (!syncTickets()) allSuccess = false;
            if (!syncPosts()) allSuccess = false;
            if (!syncPendingUpdates()) allSuccess = false;
            if (!syncPendingDeletes()) allSuccess = false;
            if (!syncReviews()) allSuccess = false;
            if (!syncMessages()) allSuccess = false;
        } catch (Exception e) {
            Log.e(TAG, "Error during syncAll: " + e.getMessage());
            allSuccess = false;
        } finally {
            isSyncing.set(false);
        }
        Log.d(TAG, "syncAll finished, success=" + allSuccess);
        return allSuccess;
    }

    // ===================================================================
    // TICKETS
    // ===================================================================
    private boolean syncTickets() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<VerificationTicket> pending = ticketDao.getPendingByUser(activeUid);
        if (pending == null || pending.isEmpty()) return true;

        List<VerificationTicket> toSync = new ArrayList<>();
        for (VerificationTicket t : pending) {
            if (Constants.SYNC_PENDING_CREATE.equals(t.getSyncStatus())) {
                toSync.add(t);
            }
        }
        if (toSync.isEmpty()) return true;

        CountDownLatch latch = new CountDownLatch(toSync.size());
        AtomicBoolean success = new AtomicBoolean(true);

        for (VerificationTicket ticket : toSync) {
            ticket.setSyncStatus(Constants.SYNC_SYNCING);
            ticketDao.upsert(ticket);

            ticketRemote.submit(ticket, new AppCallback<String>() {
                @Override
                public void onSuccess(String id) {
                    ticket.setTicketId(id);
                    ticket.setSyncStatus(Constants.SYNC_SYNCED);
                    ticketDao.upsert(ticket);
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    ticket.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                    ticketDao.upsert(ticket);
                    Log.e(TAG, "Lỗi sync ticket: " + e.getMessage());
                    success.set(false);
                    latch.countDown();
                }
            });
        }

        return awaitLatch(latch, "tickets", success);
    }

    // ===================================================================
    // POSTS — CREATE
    // ===================================================================
    private boolean syncPosts() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<AccommodationPost> all = postDao.getAll();
        if (all == null || all.isEmpty()) return true;

        List<AccommodationPost> toCreate = new ArrayList<>();
        for (AccommodationPost p : all) {
            if (Constants.SYNC_PENDING_CREATE.equals(p.getSyncStatus())
                    && activeUid.equals(p.getUserId())) {
                toCreate.add(p);
            }
        }
        if (toCreate.isEmpty()) return true;

        CountDownLatch latch = new CountDownLatch(toCreate.size());
        AtomicBoolean success = new AtomicBoolean(true);

        for (AccommodationPost p : toCreate) {
            p.setSyncStatus(Constants.SYNC_SYNCING);
            postDao.upsert(p);
            uploadLocalImagesAndRemoteCreate(p, latch, success);
        }

        return awaitLatch(latch, "posts.create", success);
    }

    private void uploadLocalImagesAndRemoteCreate(AccommodationPost p,
                                                  CountDownLatch latch,
                                                  AtomicBoolean success) {
        List<String> images = p.getImages() != null
                ? new ArrayList<>(p.getImages())
                : new ArrayList<>();

        uploadPendingImages(p, images, 0, new AppCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> remoteImageUrls) {
                p.setImages(remoteImageUrls);
                postRemote.create(p, new AppCallback<String>() {
                    @Override
                    public void onSuccess(String id) {
                        p.setPostId(id);
                        p.setSyncStatus(Constants.SYNC_SYNCED);
                        postDao.upsert(p);
                        Log.d(TAG, "Đã đồng bộ Bài đăng lên Firestore: " + id);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        p.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                        postDao.upsert(p);
                        Log.e(TAG, "Thất bại đồng bộ Bài đăng: " + e.getMessage());
                        success.set(false);
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                p.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                postDao.upsert(p);
                Log.e(TAG, "Lỗi upload ảnh offline cho post: " + e.getMessage());
                success.set(false);
                latch.countDown();
            }
        });
    }

    private void uploadPendingImages(AccommodationPost post,
                                     List<String> images,
                                     int index,
                                     AppCallback<List<String>> cb) {
        if (index >= images.size()) {
            cb.onSuccess(images);
            return;
        }

        String imgStr = images.get(index);
        if (imgStr == null
                || imgStr.startsWith("http://")
                || imgStr.startsWith("https://")) {
            uploadPendingImages(post, images, index + 1, cb);
            return;
        }

        if (imgStr.startsWith("content://") || imgStr.startsWith("file://")) {
            try {
                android.net.Uri uri = android.net.Uri.parse(imgStr);
                java.io.File file = FileUtils.getFileFromUri(ctx, uri);
                if (file != null) {
                    CloudinaryUploader.uploadImage(file, new AppCallback<String>() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            FileUtils.deleteLocalFile(ctx, imgStr);
                            images.set(index, imageUrl);
                            if (post != null) {
                                post.setImages(new ArrayList<>(images));
                                postDao.upsert(post);
                            }
                            uploadPendingImages(post, images, index + 1, cb);
                        }

                        @Override
                        public void onError(Exception e) {
                            cb.onError(e);
                        }
                    });
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi parse URI ảnh local: " + e.getMessage());
            }
        }
        uploadPendingImages(post, images, index + 1, cb);
    }

    // ===================================================================
    // POSTS — UPDATE
    // ===================================================================
    private boolean syncPendingUpdates() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<AccommodationPost> all = postDao.getAll();
        if (all == null || all.isEmpty()) return true;

        List<AccommodationPost> toUpdate = new ArrayList<>();
        for (AccommodationPost p : all) {
            if (Constants.SYNC_PENDING_UPDATE.equals(p.getSyncStatus())
                    && activeUid.equals(p.getUserId())) {
                toUpdate.add(p);
            }
        }
        if (toUpdate.isEmpty()) return true;

        CountDownLatch latch = new CountDownLatch(toUpdate.size());
        AtomicBoolean success = new AtomicBoolean(true);

        for (AccommodationPost p : toUpdate) {
            p.setSyncStatus(Constants.SYNC_SYNCING);
            postDao.upsert(p);
            postRemote.update(p, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    p.setSyncStatus(Constants.SYNC_SYNCED);
                    postDao.upsert(p);
                    Log.d(TAG, "Đã sync update bài đăng: " + p.getPostId());
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    p.setSyncStatus(Constants.SYNC_PENDING_UPDATE);
                    postDao.upsert(p);
                    Log.e(TAG, "Lỗi sync update bài đăng: " + e.getMessage());
                    success.set(false);
                    latch.countDown();
                }
            });
        }

        return awaitLatch(latch, "posts.update", success);
    }

    // ===================================================================
    // POSTS — DELETE
    // ===================================================================
    private boolean syncPendingDeletes() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<AccommodationPost> all = postDao.getAll();
        if (all == null || all.isEmpty()) return true;

        List<AccommodationPost> toDelete = new ArrayList<>();
        for (AccommodationPost p : all) {
            if (Constants.SYNC_PENDING_DELETE.equals(p.getSyncStatus())
                    && activeUid.equals(p.getUserId())) {
                toDelete.add(p);
            }
        }
        if (toDelete.isEmpty()) return true;

        CountDownLatch latch = new CountDownLatch(toDelete.size());
        AtomicBoolean success = new AtomicBoolean(true);

        for (AccommodationPost p : toDelete) {
            p.setSyncStatus(Constants.SYNC_SYNCING);
            postDao.upsert(p);
            postRemote.delete(p.getPostId(), new AppCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    postDao.deleteById(p.getPostId());
                    Log.d(TAG, "Đã sync delete bài đăng: " + p.getPostId());
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    p.setSyncStatus(Constants.SYNC_PENDING_DELETE);
                    postDao.upsert(p);
                    Log.e(TAG, "Lỗi sync delete bài đăng: " + e.getMessage());
                    success.set(false);
                    latch.countDown();
                }
            });
        }

        return awaitLatch(latch, "posts.delete", success);
    }

    // ===================================================================
    // REVIEWS
    // ===================================================================
    private boolean syncReviews() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<Review> pending = reviewDao.getPendingReviews();
        if (pending == null || pending.isEmpty()) return true;

        List<Review> toSync = new ArrayList<>();
        for (Review r : pending) {
            if (activeUid.equals(r.getReviewerId())) {
                toSync.add(r);
            }
        }
        if (toSync.isEmpty()) return true;

        CountDownLatch latch = new CountDownLatch(toSync.size());
        AtomicBoolean success = new AtomicBoolean(true);

        for (Review r : toSync) {
            // ✅ FIX: đánh dấu SYNCING để recovery biết nếu bị kill giữa chừng
            r.setSyncStatus(Constants.SYNC_SYNCING);
            reviewDao.upsert(r);

            reviewRemote.create(r, new AppCallback<String>() {
                @Override
                public void onSuccess(String id) {
                    r.setReviewId(id != null ? id : r.getReviewId());
                    r.setSyncStatus(Constants.SYNC_SYNCED);
                    reviewDao.upsert(r);
                    Log.d(TAG, "Đã sync review: " + id);
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    r.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                    reviewDao.upsert(r);
                    Log.e(TAG, "Lỗi sync review: " + e.getMessage());
                    success.set(false);
                    latch.countDown();
                }
            });
        }

        return awaitLatch(latch, "reviews", success);
    }

    // ===================================================================
    // MESSAGES
    // ===================================================================
    private boolean syncMessages() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<Message> pending = messageDao.getPendingMessages();
        if (pending == null || pending.isEmpty()) return true;

        List<Message> toSync = new ArrayList<>();
        for (Message msg : pending) {
            if (msg.getRoomId() == null || msg.getMessageId() == null) continue;
            if (!activeUid.equals(msg.getSenderId())) continue;
            toSync.add(msg);
        }
        if (toSync.isEmpty()) return true;

        CountDownLatch latch = new CountDownLatch(toSync.size());
        AtomicBoolean success = new AtomicBoolean(true);

        for (Message msg : toSync) {
            // ✅ FIX: đánh dấu SYNCING
            msg.setSyncStatus(Constants.SYNC_SYNCING);
            messageDao.upsert(msg);

            if ("IMAGE".equals(msg.getType())
                    && msg.getImageUrl() != null
                    && (msg.getImageUrl().startsWith("content://")
                    || msg.getImageUrl().startsWith("file://"))) {
                try {
                    String localImgPath = msg.getImageUrl();
                    android.net.Uri uri = android.net.Uri.parse(localImgPath);
                    java.io.File file = FileUtils.getFileFromUri(ctx, uri);
                    if (file != null) {
                        CloudinaryUploader.uploadImage(file, new AppCallback<String>() {
                            @Override
                            public void onSuccess(String imageUrl) {
                                FileUtils.deleteLocalFile(ctx, localImgPath);
                                msg.setImageUrl(imageUrl);
                                messageDao.upsert(msg);
                                sendPendingRemoteMessage(msg, latch, success);
                            }

                            @Override
                            public void onError(Exception e) {
                                msg.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                                messageDao.upsert(msg);
                                Log.e(TAG, "Lỗi upload ảnh tin nhắn offline: " + e.getMessage());
                                success.set(false);
                                latch.countDown();
                            }
                        });
                        continue;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi parse URI ảnh tin nhắn: " + e.getMessage());
                }
            }
            sendPendingRemoteMessage(msg, latch, success);
        }

        return awaitLatch(latch, "messages", success);
    }

    private void sendPendingRemoteMessage(Message msg,
                                          CountDownLatch latch,
                                          AtomicBoolean success) {
        messageRemote.sendMessage(msg.getRoomId(), msg, new AppCallback<String>() {
            @Override
            public void onSuccess(String id) {
                msg.setSyncStatus(Constants.SYNC_SYNCED);
                messageDao.upsert(msg);
                Log.d(TAG, "Đã sync message: " + id);
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                msg.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                messageDao.upsert(msg);
                Log.e(TAG, "Lỗi sync message: " + e.getMessage());
                success.set(false);
                latch.countDown();
            }
        });
    }

    // ===================================================================
    // HELPER
    // ===================================================================
    private boolean awaitLatch(CountDownLatch latch, String tag, AtomicBoolean success) {
        try {
            boolean done = latch.await(BATCH_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!done) {
                Log.w(TAG, "Timeout khi sync " + tag
                        + " (còn " + latch.getCount() + " item)");
                // ✅ FIX: các item còn kẹt SYNCING sẽ được recoverStuckSyncing()
                //         ở lần sync sau reset lại — không cần xử lý thêm ở đây.
                return false;
            }
            return success.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Interrupted khi sync " + tag);
            return false;
        }
    }
}