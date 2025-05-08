package com.example.event_hub.Model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AuthRepository {

    private static volatile AuthRepository INSTANCE;
    private final ExecutorService executorService;

    private String currentUserId = null;
    private String currentJwtToken = null;
    private final List<UserModel> simulatedUsers = new ArrayList<>();

    // MODIFIED: LiveData for more granular state
    private final MutableLiveData<ResultWrapper<AuthResponse>> _loginState = new MutableLiveData<>();
    public LiveData<ResultWrapper<AuthResponse>> loginState = _loginState;

    private final MutableLiveData<ResultWrapper<AuthResponse>> _registrationState = new MutableLiveData<>();
    public LiveData<ResultWrapper<AuthResponse>> registrationState = _registrationState;

    private final MutableLiveData<ResultWrapper<Void>> _logoutState = new MutableLiveData<>();
    public LiveData<ResultWrapper<Void>> logoutState = _logoutState;

    // Keep a simple LiveData for current user ID if needed by multiple observers directly
    private final MutableLiveData<String> _observableCurrentUserId = new MutableLiveData<>(null);
    public LiveData<String> observableCurrentUserId = _observableCurrentUserId;


    private AuthRepository() {
        executorService = Executors.newSingleThreadExecutor();
        initializeAuthPlaceholderData();
    }

    public static AuthRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (AuthRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AuthRepository();
                }
            }
        }
        return INSTANCE;
    }

    private void initializeAuthPlaceholderData() {
        UserDetails details1 = new UserDetails("Test User FullName", "Bio...", "test_avatar.png");
        simulatedUsers.add(new UserModel("user_jwt_123", "test@example.com", "test@example.com", "user", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10)), "active", details1));
        UserDetails detailsBanned = new UserDetails("Banned User", "This user is banned.", "banned_avatar.png");
        simulatedUsers.add(new UserModel("user_banned_789", "banned@test.com", "banned@test.com", "user", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(20)), "banned", detailsBanned));
    }

    public void login(String email, String password) {
        _loginState.postValue(new ResultWrapper.Loading<>()); // MODIFIED
        executorService.submit(() -> {
            try {
                Thread.sleep(1000);
                Optional<UserModel> userOpt = simulatedUsers.stream()
                        .filter(u -> email.equals(u.getLogin()) && "active".equals(u.getStatus()))
                        .findFirst();

                if (userOpt.isPresent() && "password123".equals(password)) {
                    UserModel user = userOpt.get();
                    currentUserId = user.getUserId();
                    currentJwtToken = "simulated_jwt_for_" + currentUserId + "_" + System.currentTimeMillis();
                    AuthResponse response = new AuthResponse(true, "Login successful!", currentUserId, currentJwtToken);
                    _loginState.postValue(new ResultWrapper.Success<>(response)); // MODIFIED
                    _observableCurrentUserId.postValue(currentUserId); // ADDED
                } else {
                    boolean userExists = simulatedUsers.stream().anyMatch(u -> email.equals(u.getLogin()));
                    String failMessage = "Invalid credentials.";
                    if(userExists && !userOpt.isPresent()){
                        failMessage = "Account is not active.";
                    }
                    AuthResponse response = new AuthResponse(false, failMessage, null, null);
                    _loginState.postValue(new ResultWrapper.Error<>(failMessage)); // MODIFIED - pass message directly to Error
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _loginState.postValue(new ResultWrapper.Error<>("Login interrupted.")); // MODIFIED
            }
        });
    }

    public void register(String username, String email, String password) {
        _registrationState.postValue(new ResultWrapper.Loading<>()); // MODIFIED
        executorService.submit(() -> {
            try {
                Thread.sleep(1200);
                boolean exists = simulatedUsers.stream().anyMatch(u -> email.equals(u.getLogin()));
                if (exists) {
                    _registrationState.postValue(new ResultWrapper.Error<>("Login/Email already exists.")); // MODIFIED
                    return;
                }

                String newUserId = "user_reg_" + System.currentTimeMillis();
                UserDetails details = new UserDetails(username, "Newly registered user.", "default_avatar.png");
                UserModel newUser = new UserModel(newUserId, email, email, "user", new Date(), "active", details);
                simulatedUsers.add(newUser);

                AuthResponse response = new AuthResponse(true, "Registration successful! Please login.", newUserId, null);
                _registrationState.postValue(new ResultWrapper.Success<>(response)); // MODIFIED
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _registrationState.postValue(new ResultWrapper.Error<>("Registration interrupted.")); // MODIFIED
            }
        });
    }

    public void logout() {
        _logoutState.postValue(new ResultWrapper.Loading<>()); // ADDED
        // Simulate a small delay for logout if desired
        executorService.submit(() -> {
            // try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            currentUserId = null;
            currentJwtToken = null;
            _observableCurrentUserId.postValue(null); // ADDED
            _logoutState.postValue(new ResultWrapper.Success<>(null)); // MODIFIED (Void represented by null data)
            System.out.println("AuthRepository: User logged out.");
        });
    }

    public String getCurrentToken() {
        return currentJwtToken;
    }

    // This direct getter is still useful for synchronous checks within ViewModels or other synchronous logic
    public String getCurrentUserIdSynchronous() {
        return currentUserId;
    }

    public boolean isLoggedInSynchronous() {
        return currentUserId != null && currentJwtToken != null;
    }
}