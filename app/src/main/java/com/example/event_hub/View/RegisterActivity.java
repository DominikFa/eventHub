package com.example.event_hub.View; // Corrected package name

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.event_hub.R;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.Model.ResultWrapper; // Corrected import for ResultWrapper

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText etUsernameRegister, etEmailRegister, etPasswordRegister;
    private com.google.android.material.button.MaterialButton btnRegister;
    private TextView tvLoginPrompt;
    private com.google.android.material.textfield.TextInputLayout tilUsernameRegister, tilEmailRegister, tilPasswordRegister;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Your XML file name

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
            } else if (password.length() < 6) { // Example: Minimum password length
                tilPasswordRegister.setError("Password must be at least 6 characters");
                hasError = true;
            }

            if (hasError) {
                return;
            }

            authViewModel.register(username, email, password);
        });

        tvLoginPrompt.setOnClickListener(v -> {
            // Navigate back to LoginActivity
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clears the registration activity from back stack
            startActivity(intent);
            // finish(); // Optionally finish RegisterActivity
        });

        authViewModel.registrationState.observe(this, result -> {
            if (result instanceof ResultWrapper.Loading) {
                btnRegister.setEnabled(false);
                btnRegister.setText("Registering...");
            } else if (result instanceof ResultWrapper.Success) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Zarejestruj");
                Toast.makeText(RegisterActivity.this, "Registration Successful! Please login.", Toast.LENGTH_LONG).show();
                // Navigate to Login screen after successful registration
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finishAffinity(); // Finish this activity and all parent activities up to login
            } else if (result instanceof ResultWrapper.Error) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Zarejestruj");
                String errorMessage = ((ResultWrapper.Error<?>) result).getMessage();
                // Show error, perhaps on the relevant field or a general error view
                // For simplicity, using Toast, but TextInputLayout.setError is better for field-specific errors.
                if (errorMessage != null && errorMessage.toLowerCase().contains("email")) {
                    tilEmailRegister.setError(errorMessage);
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}