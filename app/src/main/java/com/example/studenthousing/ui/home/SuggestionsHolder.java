package com.example.studenthousing.ui.home;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.MatchSuggestion;

import java.util.ArrayList;
import java.util.List;

/**
 * Lưu danh sách gợi ý đổi nhà bền vững qua SharedPreferences.
 *
 * Trước đây danh sách chỉ nằm trong biến static: khi hệ thống thu hồi tiến trình
 * (process death) hoặc người dùng xoay màn hình, dữ liệu sẽ mất và
 * SuggestionsActivity hiển thị rỗng. Ở đây chỉ lưu khoá (postId của tôi,
 * postId đối tác, điểm tương thích) rồi đọc lại đối tượng đầy đủ từ PostDao.
 */
public class SuggestionsHolder {

    private static final String PREFS = "suggestions_prefs";
    private static final String KEY_ENTRIES = "entries";
    private static final String SEP = "|";

    private SuggestionsHolder() {
    }

    public static void setSuggestions(Context ctx, List<MatchSuggestion> list) {
        if (ctx == null) return;
        StringBuilder sb = new StringBuilder();
        if (list != null) {
            for (MatchSuggestion s : list) {
                if (s == null || s.getMine() == null || s.getPartner() == null) continue;
                String mineId = s.getMine().getPostId();
                String partnerId = s.getPartner().getPostId();
                if (mineId == null || partnerId == null) continue;
                if (sb.length() > 0) sb.append('\n');
                sb.append(mineId).append(SEP).append(partnerId);
            }
        }
        prefs(ctx).edit().putString(KEY_ENTRIES, sb.toString()).apply();
    }

    public static List<MatchSuggestion> getSuggestions(Context ctx) {
        List<MatchSuggestion> result = new ArrayList<>();
        if (ctx == null) return result;

        String raw = prefs(ctx).getString(KEY_ENTRIES, "");
        if (raw == null || raw.trim().isEmpty()) return result;

        PostDao dao = new PostDao(ctx);
        for (String line : raw.split("\n")) {
            if (line == null || line.trim().isEmpty()) continue;
            String[] parts = line.split("\\" + SEP);
            if (parts.length < 2) continue;
            AccommodationPost mine = dao.getById(parts[0]);
            AccommodationPost partner = dao.getById(parts[1]);
            if (mine == null || partner == null) continue;
            result.add(new MatchSuggestion(mine, partner));
        }
        return result;
    }

    public static void clear(Context ctx) {
        if (ctx == null) return;
        prefs(ctx).edit().remove(KEY_ENTRIES).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
