package com.example.studenthousing.ui.profile;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.model.AccommodationPost;

import java.util.List;
import java.util.Locale;

public final class ProfileHeader {

    private ProfileHeader() {}

    public static void bind(View root, String name, String subtitle, String avatarUrl,
                            Integer posts, Integer reviews, Integer exchanges) {
        bind(root, name, subtitle, avatarUrl, false, posts, reviews, exchanges);
    }

    public static void bind(View root, String name, String subtitle, String avatarUrl, boolean isVerified,
                            Integer posts, Integer reviews, Integer exchanges) {
        ImageView ivAvatarImg = root.findViewById(R.id.ivProfileAvatarImg);
        TextView tvAvatar = root.findViewById(R.id.tvProfileAvatar);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (ivAvatarImg != null) {
                ivAvatarImg.setVisibility(View.VISIBLE);
                Glide.with(root.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .into(ivAvatarImg);
            }
            if (tvAvatar != null) tvAvatar.setVisibility(View.GONE);
        } else {
            if (ivAvatarImg != null) ivAvatarImg.setVisibility(View.GONE);
            if (tvAvatar != null) {
                tvAvatar.setVisibility(View.VISIBLE);
                tvAvatar.setText(initialOf(name));
            }
        }

        View badge = root.findViewById(R.id.tvProfileVerifiedBadge);
        if (badge != null) {
            badge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        }

        ((TextView) root.findViewById(R.id.tvProfileName))
                .setText(name != null && !name.trim().isEmpty() ? name : "Người dùng");

        TextView sub = root.findViewById(R.id.tvProfileSub);
        sub.setText(subtitle != null ? subtitle : "");
        sub.setVisibility(subtitle == null || subtitle.trim().isEmpty() ? View.GONE : View.VISIBLE);

        setStat(root, R.id.tvStatPosts, posts);
        setStat(root, R.id.tvStatReviews, reviews);
        setStat(root, R.id.tvStatExchanges, exchanges);
    }

    public static int countPostsOf(List<AccommodationPost> posts, String userId) {
        if (posts == null || userId == null) return 0;
        int n = 0;
        for (AccommodationPost p : posts) {
            if (userId.equals(p.getUserId())) n++;
        }
        return n;
    }

    private static void setStat(View root, int id, Integer value) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(value == null ? "–" : String.valueOf(value));
    }

    private static String initialOf(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] words = name.trim().split("\\s+");
        String last = words[words.length - 1];
        return last.substring(0, 1).toUpperCase(new Locale("vi"));
    }
}
