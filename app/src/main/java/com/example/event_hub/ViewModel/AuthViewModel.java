package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.AuthRepository;
import com.example.event_hub.Model.AuthResponse;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.UserModel;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    public LiveData<ResultWrapper<AuthResponse>> loginState;
    public LiveData<ResultWrapper<AuthResponse>> registrationState;
    public LiveData<ResultWrapper<Void>> logoutState;

    // Derived from AuthRepository's observable LiveData
    public LiveData<String> currentUserId;
    public LiveData<String> currentJwtToken;


    public AuthViewModel() {
        authRepository = AuthRepository.getInstance();
        loginState = authRepository.loginState;
        registrationState = authRepository.registrationState;
        logoutState = authRepository.logoutState;

        currentUserId = authRepository.observableCurrentUserId;
        currentJwtToken = authRepository.observableCurrentJwtToken;
    }

    public void login(String email, String password) {
        authRepository.login(email, password);
    }

    public void register(String username, String email, String password) {
        authRepository.register(username, email, password);
    }

    public void logout() {
        authRepository.logout();
    }

    public boolean isLoggedIn() { // Synchronous check based on cached values in repo
        return authRepository.isLoggedInSynchronous();
    }

    public UserModel getCurrentUserSynchronous() {
        String userId = authRepository.getCurrentUserIdSynchronous();
        if (userId != null) {
            return authRepository.getUserByIdSynchronous(userId);
        }
        return null;
    }
}