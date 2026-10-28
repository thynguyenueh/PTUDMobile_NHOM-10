package com.example.studenthousing.ui.dialog;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.example.studenthousing.R;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class SearchListDialog {

    public interface OnPick {
        void onPick(String value);
    }

    private SearchListDialog() {}

    public static void show(Context context, String title, String[] items, OnPick callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_search_university, null);
        EditText etSearch = view.findViewById(R.id.etSearchUniversity);
        ListView list = view.findViewById(R.id.lvUniversities);
        etSearch.setHint("Tìm kiếm...");

        List<String> shown = new ArrayList<>(Arrays.asList(items));
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, shown);
        list.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view)
                .create();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = normalize(s.toString());
                shown.clear();
                for (String item : items) {
                    if (q.isEmpty() || normalize(item).contains(q)) shown.add(item);
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        list.setOnItemClickListener((parent, v, position, id) -> {
            callback.onPick(shown.get(position));
            dialog.dismiss();
        });

        dialog.show();
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
