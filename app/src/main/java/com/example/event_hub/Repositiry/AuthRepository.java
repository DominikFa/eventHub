package com.example.event_hub.Repositiry;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.event_hub.Model.AuthResponse;
import com.example.event_hub.Model.LoginRequest;
import com.example.event_hub.Model.RegisterRequest;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.UserModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private static volatile AuthRepository INSTANCE;
    private final ApiService apiService;

    private UserModel currentUserCache = null;
    private String currentJwtTokenCache = null;

    private final MutableLiveData<ResultWrapper<AuthResponse>> _loginState = new MutableLiveData<>();
    public LiveData<ResultWrapper<AuthResponse>> loginState = _loginState;

    private final MutableLiveData<ResultWrapper<UserModel>> _registrationState = new MutableLiveData<>();
    public LiveData<ResultWrapper<UserModel>> registrationState = _registrationState;

    private final MutableLiveData<ResultWrapper<Void>> _logoutState = new MutableLiveData<>();
    public LiveData<ResultWrapper<Void>> logoutState = _logoutState;


    private final MutableLiveData<Long> _observableCurrentUserId = new MutableLiveData<>(null);
    public LiveData<Long> observableCurrentUserId = _observableCurrentUserId;

    private final MutableLiveData<String> _observableCurrentJwtToken = new MutableLiveData<>(null);
    public LiveData<String> observableCurrentJwtToken = _observableCurrentJwtToken;

    private AuthRepository() {
        this.apiService = ApiClient.getApiService();
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

    public void login(String login, String password) {
        _loginState.postValue(new ResultWrapper.Loading<>());
        LoginRequest loginRequest = new LoginRequest(login, password);

        apiService.loginUser(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    currentUserCache = authResponse.getUser();
                    currentJwtTokenCache = authResponse.getToken();

                    if (currentUserCache != null) {

                        _observableCurrentUserId.postValue(currentUserCache.getId());
                    }
                    _observableCurrentJwtToken.postValue(currentJwtTokenCache);

                    _loginState.postValue(new ResultWrapper.Success<>(authResponse));
                } else {
                    _loginState.postValue(new ResultWrapper.Error<>("Login failed: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                _loginState.postValue(new ResultWrapper.Error<>(t.getMessage() != null ? t.getMessage() : "Network error during login."));
            }
        });
    }

    public void register(String name, String login, String password) {
        _registrationState.postValue(new ResultWrapper.Loading<>());
        RegisterRequest registerRequest = new RegisterRequest(name, login, password);
        apiService.registerUser(registerRequest).enqueue(createCallback(_registrationState));
    }

    public void logout() {
        _logoutState.postValue(new ResultWrapper.Loading<>());
        currentUserCache = null;
        currentJwtTokenCache = null;
        _observableCurrentUserId.postValue(null);
        _observableCurrentJwtToken.postValue(null);
        _logoutState.postValue(new ResultWrapper.Success<>(null));
    }

    public String getCurrentTokenSynchronous() {
        return currentJwtTokenCache;
    }

    // ZMIANA: Metoda zwraca teraz Long
    public Long getCurrentUserIdSynchronous() {
        return currentUserCache != null ? currentUserCache.getId() : null;
    }

    public UserModel getCurrentUserSynchronous() {
        return currentUserCache;
    }

    public boolean isLoggedInSynchronous() {
        return currentUserCache != null && currentJwtTokenCache != null;
    }

    private <T> Callback<T> createCallback(MutableLiveData<ResultWrapper<T>> liveData) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(new ResultWrapper.Success<>(response.body()));
                } else if (response.isSuccessful()) {
                    liveData.postValue(new ResultWrapper.Success<>(null));
                } else {
                    liveData.postValue(new ResultWrapper.Error<>("API Error: " + response.code()));
                }
            }
            @Override
            public void onFailure(Call<T> call, Throwable t) {
                liveData.postValue(new ResultWrapper.Error<>(t.getMessage()));
            }
        };
    }
}