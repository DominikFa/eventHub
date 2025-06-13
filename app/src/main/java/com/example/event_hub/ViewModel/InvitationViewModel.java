// src/main/java/com/example/event_hub/ViewModel/InvitationViewModel.java
package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Repositiry.AuthRepository;
import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.InvitationModel;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.ResultWrapper;

import java.util.List;

public class InvitationViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private final AuthRepository authRepository;

    private final MediatorLiveData<ResultWrapper<List<InvitationModel>>> _invitationsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<InvitationModel>>> invitationsState = _invitationsState;

    private final MediatorLiveData<ResultWrapper<Void>> _actionStatus = new MediatorLiveData<>();
    public LiveData<ResultWrapper<Void>> actionStatus = _actionStatus;

    private final Observer<ResultWrapper<Void>> voidOperationObserver;

    public InvitationViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        authRepository = AuthRepository.getInstance(); // Get instance of AuthRepository

        _invitationsState.setValue(new ResultWrapper.Idle<>());
        _actionStatus.setValue(new ResultWrapper.Idle<>());

        voidOperationObserver = voidResultWrapper -> {
            if (!(voidResultWrapper instanceof ResultWrapper.Loading)) {
                _actionStatus.setValue(voidResultWrapper);
                // REMOVED: _actionStatus.removeSource(eventHubRepository.voidOperationState); // THIS LINE IS PROBLEMATIC IN A LOOP
            } else {
                _actionStatus.setValue(new ResultWrapper.Loading<>());
            }
        };

        _invitationsState.addSource(eventHubRepository.userInvitationsState, resultWrapper -> {
            if (resultWrapper instanceof ResultWrapper.Success) {
                PaginatedResponse<InvitationModel> response = ((ResultWrapper.Success<PaginatedResponse<InvitationModel>>) resultWrapper).getData();
                if (response != null) {
                    _invitationsState.setValue(new ResultWrapper.Success<>(response.getContent()));
                } else {
                    _invitationsState.setValue(new ResultWrapper.Error<>("API response was empty."));
                }
            } else if (resultWrapper instanceof ResultWrapper.Error) {
                _invitationsState.setValue(new ResultWrapper.Error<>(((ResultWrapper.Error<?>) resultWrapper).getMessage()));
            } else if (resultWrapper instanceof ResultWrapper.Loading) {
                _invitationsState.setValue(new ResultWrapper.Loading<>());
            } else if (resultWrapper instanceof ResultWrapper.Idle) {
                _invitationsState.setValue(new ResultWrapper.Idle<>());
            }
        });

        // Add source to observe void operations only once in the constructor
        _actionStatus.addSource(eventHubRepository.voidOperationState, voidOperationObserver);
    }

    /**
     * Fetches the current user's invitations using their JWT for authentication.
     * @param page The page number to fetch.
     * @param size The number of items per page.
     */
    public void fetchInvitations(int page, int size) {
        String authToken = authRepository.getCurrentTokenSynchronous();
        if (authToken == null || authToken.isEmpty()) {
            _invitationsState.setValue(new ResultWrapper.Error<>("Authentication required to fetch invitations."));
            return;
        }

        // Only fetch if not already loading
        if (!(_invitationsState.getValue() instanceof ResultWrapper.Loading)) {
            eventHubRepository.fetchInvitationsForUser(authToken, page, size);
        }
    }

    // REMOVED setupActionObserver() as it causes re-adding/removing source in loop

    public void sendInvitation(Long eventId, Long recipientAccountId, String authTokenSender) {
        if (eventId == null || recipientAccountId == null || authTokenSender == null) {
            _actionStatus.setValue(new ResultWrapper.Error<>("Missing data for sending invitation."));
            return;
        }
        // REMOVED setupActionObserver();
        eventHubRepository.sendInvitation(eventId, recipientAccountId, authTokenSender);
    }

    public void acceptInvitation(Long invitationId, String authTokenActingUser) {
        performInvitationAction(invitationId, "ACCEPTED", authTokenActingUser);
    }

    public void declineInvitation(Long invitationId, String authTokenActingUser) {
        performInvitationAction(invitationId, "DECLINED", authTokenActingUser);
    }

    public void cancelInvitation(Long invitationId, String authTokenActingUser) {
        performInvitationAction(invitationId, "REVOKED", authTokenActingUser);
    }

    private void performInvitationAction(Long invitationId, String status, String authTokenActingUser) {
        if (invitationId == null || authTokenActingUser == null) {
            _actionStatus.setValue(new ResultWrapper.Error<>("Missing ID or auth token for invitation action."));
            return;
        }
        _actionStatus.setValue(new ResultWrapper.Loading<>());
        // REMOVED setupActionObserver();
        eventHubRepository.updateInvitationStatus(invitationId, status, authTokenActingUser);
    }

    public void resetActionStatus() {
        // Since voidOperationObserver no longer removes source, no need to re-add it here.
        // Just set to Idle.
        _actionStatus.setValue(new ResultWrapper.Idle<>());
    }

    public void clearInvitationState() {
        _invitationsState.postValue(new ResultWrapper.Idle<>());
        resetActionStatus();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _invitationsState.removeSource(eventHubRepository.userInvitationsState);
        _actionStatus.removeSource(eventHubRepository.voidOperationState); // Ensure this is still removed when ViewModel is cleared
    }
}