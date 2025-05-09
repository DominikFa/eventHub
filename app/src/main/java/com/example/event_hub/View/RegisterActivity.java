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

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText etUsernameRegister, etEmailRegister, etPasswordRegister;
    private com.google.android.material.button.MaterialButton btnRegister;
    private TextView tvLoginPrompt;
    private com.google.android.material.textfield.TextInputLayout tilUsernameRegister, tilEmailRegister, tilPasswordRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etUsernameRegister = findViewById(R.id.et_username_register);
        etEmailRegister = findViewById(R.id.et_email_register);
        etPasswordRegister = findViewById(R.id.et_password_register);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginPrompt = findViewById(R.id.tv_login_prompt);

        tilUsernameRegister = findViewById(R.id.til_username_register);
        tilEmailRegister = findViewById(R.id.til_email_register);
        tilPasswordRegister = findViewById(R.id.til_password_register);

        btnRegister.setOnClickListener(v -> {
            String username = etUsernameRegister.getText().toString().trim();
            String email = etEmailRegister.getText().toString().trim();
            String password = etPasswordRegister.getText().toString().trim();

            tilUsernameRegister.setError(null);
            tilEmailRegister.setError(null);
            tilPasswordRegister.setError(null);

            boolean hasError = false;
            if (username.isEmpty()) {
                tilUsernameRegister.setError("Username cannot be empty");
                hasError = true;
            }
            if (email.isEmpty()) {
                tilEmailRegister.setError("Email cannot be empty");
                hasError = true;
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmailRegister.setError("Enter a valid email address");
                hasError = true;
            }
            if (password.isEmpty()) {
                tilPasswordRegister.setError("Password cannot be empty");
                hasError = true;
            } else if (password.length() < 6) { // Example: Password minimum length
                tilPasswordRegister.setError("Password must be at least 6 characters");
                hasError = true;
            }

            if (hasError) {
                return;
            }

            authViewModel.register(username, email, password);
        });

        tvLoginPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear RegisterActivity from backstack
            startActivity(intent);
            finish();
        });

        authViewModel.registrationState.observe(this, result -> {
            btnRegister.setEnabled(!(result instanceof ResultWrapper.Loading));
            if (result instanceof ResultWrapper.Loading) {
                btnRegister.setText("Registering...");
            } else {
                btnRegister.setText(getString(R.string.register_button_text)); // Use string resource
            }

            if (result instanceof ResultWrapper.Success) {
                ResultWrapper.Success<AuthResponse> successResult = (ResultWrapper.Success<AuthResponse>) result;
                AuthResponse response = successResult.getData();
                String message = "Registration Successful! Please login.";
                if (response != null && response.getMessage() != null && !response.getMessage().isEmpty()){
                    message = response.getMessage();
                }
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();

                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finishAffinity(); // Finish this activity and all parent activities.
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                String errorMessage = errorResult.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.toLowerCase().contains("email")) {
                        tilEmailRegister.setError(errorMessage);
                    } else if (errorMessage.toLowerCase().contains("username")) {
                        tilUsernameRegister.setError(errorMessage);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration Failed: Unknown error", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}