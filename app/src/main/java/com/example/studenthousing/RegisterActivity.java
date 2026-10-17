package com.example.studenthousing;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private ImageView btnBack, btnTogglePassword;
    private EditText etParentName, etPhone, etPassword, etConfirmPassword;
    private LinearLayout btnSelectUniversity;
    private TextView tvUniversity, tvLoginLink, tvHeaderTitle;
    private MaterialCheckBox cbTerms;
    private AppCompatButton btnContinue;
    private boolean isPasswordVisible = false;
    private boolean isEditMode = false;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        session = new SessionManager(this);
        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        initViews();
        setupMode();
        setupEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        etParentName = findViewById(R.id.etParentName);
        etPhone = findViewById(R.id.etPhone);
        btnSelectUniversity = findViewById(R.id.btnSelectUniversity);
        tvUniversity = findViewById(R.id.tvUniversity);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnContinue = findViewById(R.id.btnContinue);
        tvLoginLink = findViewById(R.id.tvLoginLink);
    }

    private void setupMode() {
        if (isEditMode) {
            if (tvHeaderTitle != null) {
                tvHeaderTitle.setText("Chỉnh sửa thông tin");
            }
            if (btnContinue != null) {
                btnContinue.setText("Lưu thay đổi");
            }
            if (tvLoginLink != null) {
                tvLoginLink.setVisibility(View.GONE);
            }
            if (cbTerms != null) {
                cbTerms.setChecked(true);
            }

            // Prefill current user data
            if (session.getUserPhone() != null) {
                etPhone.setText(session.getUserPhone());
            }
            if (session.getUserFamilyName() != null) {
                etParentName.setText(session.getUserFamilyName());
            }
        }
    }

    private void setupEvents() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSelectUniversity != null) {
            btnSelectUniversity.setOnClickListener(v -> showUniversitySearchDialog());
        }

        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        }

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> handleContinueOrSave());
        }

        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> finish());
        }
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.getText().length());
    }

    private void showUniversitySearchDialog() {
        String[] universities = {
                "Đại học Bách Khoa Hà Nội (HUST)",
                "Đại học Quốc gia Hà Nội (VNU-HN)",
                "Đại học Kinh tế Quốc dân (NEU)",
                "Đại học Ngoại thương (FTU)",
                "Đại học Y Hà Nội (HMU)",
                "Học viện Công nghệ Bưu chính Viễn thông (PTIT)",
                "Đại học Quốc gia Thành phố Hồ Chí Minh (VNU-HCM)",
                "Trường Đại học Bách Khoa - ĐHQG TP.HCM (HCMUT)",
                "Trường Đại học Kinh tế TP. Hồ Chí Minh (UEH)",
                "Trường Đại học Khoa học Tự nhiên - ĐHQG TP.HCM (HCMUS)",
                "Trường Đại học Khoa học Xã hội và Nhân văn - ĐHQG TP.HCM (USSH)",
                "Trường Đại học Công nghệ Thông tin - ĐHQG TP.HCM (UIT)",
                "Trường Đại học Quốc tế - ĐHQG TP.HCM (IU)",
                "Trường Đại học Ngoại thương Cơ sở II (FTU CS2)",
                "Trường Đại học Sư phạm TP.HCM (HCMUE)",
                "Trường Đại học Y Dược TP.HCM (UMP)",
                "Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE)",
                "Trường Đại học Tôn Đức Thắng (TDTU)",
                "Trường Đại học Công nghiệp TP.HCM (IUH)",
                "Trường Đại học Ngân hàng TP.HCM (HUB)",
                "Trường Đại học Luật TP.HCM (ULH)",
                "Trường Đại học Cần Thơ (CTU)",
                "Đại học Đà Nẵng (UD)",
                "Đại học Huế (HueU)",
                "Trường Đại học Nha Trang (NTU)",
                "Trường Đại học Mở TP.HCM (HCMOU)",
                "Trường Đại học Kinh tế - Luật ĐHQG TP.HCM (UEL)",
                "Trường Đại học FPT",
                "Trường Đại học RMIT Việt Nam",
                "Trường Đại học Văn Lang",
                "Trường Đại học Hoa Sen",
                "Trường Đại học Kiến trúc TP.HCM"
        };

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_university, null);
        EditText etSearchUniversity = dialogView.findViewById(R.id.etSearchUniversity);
        ListView lvUniversities = dialogView.findViewById(R.id.lvUniversities);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>(Arrays.asList(universities))) {
            private final List<String> originalList = new ArrayList<>(Arrays.asList(universities));
            private final List<String> filteredList = new ArrayList<>(Arrays.asList(universities));

            @Override
            public int getCount() {
                return filteredList.size();
            }

            @Override
            public String getItem(int position) {
                return filteredList.get(position);
            }

            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        List<String> list = new ArrayList<>();
                        if (constraint == null || constraint.length() == 0) {
                            list.addAll(originalList);
                        } else {
                            String filterPattern = removeAccent(constraint.toString().trim());
                            for (String item : originalList) {
                                if (removeAccent(item).contains(filterPattern)) {
                                    list.add(item);
                                }
                            }
                        }
                        results.values = list;
                        results.count = list.size();
                        return results;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        filteredList.clear();
                        if (results.values != null) {
                            filteredList.addAll((List<String>) results.values);
                        }
                        notifyDataSetChanged();
                    }
                };
            }
        };

        lvUniversities.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        etSearchUniversity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvUniversities.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUni = adapter.getItem(position);
            if (selectedUni != null) {
                tvUniversity.setText(selectedUni);
                tvUniversity.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private static String removeAccent(String s) {
        if (s == null) return "";
        String temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('Đ', 'd')
                .replace('đ', 'd')
                .toLowerCase();
    }

    private void handleContinueOrSave() {
        String parentName = etParentName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(parentName)) {
            etParentName.setError("Vui lòng nhập họ và tên phụ huynh");
            etParentName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Mật khẩu phải từ 8 ký tự trở lên");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không trùng khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý với Điều khoản dịch vụ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chuyển sang Bước 2: Thông tin Chỗ ở (có hỗ trợ chỉnh sửa thông tin chỗ ở)
        Intent intent = new Intent(RegisterActivity.this, RegisterStep2Activity.class);
        intent.putExtra("IS_EDIT_MODE", isEditMode);
        intent.putExtra("PARENT_NAME", parentName);
        intent.putExtra("PHONE", phone);
        startActivity(intent);
        if (isEditMode) {
            finish();
        }
    }
}
