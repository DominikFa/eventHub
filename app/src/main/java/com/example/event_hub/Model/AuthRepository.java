package com.example.event_hub.Model;

import androidx.core.util.Pair; // For returning userId and role
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthRepository {

    private static volatile AuthRepository INSTANCE;
    private final ExecutorService executorService;

    private String currentUserIdCache = null;
    private String currentJwtTokenCache = null;
    private final List<UserModel> simulatedUsers = new ArrayList<>();

    private final MutableLiveData<ResultWrapper<AuthResponse>> _loginState = new MutableLiveData<>();
    public LiveData<ResultWrapper<AuthResponse>> loginState = _loginState;

    private final MutableLiveData<ResultWrapper<AuthResponse>> _registrationState = new MutableLiveData<>();
    public LiveData<ResultWrapper<AuthResponse>> registrationState = _registrationState;

    private final MutableLiveData<ResultWrapper<Void>> _logoutState = new MutableLiveData<>();
    public LiveData<ResultWrapper<Void>> logoutState = _logoutState;

    private final MutableLiveData<String> _observableCurrentUserId = new MutableLiveData<>(null);
    public LiveData<String> observableCurrentUserId = _observableCurrentUserId;

    private final MutableLiveData<String> _observableCurrentJwtToken = new MutableLiveData<>(null);
    public LiveData<String> observableCurrentJwtToken = _observableCurrentJwtToken;

    private static final String JWT_PREFIX = "SIMULATED_JWT::";
    private static final Pattern JWT_PATTERN = Pattern.compile(
            JWT_PREFIX + "userId=([^:]+)::role=([^:]+)::timestamp=(\\d+)");


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
        simulatedUsers.clear();
        UserDetails detailsAdmin = new UserDetails("Admin FullName", "I am the administrator.", "admin_avatar.png");
        simulatedUsers.add(new UserModel("admin_user_007", "admin@example.com", "admin@example.com", "admin", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)), "active", detailsAdmin));

        UserDetails details1 = new UserDetails("Test User FullName", "Bio...", "test_avatar.png");
        simulatedUsers.add(new UserModel("user_jwt_123", "test@example.com", "test@example.com", "user", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10)), "active", details1));
        UserDetails detailsBanned = new UserDetails("Banned User", "This user is banned.", "banned_avatar.png");
        simulatedUsers.add(new UserModel("user_banned_789", "banned@test.com", "banned@test.com", "user", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(20)), "banned", detailsBanned));
        System.out.println("AuthRepository: Placeholder data initialized with admin.");
    }

    public void login(String email, String password) {
        _loginState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
                Optional<UserModel> userOpt = simulatedUsers.stream()
                        .filter(u -> email.equals(u.getLogin()) && "active".equals(u.getStatus()))
                        .findFirst();

                if (userOpt.isPresent() && "password123".equals(password)) { // Hardcoded password for simulation
                    UserModel user = userOpt.get();
                    currentUserIdCache = user.getUserId();
                    currentJwtTokenCache = JWT_PREFIX + "userId=" + user.getUserId() +
                            "::role=" + user.getRole() +
                            "::timestamp=" + System.currentTimeMillis();

                    _observableCurrentUserId.postValue(currentUserIdCache);
                    _observableCurrentJwtToken.postValue(currentJwtTokenCache);

                    AuthResponse response = new AuthResponse(true, "Login successful!", currentUserIdCache, currentJwtTokenCache);
                    _loginState.postValue(new ResultWrapper.Success<>(response));
                } else {
                    boolean userExistsButNotActive = simulatedUsers.stream().anyMatch(u -> email.equals(u.getLogin()) && !"active".equals(u.getStatus()));
                    String failMessage = userExistsButNotActive ? "Account is not active." : "Invalid credentials.";
                    _loginState.postValue(new ResultWrapper.Error<>(failMessage));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _loginState.postValue(new ResultWrapper.Error<>("Login interrupted."));
            }
        });
    }

    public void register(String username, String email, String password) {
        _registrationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(1200); // Simulate network delay
                boolean emailExists = simulatedUsers.stream().anyMatch(u -> email.equals(u.getLogin()) || email.equals(u.getEmail()));
                boolean usernameExists = simulatedUsers.stream().anyMatch(u -> username.equals(u.getLogin()));


                if (emailExists) {
                    _registrationState.postValue(new ResultWrapper.Error<>("Email already registered."));
                    return;
                }
                if (usernameExists) {
                    _registrationState.postValue(new ResultWrapper.Error<>("Username already taken."));
                    return;
                }

                String newUserId = "user_reg_" + System.currentTimeMillis();
                UserDetails details = new UserDetails(username, "Newly registered user.", "default_avatar.png");
                // For simulation, login and email can be the same initially if username is the primary login.
                // Adjust if your system strictly separates them or uses email as login.
                UserModel newUser = new UserModel(newUserId, username, email, "user", new Date(), "active", details);
                simulatedUsers.add(newUser);

                AuthResponse response = new AuthResponse(true, "Registration successful! Please login.", newUserId, null); // No token on register
                _registrationState.postValue(new ResultWrapper.Success<>(response));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _registrationState.postValue(new ResultWrapper.Error<>("Registration interrupted."));
            }
        });
    }

    public void logout() {
        _logoutState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            // Simulate clearing session data
            currentUserIdCache = null;
            currentJwtTokenCache = null;
            _observableCurrentUserId.postValue(null);
            _observableCurrentJwtToken.postValue(null);
            _logoutState.postValue(new ResultWrapper.Success<>(null));
        });
    }

    public String getCurrentTokenSynchronous() {
        return currentJwtTokenCache;
    }

    public String getCurrentUserIdSynchronous() {
        return currentUserIdCache;
    }
    public UserModel getUserByIdSynchronous(String userId) {
        if (userId == null) return null;
        return simulatedUsers.stream().filter(u -> u.getUserId().equals(userId)).findFirst().orElse(null);
    }


    public boolean isLoggedInSynchronous() {
        return currentUserIdCache != null && currentJwtTokenCache != null;
    }

    public Optional<Pair<String, String>> validateTokenAndExtractPrincipal(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Matcher matcher = JWT_PATTERN.matcher(token);
        if (matcher.matches()) {
            String userId = matcher.group(1);
            String role = matcher.group(2);
            // String timestamp = matcher.group(3); // Could check timestamp for expiration

            Optional<UserModel> userOpt = simulatedUsers.stream()
                    .filter(u -> userId.equals(u.getUserId()) &&
                            role.equals(u.getRole()) &&
                            "active".equals(u.getStatus())) // Ensure user is still active
                    .findFirst();

            if (userOpt.isPresent()) {
                return Optional.of(new Pair<>(userId, role));
            } else {
                System.err.println("AuthRepository: Token valid structure, but user " + userId + " not found, not active, or role mismatch.");
            }
        } else {
            System.err.println("AuthRepository: Invalid token format: " + token);
        }
        return Optional.empty();
    }
    // Method in EventHubRepository.java related to user deletion, simplified for AuthRepository context
    /**
     * Deletes a user account. (Requires appropriate permissions).
     * This is a simplified conceptual placement. In a real system, user management might be
     * in a separate service/repository or EventHubRepository would call AuthRepository.
     *
     * @param userIdToDelete The ID of the user to delete.
     * @param authToken      The JWT token for authentication (likely admin or self).
     * @return ResultWrapper indicating success or failure.
     */
    public LiveData<ResultWrapper<Void>> deleteUser(String userIdToDelete, String authToken) {
        MutableLiveData<ResultWrapper<Void>> deleteResult = new MutableLiveData<>();
        deleteResult.postValue(new ResultWrapper.Loading<>());

        executorService.submit(() -> {
            Optional<Pair<String, String>> principalOpt = validateTokenAndExtractPrincipal(authToken);
            if (!principalOpt.isPresent()) {
                deleteResult.postValue(new ResultWrapper.Error<>("Unauthorized: Invalid token."));
                return;
            }
            Pair<String, String> principal = principalOpt.get();

            // Check permissions: User can delete self, or admin can delete others
            boolean isAdmin = "admin".equalsIgnoreCase(principal.second);
            if (!userIdToDelete.equals(principal.first) && !isAdmin) {
                deleteResult.postValue(new ResultWrapper.Error<>("Permission Denied: Insufficient rights to delete user."));
                return;
            }

            // Design Consideration: Prevent deletion of the last admin account.
            // This is a safety measure. If there's only one admin, deleting it would lock out admin functions.
            // In a real system, this might involve more complex checks or procedures for such a case.
            Optional<UserModel> userToDeleteOpt = simulatedUsers.stream().filter(u -> userIdToDelete.equals(u.getUserId())).findFirst();
            if (userToDeleteOpt.isPresent() && "admin".equalsIgnoreCase(userToDeleteOpt.get().getRole())) {
                long adminCount = simulatedUsers.stream().filter(u -> "admin".equalsIgnoreCase(u.getRole()) && "active".equals(u.getStatus())).count();
                if (adminCount <= 1) {
                    deleteResult.postValue(new ResultWrapper.Error<>("Deletion failed: Cannot delete the last active admin account."));
                    return;
                }
            }

            boolean removed = simulatedUsers.removeIf(u -> userIdToDelete.equals(u.getUserId()));
            if (removed) {
                // If this is the currently logged-in user deleting themselves, clear session
                if (userIdToDelete.equals(currentUserIdCache)) {
                    logout(); // This will also update observables
                }
                deleteResult.postValue(new ResultWrapper.Success<>(null));
                // Note: Cascading deletes (events, participations by this user) would be handled
                // by EventHubRepository or a higher-level service.
            } else {
                deleteResult.postValue(new ResultWrapper.Error<>("Deletion failed: User not found."));
            }
        });
        return deleteResult;
    }
}