package com.example.studenthousing.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.local.UserDao;
import com.example.studenthousing.data.model.ChatRoom;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.TimeFormatter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomAdapter extends ListAdapter<ChatRoom, ChatRoomAdapter.ChatRoomViewHolder> {

    public interface OnChatRoomClickListener {
        void onChatRoomClick(@NonNull ChatRoom room);
    }

    private final OnChatRoomClickListener listener;
    private static final Map<String, String> partnerNameCache = new ConcurrentHashMap<>();

    public ChatRoomAdapter(@NonNull OnChatRoomClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ChatRoom> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatRoom oldItem, @NonNull ChatRoom newItem) {
            return Objects.equals(oldItem.getRoomId(), newItem.getRoomId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatRoom oldItem, @NonNull ChatRoom newItem) {
            return oldItem.getLastMessageTime() == newItem.getLastMessageTime()
                    && Objects.equals(oldItem.getLastMessage(), newItem.getLastMessage())
                    && Objects.equals(oldItem.getLastSenderId(), newItem.getLastSenderId());
        }
    };

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static String resolvePartnerName(Context ctx, String targetUserId) {
        if (targetUserId == null) {
            return ctx.getString(R.string.chat_default_partner_name);
        }
        String cached = partnerNameCache.get(targetUserId);
        if (cached != null && !cached.isEmpty()) return cached;

        String name = null;
        try {
            User partner = new UserDao(ctx).getById(targetUserId);
            if (partner != null && partner.getFamilyName() != null
                    && !partner.getFamilyName().isEmpty()) {
                name = partner.getFamilyName();
            }
        } catch (Exception ignored) { }

        if (name == null || name.isEmpty()) {
            name = ctx.getString(R.string.chat_default_partner_name);
        }
        partnerNameCache.put(targetUserId, name);
        return name;
    }

    public static class ChatRoomViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvAvatar;
        private final TextView tvTitle;
        private final TextView tvHouseTitle;
        private final TextView tvLastMessage;
        private final TextView tvTime;
        private final TextView tvUnreadBadge;
        private final OnChatRoomClickListener listener;

        ChatRoomViewHolder(@NonNull View itemView, @NonNull OnChatRoomClickListener listener) {
            super(itemView);
            this.listener = listener;

            tvAvatar = itemView.findViewById(R.id.tvChatAvatar);
            tvTitle = itemView.findViewById(R.id.tvChatTitle);
            tvHouseTitle = itemView.findViewById(R.id.tvHouseTitle);
            tvLastMessage = itemView.findViewById(R.id.tvChatLastMessage);
            tvTime = itemView.findViewById(R.id.tvChatTime);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }

        void bind(@NonNull ChatRoom room) {
            Context ctx = itemView.getContext();
            String currentUserId = SessionManager.getUid(ctx);

            String targetUserId = null;
            if (room.getParticipantIds() != null) {
                for (String pid : room.getParticipantIds()) {
                    if (pid != null && !Objects.equals(pid, currentUserId)) {
                        targetUserId = pid;
                        break;
                    }
                }
                if (targetUserId == null && !room.getParticipantIds().isEmpty()) {
                    targetUserId = room.getParticipantIds().get(0);
                }
            }

            String partnerName = resolvePartnerName(ctx, targetUserId);

            tvTitle.setText(partnerName);

            String firstLetter = partnerName.isEmpty() ? "?" : partnerName.substring(0, 1).toUpperCase();
            tvAvatar.setText(firstLetter);

            if (tvHouseTitle != null) {
                tvHouseTitle.setVisibility(View.GONE);
            }

            String rawMsg = room.getLastMessage() != null && !room.getLastMessage().isEmpty()
                    ? room.getLastMessage()
                    : "Chưa có tin nhắn";

            boolean isMyMessage = Objects.equals(currentUserId, room.getLastSenderId());
            tvLastMessage.setText(isMyMessage ? "Bạn: " + rawMsg : rawMsg);

            if (room.getLastMessageTime() > 0) {
                tvTime.setText(TimeFormatter.relative(ctx, room.getLastMessageTime()));
            } else {
                tvTime.setText("Vừa xong");
            }

            if (tvUnreadBadge != null) {
                boolean isUnread = room.getLastMessageTime() > 0
                        && room.getLastSenderId() != null
                        && !Objects.equals(currentUserId, room.getLastSenderId());
                if (isUnread) {
                    tvUnreadBadge.setVisibility(View.VISIBLE);
                    tvUnreadBadge.setText("•");
                } else {
                    tvUnreadBadge.setVisibility(View.GONE);
                }
            }

            itemView.setOnClickListener(v -> listener.onChatRoomClick(room));
        }
    }
}