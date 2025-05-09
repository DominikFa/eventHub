package com.example.event_hub.View; // Updated package name

import android.content.Intent;
import android.os.Bundle;
// import android.view.View; // Not explicitly used in this version
// import android.widget.Button; // Using MaterialButton directly by ID
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.event_hub.R; // Make sure this matches your project's R file
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.Model.ResultWrapper;
// Import other activities you might navigate to
// import com.example.event_hub.view.RegisterActivity;
// import com.example.event_hub.view.MainActivity;


public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText etUsernameLogin; // Changed ID to match XML
    private EditText etPasswordLogin; // Changed ID to match XML
    private com.google.android.material.button.MaterialButton btnLogin;
    private TextView tvRegisterPrompt;
    private com.google.android.material.textfield.TextInputLayout tilUsernameLogin; // For showing errors
    private com.google.android.material.textfield.TextInputLayout tilPasswordLogin; // For showing errors

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure your XML layout file is named correctly, e.g., "activity_login.xml"
        // and is in the res/layout directory.
        setContentView(R.layout.activity_login);

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Find views by their IDs from the XML
        etUsernameLogin = findViewById(R.id.et_username_login);
        etPasswordLogin = findViewById(R.id.et_password_login);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterPrompt = findViewById(R.id.tv_register_prompt);
        tilUsernameLogin = findViewById(R.id.til_username_login);
        tilPasswordLogin = findViewById(R.id.til_password_login);

        // Set click listener for the login button
        btnLogin.setOnClickListener(v -> {
            String emailOrUsername = etUsernameLogin.getText().toString().trim();
            String password = etPasswordLogin.getText().toString().trim();

            // Clear previous errors
            tilUsernameLogin.setError(null);
            tilPasswordLogin.setError(null);

            if (emailOrUsername.isEmpty()) {
                tilUsernameLogin.setError("Login cannot be empty");
                // Toast.makeText(LoginActivity.this, "Please enter email/username", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                tilPasswordLogin.setError("Password cannot be empty");
                // Toast.makeText(LoginActivity.this, "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }
            authViewModel.login(emailOrUsername, password);
        });

        // Set click listener for the register prompt text
        tvRegisterPrompt.setOnClickListener(v -> {
            // Example: Navigate to RegisterActivity
            // Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "Navigate to Registration Screen", Toast.LENGTH_SHORT).show();
        });

        // Observe the login state from AuthViewModel
        authViewModel.loginState.observe(this, result -> {
            if (result instanceof ResultWrapper.Loading) {
                // Show progress indicator, disable button
                btnLogin.setEnabled(false);
                btnLogin.setText("Logging in..."); // Optional: change button text
            } else if (result instanceof ResultWrapper.Success) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Zaloguj"); // Reset button text
                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                // Example: Navigate to MainActivity after successful login
                // Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                // startActivity(intent);
                // finish(); // Finish LoginActivity so user can't go back to it with back button
            } else if (result instanceof ResultWrapper.Error) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Zaloguj"); // Reset button text
                String errorMessage = ((ResultWrapper.Error<?>) result).getMessage();
                // Display error message, perhaps in a TextView or a Toast
                // For more specific errors, you might set error on tilPasswordLogin or a general error view
                tilPasswordLogin.setError(errorMessage); // Show error associated with password field or a general one
                Toast.makeText(LoginActivity.this, "Login Failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}