package com.example.studenthousing.ui.post;

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
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.UiState;
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
        etDescription = findViewById(R.id.etDescription);
        etCustomUtility = findViewById(R.id.etCustomUtility);
        View btnAddCustomUtility = findViewById(R.id.btnAddCustomUtility);
        if (btnAddCustomUtility != null) {
            btnAddCustomUtility.setOnClickListener(v -> addCustomUtilityFromInput());
        }
        btnSubmitPost = findViewById(R.id.btnSubmitPost);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);
        tvPhotoHint = findViewById(R.id.tvPhotoHint);
        rvSelectedPhotos = findViewById(R.id.rvSelectedPhotos);
        progressBar = findViewById(R.id.progressBar);
        chipGroupUtilities = findViewById(R.id.chipGroupUtilities);
        setupUtilitiesFromAsset();

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
        ArrayList<String> images = getIntent().getStringArrayListExtra("POST_IMAGES");

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

        if (images != null) {
            for (String img : images) {
                if (img != null && !img.trim().isEmpty()) {
                    Uri uri = Uri.parse(img);
                    if (!selectedImageUris.contains(uri)) {
                        selectedImageUris.add(uri);
                    }
                }
            }
            updatePhotoPreview();
        }

        if (chipGroupUtilities != null) {
            setupUtilitiesFromAsset();
            if (utilities != null) {
                for (String util : utilities) {
                    boolean found = false;
                    for (int i = 0; i < chipGroupUtilities.getChildCount(); i++) {
                        View child = chipGroupUtilities.getChildAt(i);
                        if (child instanceof Chip) {
                            Chip c = (Chip) child;
                            if (c.getText().toString().trim().equalsIgnoreCase(util.trim())) {
                                c.setChecked(true);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        addUtilityChip(util, true, "ic_check");
                    }
                }
            }
        }
        if (tvPhotoHint != null) tvPhotoHint.setText("Bấm để chọn hoặc chỉnh sửa ảnh");

        if (btnSubmitPost != null) btnSubmitPost.setText("Cập nhật bài đăng");
    }

    private void addCustomUtilityFromInput() {
        if (etCustomUtility == null) return;
        String text = etCustomUtility.getText() != null ? etCustomUtility.getText().toString().trim() : "";
        if (!text.isEmpty()) {
            addUtilityChip(text, true, "ic_check");
            etCustomUtility.setText("");
        }
    }

    private void setupUtilitiesFromAsset() {
        if (chipGroupUtilities == null) return;
        chipGroupUtilities.removeAllViews();
        List<com.example.studenthousing.data.model.Utility> utilities =
                com.example.studenthousing.data.loader.UtilitiesDataLoader.loadUtilities(this);
        for (com.example.studenthousing.data.model.Utility u : utilities) {
            addUtilityChip(u.getLabel(), false, u.getIcon());
        }
    }

    private int getIconResId(String iconName) {
        if (iconName == null || iconName.trim().isEmpty()) {
            return R.drawable.ic_check;
        }
        switch (iconName.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "ic_wifi": return R.drawable.ic_wifi;
            case "ic_parking": return R.drawable.ic_parking_car;
            case "ic_ac": return R.drawable.ic_air_conditioner;
            case "ic_washer": return R.drawable.ic_check;
            case "ic_fridge": return R.drawable.ic_kitchen;
            case "ic_bath": return R.drawable.ic_check;
            case "ic_security": return R.drawable.ic_shield_check;
            case "ic_elevator": return R.drawable.ic_check;
            case "ic_pet": return R.drawable.ic_check;
            default: return R.drawable.ic_check;
        }
    }

    private void addUtilityChip(String text, boolean checked, String iconName) {
        if (chipGroupUtilities == null || text == null || text.trim().isEmpty()) return;
        String trimmedText = text.trim();
        for (int i = 0; i < chipGroupUtilities.getChildCount(); i++) {
            View child = chipGroupUtilities.getChildAt(i);
            if (child instanceof Chip) {
                Chip c = (Chip) child;
                if (c.getText().toString().trim().equalsIgnoreCase(trimmedText)) {
                    if (checked) c.setChecked(true);
                    return;
                }
            }
        }
        Chip chip = new Chip(new android.view.ContextThemeWrapper(this, R.style.Widget_App_Chip), null, 0);
        chip.setText(trimmedText);
        chip.setCheckable(true);
        chip.setChecked(checked);

        int iconRes = getIconResId(iconName);
        chip.setChipIcon(androidx.core.content.ContextCompat.getDrawable(this, iconRes));
        chip.setChipIconVisible(true);

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
            if (tilAdminAddress != null) tilAdminAddress.setError("Vui lòng chọn Phường/Xã");
            else Toast.makeText(this, "Vui lòng chọn Phường/Xã", Toast.LENGTH_SHORT).show();
            return;
        }
        if (houseNum.isEmpty()) {
            if (tilHouseNumber != null) tilHouseNumber.setError("Vui lòng nhập số nhà");
            else Toast.makeText(this, "Vui lòng nhập số nhà", Toast.LENGTH_SHORT).show();
            return;
        }
        if (streetAddr.isEmpty()) {
            if (tilStreetAddress != null) tilStreetAddress.setError("Vui lòng nhập tên đường");
            else Toast.makeText(this, "Vui lòng nhập tên đường", Toast.LENGTH_SHORT).show();
            return;
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

        String fullAddress = houseNum + " " + streetAddr + ", " + adminAddr;
        viewModel.updatePost(editPostId, selectedImageUris, title, fullAddress, desc, utilities, selectedCityCode, selectedWardCode, selectedWardName, houseNum, streetAddr);
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
            if (tilAdminAddress != null)
                tilAdminAddress.setError("Vui lòng chọn Tỉnh/Thành, Phường/Xã");
            else
                Toast.makeText(this, "Vui lòng chọn Tỉnh/Thành, Phường/Xã", Toast.LENGTH_SHORT).show();
            return;
        }
        if (houseNum.isEmpty()) {
            if (tilHouseNumber != null) tilHouseNumber.setError("Vui lòng nhập số nhà");
            else Toast.makeText(this, "Vui lòng nhập số nhà", Toast.LENGTH_SHORT).show();
            return;
        }
        if (streetAddr.isEmpty()) {
            if (tilStreetAddress != null) tilStreetAddress.setError("Vui lòng nhập tên đường");
            else Toast.makeText(this, "Vui lòng nhập tên đường", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 hình ảnh chỗ ở", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullAddress = houseNum + " " + streetAddr + ", " + adminAddr;

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

        final int fCityCode = selectedCityCode;
        final int fWardCode = selectedWardCode;
        final String finalTitle = title;
        final String finalFullAddress = fullAddress;
        final String finalDesc = desc;
        final List<String> finalUtilities = utilities;
        final String finalHouseNum = houseNum;
        final String finalStreetName = streetAddr;
    }

    private void openAddressPicker() {
        com.example.studenthousing.util.ProvincePickerHelper.showPicker(this, (cityName, ward) -> {
            if (cityName != null) {
                selectedCityCode = 79;
                selectedWardCode = ward != null ? ward.getCode() : 0;
                selectedWardName = ward != null ? ward.getName() : "";

                String wardName = selectedWardName;
                String provName = cityName;

                StringBuilder sb = new StringBuilder();
                if (!wardName.isEmpty()) sb.append(wardName).append(", ");
                sb.append(provName);

                if (etAdminAddress != null) {
                    etAdminAddress.setText(sb.toString());
                }
                if (tilAdminAddress != null) {
                    tilAdminAddress.setError(null);
                }
            }
        });
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
