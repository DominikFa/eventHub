package com.example.event_hub.ViewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.event_hub.Repositiry.AuthRepository;
import com.example.event_hub.Model.AuthResponse;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.UserModel;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    public LiveData<ResultWrapper<AuthResponse>> loginState;
    public LiveData<ResultWrapper<UserModel>> registrationState;
    public LiveData<ResultWrapper<Void>> logoutState;
    public LiveData<Long> currentUserId;
    public LiveData<String> currentJwtToken;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = AuthRepository.getInstance();
        loginState = authRepository.loginState;
        registrationState = authRepository.registrationState;
        logoutState = authRepository.logoutState;
        currentUserId = authRepository.observableCurrentUserId; // Zaktualizowano referencję
        currentJwtToken = authRepository.observableCurrentJwtToken;
    }

    public void login(String email, String password) {
        authRepository.login(email, password);
    }

    public void register(String name, String email, String password) {
        authRepository.register(name, email, password);
    }

    public void logout() {
        authRepository.logout();
    }

    public String getCurrentToken() {
        return authRepository.getCurrentTokenSynchronous();
    }

    public UserModel getCurrentUserSynchronous() {
        return authRepository.getCurrentUserSynchronous();
    }
}