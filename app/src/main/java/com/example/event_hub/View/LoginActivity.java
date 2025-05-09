package com.example.event_hub.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.event_hub.R;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.AuthResponse;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText etUsernameLogin;
    private EditText etPasswordLogin;
    private com.google.android.material.button.MaterialButton btnLogin;
    private TextView tvRegisterPrompt;
    private com.google.android.material.textfield.TextInputLayout tilUsernameLogin;
    private com.google.android.material.textfield.TextInputLayout tilPasswordLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etUsernameLogin = findViewById(R.id.et_username_login);
        etPasswordLogin = findViewById(R.id.et_password_login);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterPrompt = findViewById(R.id.tv_register_prompt);
        tilUsernameLogin = findViewById(R.id.til_username_login);
        tilPasswordLogin = findViewById(R.id.til_password_login);

        btnLogin.setOnClickListener(v -> {
            String emailOrUsername = etUsernameLogin.getText().toString().trim();
            String password = etPasswordLogin.getText().toString().trim();

            tilUsernameLogin.setError(null);
            tilPasswordLogin.setError(null);

            boolean isValid = true;
            if (emailOrUsername.isEmpty()) {
                tilUsernameLogin.setError("Login cannot be empty");
                isValid = false;
            }
            if (password.isEmpty()) {
                tilPasswordLogin.setError("Password cannot be empty");
                isValid = false;
            }
            if (!isValid) return;

            authViewModel.login(emailOrUsername, password);
        });

        tvRegisterPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        authViewModel.loginState.observe(this, result -> {
            btnLogin.setEnabled(!(result instanceof ResultWrapper.Loading));
            if (result instanceof ResultWrapper.Loading) {
                btnLogin.setText("Logging in...");
            } else {
                btnLogin.setText(getString(R.string.login_button_text)); // Use string resource
            }

            if (result instanceof ResultWrapper.Success) {
                ResultWrapper.Success<AuthResponse> successResult = (ResultWrapper.Success<AuthResponse>) result;
                AuthResponse response = successResult.getData();
                String message = "Login Successful!";
                if (response != null && response.getMessage() != null && !response.getMessage().isEmpty()){
                    message = response.getMessage();
                }
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                String errorMessage = errorResult.getMessage();
                tilPasswordLogin.setError(errorMessage != null ? errorMessage : "Login Failed");
            }
        });
    }
}