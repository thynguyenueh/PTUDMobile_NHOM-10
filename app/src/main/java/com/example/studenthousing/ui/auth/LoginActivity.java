package com.example.studenthousing.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.studenthousing.BuildConfig;
import com.example.studenthousing.R;
import com.example.studenthousing.data.local.DatabaseHelper;
import com.example.studenthousing.ui.home.HomeActivity;
import com.example.studenthousing.ui.viewmodel.AuthViewModel;
import com.example.studenthousing.util.DemoDataSeeder;
import com.example.studenthousing.util.OtpHelper;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_INIT = "app_init";
    private static final String KEY_DEMO_SEEDED = "demo_seeded_v2";
    private static final String WEB_CLIENT_ID =
            "643135707324-45gf2nbr40nhaubqa9a1jm547j19fqtl.apps.googleusercontent.com";

    private TextInputLayout tilPhone;
    private EditText etPhone;
    private MaterialButton btnLogin, btnGoogle;
    private AuthViewModel viewModel;
    private SessionManager session;

    private GoogleSignInClient googleClient;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null) viewModel.firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(this, "Lỗi đăng nhập Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Seed demo CHỈ 1 LẦN, chỉ DEBUG build
        seedDemoDataOnce();

        session = new SessionManager(this);
        if (session.isLoggedIn()) {
            goToHome();
            return;
        }

        // ✅ KHÔNG còn block DemoDataSeeder.seedLocalAndRemote() cũ
        setContentView(R.layout.activity_login);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        viewModel.getGoogleAuthState().observe(this, this::renderGoogleState);

        initViews();
        setupGoogleSignIn();
        setupEvents();
    }

    private void seedDemoDataOnce() {
        if (!BuildConfig.DEBUG) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_INIT, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_DEMO_SEEDED, false)) return;

        try {
            DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase();
            DemoDataSeeder.seedLocalAndRemote(getApplicationContext());
            prefs.edit().putBoolean(KEY_DEMO_SEEDED, true).apply();
            Log.d("LoginActivity", "Demo data seeded (one-time, DEBUG only)");
        } catch (Throwable e) {
            Log.e("LoginActivity", "Lỗi seed demo: " + e.getMessage(), e);
        }
    }

    private void initViews() {
        tilPhone = findViewById(R.id.tilPhone);
        etPhone = findViewById(R.id.etPhone);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
    }

    private void setupEvents() {
        btnLogin.setOnClickListener(v -> handleLogin());

        btnGoogle.setOnClickListener(v ->
                googleClient.signOut().addOnCompleteListener(t ->
                        googleSignInLauncher.launch(googleClient.getSignInIntent())));
    }

    private void handleLogin() {
        if (tilPhone != null) tilPhone.setError(null);

        String rawPhone = etPhone != null && etPhone.getText() != null
                ? etPhone.getText().toString().trim() : "";

        if (rawPhone.isEmpty()) {
            if (tilPhone != null) tilPhone.setError("Vui lòng nhập số điện thoại");
            if (etPhone != null) etPhone.requestFocus();
            return;
        }
        if (!OtpHelper.isValidVnPhone(rawPhone)) {
            if (tilPhone != null) tilPhone.setError("Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)");
            if (etPhone != null) etPhone.requestFocus();
            return;
        }

        String phone = OtpHelper.toLocal(rawPhone);
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang gửi mã...");

        OtpHelper.send(this, phone, new OtpHelper.SendCallback() {
            @Override
            public void onCodeSent(String verificationId) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Tiếp tục (Gửi mã OTP)");
                    Intent intent = new Intent(LoginActivity.this, OtpActivity.class);
                    intent.putExtra(OtpActivity.EXTRA_VERIFICATION_ID, verificationId);
                    intent.putExtra(OtpActivity.EXTRA_PHONE, phone);
                    intent.putExtra(OtpActivity.EXTRA_MODE, OtpActivity.MODE_LOGIN);
                    startActivity(intent);
                });
            }

            @Override
            public void onFailed(String message) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Tiếp tục (Gửi mã OTP)");
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void renderGoogleState(UiState<AuthViewModel.GoogleAuthResult> state) {
        if (state == null) return;
        if (state.getScreenState() == UiState.ScreenState.ERROR) {
            Toast.makeText(this,
                    state.getErrorMessage() != null ? state.getErrorMessage() : "Lỗi xác thực Google",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (state.getContent() != null) {
            AuthViewModel.GoogleAuthResult res = state.getContent();
            if (res.isRegistrationRequired) {
                Toast.makeText(this, "Vui lòng hoàn tất hồ sơ", Toast.LENGTH_LONG).show();
                Intent i = new Intent(this, RegisterActivity.class);
                i.putExtra("GOOGLE_NAME", res.user.getFamilyName());
                i.putExtra("GOOGLE_UID", res.user.getUserId());
                startActivity(i);
            } else {
                Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                goToHome();
            }
        }
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}