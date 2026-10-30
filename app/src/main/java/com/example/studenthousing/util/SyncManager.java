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

import com.example.studenthousing.data.local.ExchangeDao;
import com.example.studenthousing.data.local.MessageDao;
import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.local.ReviewDao;
import com.example.studenthousing.data.local.VerificationTicketDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.data.model.Review;
import com.example.studenthousing.data.model.VerificationTicket;
import com.example.studenthousing.data.remote.ExchangeRemote;
import com.example.studenthousing.data.remote.MessageRemote;
import com.example.studenthousing.data.remote.PostRemote;
import com.example.studenthousing.data.remote.ReviewRemote;
import com.example.studenthousing.data.remote.VerificationTicketRemote;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context ctx;
    private final ExchangeDao exchangeDao;
    private final ReviewDao reviewDao;
    private final PostDao postDao;
    private final MessageDao messageDao;
    private final VerificationTicketDao ticketDao;

    private final ExchangeRemote exchangeRemote;
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
        this.exchangeDao = new ExchangeDao(this.ctx);
        this.reviewDao = new ReviewDao(this.ctx);
        this.postDao = new PostDao(this.ctx);
        this.messageDao = new MessageDao(this.ctx);
        this.ticketDao = new VerificationTicketDao(this.ctx);

        this.exchangeRemote = new ExchangeRemote();
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
                syncAll();
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

    public boolean syncAll() {
        if (!isSyncing.compareAndSet(false, true)) return true;
        boolean allSuccess = true;
        try {
            allSuccess &= syncTickets();
            allSuccess &= syncPosts();
            allSuccess &= syncPendingUpdates();
            allSuccess &= syncPendingDeletes();
            allSuccess &= syncExchanges();
            allSuccess &= syncReviews();
            allSuccess &= syncMessages();
        } catch (Exception e) {
            Log.e(TAG, "Error during syncAll: " + e.getMessage());
            allSuccess = false;
        } finally {
            isSyncing.set(false);
        }
        return allSuccess;
    }

    private boolean syncTickets() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<VerificationTicket> pendingTickets = ticketDao.getPendingByUser(activeUid);
        if (pendingTickets != null) {
            for (VerificationTicket ticket : pendingTickets) {
                if (Constants.SYNC_PENDING_CREATE.equals(ticket.getSyncStatus())) {
                    ticketRemote.submit(ticket, new AppCallback<String>() {
                        @Override
                        public void onSuccess(String id) {
                            ticket.setTicketId(id);
                            ticket.setSyncStatus(Constants.SYNC_SYNCED);
                            ticketDao.upsert(ticket);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Lỗi sync ticket: " + e.getMessage());
                        }
                    });
                }
            }
        }
        return true;
    }

    private boolean syncPosts() {
        List<AccommodationPost> pendingCreates = postDao.getAll();
        if (pendingCreates != null) {
            for (AccommodationPost p : pendingCreates) {
                if (Constants.SYNC_PENDING_CREATE.equals(p.getSyncStatus()) || Constants.SYNC_PENDING_UPDATE.equals(p.getSyncStatus())) {
                    p.setSyncStatus(Constants.SYNC_SYNCING);
                    postDao.upsert(p);
                    uploadLocalImagesAndRemoteCreate(p);
                }
            }
        }
        return true;
    }

    private void uploadLocalImagesAndRemoteCreate(AccommodationPost p) {
        List<String> images = p.getImages() != null ? new java.util.ArrayList<>(p.getImages()) : new java.util.ArrayList<>();
        uploadPendingImages(p, images, 0, new AppCallback<>() {
            @Override
            public void onSuccess(List<String> remoteImageUrls) {
                p.setImages(remoteImageUrls);
                postRemote.create(p, new AppCallback<String>() {
                    @Override
                    public void onSuccess(String id) {
                        String oldId = p.getPostId();
                        if (oldId != null && oldId.startsWith("temp_") && !oldId.equals(id)) {
                            postDao.deleteById(oldId);
                        }
                        p.setPostId(id);
                        p.setSyncStatus(Constants.SYNC_SYNCED);
                        postDao.upsert(p);
                        Log.d(TAG, "Đã đồng bộ Bài đăng lên Firestore: " + id);
                    }

                    @Override
                    public void onError(Exception e) {
                        p.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                        postDao.upsert(p);
                        Log.e(TAG, "Thất bại đồng bộ Bài đăng: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                p.setSyncStatus(Constants.SYNC_PENDING_CREATE);
                postDao.upsert(p);
                Log.e(TAG, "Lỗi upload ảnh offline cho post: " + e.getMessage());
            }
        });
    }

    private void uploadPendingImages(AccommodationPost post, List<String> images, int index, AppCallback<List<String>> cb) {
        if (index >= images.size()) {
            cb.onSuccess(images);
            return;
        }

        String imgStr = images.get(index);
        if (imgStr != null && (imgStr.startsWith("http://") || imgStr.startsWith("https://"))) {
            uploadPendingImages(post, images, index + 1, cb);
            return;
        }

        if (imgStr != null && (imgStr.startsWith("content://") || imgStr.startsWith("file://"))) {
            try {
                android.net.Uri uri = android.net.Uri.parse(imgStr);
                java.io.File file = FileUtils.getFileFromUri(ctx, uri);
                if (file != null) {
                    CloudinaryUploader.uploadImage(file, new CloudinaryUploader.UploadCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            FileUtils.deleteLocalFile(ctx, imgStr);

                            images.set(index, imageUrl);
                            if (post != null) {
                                post.setImages(new java.util.ArrayList<>(images));
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

    private boolean syncPendingUpdates() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<AccommodationPost> pending = postDao.getAll();
        if (pending != null) {
            for (AccommodationPost p : pending) {
                if (Constants.SYNC_PENDING_UPDATE.equals(p.getSyncStatus()) && activeUid.equals(p.getUserId())) {
                    p.setSyncStatus(Constants.SYNC_SYNCING);
                    postDao.upsert(p);
                    postRemote.update(p, new AppCallback<Void>() {
                        @Override public void onSuccess(Void data) {
                            p.setSyncStatus(Constants.SYNC_SYNCED);
                            postDao.upsert(p);
                            Log.d(TAG, "Đã sync update bài đăng: " + p.getPostId());
                        }
                        @Override public void onError(Exception e) {
                            p.setSyncStatus(Constants.SYNC_PENDING_UPDATE);
                            postDao.upsert(p);
                            Log.e(TAG, "Lỗi sync update bài đăng: " + e.getMessage());
                        }
                    });
                }
            }
        }
        return true;
    }

    private boolean syncPendingDeletes() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<AccommodationPost> pendingDeletes = postDao.getAll();
        if (pendingDeletes != null) {
            for (AccommodationPost p : pendingDeletes) {
                if (Constants.SYNC_PENDING_DELETE.equals(p.getSyncStatus()) && activeUid.equals(p.getUserId())) {
                    p.setSyncStatus(Constants.SYNC_SYNCING);
                    postDao.upsert(p);
                    postRemote.delete(p.getPostId(), new AppCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            postDao.deleteById(p.getPostId());
                            Log.d(TAG, "Đã sync delete bài đăng: " + p.getPostId());
                        }

                        @Override
                        public void onError(Exception e) {
                            p.setSyncStatus(Constants.SYNC_PENDING_DELETE);
                            postDao.upsert(p);
                            Log.e(TAG, "Lỗi sync delete bài đăng: " + e.getMessage());
                        }
                    });
                }
            }
        }
        return true;
    }

    private boolean syncExchanges() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<ExchangeRequest> pending = exchangeDao.getPendingRequests();
        if (pending != null) {
            for (ExchangeRequest req : pending) {
                if (activeUid.equals(req.getSenderId())) {
                    exchangeRemote.create(req, new AppCallback<String>() {
                        @Override
                        public void onSuccess(String id) {
                            req.setSyncStatus(Constants.SYNC_SYNCED);
                            exchangeDao.upsert(req);
                            Log.d(TAG, "Đã sync exchange request: " + id);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Lỗi sync exchange request: " + e.getMessage());
                        }
                    });
                }
            }
        }
        return true;
    }

    private boolean syncReviews() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<Review> pendingReviews = reviewDao.getPendingReviews();
        if (pendingReviews != null) {
            for (Review r : pendingReviews) {
                if (activeUid.equals(r.getReviewerId())) {
                    reviewRemote.create(r, new AppCallback<String>() {
                        @Override
                        public void onSuccess(String id) {
                            r.setSyncStatus(Constants.SYNC_SYNCED);
                            reviewDao.upsert(r);
                            Log.d(TAG, "Đã sync review: " + id);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Lỗi sync review: " + e.getMessage());
                        }
                    });
                }
            }
        }
        return true;
    }

    private boolean syncMessages() {
        String activeUid = SessionManager.getUid(ctx);
        if (activeUid == null) return true;

        List<Message> pendingMsgs = messageDao.getPendingMessages();
        if (pendingMsgs != null) {
            for (Message msg : pendingMsgs) {
                if (msg.getRoomId() != null && msg.getMessageId() != null) {
                    if (!activeUid.equals(msg.getSenderId())) continue;
                    if ("IMAGE".equals(msg.getType()) && msg.getImageUrl() != null &&
                            (msg.getImageUrl().startsWith("content://") || msg.getImageUrl().startsWith("file://"))) {
                        try {
                            String localImgPath = msg.getImageUrl();
                            android.net.Uri uri = android.net.Uri.parse(localImgPath);
                            java.io.File file = FileUtils.getFileFromUri(ctx, uri);
                            if (file != null) {
                                CloudinaryUploader.uploadImage(file, new CloudinaryUploader.UploadCallback() {
                                    @Override
                                    public void onSuccess(String imageUrl) {
                                        FileUtils.deleteLocalFile(ctx, localImgPath);
                                        msg.setImageUrl(imageUrl);
                                        messageDao.upsert(msg);
                                        sendPendingRemoteMessage(msg);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Log.e(TAG, "Lỗi upload ảnh tin nhắn offline: " + e.getMessage());
                                    }
                                });
                                continue;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi parse URI ảnh tin nhắn: " + e.getMessage());
                        }
                    }
                    sendPendingRemoteMessage(msg);
                }
            }
        }
        return true;
    }

    private void sendPendingRemoteMessage(Message msg) {
        messageRemote.sendMessage(msg.getRoomId(), msg, new AppCallback<String>() {
            @Override
            public void onSuccess(String id) {
                msg.setSyncStatus(Constants.SYNC_SYNCED);
                messageDao.upsert(msg);
                Log.d(TAG, "Đã sync message: " + id);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Lỗi sync message: " + e.getMessage());
            }
        });
    }
}
