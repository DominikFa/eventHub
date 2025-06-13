// src/main/java/com/example/event_hub/ViewModel/ProfileViewModel.java
package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.UserSummary;
import com.example.event_hub.Repositiry.AuthRepository;
import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.ResultWrapper;

import java.util.List;
import java.util.stream.Collectors; // Import for stream operations
import java.util.ArrayList; // Import for ArrayList


public class ProfileViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private final AuthRepository authRepository;
    private final MediatorLiveData<ResultWrapper<UserModel>> _userProfileState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<UserModel>> userProfileState = _userProfileState;

    // Change to MediatorLiveData for transformation from UserSummary to UserModel
    public final MediatorLiveData<ResultWrapper<PaginatedResponse<UserModel>>> allUserAccountsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<Void>> userActionState;
    private Long currentUserIdForProfile;

    public ProfileViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        authRepository = AuthRepository.getInstance();
        userActionState = eventHubRepository.voidOperationState;

        // Observe the allUserSummaryState from the repository and transform it to allUserAccountsState
        allUserAccountsState.addSource(eventHubRepository.allUserSummaryState, resultWrapper -> {
            if (resultWrapper instanceof ResultWrapper.Success) {
                PaginatedResponse<UserSummary> response = ((ResultWrapper.Success<PaginatedResponse<UserSummary>>) resultWrapper).getData();
                if (response != null && response.getContent() != null) {
                    // Transform List<UserSummary> to List<UserModel>
                    List<UserModel> userModels = response.getContent().stream()
                            .map(summary -> {
                                UserModel user = new UserModel();
                                user.setId(summary.getId());
                                user.setName(summary.getName());
                                user.setProfileImageUrl(summary.getProfileImageUrl());
                                // Note: Other UserModel fields like login, role, status, description, createdAt
                                // will be null as they are not available in UserSummary.
                                // If these fields are essential, the API should return UserModel directly
                                // or the UserSummary should contain more comprehensive data.
                                return user;
                            })
                            .collect(Collectors.toList());

                    PaginatedResponse<UserModel> transformedResponse = new PaginatedResponse<>();
                    transformedResponse.setContent(userModels);
                    transformedResponse.setTotalPages(response.getTotalPages());
                    transformedResponse.setTotalElements(response.getTotalElements());
                    transformedResponse.setSize(response.getSize());
                    transformedResponse.setNumber(response.getNumber());
                    transformedResponse.setFirst(response.isFirst());
                    transformedResponse.setLast(response.isLast());
                    transformedResponse.setEmpty(response.isEmpty());

                    allUserAccountsState.setValue(new ResultWrapper.Success<>(transformedResponse));
                } else {
                    // Handle empty or null response explicitly
                    allUserAccountsState.setValue(new ResultWrapper.Success<>(new PaginatedResponse<>()));
                }
            } else if (resultWrapper instanceof ResultWrapper.Error) {
                allUserAccountsState.setValue(new ResultWrapper.Error<>(((ResultWrapper.Error<?>) resultWrapper).getMessage()));
            } else if (resultWrapper instanceof ResultWrapper.Loading) {
                allUserAccountsState.setValue(new ResultWrapper.Loading<>());
            } else if (resultWrapper instanceof ResultWrapper.Idle) {
                allUserAccountsState.setValue(new ResultWrapper.Idle<>());
            }
        });


        _userProfileState.addSource(eventHubRepository.userProfileOperationState, resultWrapper -> {
            if (resultWrapper instanceof ResultWrapper.Success) {
                UserModel user = ((ResultWrapper.Success<UserModel>) resultWrapper).getData();
                if (user != null && user.getId() != null && user.getId().equals(currentUserIdForProfile)) {
                    _userProfileState.setValue(resultWrapper);
                }
            } else if (!(resultWrapper instanceof ResultWrapper.Idle)){
                _userProfileState.setValue(resultWrapper);
            }
        });
    }

    // Refactored: loadUserProfile now fetches authToken directly from AuthRepository
    // and passes it to EventHubRepository's fetchUserProfile for both cases (current user and other user).
    public void loadUserProfile(Long userId) {
        if (userId == null || userId == 0L) {
            _userProfileState.setValue(new ResultWrapper.Error<>("User ID missing."));
            return;
        }
        this.currentUserIdForProfile = userId;

        String authToken = authRepository.getCurrentTokenSynchronous(); // Get the latest token directly
        Long loggedInUserId = authRepository.getCurrentUserIdSynchronous();

        if (loggedInUserId != null && userId.equals(loggedInUserId) && authToken != null && !authToken.isEmpty()) {
            // If trying to load current user's profile and logged in, use /api/account/me
            eventHubRepository.fetchCurrentUserProfile(authToken);
        } else {
            // Otherwise, use /api/accounts/{id} (for other users, or if not logged in).
            // Pass the authToken here as well, because the backend might require it even for public profiles.
            // If authToken is null, it will be passed as null, which the repository will handle.
            eventHubRepository.fetchUserProfile(userId, authToken);
        }
    }

    public void fetchAllUsers(String authToken, int page, int size, String nameFilter, String loginFilter, List<String> sort) {
        if (authToken == null || authToken.isEmpty()) {
            // Correctly setting the error state on the appropriate LiveData
            // Use the correct LiveData for error reporting for allUserAccountsState
            allUserAccountsState.postValue(new ResultWrapper.Error<>("Authentication token missing for fetching all users."));
            return;
        }
        // Removed roleFilter and statusFilter as they are not supported by the current repository method
        eventHubRepository.fetchAllUserSummaries(authToken, page, size, nameFilter, loginFilter, sort);
    }


    public void deleteUser(Long userIdToDelete, String authToken) {
        if (userIdToDelete == null || authToken == null) {
            System.err.println("ProfileViewModel: Missing data for deleteUser call.");
            return;
        }
        eventHubRepository.deleteUser(userIdToDelete, authToken);
        if (userIdToDelete.equals(currentUserIdForProfile)) {
            _userProfileState.postValue(new ResultWrapper.Idle<>());
        }
    }

    public void banUser(Long userIdToBan, String authToken) {
        if (userIdToBan == null || authToken == null) {
            System.err.println("ProfileViewModel: Missing data for banUser call.");
            return;
        }
        eventHubRepository.banUser(userIdToBan, authToken);
    }

    public void clearProfileState() {
        currentUserIdForProfile = null;
        _userProfileState.postValue(new ResultWrapper.Idle<>());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _userProfileState.removeSource(eventHubRepository.userProfileOperationState);
        // Remove the source for allUserAccountsState to prevent memory leaks
        allUserAccountsState.removeSource(eventHubRepository.allUserSummaryState);
    }
}