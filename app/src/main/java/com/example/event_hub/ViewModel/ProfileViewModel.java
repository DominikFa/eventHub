package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.UserDetails;
import com.example.event_hub.Model.ResultWrapper;

public class ProfileViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private final MediatorLiveData<ResultWrapper<UserModel>> _userProfileState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<UserModel>> userProfileState = _userProfileState;

    public LiveData<ResultWrapper<UserModel>> profileEditState;
    public LiveData<ResultWrapper<Void>> userActionState;

    private String currentUserIdForProfile;

    public ProfileViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        profileEditState = eventHubRepository.userProfileOperationState;
        userActionState = eventHubRepository.voidOperationState;

        _userProfileState.addSource(eventHubRepository.userProfileOperationState, resultWrapper -> {
            if (resultWrapper instanceof ResultWrapper.Success) {
                UserModel user = ((ResultWrapper.Success<UserModel>) resultWrapper).getData();
                if (user != null && (user.getUserId().equals(currentUserIdForProfile) || currentUserIdForProfile == null) ) {
                    _userProfileState.setValue(resultWrapper);
                }
            } else if (!(resultWrapper instanceof ResultWrapper.Idle)){
                _userProfileState.setValue(resultWrapper);
            }
        });
    }

    public void loadUserProfile(String userId) {
        if (userId == null || userId.isEmpty()) {
            _userProfileState.setValue(new ResultWrapper.Error<>("User ID missing."));
            return;
        }
        this.currentUserIdForProfile = userId;
        eventHubRepository.fetchUserProfile(userId); // Repository will post Loading
    }

    public void editProfile(String userIdToEdit, UserDetails updatedDetails, String authToken) {
        if (userIdToEdit == null || updatedDetails == null || authToken == null) {
            // Optionally, if profileEditState were mutable here:
            // ((MutableLiveData<ResultWrapper<UserModel>>)profileEditState).postValue(new ResultWrapper.Error<>("Client Error: Missing data for edit."));
            System.err.println("ProfileViewModel: Missing data for editProfile call.");
            return;
        }
        eventHubRepository.editProfile(userIdToEdit, updatedDetails, authToken);
    }

    public void deleteUser(String userIdToDelete, String authToken) {
        if (userIdToDelete == null || authToken == null) {
            System.err.println("ProfileViewModel: Missing data for deleteUser call.");
            return;
        }
        eventHubRepository.deleteUser(userIdToDelete, authToken);
        if (userIdToDelete.equals(currentUserIdForProfile)) {
            _userProfileState.postValue(new ResultWrapper.Idle<>());
        }
    }

    public void banUser(String userIdToBan, String authToken) {
        if (userIdToBan == null || authToken == null) {
            System.err.println("ProfileViewModel: Missing data for banUser call.");
            return;
        }
        eventHubRepository.banUser(userIdToBan, authToken);
    }

    public void clearProfileState() {
        currentUserIdForProfile = null;
        _userProfileState.setValue(new ResultWrapper.Idle<>());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _userProfileState.removeSource(eventHubRepository.userProfileOperationState);
    }
}