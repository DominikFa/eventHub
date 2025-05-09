package com.example.event_hub.ViewModel; // Corrected package name in thought process, should be ViewModel

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
// import androidx.lifecycle.MutableLiveData; // Not directly used for new states here
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.Model.ResultWrapper; // Corrected import path

import java.util.List;

public class EventDetailViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    public final ParticipantViewModel participantViewModel; // Made public for easier access from fragment if needed
    public final MediaViewModel mediaViewModel; // Made public

    private String currentEventId;

    private final MediatorLiveData<ResultWrapper<EventModel>> _eventDetailState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<EventModel>> eventDetailState = _eventDetailState;

    public LiveData<ResultWrapper<List<UserModel>>> participantsState;
    public LiveData<ResultWrapper<List<MediaModel>>> mediaListState;
    public LiveData<ResultWrapper<Void>> participantActionStatus;
    public LiveData<ResultWrapper<MediaModel>> mediaUploadOperationState;
    public LiveData<ResultWrapper<Void>> mediaDeleteOperationState;
    public LiveData<ResultWrapper<Void>> eventActionState; // Renamed from joinEventOperationState for join/leave


    public EventDetailViewModel() {
        this.eventHubRepository = EventHubRepository.getInstance();
        this.participantViewModel = new ParticipantViewModel(); // Instantiating child VMs
        this.mediaViewModel = new MediaViewModel();

        this.participantsState = participantViewModel.participantsState;
        this.mediaListState = mediaViewModel.mediaListState;
        this.participantActionStatus = participantViewModel.participantActionStatus;
        this.mediaUploadOperationState = mediaViewModel.mediaUploadOperationState;

        // Assuming media delete also uses the generic voidOperationState from repo,
        // if MediaViewModel doesn't have its own specific one.
        // If MediaViewModel is updated to have its own, point to that.
        this.mediaDeleteOperationState = eventHubRepository.voidOperationState; // Or mediaViewModel.deleteActionState

        _eventDetailState.addSource(eventHubRepository.singleEventOperationState, eventModelResultWrapper -> {
            if (eventModelResultWrapper instanceof ResultWrapper.Success) {
                EventModel event = ((ResultWrapper.Success<EventModel>) eventModelResultWrapper).getData();
                if (event != null && event.getId().equals(currentEventId)) {
                    _eventDetailState.setValue(eventModelResultWrapper);
                }
            } else {
                _eventDetailState.setValue(eventModelResultWrapper);
            }
        });

        this.eventActionState = eventHubRepository.voidOperationState;
    }

    public void loadEventAllDetails(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _eventDetailState.setValue(new ResultWrapper.Error<>("Event ID is missing."));
            participantViewModel.loadParticipants(null);
            mediaViewModel.loadMediaForEvent(null);
            return;
        }
        this.currentEventId = eventId;

        _eventDetailState.setValue(new ResultWrapper.Loading<>());
        System.out.println("EventDetailViewModel: Loading details for eventId: " + eventId);
        eventHubRepository.fetchEventDetails(eventId);

        System.out.println("EventDetailViewModel: Triggering participant load for eventId: " + eventId);
        participantViewModel.loadParticipants(eventId);

        System.out.println("EventDetailViewModel: Triggering media load for eventId: " + eventId);
        mediaViewModel.loadMediaForEvent(eventId);
    }

    public void joinCurrentEvent(String userId) {
        if (currentEventId == null || currentEventId.isEmpty()) {
            System.err.println("EventDetailViewModel: Cannot join event, event ID not set.");
            // Update eventActionState with error if needed, though repo will also do it.
            // ((MutableLiveData<ResultWrapper<Void>>)eventActionState).setValue(new ResultWrapper.Error<>("Event ID not set"));
            return;
        }
        if (userId == null || userId.isEmpty()) {
            System.err.println("EventDetailViewModel: Cannot join event, user ID not set.");
            return;
        }
        System.out.println("EventDetailViewModel: User " + userId + " attempting to join event " + currentEventId);
        eventHubRepository.joinPublicEvent(currentEventId, userId);
        // Participant list will refresh due to repo's fetchEventParticipants call in joinPublicEvent
    }

    /**
     * Allows the current user to leave the event being detailed.
     * @param userId The ID of the user leaving the event.
     */
    public void leaveCurrentEvent(String userId) {
        if (currentEventId == null || currentEventId.isEmpty()) {
            System.err.println("EventDetailViewModel: Cannot leave event, event ID not set.");
            // ((MutableLiveData<ResultWrapper<Void>>)eventActionState).setValue(new ResultWrapper.Error<>("Event ID not set"));
            return;
        }
        if (userId == null || userId.isEmpty()) {
            System.err.println("EventDetailViewModel: Cannot leave event, user ID not set.");
            return;
        }
        System.out.println("EventDetailViewModel: User " + userId + " attempting to leave event " + currentEventId);
        eventHubRepository.leaveEvent(currentEventId, userId);
        // Participant list will refresh due to repo's fetchEventParticipants call in leaveEvent
    }

    // --- Delegate methods to child ViewModels ---
    public void deleteParticipant(String participantAccountId) {
        if (currentEventId == null) {
            System.err.println("EventDetailViewModel: Cannot delete participant, event ID not set.");
            return;
        }
        // ParticipantViewModel's deleteParticipant takes eventId and participantAccountId
        // However, our ParticipantViewModel's deleteParticipant method was simplified.
        // Let's assume it knows its currentEventId context or we pass it.
        // For now, directly calling repo for simplicity as child VM might not have eventId context easily.
        // Or, ensure child VMs are initialized with eventId.
        // participantViewModel.deleteParticipant(currentEventId, participantAccountId);
        // For this refactor, let's assume EventDetailViewModel handles actions requiring eventId directly or via specific methods.
        eventHubRepository.deleteParticipant(currentEventId, participantAccountId); // Example direct call
    }

    public void viewParticipantProfile(String participantAccountId) {
        participantViewModel.viewParticipantProfile(participantAccountId); // This just triggers navigation
    }

    public void uploadMediaToCurrentEvent(MediaModel mediaToUpload) {
        if (currentEventId == null) {
            System.err.println("EventDetailViewModel: Event context not set for media upload.");
            return;
        }
        mediaToUpload.setEventId(currentEventId); // Ensure eventId is set on the model
        mediaViewModel.uploadMedia(mediaToUpload);
    }

    public void deleteMediaFromCurrentEvent(String mediaId) {
        if (currentEventId == null) {
            System.err.println("EventDetailViewModel: Event context not set for media deletion.");
            return;
        }
        mediaViewModel.deleteMedia(mediaId); // MediaViewModel's deleteMedia needs to know eventId for refresh
        // The current MediaViewModel.deleteMedia takes mediaId only
        // and relies on its currentObservingEventIdForMedia.
        // This should be fine if loadMediaForEvent was called.
    }

    public LiveData<String> getNavigateToParticipantProfileId() {
        return participantViewModel.navigateToParticipantProfileId;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        _eventDetailState.removeSource(eventHubRepository.singleEventOperationState);
        // Child ViewModels' onCleared will be called if they are AndroidX ViewModels
        // and managed by a ViewModelProvider with the correct scope.
        participantViewModel.onCleared(); // Manually call if not managed by provider with this VM
        mediaViewModel.onCleared();       // Manually call
        System.out.println("EventDetailViewModel: Cleared.");
    }
}
