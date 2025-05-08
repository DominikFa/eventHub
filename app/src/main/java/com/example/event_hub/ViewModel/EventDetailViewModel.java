package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.Model.ResultWrapper; // Ensure this import path is correct

import java.util.List;

public class EventDetailViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;

    // Child ViewModels for managing related data.
    // These would ideally be injected or provided by a ViewModelProvider.Factory.
    // For this example, we'll instantiate them directly, assuming no complex constructor args for them.
    private final ParticipantViewModel participantViewModel;
    private final MediaViewModel mediaViewModel;

    private String currentEventId;

    // LiveData for the main event details
    private final MediatorLiveData<ResultWrapper<EventModel>> _eventDetailState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<EventModel>> eventDetailState = _eventDetailState;

    // Expose LiveData from child ViewModels
    public LiveData<ResultWrapper<List<UserModel>>> participantsState;
    public LiveData<ResultWrapper<List<MediaModel>>> mediaListState;
    public LiveData<ResultWrapper<Void>> participantActionStatus; // From ParticipantViewModel
    public LiveData<ResultWrapper<MediaModel>> mediaUploadOperationState; // From MediaViewModel
    public LiveData<ResultWrapper<Void>> mediaDeleteOperationState; // From MediaViewModel
    public LiveData<ResultWrapper<Void>> joinEventOperationState; // For join/leave actions on this event


    public EventDetailViewModel() {
        this.eventHubRepository = EventHubRepository.getInstance();

        // Initialize child ViewModels
        // In a real app with Hilt or a ViewModelFactory, these would be injected.
        this.participantViewModel = new ParticipantViewModel();
        this.mediaViewModel = new MediaViewModel();

        // Expose LiveData from child ViewModels
        this.participantsState = participantViewModel.participantsState;
        this.mediaListState = mediaViewModel.mediaListState;
        this.participantActionStatus = participantViewModel.participantActionStatus;
        this.mediaUploadOperationState = mediaViewModel.mediaUploadOperationState;
        this.mediaDeleteOperationState = mediaViewModel.mediaDeleteOperationState; // Assuming this exists on MediaVM

        // Observe the repository's state for a single event operation (used by fetchEventDetails)
        _eventDetailState.addSource(eventHubRepository.singleEventOperationState, eventModelResultWrapper -> {
            if (eventModelResultWrapper instanceof ResultWrapper.Success) {
                EventModel event = ((ResultWrapper.Success<EventModel>) eventModelResultWrapper).getData();
                // Ensure this update is for the event this ViewModel is currently focused on.
                if (event != null && event.getId().equals(currentEventId)) {
                    _eventDetailState.setValue(eventModelResultWrapper);
                } else if (event == null && !(eventModelResultWrapper instanceof ResultWrapper.Loading)) {
                    // If data is null and not loading, could be an error or irrelevant update.
                }
            } else { // Pass through Loading or Error states
                _eventDetailState.setValue(eventModelResultWrapper);
            }
        });

        // LiveData for join/leave actions specifically for the event being detailed
        this.joinEventOperationState = eventHubRepository.voidOperationState;
    }

    /**
     * Loads all necessary data for the event detail screen.
     * @param eventId The ID of the event to load.
     */
    public void loadEventAllDetails(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _eventDetailState.setValue(new ResultWrapper.Error<>("Event ID is missing."));
            // Also set error states for participant and media lists if desired
            participantViewModel.loadParticipants(null); // Will set its own error state
            mediaViewModel.loadMediaForEvent(null);   // Will set its own error state
            return;
        }
        this.currentEventId = eventId;

        // 1. Load main event details
        _eventDetailState.setValue(new ResultWrapper.Loading<>());
        System.out.println("EventDetailViewModel: Loading details for eventId: " + eventId);
        eventHubRepository.fetchEventDetails(eventId);

        // 2. Load participants for this event using ParticipantViewModel
        System.out.println("EventDetailViewModel: Triggering participant load for eventId: " + eventId);
        participantViewModel.loadParticipants(eventId);

        // 3. Load media for this event using MediaViewModel
        System.out.println("EventDetailViewModel: Triggering media load for eventId: " + eventId);
        mediaViewModel.loadMediaForEvent(eventId);
    }

    /**
     * Allows the current user to join the event being detailed.
     * @param userId The ID of the user joining the event.
     */
    public void joinCurrentEvent(String userId) {
        if (currentEventId == null || currentEventId.isEmpty()) {
            System.err.println("EventDetailViewModel: Cannot join event, event ID not set.");
            // Optionally update joinEventOperationState with an error
            return;
        }
        if (userId == null || userId.isEmpty()) {
            System.err.println("EventDetailViewModel: Cannot join event, user ID not set.");
            return;
        }
        System.out.println("EventDetailViewModel: User " + userId + " attempting to join event " + currentEventId);
        eventHubRepository.joinPublicEvent(currentEventId, userId);
        // After join, participant list might need refresh.
        // The repo's joinPublicEvent doesn't auto-refresh participant list.
        // So, after successful join, we might need to trigger:
        // participantViewModel.loadParticipants(currentEventId);
        // This can be done by observing joinEventOperationState for success.
    }


    // --- Delegate methods to child ViewModels (or UI can access them directly) ---

    public void deleteParticipant(String participantAccountId) {
        if (currentEventId == null) return;
        participantViewModel.deleteParticipant(currentEventId, participantAccountId);
    }

    public void viewParticipantProfile(String participantAccountId) {
        participantViewModel.viewParticipantProfile(participantAccountId);
    }

    public void uploadMediaToCurrentEvent(MediaModel mediaToUpload) {
        if (currentEventId == null) {
            System.err.println("EventDetailViewModel: Event context not set for media upload.");
            // Update mediaUploadOperationState with an error if it's used for pre-validation
            // This requires MediaViewModel to expose a way to set such an error, or this VM uses its own status.
            return;
        }
        // mediaViewModel will use its currentObservingEventId, which should be set by loadEventAllDetails
        mediaViewModel.uploadMedia(mediaToUpload);
    }

    public void deleteMediaFromCurrentEvent(String mediaId) {
        if (currentEventId == null) return;
        // mediaViewModel will use its currentObservingEventId
        mediaViewModel.deleteMedia(mediaId);
    }

    public LiveData<String> getNavigateToParticipantProfileId() {
        return participantViewModel.navigateToParticipantProfileId;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        _eventDetailState.removeSource(eventHubRepository.singleEventOperationState);
        // Child ViewModels will handle their own onCleared if they are true Android ViewModels
        // and managed by a ViewModelProvider. If instantiated directly like here,
        // their onCleared might not be called automatically unless this VM calls them.
        // For simplicity, we assume they manage their observers.
        System.out.println("EventDetailViewModel: Cleared.");
    }
}