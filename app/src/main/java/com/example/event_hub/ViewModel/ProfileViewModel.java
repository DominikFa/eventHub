package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.UserDetails;
import com.example.event_hub.Model.ResultWrapper; // Ensure this import path is correct

public class ProfileViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;

    // LiveData for the state of the fetched user profile
    private final MediatorLiveData<ResultWrapper<UserModel>> _userProfileState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<UserModel>> userProfileState = _userProfileState;

    // LiveData for the state of profile edit operation
    public LiveData<ResultWrapper<UserModel>> profileEditOperationState; // Observe directly from repo

    // LiveData for the state of void operations like ban/delete user
    public LiveData<ResultWrapper<Void>> userActionOperationState; // Observe directly from repo

    private String currentUserIdForProfile; // To keep track of which user's profile is being observed

    public ProfileViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        // Initialize LiveData from repository
        profileEditOperationState = eventHubRepository.userProfileOperationState; // Assuming edit also uses this
        userActionOperationState = eventHubRepository.voidOperationState; // For ban/delete user
    }

    /**
     * Fetches the user profile for a given userId.
     * The result is observed on userProfileState.
     * @param userId The ID of the user whose profile is to be fetched.
     */
    public void loadUserProfile(String userId) {
        if (userId == null || userId.isEmpty()) {
            _userProfileState.setValue(new ResultWrapper.Error<>("User ID cannot be null or empty."));
            return;
        }
        this.currentUserIdForProfile = userId;
        _userProfileState.setValue(new ResultWrapper.Loading<>()); // Set loading immediately

        // Remove previous source if any to avoid multiple updates from old requests
        _userProfileState.removeSource(eventHubRepository.userProfileOperationState);

        // Add source to observe the repository's state for this specific operation
        _userProfileState.addSource(eventHubRepository.userProfileOperationState, userModelResultWrapper -> {
            // Only update if the result is for the currently requested user ID,
            // or if the repo's LiveData is dedicated to "the last fetched/edited profile".
            // For this design, userProfileOperationState in repo is generic for the last profile op.
            // So, if the ViewModel triggers fetchUserProfile(userId), it expects the next emission
            // on userProfileOperationState to be for that userId.
            if (userModelResultWrapper instanceof ResultWrapper.Success) {
                UserModel fetchedUser = ((ResultWrapper.Success<UserModel>) userModelResultWrapper).getData();
                if (fetchedUser != null && fetchedUser.getUserId().equals(currentUserIdForProfile)) {
                    _userProfileState.setValue(userModelResultWrapper);
                } else if (fetchedUser == null && !(userModelResultWrapper instanceof ResultWrapper.Loading)) {
                    // If data is null but it's not loading, could be an error or an irrelevant update
                    // This check might need refinement based on how repo state is managed
                }
            } else { // Error or Loading
                _userProfileState.setValue(userModelResultWrapper);
            }
        });
        eventHubRepository.fetchUserProfile(userId);
    }

    /**
     * Edits the profile details of the currently loaded/specified user.
     * Assumes that userProfileState holds the current user.
     * @param updatedDetails The UserDetails object with updated information.
     */
    public void editProfile(UserDetails updatedDetails) {
        ResultWrapper<UserModel> currentUserState = _userProfileState.getValue();

        if (currentUserState instanceof ResultWrapper.Success) {
            UserModel currentUser = ((ResultWrapper.Success<UserModel>) currentUserState).getData();
            if (currentUser != null && currentUser.getUserId() != null) {
                System.out.println("ProfileViewModel: Attempting to edit profile for userId: " + currentUser.getUserId());
                // The repository's userProfileOperationState will be updated by editProfile.
                // The UI should observe profileEditOperationState (which points to repo's state).
                // After a successful edit, loadUserProfile might be called again if a fresh full state is needed
                // or if the edit operation itself returns the full updated UserModel.
                eventHubRepository.editProfile(currentUser.getUserId(), updatedDetails);
            } else {
                // Post to a local action status if needed, or rely on UI observing profileEditOperationState
                System.err.println("ProfileViewModel Edit Error: Current user or user ID is null.");
                // _someLocalActionStatus.setValue(new ResultWrapper.Error("Cannot edit: User data is invalid."));
            }
        } else {
            System.err.println("ProfileViewModel Edit Error: User profile not successfully loaded.");
            // _someLocalActionStatus.setValue(new ResultWrapper.Error("Cannot edit: User profile not loaded."));
        }
    }

    /**
     * Deletes the user account associated with the given userId.
     * @param userId The ID of the user to delete.
     */
    public void deleteUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            // Optionally post to a local status LiveData if userActionOperationState is too generic
            System.err.println("ProfileViewModel Delete Error: User ID cannot be null or empty.");
            return;
        }
        System.out.println("ProfileViewModel: Attempting to delete user: " + userId);
        // The UI should observe userActionOperationState (points to repo's voidOperationState)
        eventHubRepository.deleteUser(userId);

        // If the deleted user is the one whose profile is currently shown, clear it
        if (userId.equals(currentUserIdForProfile) && _userProfileState.getValue() instanceof ResultWrapper.Success) {
            _userProfileState.postValue(new ResultWrapper.Idle<>()); // Or Error("User was deleted")
        }
    }

    /**
     * Bans the user associated with the given userId.
     * @param userId The ID of the user to ban.
     */
    public void banUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            System.err.println("ProfileViewModel Ban Error: User ID cannot be null or empty.");
            return;
        }
        System.out.println("ProfileViewModel: Attempting to ban user: " + userId);
        // The UI should observe userActionOperationState
        eventHubRepository.banUser(userId);

        // If the banned user is the one whose profile is currently shown, refresh it
        // as banUser in repo calls fetchUserProfile.
        // The observation on _userProfileState (via loadUserProfile's setup) should pick up the change.
        // No direct action needed here if the above observation is set up correctly.
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove any sources from MediatorLiveData to prevent memory leaks
        _userProfileState.removeSource(eventHubRepository.userProfileOperationState);
    }
}