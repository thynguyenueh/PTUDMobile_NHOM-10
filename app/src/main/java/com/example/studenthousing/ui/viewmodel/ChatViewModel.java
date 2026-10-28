package com.example.studenthousing.ui.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.Message;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.ChatRepository;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.CloudinaryUploader;
import com.example.studenthousing.util.FileUtils;
import com.example.studenthousing.util.UiState;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<UiState<List<Message>>> messagesState = new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<User> targetUserProfile = new MutableLiveData<>();
    private final MutableLiveData<String> actionEvent = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration;
    private String currentUserId;
    private String targetUserId;
    private String roomId;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        chatRepository = new ChatRepository(application);
        userRepository = new UserRepository(application);
    }

    public LiveData<UiState<List<Message>>> getMessagesState() {
        return messagesState;
    }

    public LiveData<User> getTargetUserProfile() {
        return targetUserProfile;
    }

    public LiveData<String> getActionEvent() {
        return actionEvent;
    }

    public String getRoomId() {
        return roomId;
    }

    public void initChat(String currentUserId, String targetUserId, String passedRoomId,
                         String postId, String roomName) {
        if (currentUserId == null || currentUserId.trim().isEmpty()
                || targetUserId == null || targetUserId.trim().isEmpty()
                || currentUserId.equals(targetUserId)) {
            messagesState.setValue(UiState.error("Chat room requires two different users"));
            return;
        }

        this.currentUserId = currentUserId.trim();
        this.targetUserId = targetUserId.trim();

        if (passedRoomId != null && !passedRoomId.isEmpty()) {
            this.roomId = passedRoomId.trim();
        } else {
            this.roomId = chatRepository.getRoomId(this.currentUserId, this.targetUserId);
        }

        chatRepository.ensureRoom(
                this.roomId,
                Arrays.asList(currentUserId, targetUserId),
                postId,
                roomName,
                new AppCallback<Void>() {
                    @Override public void onSuccess(Void d) {}
                    @Override public void onError(Exception e) {}
                });

        if (targetUserId != null && !targetUserId.isEmpty()) {
            userRepository.getUserProfile(targetUserId, new AppCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) targetUserProfile.postValue(user);
                }

                @Override
                public void onError(Exception e) {}
            });
        }

        startListeningMessages();
    }

    private void startListeningMessages() {
        if (listenerRegistration != null) listenerRegistration.remove();
        messagesState.setValue(UiState.loading());

        listenerRegistration = chatRepository.listenMessages(roomId, new AppCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                messagesState.postValue(UiState.content(messages == null ? new ArrayList<>() : messages, UiState.SyncState.SYNCED));
            }

            @Override
            public void onError(Exception e) {
                messagesState.postValue(UiState.error(e != null && e.getMessage() != null ? e.getMessage() : "Lỗi đọc tin nhắn"));
            }
        });
    }

    public void sendMessage(String text) {
        if (text == null || text.trim().isEmpty() || roomId == null || currentUserId == null) return;

        Message msg = new Message();
        msg.setRoomId(roomId);
        msg.setSenderId(currentUserId);
        msg.setContent(text.trim());
        msg.setType("TEXT");
        msg.setSentAt(System.currentTimeMillis());

        chatRepository.sendMessage(roomId, msg, new AppCallback<String>() {
            @Override
            public void onSuccess(String data) {
                actionEvent.postValue("MESSAGE_SENT");
            }

            @Override
            public void onError(Exception e) {
                actionEvent.postValue("ERROR: " + (e != null ? e.getMessage() : "Lỗi gửi tin nhắn"));
            }
        });
    }

    public void uploadAndSendImage(Uri uri, String caption) {
        if (uri == null || roomId == null || currentUserId == null) return;

        if (!com.example.studenthousing.util.NetworkUtils.isOnline(getApplication())) {
            String persistentUri = FileUtils.saveUriToLocalFile(getApplication(), uri);
            Message msg = new Message();
            msg.setRoomId(roomId);
            msg.setSenderId(currentUserId);
            msg.setType("IMAGE");
            msg.setImageUrl(persistentUri != null ? persistentUri : uri.toString());
            msg.setContent(caption != null && !caption.trim().isEmpty() ? caption.trim() : null);
            msg.setSentAt(System.currentTimeMillis());

            chatRepository.sendMessage(roomId, msg, new AppCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    actionEvent.postValue("MESSAGE_SENT");
                }

                @Override
                public void onError(Exception e) {
                    actionEvent.postValue("ERROR: " + (e != null ? e.getMessage() : "Lỗi gửi ảnh"));
                }
            });
            return;
        }

        File file = FileUtils.getFileFromUri(getApplication(), uri);
        if (file == null) {
            actionEvent.postValue("ERROR: Không thể đọc tệp ảnh");
            return;
        }

        actionEvent.postValue("UPLOADING_IMAGE");

        CloudinaryUploader.uploadImage(file, new CloudinaryUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                Message msg = new Message();
                msg.setRoomId(roomId);
                msg.setSenderId(currentUserId);
                msg.setType("IMAGE");
                msg.setImageUrl(imageUrl);
                msg.setContent(caption != null && !caption.trim().isEmpty() ? caption.trim() : null);
                msg.setSentAt(System.currentTimeMillis());

                chatRepository.sendMessage(roomId, msg, new AppCallback<String>() {
                    @Override
                    public void onSuccess(String data) {
                        actionEvent.postValue("MESSAGE_SENT");
                    }

                    @Override
                    public void onError(Exception e) {
                        actionEvent.postValue("ERROR: " + (e != null ? e.getMessage() : "Lỗi gửi ảnh"));
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                actionEvent.postValue("ERROR: " + (e != null ? e.getMessage() : "Lỗi tải ảnh"));
            }
        });
    }

    @Override
    protected void onCleared() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        super.onCleared();
    }
}
