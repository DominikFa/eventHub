package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.InvitationModel;
import com.example.event_hub.Model.ResultWrapper; // Ensure this import path is correct

import java.util.List;

public class InvitationViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private String currentObservingAccountId = null;

    // LiveData for the list of invitations for the current user
    private final MediatorLiveData<ResultWrapper<List<InvitationModel>>> _invitationsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<InvitationModel>>> invitationsState = _invitationsState;

    // LiveData for specific action statuses like accept/decline/cancel
    private final MediatorLiveData<ResultWrapper<Void>> _actionStatus = new MediatorLiveData<>();
    public LiveData<ResultWrapper<Void>> actionStatus = _actionStatus;

    // Observer for repository's void operation state (used for accept/decline/cancel)
    private Observer<ResultWrapper<Void>> voidOperationObserver;


    public InvitationViewModel() {
        eventHubRepository = EventHubRepository.getInstance();

        // Initialize the observer for void operations
        voidOperationObserver = new Observer<ResultWrapper<Void>>() {
            @Override
            public void onChanged(ResultWrapper<Void> voidResultWrapper) {
                // We only care about non-loading states for the action status
                if (!(voidResultWrapper instanceof ResultWrapper.Loading)) {
                    _actionStatus.setValue(voidResultWrapper);
                    // Important: Remove observer after receiving a terminal state (Success/Error)
                    // to prevent this action's status from being updated by subsequent unrelated void operations.
                    _actionStatus.removeSource(eventHubRepository.voidOperationState);
                } else {
                    // If it's loading, reflect that in the action status
                    _actionStatus.setValue(new ResultWrapper.Loading<>());
                }
            }
        };
    }

    /**
     * Fetches all invitations for a specific user (account).
     * The result is observed on invitationsState.
     * @param accountId The ID of the user whose invitations are to be fetched.
     */
    public void fetchInvitations(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            _invitationsState.setValue(new ResultWrapper.Error<>("Account ID is missing for fetching invitations."));
            return;
        }

        // If we are already observing for a different user or a new request for the same user,
        // remove the old source to prevent unwanted updates or duplicate observers.
        if (currentObservingAccountId != null) {
            _invitationsState.removeSource(eventHubRepository.userInvitationsState);
        }
        currentObservingAccountId = accountId;

        _invitationsState.setValue(new ResultWrapper.Loading<>()); // Show loading state
        System.out.println("InvitationViewModel: Fetching invitations for accountId: " + accountId);

        _invitationsState.addSource(eventHubRepository.userInvitationsState, invitationListWrapper -> {
            // This observer will be called whenever the repository's userInvitationsState changes.
            // We assume fetchInvitationsForUser in repo updates this specific LiveData.
            _invitationsState.setValue(invitationListWrapper);
        });
        eventHubRepository.fetchInvitationsForUser(accountId); // Trigger the fetch in the repository
    }

    /**
     * Accepts an invitation.
     * @param invitationId The ID of the invitation to accept.
     */
    public void acceptInvitation(String invitationId) {
        performInvitationAction(invitationId, "accepted", "Accepting invitation: ");
    }

    /**
     * Declines an invitation.
     * @param invitationId The ID of the invitation to decline.
     */
    public void declineInvitation(String invitationId) {
        performInvitationAction(invitationId, "declined", "Declining invitation: ");
    }

    /**
     * Cancels an invitation (typically performed by the sender/organizer, but simulated here).
     * @param invitationId The ID of the invitation to cancel.
     */
    public void cancelInvitation(String invitationId) {
        performInvitationAction(invitationId, "revoked", "Canceling invitation: ");
    }

    private void performInvitationAction(String invitationId, String status, String logPrefix) {
        if (invitationId == null || invitationId.isEmpty()) {
            _actionStatus.setValue(new ResultWrapper.Error<>("Invitation ID is missing for action."));
            return;
        }
        if (currentObservingAccountId == null) {
            _actionStatus.setValue(new ResultWrapper.Error<>("User context (account ID) not set. Fetch invitations first."));
            return;
        }

        System.out.println("InvitationViewModel: " + logPrefix + invitationId);
        _actionStatus.setValue(new ResultWrapper.Loading<>()); // Indicate loading for this specific action

        // Remove any existing source from previous action before adding a new one
        _actionStatus.removeSource(eventHubRepository.voidOperationState);
        // Observe the repository's voidOperationState for the result of this action
        _actionStatus.addSource(eventHubRepository.voidOperationState, voidOperationObserver);

        eventHubRepository.updateInvitationStatus(invitationId, status, currentObservingAccountId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up observers to prevent memory leaks
        if (currentObservingAccountId != null) { // Check if source was potentially added
            _invitationsState.removeSource(eventHubRepository.userInvitationsState);
        }
        _actionStatus.removeSource(eventHubRepository.voidOperationState);
        currentObservingAccountId = null;
        System.out.println("InvitationViewModel: Cleared.");
    }
}