package com.example.studenthousing.ui.home;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.MatchSuggestion;
import com.example.studenthousing.data.repository.ExchangeRepository;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.ui.adapter.SuggestionAdapter;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SuggestionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recyclerSuggestions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SuggestionAdapter adapter = new SuggestionAdapter(SuggestionsHolder.getSuggestions(this));
        recyclerView.setAdapter(adapter);
        TextView empty = findViewById(R.id.tvSuggestionsEmpty);
        if (empty != null) empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);

        String uid = new SessionManager(this).getUserId();
        if (uid != null && !uid.isEmpty()) refreshSuggestions(uid, adapter, empty);
    }

    private void refreshSuggestions(String uid, SuggestionAdapter adapter, TextView empty) {
        new ExchangeRepository(this).getAllForUser(uid, new AppCallback<List<ExchangeRequest>>() {
            @Override public void onSuccess(List<ExchangeRequest> requests) {
                Set<String> activeIds = new HashSet<>();
                if (requests != null) for (ExchangeRequest req : requests) {
                    String status = req.getStatus();
                    if (Constants.REQ_PENDING.equalsIgnoreCase(status)
                            || Constants.REQ_EXCHANGING.equalsIgnoreCase(status)
                            || Constants.REQ_COMPLETED.equalsIgnoreCase(status)) {
                        activeIds.add(req.getSenderPostId());
                        activeIds.add(req.getReceiverPostId());
                    }
                }
                new PostRepository(SuggestionsActivity.this).getLatestPosts(100, new AppCallback<List<AccommodationPost>>() {
                    @Override public void onSuccess(List<AccommodationPost> posts) {
                        List<MatchSuggestion> current = new ArrayList<>();
                        // Chỉ đọc từ nguồn lưu bền vững một lần để tránh truy vấn DB lặp lại
                        List<MatchSuggestion> stored = SuggestionsHolder.getSuggestions(SuggestionsActivity.this);
                        for (MatchSuggestion suggestion : stored) {
                            AccommodationPost mine = suggestion.getMine();
                            AccommodationPost partner = suggestion.getPartner();
                            if (mine == null || partner == null || !"AVAILABLE".equalsIgnoreCase(mine.getStatus())
                                    || !"AVAILABLE".equalsIgnoreCase(partner.getStatus())
                                    || activeIds.contains(partner.getPostId())) continue;
                            current.add(suggestion);
                        }
                        SuggestionsHolder.setSuggestions(SuggestionsActivity.this, current);
                        adapter.replaceItems(current);
                        if (empty != null) empty.setVisibility(current.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override public void onError(Exception e) {
                        if (empty != null && adapter.getItemCount() == 0) empty.setVisibility(View.VISIBLE);
                    }
                });
            }
            @Override public void onError(Exception e) {
                Toast.makeText(SuggestionsActivity.this, "Không thể làm mới gợi ý lúc này", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
