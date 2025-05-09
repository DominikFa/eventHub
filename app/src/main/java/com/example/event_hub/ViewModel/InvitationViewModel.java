package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.InvitationModel;
import com.example.event_hub.Model.ResultWrapper;

import java.util.ArrayList;
import java.util.List;

public class InvitationViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private String currentObservingAccountId = null;
    private List<EventModel> eventCacheForTitles = new ArrayList<>();

    private final MediatorLiveData<ResultWrapper<List<InvitationModel>>> _invitationsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<InvitationModel>>> invitationsState = _invitationsState;

    private final MediatorLiveData<ResultWrapper<Void>> _actionStatus = new MediatorLiveData<>();
    public LiveData<ResultWrapper<Void>> actionStatus = _actionStatus;

    private Observer<ResultWrapper<Void>> voidOperationObserver;

    public InvitationViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        _invitationsState.setValue(new ResultWrapper.Idle<>());
        _actionStatus.setValue(new ResultWrapper.Idle<>());

        voidOperationObserver = voidResultWrapper -> {
            // This observer is active only when eventHubRepository.voidOperationState is a source.
            if (!(voidResultWrapper instanceof ResultWrapper.Loading)) {
                _actionStatus.setValue(voidResultWrapper);
                // Remove the source after processing a terminal event (Success/Error)
                // to ensure this observer instance doesn't react to future, unrelated void operations.
                _actionStatus.removeSource(eventHubRepository.voidOperationState);
            } else {
                // Propagate loading state
                _actionStatus.setValue(new ResultWrapper.Loading<>());
            }
        };
    }

    public void fetchInvitations(String accountId, List<EventModel> eventListCache) {
        if (accountId == null || accountId.isEmpty()) {
            _invitationsState.setValue(new ResultWrapper.Error<>("Account ID is missing for fetching invitations."));
            currentObservingAccountId = null; // Reset if ID becomes invalid
            _invitationsState.removeSource(eventHubRepository.userInvitationsState); // Clean up previous source
            return;
        }
        this.eventCacheForTitles = eventListCache != null ? new ArrayList<>(eventListCache) : new ArrayList<>();

        // If observing a different account, or if not currently observing, set up the source.
        if (currentObservingAccountId == null || !currentObservingAccountId.equals(accountId)) {
            _invitationsState.removeSource(eventHubRepository.userInvitationsState); // Remove previous source
            currentObservingAccountId = accountId;
            _invitationsState.setValue(new ResultWrapper.Loading<>()); // Set loading for new fetch
            _invitationsState.addSource(eventHubRepository.userInvitationsState, resultWrapper -> {
                // We expect userInvitationsState to be specific to the fetched user,
                // so direct setValue is fine.
                _invitationsState.setValue(resultWrapper);
            });
            eventHubRepository.fetchInvitationsForUser(accountId);
        } else if (!(_invitationsState.getValue() instanceof ResultWrapper.Loading)) {
            // If same accountId but not currently loading (e.g., re-fetch explicitly called), set loading.
            _invitationsState.setValue(new ResultWrapper.Loading<>());
            eventHubRepository.fetchInvitationsForUser(accountId); // Trigger fetch again
        }
        // If it's the same accountId and already loading, do nothing more.
    }


    private void setupActionObserver() {
        // Ensure any previous instance of this specific observer on voidOperationState is removed
        // before adding a new one. This prevents _actionStatus from having voidOperationState
        // as a source multiple times through different instances of voidOperationObserver (if it were re-assigned).
        // However, voidOperationObserver is a single instance here.
        // The critical part is that _actionStatus.removeSource is called when the operation completes.
        _actionStatus.removeSource(eventHubRepository.voidOperationState); // Clean up before adding
        _actionStatus.addSource(eventHubRepository.voidOperationState, voidOperationObserver);
    }

    public void sendInvitation(String eventId, String recipientAccountId, String authTokenSender) {
        if (eventId == null || recipientAccountId == null || authTokenSender == null) {
            _actionStatus.setValue(new ResultWrapper.Error<>("Missing data for sending invitation."));
            return;
        }
        _actionStatus.setValue(new ResultWrapper.Loading<>());
        setupActionObserver();
        eventHubRepository.sendInvitation(eventId, recipientAccountId, authTokenSender);
    }

    public void acceptInvitation(String invitationId, String authTokenActingUser) {
        performInvitationAction(invitationId, "accepted", authTokenActingUser);
    }

    public void declineInvitation(String invitationId, String authTokenActingUser) {
        performInvitationAction(invitationId, "declined", authTokenActingUser);
    }

    public void cancelInvitation(String invitationId, String authTokenActingUser) {
        performInvitationAction(invitationId, "revoked", authTokenActingUser);
    }

    private void performInvitationAction(String invitationId, String status, String authTokenActingUser) {
        if (invitationId == null || authTokenActingUser == null) {
            _actionStatus.setValue(new ResultWrapper.Error<>("Missing ID or auth token for invitation action."));
            return;
        }
        _actionStatus.setValue(new ResultWrapper.Loading<>());
        setupActionObserver();
        eventHubRepository.updateInvitationStatus(invitationId, status, authTokenActingUser);
    }

    public List<EventModel> getEventCacheForTitles() { return eventCacheForTitles; }

    public void resetActionStatus() {
        _actionStatus.removeSource(eventHubRepository.voidOperationState);
        _actionStatus.setValue(new ResultWrapper.Idle<>());
    }

    public void clearInvitationState() {
        if (currentObservingAccountId != null) {
            _invitationsState.removeSource(eventHubRepository.userInvitationsState);
        }
        _invitationsState.setValue(new ResultWrapper.Idle<>());
        resetActionStatus();
        currentObservingAccountId = null;
        eventCacheForTitles.clear();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _invitationsState.removeSource(eventHubRepository.userInvitationsState);
        _actionStatus.removeSource(eventHubRepository.voidOperationState);
    }
}