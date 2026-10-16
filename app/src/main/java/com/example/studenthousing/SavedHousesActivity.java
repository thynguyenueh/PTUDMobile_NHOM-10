package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SavedHousesActivity extends AppCompatActivity {

    private LinearLayout layoutSavedHousesContainer;
    private TextView tvTotalSaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_houses);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        layoutSavedHousesContainer = findViewById(R.id.layoutSavedHousesContainer);
        tvTotalSaved = findViewById(R.id.tvTotalSaved);

        View btnGoToSearch = findViewById(R.id.btnGoToSearch);
        if (btnGoToSearch != null) {
            btnGoToSearch.setOnClickListener(v ->
                    startActivity(new Intent(SavedHousesActivity.this, HomeActivity.class)));
        }

        displaySavedHouses();
    }

    private void displaySavedHouses() {
        if (layoutSavedHousesContainer == null) return;
        layoutSavedHousesContainer.removeAllViews();

        tvTotalSaved.setText("2 căn nhà đã lưu");

        addSavedHouseItemView(
                "Căn hộ cao cấp Thảo Điền",
                "5.0 Tr/tháng · 65 m²",
                "Địa chỉ: Phường Thảo Điền, Quận 2, TP. Hồ Chí Minh (1.2 km)"
        );

        addSavedHouseItemView(
                "Nhà nguyên căn Linh Xuân",
                "3.2 Tr/tháng · 45 m²",
                "Địa chỉ: Phường Linh Xuân, TP. Thủ Đức, TP. Hồ Chí Minh (1.8 km)"
        );
    }

    private void addSavedHouseItemView(String title, String priceArea, String address) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_saved_house, layoutSavedHousesContainer, false);

        TextView tvTitle = itemView.findViewById(R.id.tvSavedTitle);
        TextView tvPriceArea = itemView.findViewById(R.id.tvSavedPriceArea);
        TextView tvAddress = itemView.findViewById(R.id.tvSavedAddress);

        View btnViewDetail = itemView.findViewById(R.id.btnViewSavedDetail);
        View btnRemove = itemView.findViewById(R.id.btnRemoveSaved);

        tvTitle.setText(title);
        tvPriceArea.setText(priceArea);
        tvAddress.setText(address);

        if (btnViewDetail != null) {
            btnViewDetail.setOnClickListener(v ->
                    startActivity(new Intent(SavedHousesActivity.this, DetailActivity.class)));
        }

        if (btnRemove != null) {
            btnRemove.setOnClickListener(v -> {
                layoutSavedHousesContainer.removeView(itemView);
                Toast.makeText(SavedHousesActivity.this, "Đã bỏ lưu: " + title, Toast.LENGTH_SHORT).show();
            });
        }

        layoutSavedHousesContainer.addView(itemView);
    }
}
