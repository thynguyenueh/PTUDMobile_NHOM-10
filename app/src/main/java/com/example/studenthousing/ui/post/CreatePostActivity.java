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
import com.example.studenthousing.data.loader.UtilitiesDataLoader;
import com.example.studenthousing.data.local.VerifiedAddressDao;
import com.example.studenthousing.data.model.Utility;
import com.example.studenthousing.data.model.VerifiedAddress;
import com.example.studenthousing.ui.adapter.SelectedPhotoAdapter;
import com.example.studenthousing.ui.viewmodel.CreatePostViewModel;
import com.example.studenthousing.util.SessionManager;
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
    private int selectedWardCode = 27256; // Default fallback, sẽ được ghi đè từ VerifiedAddress
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

        TextView tvVerifyPrompt = findViewById(R.id.tvVerifyAddressPrompt);
        if (tvVerifyPrompt != null) {
            tvVerifyPrompt.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, com.example.studenthousing.ui.verify.ResidenceVerificationActivity.class);
                startActivity(intent);
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
            setupUtilitiesFromAsset(); // Reset lại list ban đầu
            if (utilities != null) {
                for (String utilCode : utilities) { // utilCode lấy từ DB truyền sang
                    boolean found = false;
                    for (int i = 0; i < chipGroupUtilities.getChildCount(); i++) {
                        View child = chipGroupUtilities.getChildAt(i);
                        if (child instanceof Chip) {
                            Chip c = (Chip) child;
                            String chipCode = c.getTag() != null ? c.getTag().toString() : c.getText().toString();
                            if (chipCode.equalsIgnoreCase(utilCode.trim())) {
                                c.setChecked(true);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        // Nếu là custom utility không có trong assets
                        Utility u = UtilitiesDataLoader.getByCode(this, utilCode);
                        String displayLabel = (u != null) ? u.getLabel() : utilCode;
                        addUtilityChip(displayLabel, utilCode, true, "ic_check");
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
            addUtilityChip(text, text, true, "ic_check"); // code = text đối với custom
            etCustomUtility.setText("");
        }
    }

    private void setupUtilitiesFromAsset() {
        if (chipGroupUtilities == null) return;
        chipGroupUtilities.removeAllViews();
        List<Utility> utilities = UtilitiesDataLoader.loadUtilities(this);
        for (Utility u : utilities) {
            addUtilityChip(u.getLabel(), u.getCode(), false, u.getIcon());
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
            case "ic_washer": return R.drawable.ic_washer;
            case "ic_fridge": return R.drawable.ic_kitchen;
            case "ic_bath": return R.drawable.ic_bath;
            case "ic_security": return R.drawable.ic_shield_check;
            case "ic_elevator": return R.drawable.ic_elevator;
            case "ic_pet": return R.drawable.ic_pet;
            default: return R.drawable.ic_check;
        }
    }

    private void addUtilityChip(String text, String code, boolean checked, String iconName) {
        if (chipGroupUtilities == null || text == null || text.trim().isEmpty()) return;
        String trimmedText = text.trim();

        // Nếu code bị null hoặc rỗng (custom utility do user tự nhập), lấy text làm code luôn
        String finalCode = (code == null || code.trim().isEmpty()) ? trimmedText : code.trim();

        // 1. Kiểm tra xem chip đã tồn tại chưa (So sánh dựa trên Code trong Tag)
        for (int i = 0; i < chipGroupUtilities.getChildCount(); i++) {
            View child = chipGroupUtilities.getChildAt(i);
            if (child instanceof Chip) {
                Chip c = (Chip) child;
                String chipCode = c.getTag() != null ? c.getTag().toString() : c.getText().toString();
                if (chipCode.equalsIgnoreCase(finalCode)) {
                    if (checked) c.setChecked(true);
                    return; // Nếu đã có thì dừng lại
                }
            }
        }

        // 2. Tạo chip mới nếu chưa có
        Chip chip = new Chip(new android.view.ContextThemeWrapper(this, R.style.Widget_App_Chip), null, 0);

        // setText: Hiển thị Label tiếng Việt (VD: "Máy lạnh")
        chip.setText(trimmedText);

        // setTag: Ẩn Code bên trong (VD: "AIR_CONDITIONER") để tí nữa lưu vào DB
        chip.setTag(finalCode);

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
        String fullAddress = etAdminAddress != null ? etAdminAddress.getText().toString().trim() : "";
        String desc = etDescription != null ? etDescription.getText().toString().trim() : "";

        if (title.isEmpty()) {
            if (tilTitle != null) tilTitle.setError("Vui lòng nhập tiêu đề bài đăng");
            else Toast.makeText(this, "Vui lòng nhập tiêu đề bài đăng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fullAddress.isEmpty()) {
            if (tilAdminAddress != null) tilAdminAddress.setError("Vui lòng chọn địa chỉ đã xác thực");
            else Toast.makeText(this, "Vui lòng chọn địa chỉ đã xác thực", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> utilities = new ArrayList<>();
        if (chipGroupUtilities != null) {
            for (int i = 0; i < chipGroupUtilities.getChildCount(); i++) {
                View child = chipGroupUtilities.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (chip.isChecked()) {
                        String code = chip.getTag() != null ? chip.getTag().toString() : chip.getText().toString();
                        utilities.add(code);
                    }
                }
            }
        }

        viewModel.updatePost(editPostId, selectedImageUris, title, fullAddress, desc, utilities, selectedCityCode, selectedWardCode, selectedWardName, "", "");
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

        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        String fullAddress = etAdminAddress != null ? etAdminAddress.getText().toString().trim() : "";
        String desc = etDescription != null ? etDescription.getText().toString().trim() : "";

        if (title.isEmpty()) {
            if (tilTitle != null) tilTitle.setError("Vui lòng nhập tiêu đề bài đăng");
            else Toast.makeText(this, "Vui lòng nhập tiêu đề bài đăng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fullAddress.isEmpty()) {
            if (tilAdminAddress != null)
                tilAdminAddress.setError("Vui lòng chọn địa chỉ đã xác thực");
            else
                Toast.makeText(this, "Vui lòng chọn địa chỉ đã xác thực", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra xem địa chỉ này có thực sự nằm trong danh sách đã xác thực của user hay không
        SessionManager session = new SessionManager(this);
        VerifiedAddressDao dao = new VerifiedAddressDao(this);
        VerifiedAddress va = dao.getByAddressAndUser(session.getUserId(), fullAddress);
        if (va == null) {
            if (tilAdminAddress != null) {
                tilAdminAddress.setError("Địa chỉ này chưa được xác thực cư trú");
            }
            Toast.makeText(this, "Vui lòng chọn địa chỉ đã xác thực hoặc gửi xác thực trước", Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 hình ảnh chỗ ở", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> utilities = new ArrayList<>();
        if (chipGroupUtilities != null) {
            for (int i = 0; i < chipGroupUtilities.getChildCount(); i++) {
                View child = chipGroupUtilities.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (chip.isChecked()) {
                        String code = chip.getTag() != null ? chip.getTag().toString() : chip.getText().toString();
                        utilities.add(code);
                    }
                }
            }
        }

        final int fCityCode = selectedCityCode;
        final int fWardCode = selectedWardCode;
        final String finalTitle = title;
        final String finalFullAddress = fullAddress;
        final String finalDesc = desc;
        final List<String> finalUtilities = utilities;
        final String finalHouseNum = "";
        final String finalStreetName = "";

        viewModel.createPost(
                selectedImageUris,
                finalTitle,
                finalFullAddress,
                finalDesc,
                finalUtilities,
                fCityCode,
                fWardCode,
                selectedWardName,
                finalHouseNum,
                finalStreetName
        );
    }

    private void openAddressPicker() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        VerifiedAddressDao dao = new VerifiedAddressDao(this);
        List<VerifiedAddress> verifiedAddresses = dao.getByUserId(userId);

        if (verifiedAddresses == null || verifiedAddresses.isEmpty()) {
            TextView tvVerifyPrompt = findViewById(R.id.tvVerifyAddressPrompt);
            if (tvVerifyPrompt != null) tvVerifyPrompt.setVisibility(View.VISIBLE);

            new AlertDialog.Builder(this)
                    .setTitle("Chưa có địa chỉ xác thực")
                    .setMessage("Để đảm bảo uy tín và an toàn, bài đăng phải có địa chỉ trùng khớp với Giấy xác thực cư trú (CT07). Bạn vui lòng xác thực địa chỉ trước khi đăng bài.")
                    .setPositiveButton("Xác thực ngay", (dialog, which) -> {
                        android.content.Intent intent = new android.content.Intent(this, com.example.studenthousing.ui.verify.ResidenceVerificationActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
            return;
        }

        String[] addresses = new String[verifiedAddresses.size()];
        for (int i = 0; i < verifiedAddresses.size(); i++) {
            addresses[i] = verifiedAddresses.get(i).getFullAddress();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn địa chỉ đã xác thực")
                .setItems(addresses, (dialog, which) -> {
                    VerifiedAddress selected = verifiedAddresses.get(which);
                    if (etAdminAddress != null) {
                        etAdminAddress.setText(selected.getFullAddress());
                    }
                    if (tilAdminAddress != null) {
                        tilAdminAddress.setError(null);
                    }
                    TextView tvVerifyPrompt = findViewById(R.id.tvVerifyAddressPrompt);
                    if (tvVerifyPrompt != null) tvVerifyPrompt.setVisibility(View.GONE);
                })
                .setPositiveButton("Thêm địa chỉ mới", (dialog, which) -> {
                    android.content.Intent intent = new android.content.Intent(this, com.example.studenthousing.ui.verify.ResidenceVerificationActivity.class);
                    startActivity(intent);
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
            Toast.makeText(this, isEditMode ? "Cập nhật bài đăng thành công!" : "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            resetButton();
        }
    }

    private void resetButton() {
        if (btnSubmitPost != null) {
            btnSubmitPost.setEnabled(true);
            btnSubmitPost.setText(isEditMode ? "Cập nhật bài đăng" : "Đăng bài ngay");
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
}