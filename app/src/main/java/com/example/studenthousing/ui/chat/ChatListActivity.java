package com.example.studenthousing.ui.chat;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.databinding.ActivityChatListBinding;
import com.example.studenthousing.ui.adapter.ChatRoomAdapter;
import com.example.studenthousing.ui.home.HomeActivity;
import com.example.studenthousing.ui.post.SavedHousesActivity;
import com.example.studenthousing.ui.profile.ProfileActivity;
import com.example.studenthousing.ui.viewmodel.ChatListViewModel;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity implements ChatRoomAdapter.OnChatRoomClickListener {

    private ActivityChatListBinding binding;
    private ChatListViewModel viewModel;
    private SessionManager session;
    private ChatRoomAdapter chatRoomAdapter;
    private List<ChatRoom> allRooms = new ArrayList<>();
    private final Map<String, String> partnerNames = new HashMap<>();
    private final Map<String, Integer> unreadCounts = new HashMap<>();
    private final java.util.Set<String> requestedPartnerProfiles = new java.util.HashSet<>();
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        userRepository = new UserRepository(this);
        viewModel = new ViewModelProvider(this).get(ChatListViewModel.class);

        binding.rvChatRooms.setLayoutManager(new LinearLayoutManager(this));
        chatRoomAdapter = new ChatRoomAdapter(this);
        binding.rvChatRooms.setAdapter(chatRoomAdapter);

        if (binding.swipeRefreshChatList != null) {
            binding.swipeRefreshChatList.setOnRefreshListener(this::loadChatRooms);
            binding.swipeRefreshChatList.setColorSchemeResources(R.color.primary);
        }
        viewModel.getRooms().observe(this, this::renderRooms);

        setupBottomNav();
        setupSearch();

        loadChatRooms();
    }

    private void setupSearch() {
        if (binding.edtSearchChat != null) {
            binding.edtSearchChat.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterRooms(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filterRooms(String query) {
        if (query == null || query.trim().isEmpty()) {
            chatRoomAdapter.submitList(allRooms);
            updateEmptyState(allRooms.isEmpty());
            return;
        }
        String q = query.toLowerCase().trim();
        List<ChatRoom> filtered = new ArrayList<>();
        for (ChatRoom r : allRooms) {
            String partnerId = getPartnerId(r);
            String partnerName = partnerId == null ? "" : partnerNames.getOrDefault(partnerId, "");
            if (partnerName.toLowerCase(java.util.Locale.ROOT).contains(q) ||
                    (r.getLastMessage() != null && r.getLastMessage().toLowerCase(java.util.Locale.ROOT).contains(q))) {
                filtered.add(r);
            }
        }
        chatRoomAdapter.submitList(filtered);
        updateEmptyState(filtered.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        if (binding.layoutEmptyState != null) {
            binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        if (targetActivity == HomeActivity.class) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }
        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, 0, 0);
        startActivity(intent, options.toBundle());
        finish();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_messages);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    navigateTo(HomeActivity.class);
                    return true;
                } else if (itemId == R.id.nav_saved) {
                    navigateTo(SavedHousesActivity.class);
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    navigateTo(ProfileActivity.class);
                    return true;
                }
                return false;
            });
        }
    }

    private void loadChatRooms() {
        if (binding.swipeRefreshChatList != null) binding.swipeRefreshChatList.setRefreshing(true);
        String userId = session.getUserId() != null ? session.getUserId() : "current_user";
        viewModel.observeRooms(userId);
    }

    private void renderRooms(UiState<List<ChatRoom>> state) {
        if (binding.swipeRefreshChatList != null) binding.swipeRefreshChatList.setRefreshing(false);
        if (state == null) return;
        if (state.getScreenState() == UiState.ScreenState.ERROR) {
            allRooms = new ArrayList<>();
            chatRoomAdapter.submitList(allRooms);
            updateEmptyState(true);
            return;
        }
        if (state.getContent() == null) return;
        allRooms = state.getContent();
        updateUnreadBadge();
        loadPartnerNames(allRooms);
        String query = binding.edtSearchChat == null ? "" : binding.edtSearchChat.getText().toString();
        filterRooms(query);
    }

    private void updateUnreadBadge() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        com.google.android.material.badge.BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_messages);
        String userId = session.getUserId();
        int unreadRooms = 0;
        if (userId != null) {
            for (ChatRoom room : allRooms) {
                if (room != null && room.getLastMessageTime() > 0
                        && room.getLastSenderId() != null
                        && !userId.equals(room.getLastSenderId())) unreadRooms++;
            }
        }
        badge.setVisible(unreadRooms > 0);
        if (unreadRooms > 0) badge.setNumber(unreadRooms);
    }

    private String getPartnerId(ChatRoom room) {
        if (room == null || room.getParticipantIds() == null) return null;
        String currentUserId = session.getUserId();
        for (String id : room.getParticipantIds()) {
            if (id != null && !id.equals(currentUserId)) return id;
        }
        return null;
    }

    private void loadPartnerNames(List<ChatRoom> rooms) {
        for (ChatRoom room : rooms) {
            String partnerId = getPartnerId(room);
            if (partnerId == null || partnerNames.containsKey(partnerId) || !requestedPartnerProfiles.add(partnerId)) continue;
            userRepository.getUserProfile(partnerId, new com.example.studenthousing.util.AppCallback<com.example.studenthousing.data.model.User>() {
                @Override public void onSuccess(com.example.studenthousing.data.model.User user) {
                    if (user != null && user.getFamilyName() != null) {
                        partnerNames.put(partnerId, user.getFamilyName());
                        runOnUiThread(() -> filterRooms(binding.edtSearchChat.getText().toString()));
                    }
                }
                @Override public void onError(Exception error) { requestedPartnerProfiles.remove(partnerId); }
            });
        }
    }
    @Override
    public void onChatRoomClick(@NonNull ChatRoom room) {
        Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
        intent.putExtra("ROOM_ID", room.getRoomId());

        String currentUserId = session.getUserId() != null ? session.getUserId() : "current_user";
        String partnerId = null;
        if (room.getParticipantIds() != null) {
            for (String pid : room.getParticipantIds()) {
                if (!currentUserId.equals(pid)) { partnerId = pid; break; }
            }
            if (partnerId == null && !room.getParticipantIds().isEmpty()) {
                partnerId = room.getParticipantIds().get(0);
            }
        }
        String partnerName = null;
        if (partnerId != null) {
            com.example.studenthousing.data.local.UserDao userDao = new com.example.studenthousing.data.local.UserDao(this);
            com.example.studenthousing.data.model.User partner = userDao.getById(partnerId);
            if (partner != null && partner.getFamilyName() != null && !partner.getFamilyName().isEmpty()) {
                partnerName = partner.getFamilyName();
            }
        }
        if (partnerName == null || partnerName.isEmpty()) {
            partnerName = "Hộ gia đình đối tác";
        }

        intent.putExtra("target_user_id", partnerId != null ? partnerId : "partner_user");
        intent.putExtra("ROOM_NAME", partnerName);
        startActivity(intent);
    }
}
