package com.example.studenthousing.ui.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.domain.FilterCriteria;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.UiState;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchViewModel extends AndroidViewModel {

    private static final String PREFS = "search_prefs";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY = 8;

    public enum SearchMode { IDLE, RESULTS, EMPTY }

    public static class SearchResultData {
        public final SearchMode mode;
        public final List<AccommodationPost> results;
        public final String emptyMessage;

        public SearchResultData(SearchMode mode, List<AccommodationPost> results, String emptyMessage) {
            this.mode = mode;
            this.results = results;
            this.emptyMessage = emptyMessage;
        }
    }

    private final PostRepository postRepository;
    private final SharedPreferences prefs;

    private final MutableLiveData<UiState<SearchResultData>> searchState = new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<List<String>> historyState = new MutableLiveData<>();

    private List<AccommodationPost> allPosts = new ArrayList<>();
    private boolean loaded = false;

    public SearchViewModel(@NonNull Application application) {
        super(application);
        postRepository = new PostRepository(application);
        prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        loadHistory();
    }

    public LiveData<UiState<SearchResultData>> getSearchState() {
        return searchState;
    }

    public LiveData<List<String>> getHistoryState() {
        return historyState;
    }

    public void loadPosts() {
        searchState.setValue(UiState.loading());
        postRepository.getLatestPosts(200, new AppCallback<List<AccommodationPost>>() {
            @Override
            public void onSuccess(List<AccommodationPost> posts) {
                allPosts = posts != null ? posts : new ArrayList<>();
                loaded = true;
                searchState.postValue(UiState.content(new SearchResultData(SearchMode.IDLE, new ArrayList<>(), null), UiState.SyncState.SYNCED));
            }

            @Override
            public void onError(Exception e) {
                searchState.postValue(UiState.error("Không tải được dữ liệu"));
            }
        });
    }

    public void runSearch(String query, FilterCriteria filter) {
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasFilter = filter != null && filter.isActive();

        if (!hasQuery && !hasFilter) {
            searchState.setValue(UiState.content(new SearchResultData(SearchMode.IDLE, new ArrayList<>(), null), UiState.SyncState.SYNCED));
            return;
        }

        if (!loaded) return;

        String q = query != null ? query : "";
        String[] tokens = normalize(q).split(" ");
        List<AccommodationPost> results = new ArrayList<>();

        for (AccommodationPost p : allPosts) {
            if (matchesQuery(p, tokens, hasQuery) && (filter == null || filter.matches(p))) {
                results.add(p);
            }
        }

        if (results.isEmpty()) {
            String msg = hasQuery ? "Không tìm thấy nhà cho \"" + q.trim() + "\"" : "Không có nhà phù hợp bộ lọc";
            searchState.setValue(UiState.content(new SearchResultData(SearchMode.EMPTY, results, msg), UiState.SyncState.SYNCED));
        } else {
            searchState.setValue(UiState.content(new SearchResultData(SearchMode.RESULTS, results, null), UiState.SyncState.SYNCED));
        }
    }

    public void saveHistory(String q) {
        if (q == null || q.trim().isEmpty()) return;
        String queryStr = q.trim();

        List<String> list = readHistoryList();
        list.removeIf(s -> s.equalsIgnoreCase(queryStr));
        list.add(0, queryStr);
        while (list.size() > MAX_HISTORY) list.remove(list.size() - 1);

        prefs.edit().putString(KEY_HISTORY, String.join("\n", list)).apply();
        historyState.setValue(list);
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
        historyState.setValue(new ArrayList<>());
    }

    private void loadHistory() {
        historyState.setValue(readHistoryList());
    }

    private List<String> readHistoryList() {
        List<String> list = new ArrayList<>();
        String raw = prefs.getString(KEY_HISTORY, "");
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split("\n")) {
                if (!s.trim().isEmpty()) list.add(s);
            }
        }
        return list;
    }

    private static boolean matchesQuery(AccommodationPost p, String[] tokens, boolean hasQuery) {
        if (!hasQuery) return true;
        String hay = normalize(p.getTitle() + " " + p.getAddress() + " " + p.getDescription());
        for (String t : tokens) {
            if (!t.isEmpty() && !hay.contains(t)) return false;
        }
        return true;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.replace('đ', 'd').replace('Đ', 'D');
        return n.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
