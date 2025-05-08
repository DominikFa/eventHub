package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.AuthRepository; // ADDED
import com.example.event_hub.Model.AuthResponse;
// ADDED: Import for ResultWrapper (adjust package if you placed it elsewhere)
import com.example.event_hub.Model.ResultWrapper;


public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository; // ADDED

    // MODIFIED: Expose ResultWrapper states from the repository
    public LiveData<ResultWrapper<AuthResponse>> loginState;
    public LiveData<ResultWrapper<AuthResponse>> registrationState;
    public LiveData<ResultWrapper<Void>> logoutState;

    // LiveData to hold the current user's ID, derived from successful login or repository state
    private final MutableLiveData<String> _currentUserId = new MutableLiveData<>(null);
    public LiveData<String> currentUserId = _currentUserId;

    // LiveData to just hold the JWT for components that might need it.
    private final MutableLiveData<String> _jwtToken = new MutableLiveData<>(null);
    public LiveData<String> jwtToken = _jwtToken;


    public AuthViewModel() {
        authRepository = AuthRepository.getInstance(); // MODIFIED
        loginState = authRepository.loginState;
        registrationState = authRepository.registrationState;
        logoutState = authRepository.logoutState;

        // Observe the repository's currentUserId to keep AuthViewModel's currentUserId in sync
        // This is one way to do it. Another is to update _currentUserId upon successful login directly.
        Transformations.map(authRepository.observableCurrentUserId, userId -> {
            _currentUserId.setValue(userId);
            if (userId != null) {
                _jwtToken.setValue(authRepository.getCurrentToken());
            } else {
                _jwtToken.setValue(null);
            }
            return userId; // map needs to return a value, though it's not directly used here
        }).observeForever(s -> {}); // Observe forever to keep it active while ViewModel is alive

        // Initialize currentUserId from repository's synchronous state if already logged in
        // (e.g. after process death and ViewModel recreation if repository state survived)
        if (authRepository.isLoggedInSynchronous()){
            _currentUserId.setValue(authRepository.getCurrentUserIdSynchronous());
            _jwtToken.setValue(authRepository.getCurrentToken());
        }
    }

    public void login(String email, String password) {
        System.out.println("AuthViewModel: Delegating login for email: " + email);
        authRepository.login(email, password);
        // Observer on loginState (from repository) in the UI will handle UI changes.
        // We can also observe it here if AuthViewModel needs to react directly, e.g., to set currentUserId.
        // However, relying on observableCurrentUserId from repo is cleaner.
    }

    public void register(String username, String email, String password) {
        System.out.println("AuthViewModel: Delegating registration for username: " + username + ", email: " + email);
        authRepository.register(username, email, password);
        // Observer on registrationState in the UI will handle UI changes.
    }

    public void logout() {
        String loggingOutUserId = _currentUserId.getValue(); // Get current user before nullifying
        System.out.println("AuthViewModel: Delegating logout for user: " + loggingOutUserId);
        authRepository.logout();
        // Observer on logoutState in UI will handle UI changes.
        // _currentUserId and _jwtToken will be updated via observation of authRepository.observableCurrentUserId
    }

    public boolean isLoggedIn() {
        // Prefer to get this from the source of truth if possible or from a derived LiveData state
        return _currentUserId.getValue() != null && _jwtToken.getValue() != null;
    }

    // getCurrentToken already provided by public LiveData<String> jwtToken;
    // public String getCurrentToken() {
    //     return _jwtToken.getValue();
    // }
}