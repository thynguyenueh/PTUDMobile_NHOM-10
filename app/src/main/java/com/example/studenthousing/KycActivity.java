package com.example.studenthousing;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class KycActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kyc);

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        if (findViewById(R.id.btnAddParentId) != null) {
            findViewById(R.id.btnAddParentId).setOnClickListener(v ->
                    Toast.makeText(this, "Tải lên CCCD phụ huynh", Toast.LENGTH_SHORT).show());
        }

        if (findViewById(R.id.btnAddStudentId) != null) {
            findViewById(R.id.btnAddStudentId).setOnClickListener(v ->
                    Toast.makeText(this, "Tải lên thẻ sinh viên", Toast.LENGTH_SHORT).show());
        }

        if (findViewById(R.id.btnSubmitKyc) != null) {
            findViewById(R.id.btnSubmitKyc).setOnClickListener(v -> {
                Toast.makeText(this, "Gửi xác minh KYC thành công!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }
}
