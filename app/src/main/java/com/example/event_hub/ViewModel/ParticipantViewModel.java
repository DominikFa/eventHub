package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.ResultWrapper; // Ensure this import path is correct

import java.util.List;

public class ParticipantViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private String currentObservingEventIdForParticipants = null;

    // LiveData for the list of participants for the currently loaded event
    private final MediatorLiveData<ResultWrapper<List<UserModel>>> _participantsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<UserModel>>> participantsState = _participantsState;

    // LiveData for reporting the status of operations like deleting a participant
    private final MediatorLiveData<ResultWrapper<Void>> _participantActionStatus = new MediatorLiveData<>();
    public LiveData<ResultWrapper<Void>> participantActionStatus = _participantActionStatus;

    // LiveData to trigger navigation or display of a selected participant's profile
    // This remains a simple trigger; the actual profile data fetching is in ProfileViewModel
    private final MutableLiveData<String> _navigateToParticipantProfileId = new MutableLiveData<>();
    public LiveData<String> navigateToParticipantProfileId = _navigateToParticipantProfileId;

    private Observer<ResultWrapper<Void>> voidOperationObserver;

    public ParticipantViewModel() {
        eventHubRepository = EventHubRepository.getInstance();

        // Observer for repository's general void operation state (used for deleteParticipant)
        voidOperationObserver = new Observer<ResultWrapper<Void>>() {
            @Override
            public void onChanged(ResultWrapper<Void> voidResultWrapper) {
                if (!(voidResultWrapper instanceof ResultWrapper.Loading)) {
                    _participantActionStatus.setValue(voidResultWrapper);
                    _participantActionStatus.removeSource(eventHubRepository.voidOperationState);
                } else {
                    _participantActionStatus.setValue(new ResultWrapper.Loading<>());
                }
            }
        };
    }

    /**
     * Fetches the list of participants for a given event ID.
     * The result is observed on participantsState.
     * @param eventId The ID of the event whose participants are to be fetched.
     */
    public void loadParticipants(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _participantsState.setValue(new ResultWrapper.Error<>("Event ID is missing for fetching participants."));
            return;
        }

        if (currentObservingEventIdForParticipants != null) {
            _participantsState.removeSource(eventHubRepository.eventParticipantsState);
        }
        currentObservingEventIdForParticipants = eventId;

        _participantsState.setValue(new ResultWrapper.Loading<>());
        System.out.println("ParticipantViewModel: Fetching participants for eventId: " + eventId);

        _participantsState.addSource(eventHubRepository.eventParticipantsState, userListWrapper -> {
            // Assuming eventParticipantsState in repo is updated by fetchEventParticipants(eventId)
            _participantsState.setValue(userListWrapper);
        });
        eventHubRepository.fetchEventParticipants(eventId);
    }

    /**
     * Triggers the display of a specific participant's profile.
     * Sets a LiveData value that the UI can observe to initiate navigation.
     * @param participantAccountId The account ID of the participant whose profile should be shown.
     */
    public void viewParticipantProfile(String participantAccountId) {
        if (participantAccountId == null || participantAccountId.isEmpty()) {
            System.err.println("ParticipantViewModel: Participant Account ID is missing for profile view.");
            // Optionally set an error state if this action has its own status LiveData
            return;
        }
        System.out.println("ParticipantViewModel: Requesting to show profile for accountId: " + participantAccountId);
        _navigateToParticipantProfileId.setValue(participantAccountId);
        _navigateToParticipantProfileId.setValue(null); // Reset after observation (single live event pattern)
    }

    /**
     * Removes a participant from the currently loaded event.
     * @param eventId The ID of the event.
     * @param participantAccountId The account ID of the participant to remove.
     */
    public void deleteParticipant(String eventId, String participantAccountId) {
        if (eventId == null || eventId.isEmpty() || participantAccountId == null || participantAccountId.isEmpty()) {
            _participantActionStatus.setValue(new ResultWrapper.Error<>("Event ID or Participant Account ID is missing for deletion."));
            return;
        }
        System.out.println("ParticipantViewModel: Attempting to delete participant " + participantAccountId + " from event " + eventId);
        _participantActionStatus.setValue(new ResultWrapper.Loading<>());

        _participantActionStatus.removeSource(eventHubRepository.voidOperationState); // Clean up previous if any
        _participantActionStatus.addSource(eventHubRepository.voidOperationState, voidOperationObserver);

        eventHubRepository.deleteParticipant(eventId, participantAccountId);
        // The participant list (_participantsState) will update automatically because
        // deleteParticipant in the repository calls fetchEventParticipants,
        // which updates the repository's _eventParticipantsState that this ViewModel observes.
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentObservingEventIdForParticipants != null) {
            _participantsState.removeSource(eventHubRepository.eventParticipantsState);
        }
        _participantActionStatus.removeSource(eventHubRepository.voidOperationState);
        currentObservingEventIdForParticipants = null;
        System.out.println("ParticipantViewModel: Cleared.");
    }
}