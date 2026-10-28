package com.example.studenthousing.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.studenthousing.R;
import com.example.studenthousing.data.local.DatabaseHelper;
import com.example.studenthousing.data.model.User;
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

    private static final String WEB_CLIENT_ID =
            "643135707324-45gf2nbr40nhaubqa9a1jm547j19fqtl.apps.googleusercontent.com";

    private TextInputLayout tilPhone, tilPassword;
    private EditText etPhone, etPassword;
    private MaterialButton btnLogin, btnGoogle;
    private android.widget.TextView tvRegister;
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

        try {
            DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase();
            DemoDataSeeder.seedLocalAndRemote(getApplicationContext());
        } catch (Throwable e) {
            android.util.Log.e("LoginActivity", "Lỗi DB: " + e.getMessage());
        }

        session = new SessionManager(this);
        if (session.isLoggedIn()) {
            goToHome();
            return;
        }

        setContentView(R.layout.activity_login);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        viewModel.getGoogleAuthState().observe(this, this::renderGoogleState);

        initViews();
        setupGoogleSignIn();
        setupEvents();
    }

    private void initViews() {
        tilPhone = findViewById(R.id.tilPhone);
        etPhone = findViewById(R.id.etPhone);
        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupEvents() {
        btnLogin.setOnClickListener(v -> handleLogin());

        btnGoogle.setOnClickListener(v ->
                googleClient.signOut().addOnCompleteListener(t ->
                        googleSignInLauncher.launch(googleClient.getSignInIntent())));

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void handleLogin() {
        tilPhone.setError(null);
        if (tilPassword != null) tilPassword.setError(null);

        String rawPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (rawPhone.isEmpty()) {
            tilPhone.setError("Vui lòng nhập số điện thoại");
            etPhone.requestFocus();
            return;
        }
        if (!OtpHelper.isValidVnPhone(rawPhone)) {
            tilPhone.setError("Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)");
            etPhone.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            if (tilPassword != null) tilPassword.setError("Vui lòng nhập mật khẩu");
            if (etPassword != null) etPassword.requestFocus();
            return;
        }

        String phone = OtpHelper.toLocal(rawPhone);
        com.example.studenthousing.data.local.UserDao userDao = new com.example.studenthousing.data.local.UserDao(this);
        User user = userDao.getByPhone(phone);

        if (user == null) {
            Toast.makeText(this, "Số điện thoại chưa được đăng ký tài khoản!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user.getPassword() != null && !user.getPassword().isEmpty() && !user.getPassword().equals(password)) {
            if (tilPassword != null) tilPassword.setError("Mật khẩu không chính xác");
            if (etPassword != null) etPassword.requestFocus();
            return;
        }

        com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(authTask -> {
            com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                if (fbUser != null) {
                    user.setUserId(fbUser.getUid());
                } else {
                    user.setUserId("user_" + phone);
                }
            }
            userDao.upsert(user);
            session.createLoginSession(user);
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            goToHome();
        });
    }

    private void renderGoogleState(UiState<AuthViewModel.GoogleAuthResult> state) {
        if (state == null) return;
        if (state.getScreenState() == UiState.ScreenState.ERROR) {
            Toast.makeText(this, state.getErrorMessage() != null ? state.getErrorMessage() : "Lỗi xác thực Google", Toast.LENGTH_SHORT).show();
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
