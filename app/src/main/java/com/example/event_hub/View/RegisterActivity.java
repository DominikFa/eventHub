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

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    // Re-added etUsernameRegister and its TextInputLayout
    private EditText etUsernameRegister, etEmailRegister, etPasswordRegister;
    private com.google.android.material.button.MaterialButton btnRegister;
    private TextView tvLoginPrompt;
    // Re-added tilUsernameRegister and its TextInputLayout
    private com.google.android.material.textfield.TextInputLayout tilUsernameRegister, tilEmailRegister, tilPasswordRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etUsernameRegister = findViewById(R.id.et_username_register); // Now for 'name'
        etEmailRegister = findViewById(R.id.et_email_register); // Now for 'login' (email format)
        etPasswordRegister = findViewById(R.id.et_password_register);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginPrompt = findViewById(R.id.tv_login_prompt);

        tilUsernameRegister = findViewById(R.id.til_username_register); // Now for 'name'
        tilEmailRegister = findViewById(R.id.til_email_register); // Now for 'login' (email format)
        tilPasswordRegister = findViewById(R.id.til_password_register);

        btnRegister.setOnClickListener(v -> {
            String username = etUsernameRegister.getText().toString().trim(); // Input for 'name'
            String email = etEmailRegister.getText().toString().trim(); // Input for 'login' (email)
            String password = etPasswordRegister.getText().toString().trim();

            tilUsernameRegister.setError(null);
            tilEmailRegister.setError(null);
            tilPasswordRegister.setError(null);

            boolean hasError = false;
            if (username.isEmpty()) {
                tilUsernameRegister.setError("Username cannot be empty"); // Validate username (name)
                hasError = true;
            }
            if (email.isEmpty()) {
                tilEmailRegister.setError("Email cannot be empty"); // Validate email (login)
                hasError = true;
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmailRegister.setError("Enter a valid email address"); // Enforce email format for login
                hasError = true;
            }
            if (password.isEmpty()) {
                tilPasswordRegister.setError("Password cannot be empty");
                hasError = true;
            } else if (password.length() < 6) {
                tilPasswordRegister.setError("Password must be at least 6 characters");
                hasError = true;
            }

            if (hasError) {
                return;
            }

            // Call register with all three parameters: name, login (email), password
            authViewModel.register(username, email, password);
        });

        tvLoginPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        authViewModel.registrationState.observe(this, result -> {
            btnRegister.setEnabled(!(result instanceof ResultWrapper.Loading));
            if (result instanceof ResultWrapper.Loading) {
                btnRegister.setText(R.string.processing_button_text);
            } else {
                btnRegister.setText(getString(R.string.register_button_text));
            }

            if (result instanceof ResultWrapper.Success) {
                Toast.makeText(RegisterActivity.this, R.string.toast_event_created_successfully, Toast.LENGTH_LONG).show();

                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finishAffinity();
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                String errorMessage = errorResult.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.toLowerCase().contains("email") || errorMessage.toLowerCase().contains("login")) {
                        tilEmailRegister.setError(errorMessage); // Error applies to email/login field
                    } else if (errorMessage.toLowerCase().contains("username") || errorMessage.toLowerCase().contains("name")) {
                        tilUsernameRegister.setError(errorMessage); // Error applies to username field
                    } else {
                        Toast.makeText(RegisterActivity.this, getString(R.string.action_failed_toast_prefix) + errorMessage, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, getString(R.string.action_failed_toast_prefix) + getString(R.string.error_unknown), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}