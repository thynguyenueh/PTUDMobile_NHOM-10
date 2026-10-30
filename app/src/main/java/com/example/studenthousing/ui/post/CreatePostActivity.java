package com.example.studenthousing.ui.post;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.ui.adapter.SelectedPhotoAdapter;
import com.example.studenthousing.ui.viewmodel.CreatePostViewModel;
import com.example.studenthousing.util.AddressFingerprintUtil;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;
import com.example.studenthousing.util.UtilityLoader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class CreatePostActivity extends AppCompatActivity {

    private TextInputLayout tilTitle, tilAdminAddress, tilStreetAddress, tilHouseNumber;
    private EditText etTitle, etAdminAddress, etStreetAddress, etHouseNumber, etDescription, etCustomUtility;
    private MaterialButton btnSubmitPost;
    private RecyclerView rvSelectedPhotos;
    private TextView tvPhotoCount, tvPhotoHint;
    private ProgressBar progressBar;
    private ChipGroup chipGroupUtilities;
    private CreatePostViewModel viewModel;
    private SelectedPhotoAdapter photoAdapter;

    private int selectedCityCode = 79;
    private int selectedWardCode = 27256;
    private String selectedWardName = "";

    private boolean isEditMode = false;
    private String editPostId = null;

    private final List<Uri> selectedImageUris = new ArrayList<>();
    private ActivityResultLauncher<String> multiImagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        viewModel = new ViewModelProvider(this).get(CreatePostViewModel.class);
        viewModel.getSubmitState().observe(this, this::renderSubmitState);

        multiImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        for (Uri uri : uris) {
                            if (uri != null && !selectedImageUris.contains(uri)) {
                                selectedImageUris.add(uri);
                            }
                        }
                        updatePhotoPreview();
                    }
                }
        );

        setupViews();
        applyEditModeExtras();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                checkAndFinish();
            }
        });
    }

    private void setupViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> checkAndFinish());
        }

        tilTitle = findViewById(R.id.tilTitle);
        tilAdminAddress = findViewById(R.id.tilAdminAddress);
        tilStreetAddress = findViewById(R.id.tilStreetAddress);
        tilHouseNumber = findViewById(R.id.tilHouseNumber);

        etTitle = findViewById(R.id.etTitle);
        etAdminAddress = findViewById(R.id.etAdminAddress);
        etStreetAddress = findViewById(R.id.etStreetAddress);
        etHouseNumber = findViewById(R.id.etHouseNumber);

        if (tilAdminAddress != null) {
            tilAdminAddress.setEndIconOnClickListener(v -> openAddressPicker());
        }
        if (etAdminAddress != null) {
            etAdminAddress.setOnClickListener(v -> openAddressPicker());
        }
        TextView tvVerifyAddressPrompt = findViewById(R.id.tvVerifyAddressPrompt);
        if (tvVerifyAddressPrompt != null) {
            String userId = SessionManager.getUid(this);
            com.example.studenthousing.data.local.VerifiedAddressDao dao = new com.example.studenthousing.data.local.VerifiedAddressDao(this);
            List<com.example.studenthousing.data.model.VerifiedAddress> verifiedList = dao.getByOwner(userId);
            boolean hasVerified = false;
            if (verifiedList != null) {
                for (com.example.studenthousing.data.model.VerifiedAddress va : verifiedList) {
                    if (va.getStatus() == null || "VERIFIED".equalsIgnoreCase(va.getStatus())) {
                        hasVerified = true;
                        break;
                    }
                }
            }
            tvVerifyAddressPrompt.setVisibility(hasVerified ? View.GONE : View.VISIBLE);
            tvVerifyAddressPrompt.setOnClickListener(v -> {
                startActivity(new Intent(this, com.example.studenthousing.ui.verify.ResidenceVerificationActivity.class));
            });
        }
        etDescription = findViewById(R.id.etDescription);
        etCustomUtility = findViewById(R.id.etCustomUtility);
        View btnAddCustomUtility = findViewById(R.id.btnAddCustomUtility);
        if (btnAddCustomUtility != null) {
            btnAddCustomUtility.setOnClickListener(v -> addCustomUtilityFromInput());
        }
        btnSubmitPost = findViewById(R.id.btnSubmitPost);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);
        tvPhotoHint = findViewById(R.id.tvPhotoHint);
        rvSelectedPhotos = null;
        progressBar = findViewById(R.id.progressBar);
        chipGroupUtilities = findViewById(R.id.chipGroupUtilities);
        if (chipGroupUtilities != null && chipGroupUtilities.getChildCount() == 0) {
            List<String> defaultUtils = UtilityLoader.loadUtilities(this);
            for (String util : defaultUtils) {
                addUtilityChip(util, false);
            }
        }

        if (rvSelectedPhotos != null) {
            rvSelectedPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            photoAdapter = new SelectedPhotoAdapter(selectedImageUris, position -> {
                if (position >= 0 && position < selectedImageUris.size()) {
                    selectedImageUris.remove(position);
                    updatePhotoPreview();
                }
            });
            rvSelectedPhotos.setAdapter(photoAdapter);
        }

        View btnAddPhoto = findViewById(R.id.btnAddPhoto);
        if (btnAddPhoto != null) {
            btnAddPhoto.setOnClickListener(v -> {
                if (multiImagePickerLauncher != null) {
                    multiImagePickerLauncher.launch("image/*");
                }
            });
        }

        if (btnSubmitPost != null) {
            btnSubmitPost.setOnClickListener(v -> {
                if (isEditMode) updatePost();
                else savePost();
            });
        }
    }

    private void applyEditModeExtras() {
        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
        editPostId = getIntent().getStringExtra("POST_ID");
        if (!isEditMode || editPostId == null) return;

        String title = getIntent().getStringExtra("POST_TITLE");
        String address = getIntent().getStringExtra("POST_ADDRESS");
        String desc = getIntent().getStringExtra("POST_DESC");
        String streetAddress = getIntent().getStringExtra("POST_STREET_NAME");
        String houseNumber = getIntent().getStringExtra("POST_HOUSE_NUMBER");
        String administrativeAddress = getIntent().getStringExtra("POST_ADMIN_ADDRESS");
        ArrayList<String> utilities = getIntent().getStringArrayListExtra("POST_UTILITIES");

        if (etTitle != null && title != null) etTitle.setText(title);
        if (etAdminAddress != null) {
            etAdminAddress.setText(administrativeAddress != null ? administrativeAddress : address);
        }
        if (etHouseNumber != null && houseNumber != null) etHouseNumber.setText(houseNumber);
        if (etStreetAddress != null && streetAddress != null) etStreetAddress.setText(streetAddress);
        if (etDescription != null && desc != null) etDescription.setText(desc);
        selectedCityCode = getIntent().getIntExtra("POST_CITY_CODE", selectedCityCode);
        selectedWardCode = getIntent().getIntExtra("POST_WARD_CODE", selectedWardCode);
        String wardName = getIntent().getStringExtra("POST_WARD_NAME");
        if (wardName != null) selectedWardName = wardName;

        if (chipGroupUtilities != null && utilities != null) {
            for (String util : utilities) {
                addUtilityChip(util, true);
            }
        }
        if (tvPhotoHint != null) tvPhotoHint.setText("Bấm để chọn hoặc chỉnh sửa ảnh");

        if (btnSubmitPost != null) btnSubmitPost.setText("Cập nhật bài đăng");
    }

    private void addCustomUtilityFromInput() {
        if (etCustomUtility == null) return;
        String text = etCustomUtility.getText() != null ? etCustomUtility.getText().toString().trim() : "";
        if (!text.isEmpty()) {
            addUtilityChip(text, true);
            etCustomUtility.setText("");
        }
    }

    private void addUtilityChip(String text, boolean checked) {
        if (chipGroupUtilities == null) return;
        for (int i = 0; i < chipGroupUtilities.getChildCount(); i++) {
            View child = chipGroupUtilities.getChildAt(i);
            if (child instanceof Chip) {
                Chip c = (Chip) child;
                if (c.getText().toString().equalsIgnoreCase(text)) {
                    c.setChecked(checked);
                    return;
                }
            }
        }
        Chip chip = new Chip(new android.view.ContextThemeWrapper(this, R.style.Widget_App_Chip), null, 0);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chipGroupUtilities.addView(chip);
    }

    private void updatePost() {
        if (editPostId == null) return;

        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        String adminAddr = etAdminAddress != null ? etAdminAddress.getText().toString().trim() : "";
        String streetAddr = etStreetAddress != null ? etStreetAddress.getText().toString().trim() : "";
        String houseNum = etHouseNumber != null ? etHouseNumber.getText().toString().trim() : "";
        String desc = etDescription != null ? etDescription.getText().toString().trim() : "";

        if (title.isEmpty()) {
            if (tilTitle != null) tilTitle.setError("Vui lòng nhập tiêu đề bài đăng");
            else Toast.makeText(this, "Vui lòng nhập tiêu đề bài đăng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (adminAddr.isEmpty()) {
            if (tilAdminAddress != null) tilAdminAddress.setError("Vui lòng nhập địa chỉ");
            else Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (houseNum.isEmpty()) houseNum = "1";
        if (streetAddr.isEmpty()) streetAddr = adminAddr;

        List<String> utilities = new ArrayList<>();
        if (chipGroupUtilities != null) {
            for (int i = 0; i < chipGroupUtilities.getChildCount(); i++) {
                View child = chipGroupUtilities.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (chip.isChecked()) utilities.add(chip.getText().toString());
                }
            }
        }

        String fullAddress = adminAddr;
        if (!houseNum.equals("1") && !streetAddr.equals(adminAddr)) {
            fullAddress = houseNum + " " + streetAddr + ", " + adminAddr;
        }
        String fingerprint = AddressFingerprintUtil.build(selectedCityCode, selectedWardCode, streetAddr, houseNum);
        viewModel.updatePost(editPostId, selectedImageUris, title, fullAddress, desc, utilities, selectedCityCode, selectedWardCode, selectedWardName, 2, fingerprint, houseNum, streetAddr);
    }

    private void updatePhotoPreview() {
        if (selectedImageUris.isEmpty()) {
            if (rvSelectedPhotos != null) rvSelectedPhotos.setVisibility(View.GONE);
            if (tvPhotoCount != null) tvPhotoCount.setVisibility(View.GONE);
        } else {
            if (rvSelectedPhotos != null) rvSelectedPhotos.setVisibility(View.VISIBLE);
            if (tvPhotoCount != null) {
                tvPhotoCount.setText(getString(R.string.create_post_photo_count, selectedImageUris.size()));
                tvPhotoCount.setVisibility(View.VISIBLE);
            }
        }
        if (photoAdapter != null) {
            photoAdapter.notifyDataSetChanged();
        }
    }

    private void checkAndFinish() {
        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        if (!title.isEmpty() || !selectedImageUris.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Hủy bài đăng?")
                    .setMessage("Các thông tin bạn vừa nhập sẽ bị mất.")
                    .setPositiveButton("Hủy bài", (dialog, which) -> finish())
                    .setNegativeButton("Tiếp tục điền", null)
                    .show();
        } else {
            finish();
        }
    }

    private void savePost() {
        if (tilTitle != null) tilTitle.setError(null);
        if (tilAdminAddress != null) tilAdminAddress.setError(null);
        if (tilStreetAddress != null) tilStreetAddress.setError(null);
        if (tilHouseNumber != null) tilHouseNumber.setError(null);

        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        String adminAddr = etAdminAddress != null ? etAdminAddress.getText().toString().trim() : "";
        String streetAddr = etStreetAddress != null ? etStreetAddress.getText().toString().trim() : "";
        String houseNum = etHouseNumber != null ? etHouseNumber.getText().toString().trim() : "";
        String desc = etDescription != null ? etDescription.getText().toString().trim() : "";

        if (title.isEmpty()) {
            if (tilTitle != null) tilTitle.setError("Vui lòng nhập tiêu đề bài đăng");
            else Toast.makeText(this, "Vui lòng nhập tiêu đề bài đăng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (adminAddr.isEmpty()) {
            if (tilAdminAddress != null) tilAdminAddress.setError("Vui lòng nhập địa chỉ");
            else Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 hình ảnh chỗ ở", Toast.LENGTH_SHORT).show();
            return;
        }

        if (houseNum.isEmpty()) houseNum = "1";
        if (streetAddr.isEmpty()) streetAddr = adminAddr;

        String fullAddress = adminAddr;
        if (!houseNum.equals("1") && !streetAddr.equals(adminAddr)) {
            fullAddress = houseNum + " " + streetAddr + ", " + adminAddr;
        }

        List<String> utilities = new ArrayList<>();
        if (chipGroupUtilities != null) {
            for (int i = 0; i < chipGroupUtilities.getChildCount(); i++) {
                View child = chipGroupUtilities.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (chip.isChecked()) utilities.add(chip.getText().toString());
                }
            }
        }

        String fingerprint = AddressFingerprintUtil.build(selectedCityCode, selectedWardCode, streetAddr, houseNum);
        final int fCityCode = selectedCityCode;
        final int fWardCode = selectedWardCode;
        final String finalTitle = title;
        final String finalFullAddress = fullAddress;
        final String finalDesc = desc;
        final List<String> finalUtilities = utilities;
        final String finalFingerprint = fingerprint;
        final String finalHouseNum = houseNum;
        final String finalStreetName = streetAddr;

        Runnable submitAction = () -> viewModel.submitPost(selectedImageUris, finalTitle, finalFullAddress, finalDesc, finalUtilities, fCityCode, fWardCode, selectedWardName, 2, finalFingerprint, finalHouseNum, finalStreetName);

        viewModel.checkFirestoreDuplicate(finalFingerprint, new AppCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isDuplicate) {
                if (Boolean.TRUE.equals(isDuplicate)) {
                    runOnUiThread(() -> new AlertDialog.Builder(CreatePostActivity.this)
                            .setTitle("Cảnh báo trùng lặp địa chỉ")
                            .setMessage("Địa chỉ này trùng khớp với một bài đăng khác đã tồn tại trên hệ thống. Bạn có chắc chắn muốn tiếp tục đăng bài?")
                            .setPositiveButton("Vẫn đăng", (dialog, which) -> submitAction.run())
                            .setNegativeButton("Hủy", null)
                            .show());
                } else {
                    submitAction.run();
                }
            }

            @Override
            public void onError(Exception e) {
                submitAction.run();
            }
        });
    }

    private void openAddressPicker() {
        String userId = SessionManager.getUid(this);
        com.example.studenthousing.data.local.VerifiedAddressDao dao = new com.example.studenthousing.data.local.VerifiedAddressDao(this);
        List<com.example.studenthousing.data.model.VerifiedAddress> verifiedList = dao.getByOwner(userId);
        List<com.example.studenthousing.data.model.VerifiedAddress> validAddresses = new ArrayList<>();
        if (verifiedList != null) {
            for (com.example.studenthousing.data.model.VerifiedAddress va : verifiedList) {
                if (va.getFullAddress() != null && !va.getFullAddress().isEmpty()) {
                    if (va.getStatus() == null || "VERIFIED".equalsIgnoreCase(va.getStatus())) {
                        validAddresses.add(va);
                    }
                }
            }
        }

        if (validAddresses.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Chưa có địa chỉ đã xác thực")
                    .setMessage("Bạn cần xác thực địa chỉ cư trú trước khi tạo bài đăng mới.")
                    .setPositiveButton("Xác thực ngay", (dialog, which) -> {
                        startActivity(new Intent(this, com.example.studenthousing.ui.verify.ResidenceVerificationActivity.class));
                    })
                    .setNegativeButton("Đóng", null)
                    .show();
            return;
        }

        String[] addressTitles = new String[validAddresses.size()];
        for (int i = 0; i < validAddresses.size(); i++) {
            addressTitles[i] = validAddresses.get(i).getFullAddress();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn địa chỉ đã xác thực")
                .setItems(addressTitles, (dialog, which) -> {
                    com.example.studenthousing.data.model.VerifiedAddress selected = validAddresses.get(which);
                    selectedCityCode = selected.getCityCode();
                    selectedWardCode = selected.getWardCode();
                    selectedWardName = "";
                    if (etAdminAddress != null) {
                        etAdminAddress.setText(selected.getFullAddress());
                    }
                    if (etHouseNumber != null && selected.getHouseNumberNorm() != null) {
                        etHouseNumber.setText(selected.getHouseNumberNorm());
                    }
                    if (etStreetAddress != null && selected.getStreetNormalized() != null) {
                        etStreetAddress.setText(selected.getStreetNormalized());
                    }
                    if (tilAdminAddress != null) {
                        tilAdminAddress.setError(null);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void renderSubmitState(UiState<String> state) {
        if (state == null) return;

        if (state.getScreenState() == UiState.ScreenState.LOADING) {
            if (btnSubmitPost != null) {
                btnSubmitPost.setEnabled(false);
                btnSubmitPost.setText("Đang xử lý...");
            }
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            return;
        }

        if (state.getScreenState() == UiState.ScreenState.ERROR) {
            resetButton();
            Toast.makeText(this, state.getErrorMessage() != null ? state.getErrorMessage() : "Lỗi xử lý", Toast.LENGTH_SHORT).show();
            return;
        }

        if (state.getContent() != null) {
            Toast.makeText(this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            resetButton();
        }
    }

    private void resetButton() {
        if (btnSubmitPost != null) {
            btnSubmitPost.setEnabled(true);
            btnSubmitPost.setText("Đăng bài ngay");
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
}
