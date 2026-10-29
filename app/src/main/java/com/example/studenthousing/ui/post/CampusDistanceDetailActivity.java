package com.example.studenthousing.ui.post;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.NearbyCampus;
import com.example.studenthousing.ui.adapter.CampusDistanceAdapter;

import java.io.Serializable;
import java.util.ArrayList;

public class CampusDistanceDetailActivity extends AppCompatActivity {

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_distance_detail);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Serializable s = getIntent().getSerializableExtra("NEARBY_CAMPUSES");
        ArrayList<NearbyCampus> list = null;
        if (s instanceof ArrayList) {
            list = (ArrayList<NearbyCampus>) s;
        }
        if (list == null) list = new ArrayList<>();

        list.sort((a, b) -> Double.compare(a.getKm(), b.getKm()));

        RecyclerView rv = findViewById(R.id.rvCampusDistances);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new CampusDistanceAdapter(this, list));
        }
    }
}