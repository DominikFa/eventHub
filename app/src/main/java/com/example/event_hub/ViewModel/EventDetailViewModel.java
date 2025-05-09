package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.Model.ResultWrapper;

import java.util.List;

public class EventDetailViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    public final ParticipantViewModel participantViewModel;
    public final MediaViewModel mediaViewModel;

    private String currentEventId;
    private boolean lastActionWasDeleteEvent = false; // To track delete event success

    private final MediatorLiveData<ResultWrapper<EventModel>> _eventDetailState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<EventModel>> eventDetailState = _eventDetailState;

    public LiveData<ResultWrapper<List<UserModel>>> participantsState;
    public LiveData<ResultWrapper<List<MediaModel>>> mediaListState;
    public LiveData<ResultWrapper<Void>> participantActionStatus;
    public LiveData<ResultWrapper<MediaModel>> mediaUploadOperationState;
    public LiveData<ResultWrapper<Void>> mediaDeleteOperationState;
    public LiveData<ResultWrapper<Void>> eventActionState; // For Join/Leave/Delete Event

    public EventDetailViewModel() {
        this.eventHubRepository = EventHubRepository.getInstance();
        this.participantViewModel = new ParticipantViewModel();
        this.mediaViewModel = new MediaViewModel();

        this.participantsState = participantViewModel.participantsState;
        this.mediaListState = mediaViewModel.mediaListState;
        this.participantActionStatus = participantViewModel.participantActionStatus;
        this.mediaUploadOperationState = mediaViewModel.mediaUploadOperationState;
        this.mediaDeleteOperationState = mediaViewModel.mediaDeleteOperationState; // Directly from repo
        this.eventActionState = eventHubRepository.voidOperationState; // For join/leave/delete event

        _eventDetailState.addSource(eventHubRepository.singleEventOperationState, resultWrapper -> {
            if (resultWrapper instanceof ResultWrapper.Success) {
                EventModel event = ((ResultWrapper.Success<EventModel>) resultWrapper).getData();
                if (event != null && event.getId().equals(currentEventId)) {
                    _eventDetailState.setValue(resultWrapper);
                }
            } else if (!(resultWrapper instanceof ResultWrapper.Idle)) { // Propagate Loading or Error
                _eventDetailState.setValue(resultWrapper);
            }
        });
    }

    public void loadEventAllDetails(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _eventDetailState.setValue(new ResultWrapper.Error<>("Event ID is missing."));
            participantViewModel.clearState(); // Clear child states
            mediaViewModel.clearState();
            return;
        }
        this.currentEventId = eventId;
        lastActionWasDeleteEvent = false; // Reset flag on new load

        _eventDetailState.setValue(new ResultWrapper.Loading<>());
        eventHubRepository.fetchEventDetails(eventId);
        participantViewModel.loadParticipants(eventId);
        mediaViewModel.loadMediaForEvent(eventId);
    }

    public void joinCurrentEvent(String authToken) { // authToken
        if (currentEventId == null || authToken == null) { return; }
        eventHubRepository.joinPublicEvent(currentEventId, authToken);
    }

    public void leaveCurrentEvent(String authToken) { // authToken
        if (currentEventId == null || authToken == null) { return; }
        eventHubRepository.leaveEvent(currentEventId, authToken);
    }

    public void deleteCurrentEvent(String authToken) { // New method, uses authToken
        if (currentEventId == null || authToken == null) {
            // Post to eventActionState if it were mutable and specific for delete ops
            System.err.println("EventDetailViewModel: Event ID or Auth Token missing for delete event.");
            return;
        }
        lastActionWasDeleteEvent = true; // Set flag
        eventHubRepository.deleteEvent(currentEventId, authToken);
    }

    // Helper for UI to check if navigation is needed after successful delete
    public boolean wasLastActionDeleteSuccess(ResultWrapper<Void> result) {
        boolean success = lastActionWasDeleteEvent && (result instanceof ResultWrapper.Success);
        if (!(result instanceof ResultWrapper.Loading)) { // Reset flag once result is processed
            lastActionWasDeleteEvent = false;
        }
        return success;
    }


    public void deleteParticipant(String participantAccountId, String authToken) { // authToken
        if (currentEventId == null || participantAccountId == null || authToken == null) { return; }
        participantViewModel.deleteParticipant(currentEventId, participantAccountId, authToken);
    }

    public void viewParticipantProfile(String participantAccountId) {
        participantViewModel.viewParticipantProfile(participantAccountId);
    }

    public void uploadMediaToCurrentEvent(MediaModel mediaToUpload, String authToken) { // authToken
        if (currentEventId == null || mediaToUpload == null || authToken == null) { return; }
        mediaToUpload.setEventId(currentEventId);
        mediaViewModel.uploadMedia(mediaToUpload, authToken);
    }

    public void deleteMediaFromCurrentEvent(String mediaId, String authToken) { // authToken
        if (currentEventId == null || mediaId == null || authToken == null) { return; }
        mediaViewModel.deleteMedia(mediaId, authToken, currentEventId); // Pass eventId for refresh context
    }

    public LiveData<String> getNavigateToParticipantProfileId() {
        return participantViewModel.navigateToParticipantProfileId;
    }

    public void clearStates() {
        currentEventId = null;
        lastActionWasDeleteEvent = false;
        _eventDetailState.setValue(new ResultWrapper.Idle<>());
        participantViewModel.clearState();
        mediaViewModel.clearState();
        // eventHubRepository.resetSingleEventOperationState(); // If this shared state needs reset too
        // eventHubRepository.resetVoidOperationState(); // If this shared state needs reset too
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _eventDetailState.removeSource(eventHubRepository.singleEventOperationState);
    }
}