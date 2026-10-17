package com.example.studenthousing;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.util.AppCallback;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class RegisterStep2Activity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;

    private ImageView btnBack;
    private TextView tvSkipHeader, tvSkipBottom, tvCity, tvWard;
    private LinearLayout btnSelectCity, btnSelectWard, btnAddPhoto, layoutPhotoContainer;
    private EditText etHouseTitle, etStreet, etHouseNumber, etDescription, etCustomAmenity;
    private AppCompatButton btnAddCustomAmenity;
    private ChipGroup chipGroupAmenities;
    private AppCompatButton btnFinishRegister;

    private boolean isEditMode = false;
    private String parentName = "";
    private String phone = "";

    private final String[] amenities = {
            "Wifi tốc độ cao", "Điều hòa", "Máy giặt",
            "Bếp nấu", "Chỗ để xe", "Ban công", "Tủ lạnh"
    };

    private final String[] cities = {
            "Thành phố Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
            "Bình Dương", "Đồng Nai", "Bà Rịa - Vũng Tàu", "Khánh Hòa", "Thừa Thiên Huế",
            "Lâm Đồng", "Đắk Lắk", "Gia Lai", "An Giang", "Kiên Giang",
            "Nghệ An", "Thanh Hóa", "Quảng Ninh", "Bắc Ninh", "Thái Nguyên"
    };

    private final String[] wards = {
            "Phường Thảo Điền", "Phường An Phú", "Phường Bến Nghé", "Phường Bến Thành",
            "Phường Đa Kao", "Phường Tân Phong", "Phường Tân Phú", "Phường Linh Trung",
            "Phường Linh Xuân", "Phường Linh Chiểu", "Phường Dĩ An", "Phường Lái Thiêu",
            "Phường Cầu Ông Lãnh", "Phường Cô Giang", "Phường Phước Long A",
            "Phường Phước Long B", "Phường Hiệp Phú", "Phường Long Thạnh Mỹ"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_step2);

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
        parentName = getIntent().getStringExtra("PARENT_NAME");
        phone = getIntent().getStringExtra("PHONE");

        initViews();
        setupMode();
        setupAmenitiesChips();
        setupEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvSkipHeader = findViewById(R.id.tvSkipHeader);
        tvSkipBottom = findViewById(R.id.tvSkipBottom);
        etHouseTitle = findViewById(R.id.etHouseTitle);
        btnSelectCity = findViewById(R.id.btnSelectCity);
        tvCity = findViewById(R.id.tvCity);
        btnSelectWard = findViewById(R.id.btnSelectWard);
        tvWard = findViewById(R.id.tvWard);
        etStreet = findViewById(R.id.etStreet);
        etHouseNumber = findViewById(R.id.etHouseNumber);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        layoutPhotoContainer = findViewById(R.id.layoutPhotoContainer);
        etCustomAmenity = findViewById(R.id.etCustomAmenity);
        btnAddCustomAmenity = findViewById(R.id.btnAddCustomAmenity);
        chipGroupAmenities = findViewById(R.id.chipGroupAmenities);
        etDescription = findViewById(R.id.etDescription);
        btnFinishRegister = findViewById(R.id.btnFinishRegister);
    }

    private void setupMode() {
        if (isEditMode) {
            if (btnFinishRegister != null) {
                btnFinishRegister.setText("Lưu tất cả thay đổi");
            }
        }
    }

    private void setupAmenitiesChips() {
        for (int i = 0; i < amenities.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(amenities[i]);
            chip.setCheckable(true);
            chip.setClickable(true);
            if (i < 3) {
                chip.setChecked(true);
            }
            chipGroupAmenities.addView(chip);
        }
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());
        tvSkipHeader.setOnClickListener(v -> finishRegistration());
        tvSkipBottom.setOnClickListener(v -> finishRegistration());

        btnSelectCity.setOnClickListener(v -> showSearchDialog("Chọn Thành phố / Tỉnh", cities, tvCity));
        btnSelectWard.setOnClickListener(v -> showSearchDialog("Chọn Phường / Xã", wards, tvWard));

        btnAddPhoto.setOnClickListener(v -> showPhotoPickerDialog());

        btnAddCustomAmenity.setOnClickListener(v -> addCustomAmenity());

        btnFinishRegister.setOnClickListener(v -> finishRegistration());
    }

    private void addCustomAmenity() {
        String customText = etCustomAmenity.getText().toString().trim();
        if (TextUtils.isEmpty(customText)) {
            Toast.makeText(this, "Vui lòng nhập tên tiện ích", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip chip = new Chip(this);
        chip.setText(customText);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setChecked(true);
        chipGroupAmenities.addView(chip);

        etCustomAmenity.setText("");
        Toast.makeText(this, "Đã thêm tiện ích: " + customText, Toast.LENGTH_SHORT).show();
    }

    private void showPhotoPickerDialog() {
        CharSequence[] options = {"Chụp ảnh mới", "Chọn từ thư viện ảnh", "Hủy"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm hình ảnh thực tế");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else if (which == 1) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null && data.getExtras() != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                addPhotoThumbnail(imageBitmap);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    addPhotoThumbnail(bitmap);
                } catch (Exception e) {
                    Toast.makeText(this, "Không thể tải ảnh", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void addPhotoThumbnail(Bitmap bitmap) {
        if (layoutPhotoContainer.getChildCount() >= 5) {
            Toast.makeText(this, "Đã đạt tối đa 5 ảnh thực tế", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageView ivThumbnail = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
        params.setMargins(0, 0, 12, 0);
        ivThumbnail.setLayoutParams(params);
        ivThumbnail.setImageBitmap(bitmap);
        ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivThumbnail.setBackgroundResource(R.drawable.bg_input_field);

        layoutPhotoContainer.addView(ivThumbnail);
        Toast.makeText(this, "Đã thêm ảnh thành công", Toast.LENGTH_SHORT).show();
    }

    private void showSearchDialog(String title, String[] items, TextView targetTextView) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_university, null);
        EditText etSearch = dialogView.findViewById(R.id.etSearchUniversity);
        ListView lvItems = dialogView.findViewById(R.id.lvUniversities);

        etSearch.setHint("Tìm kiếm...");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>(Arrays.asList(items))) {
            private final List<String> originalList = new ArrayList<>(Arrays.asList(items));
            private final List<String> filteredList = new ArrayList<>(Arrays.asList(items));

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

        lvItems.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvItems.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = adapter.getItem(position);
            if (selectedItem != null) {
                targetTextView.setText(selectedItem);
                targetTextView.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private static String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('Đ', 'd')
                .replace('đ', 'd')
                .toLowerCase();
    }

    private void finishRegistration() {
        long now = System.currentTimeMillis();
        User user = new User();
        String finalName = (parentName != null && !parentName.trim().isEmpty()) ? parentName : "Hộ gia đình mới";
        user.setUserId(phone != null && !phone.trim().isEmpty() ? "user_" + phone : "user_reg_" + now);
        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(phone);
        }
        user.setFamilyName(finalName);
        user.setRole("PARENT");
        user.setCreatedAt(now);

        UserRepository userRepository = new UserRepository(this);
        userRepository.saveUserProfile(user, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void d) {
                runOnUiThread(() -> {
                    String msg = isEditMode ? "Cập nhật thông tin & chỗ ở thành công!" : "Đăng ký & Lưu dữ liệu thành công!";
                    Toast.makeText(RegisterStep2Activity.this, msg, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterStep2Activity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterStep2Activity.this, "Đã lưu thông tin thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterStep2Activity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }
}
