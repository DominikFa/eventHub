// src/main/java/com/example/event_hub/ViewModel/ParticipantViewModel.java
package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.ParticipantModel;

public class ParticipantViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    // Stores the ID of the event whose participants are currently being observed/loaded.
    private Long currentObservingEventIdForParticipants = null;

    // LiveData holding the state of fetching participants for an event.
    private final MediatorLiveData<ResultWrapper<PaginatedResponse<ParticipantModel>>> _participantsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<PaginatedResponse<ParticipantModel>>> participantsState = _participantsState;

    // LiveData holding the status of actions related to participants (e.g., delete).
    private final MediatorLiveData<ResultWrapper<Void>> _participantActionStatus = new MediatorLiveData<>();
    public LiveData<ResultWrapper<Void>> participantActionStatus = _participantActionStatus;

    // LiveData for triggering navigation to a participant's profile.
    private final MutableLiveData<Long> _navigateToParticipantProfileId = new MutableLiveData<>();
    public LiveData<Long> navigateToParticipantProfileId = _navigateToParticipantProfileId;

    // Observer for void operations from the repository.
    private final Observer<ResultWrapper<Void>> voidOperationObserver;

    public ParticipantViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        // Initialize LiveData states to Idle
        _participantsState.setValue(new ResultWrapper.Idle<>());
        _participantActionStatus.setValue(new ResultWrapper.Idle<>());

        // Define the observer for void operations from the repository.
        // It passes the status to the ViewModel's actionStatus LiveData.
        voidOperationObserver = voidResultWrapper -> {
            if (!(voidResultWrapper instanceof ResultWrapper.Loading)) {
                _participantActionStatus.setValue(voidResultWrapper);
                // Important: Remove source after a non-loading result to avoid multiple updates
                // if the same operation state is observed by multiple components.
                _participantActionStatus.removeSource(eventHubRepository.voidOperationState);
            } else {
                _participantActionStatus.setValue(new ResultWrapper.Loading<>());
            }
        };

        // Add source to mediate participant state from the repository
        _participantsState.addSource(eventHubRepository.eventParticipantsState, _participantsState::setValue);
    }

    /**
     * Loads the list of participants for a given event.
     * Prevents redundant API calls if the same event's participants are already loading or loaded.
     * @param eventId The ID of the event to load participants for.
     */
    public void loadParticipants(Long eventId) {
        if (eventId == null) {
            _participantsState.setValue(new ResultWrapper.Error<>("Event ID is missing."));
            currentObservingEventIdForParticipants = null;
            return;
        }
        // Only trigger a new load if the eventId is different or if not currently loading
        if (!eventId.equals(currentObservingEventIdForParticipants) || !(_participantsState.getValue() instanceof ResultWrapper.Loading)) {
            _participantsState.setValue(new ResultWrapper.Loading<>()); // Indicate loading started
        }
        currentObservingEventIdForParticipants = eventId; // Update the currently observed event ID
        // Delegate to the repository to fetch participants with default pagination
        eventHubRepository.fetchEventParticipants(eventId, 0, 50);
    }

    /**
     * Triggers navigation to a participant's profile screen.
     * @param participantAccountId The ID of the participant whose profile should be viewed.
     */
    public void viewParticipantProfile(Long participantAccountId) {
        _navigateToParticipantProfileId.setValue(participantAccountId);
        // Post null immediately after consuming to ensure subsequent navigations are triggered
        _navigateToParticipantProfileId.postValue(null);
    }

    /**
     * Sends a request to delete (cancel status for) a participant from an event.
     * @param eventId The ID of the event.
     * @param participantAccountId The ID of the participant to delete.
     * @param authToken The authentication token of the user performing the action.
     */
    public void deleteParticipant(Long eventId, Long participantAccountId, String authToken) {
        if (eventId == null || participantAccountId == null || authToken == null) {
            _participantActionStatus.setValue(new ResultWrapper.Error<>("Missing info for participant deletion."));
            return;
        }
        _participantActionStatus.setValue(new ResultWrapper.Loading<>());
        // Re-add source for voidOperationState to ensure it observes the current operation.
        _participantActionStatus.removeSource(eventHubRepository.voidOperationState); // Remove previous source first
        _participantActionStatus.addSource(eventHubRepository.voidOperationState, voidOperationObserver);
        // Call repository to update participant status to "cancelled"
        eventHubRepository.updateParticipantStatus(eventId, participantAccountId, "cancelled", authToken);
    }

    /**
     * Clears all LiveData states and resets internal flags for a clean state.
     * This should be called when the associated UI component (Fragment) is destroyed.
     */
    public void clearState() {
        currentObservingEventIdForParticipants = null;
        _participantsState.postValue(new ResultWrapper.Idle<>());
        _participantActionStatus.postValue(new ResultWrapper.Idle<>());
        _navigateToParticipantProfileId.postValue(null);
        // Ensure to remove sources to prevent memory leaks and unnecessary updates
        _participantsState.removeSource(eventHubRepository.eventParticipantsState);
        _participantActionStatus.removeSource(eventHubRepository.voidOperationState);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove sources when ViewModel is cleared to prevent memory leaks
        _participantsState.removeSource(eventHubRepository.eventParticipantsState);
        _participantActionStatus.removeSource(eventHubRepository.voidOperationState);
    }
}