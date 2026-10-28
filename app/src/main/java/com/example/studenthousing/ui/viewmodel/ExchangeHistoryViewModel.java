package com.example.studenthousing.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.ChatRepository;
import com.example.studenthousing.data.repository.ExchangeRepository;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.ExchangeStatusChecker;
import com.example.studenthousing.util.UiState;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExchangeHistoryViewModel extends AndroidViewModel {

    public static class ExchangeHistoryData {
        public final List<ExchangeRequest> allRequests;
        public final Map<String, String> userNames;
        public final Map<String, String> postTitles;

        public ExchangeHistoryData(List<ExchangeRequest> allRequests,
                                   Map<String, String> userNames,
                                   Map<String, String> postTitles) {
            this.allRequests = allRequests;
            this.userNames = userNames;
            this.postTitles = postTitles;
        }
    }

    private final ExchangeRepository exchangeRepository;
    private final PostRepository postRepository;
    private final ChatRepository chatRepository;
    private final PostDao postDao;
    private final UserDao userDao;
    private final MutableLiveData<UiState<ExchangeHistoryData>> state = new MutableLiveData<>(UiState.loading());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ListenerRegistration senderListener;
    private ListenerRegistration receiverListener;

    public ExchangeHistoryViewModel(@NonNull Application application) {
        super(application);
        exchangeRepository = new ExchangeRepository(application);
        postRepository = new PostRepository(application);
        chatRepository = new ChatRepository(application);
        postDao = new PostDao(application);
        userDao = new UserDao(application);
    }

    public LiveData<UiState<ExchangeHistoryData>> getState() {
        return state;
    }

    public void observeExchangeHistory(String userId) {
        if (userId == null || userId.isEmpty()) {
            state.setValue(UiState.error("Chưa đăng nhập"));
            return;
        }

        cleanupListeners();
        state.setValue(UiState.loading());

        final List<ExchangeRequest> outList = new ArrayList<>();
        final List<ExchangeRequest> inList = new ArrayList<>();
        final boolean[] outDone = {false}, inDone = {false};

        Runnable maybeRender = () -> {
            if (outDone[0] && inDone[0]) {
                List<ExchangeRequest> allMerged = new ArrayList<>(outList);
                allMerged.addAll(inList);
                Map<String, ExchangeRequest> uniqueRequests = new HashMap<>();
                for (ExchangeRequest request : allMerged) {
                    if (request != null && request.getRequestId() != null) uniqueRequests.put(request.getRequestId(), request);
                }
                final List<ExchangeRequest> merged = new ArrayList<>(uniqueRequests.values());
                
                // LƯU Ý: Đây là kiểm tra phía client (chạy khi mở app qua ViewModel/SyncWorker), không chạy khi app không mở.
                ExchangeStatusChecker.checkAndApplyExpirations(getApplication(), merged, userId, null);

                merged.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                executor.execute(() -> {
                    Map<String, String> userNames = new HashMap<>();
                    Map<String, String> postTitles = new HashMap<>();

                    for (ExchangeRequest req : merged) {
                        boolean isInbound = userId.equals(req.getReceiverId());
                        String partnerId = isInbound ? req.getSenderId() : req.getReceiverId();
                        if (partnerId != null && !userNames.containsKey(partnerId)) {
                            User u = userDao.getById(partnerId);
                            userNames.put(partnerId, u != null && u.getFamilyName() != null ? u.getFamilyName() : partnerId);
                        }
                        String pid = isInbound ? req.getSenderPostId() : req.getReceiverPostId();
                        if (pid != null && !postTitles.containsKey(pid)) {
                            AccommodationPost p = postDao.getById(pid);
                            postTitles.put(pid, p != null && p.getTitle() != null ? p.getTitle() : "Yêu cầu đổi nhà");
                        }
                    }

                    ExchangeHistoryData data = new ExchangeHistoryData(merged, userNames, postTitles);
                    state.postValue(UiState.content(data, UiState.SyncState.SYNCED));
                });
            }
        };

        senderListener = exchangeRepository.listenBySender(userId, new AppCallback<List<ExchangeRequest>>() {
            @Override
            public void onSuccess(List<ExchangeRequest> list) {
                outList.clear();
                if (list != null) outList.addAll(list);
                outDone[0] = true;
                maybeRender.run();
            }

            @Override
            public void onError(Exception e) {
                outDone[0] = true;
                maybeRender.run();
            }
        });

        receiverListener = exchangeRepository.listenByReceiver(userId, new AppCallback<List<ExchangeRequest>>() {
            @Override
            public void onSuccess(List<ExchangeRequest> list) {
                inList.clear();
                if (list != null) inList.addAll(list);
                inDone[0] = true;
                maybeRender.run();
            }

            @Override
            public void onError(Exception e) {
                inDone[0] = true;
                maybeRender.run();
            }
        });
    }

    public void acceptExchange(ExchangeRequest request, String currentUserId, AppCallback<Void> callback) {
        exchangeRepository.updateStatus(request.getRequestId(), Constants.REQ_EXCHANGING, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                openChatRoomForAccepted(request, currentUserId);
                if (callback != null) callback.onSuccess(null);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void rejectExchange(ExchangeRequest request, AppCallback<Void> callback) {
        exchangeRepository.updateStatus(request.getRequestId(), Constants.REQ_REJECTED, callback);
    }

    private void openChatRoomForAccepted(ExchangeRequest req, String currentUserId) {
        String me = currentUserId;
        String other = me != null && me.equals(req.getSenderId()) ? req.getReceiverId() : req.getSenderId();
        if (me == null || other == null) return;
        String roomId = chatRepository.getRoomId(me, other);
        chatRepository.ensureRoom(
                roomId,
                Arrays.asList(me, other),
                req.getReceiverPostId(),
                null,
                new AppCallback<Void>() {
                    @Override public void onSuccess(Void d) {}
                    @Override public void onError(Exception e) {}
                });
    }

    private void cleanupListeners() {
        if (senderListener != null) { senderListener.remove(); senderListener = null; }
        if (receiverListener != null) { receiverListener.remove(); receiverListener = null; }
    }

    @Override
    protected void onCleared() {
        cleanupListeners();
        executor.shutdown();
        super.onCleared();
    }
}
