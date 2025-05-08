package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;


import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.UserModel; // Assuming UserModel might be needed for creator info
import com.example.event_hub.Model.ResultWrapper; // Ensure this import path is correct

import java.util.List;

public class MainViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;

    // LiveData for the list of public events
    private final MediatorLiveData<ResultWrapper<List<EventModel>>> _publicEventsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<EventModel>>> publicEventsState = _publicEventsState;

    // LiveData for the list of events attended by the current user
    // This needs a trigger, e.g., userId. Using switchMap for this.
    private final MutableLiveData<String> _userIdForAttendedEvents = new MutableLiveData<>();
    public LiveData<ResultWrapper<List<EventModel>>> attendedEventsState;


    // LiveData for the status of joining an event
    public LiveData<ResultWrapper<Void>> joinEventOperationState; // Observe directly from repo

    // LiveData for a single event when its details are requested or it's "selected"
    private final MediatorLiveData<ResultWrapper<EventModel>> _selectedEventState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<EventModel>> selectedEventState = _selectedEventState;

    private Observer<ResultWrapper<Void>> joinEventObserver;


    public MainViewModel() {
        eventHubRepository = EventHubRepository.getInstance();

        // Observe public events from repository
        _publicEventsState.addSource(eventHubRepository.publicEventsState, new Observer<ResultWrapper<List<EventModel>>>() {
            @Override
            public void onChanged(ResultWrapper<List<EventModel>> listResultWrapper) {
                _publicEventsState.setValue(listResultWrapper);
            }
        });

        // Setup attendedEventsState to react to _userIdForAttendedEvents changes
        attendedEventsState = Transformations.switchMap(_userIdForAttendedEvents, userId -> {
            if (userId == null || userId.isEmpty()) {
                MutableLiveData<ResultWrapper<List<EventModel>>> emptyResult = new MutableLiveData<>();
                emptyResult.setValue(new ResultWrapper.Error<>("User ID not provided for attended events."));
                return emptyResult;
            }
            // This call returns a new LiveData instance each time from the repo
            return eventHubRepository.fetchAttendedEvents(userId);
        });

        joinEventOperationState = eventHubRepository.voidOperationState; // For join event

        // Observer for the selected event details from the repository
        _selectedEventState.addSource(eventHubRepository.singleEventOperationState, new Observer<ResultWrapper<EventModel>>() {
            @Override
            public void onChanged(ResultWrapper<EventModel> eventModelResultWrapper) {
                // This will reflect the state of the last fetchEventDetails or createEvent call
                _selectedEventState.setValue(eventModelResultWrapper);
            }
        });
    }

    /**
     * Fetches all public events. Call this to initiate or refresh.
     */
    public void loadPublicEvents() {
        System.out.println("MainViewModel: Loading public events...");
        _publicEventsState.setValue(new ResultWrapper.Loading<>()); // Optionally show loading in VM immediately
        eventHubRepository.fetchPublicEvents();
    }

    /**
     * Fetches events attended by the current user.
     * @param userId The ID of the current user.
     */
    public void loadAttendedEvents(String userId) {
        if (userId == null || userId.isEmpty()) {
            // attendedEventsState will show error due to switchMap logic
            _userIdForAttendedEvents.setValue(null); // Trigger switchMap to handle error state
            System.out.println("MainViewModel: Cannot fetch attended events: User ID is missing.");
            return;
        }
        System.out.println("MainViewModel: Loading attended events for userId: " + userId);
        _userIdForAttendedEvents.setValue(userId); // This will trigger the switchMap
    }

    /**
     * Allows a user to join a public event.
     * @param eventId The ID of the event to join.
     * @param userId  The ID of the user joining the event.
     */
    public void joinPublicEvent(String eventId, String userId) {
        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            // Handle this error locally if joinEventOperationState isn't suitable,
            // or let the repo handle it if it checks parameters.
            // For now, let's assume repo checks.
            System.err.println("MainViewModel: Event ID or User ID is missing for join event.");
            // If you want to post to joinEventOperationState directly:
            // MutableLiveData<ResultWrapper<Void>> errorResult = new MutableLiveData<>();
            // errorResult.setValue(new ResultWrapper.Error<>("Event ID or User ID missing."));
            // joinEventOperationState = errorResult; // This replaces the repo's LiveData, be careful.
            // Better to have a dedicated _joinEventActionState in VM if such local validation is needed.
            return;
        }
        System.out.println("MainViewModel: User " + userId + " attempting to join event " + eventId);
        // UI will observe joinEventOperationState (which is eventHubRepository.voidOperationState)
        eventHubRepository.joinPublicEvent(eventId, userId);
        // After a successful join, the attended events list should be refreshed.
        // The repository's joinPublicEvent doesn't currently refresh attended events.
        // It might be better if it did, or the ViewModel can trigger it here on success.
        // For now, manual refresh might be needed by calling loadAttendedEvents(userId) on success from UI.
    }

    /**
     * Fetches details for a specific event.
     * The result is observed on selectedEventState.
     * @param eventId The ID of the event to fetch details for.
     */
    public void loadEventDetails(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _selectedEventState.setValue(new ResultWrapper.Error<>("Event ID is missing for fetching details."));
            return;
        }
        System.out.println("MainViewModel: Loading details for event " + eventId);
        _selectedEventState.setValue(new ResultWrapper.Loading<>()); // Show loading in VM immediately
        eventHubRepository.fetchEventDetails(eventId);
    }

    /**
     * Clears the selected event state, e.g., when navigating away from a detail view.
     */
    public void clearSelectedEvent() {
        _selectedEventState.setValue(new ResultWrapper.Idle<>());
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        _publicEventsState.removeSource(eventHubRepository.publicEventsState);
        _selectedEventState.removeSource(eventHubRepository.singleEventOperationState);
        // No need to remove source for attendedEventsState as switchMap handles lifecycle
        System.out.println("MainViewModel: Cleared.");
    }
}