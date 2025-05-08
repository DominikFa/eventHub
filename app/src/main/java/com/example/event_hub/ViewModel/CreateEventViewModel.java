package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.ResultWrapper; // Ensure this import path is correct

import java.util.Date;
import java.util.UUID;

public class CreateEventViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;

    // LiveData to hold the event data being input by the user for creation.
    private final MutableLiveData<EventModel> _eventFormData = new MutableLiveData<>();
    public LiveData<EventModel> eventFormData = _eventFormData;

    // LiveData for the creator's information
    private final MutableLiveData<UserModel> _creatorInfo = new MutableLiveData<>();
    public LiveData<UserModel> creatorInfo = _creatorInfo;

    // LiveData for the state of the create event operation
    // This will observe the repository's state for event creation
    public LiveData<ResultWrapper<EventModel>> createEventOperationState;


    public CreateEventViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        _eventFormData.setValue(new EventModel()); // Initialize with an empty event object

        // Observe the repository's state for a single event operation (used by createEvent)
        createEventOperationState = eventHubRepository.singleEventOperationState;
    }

    /**
     * Sets the user who is creating the event.
     * This would typically be called after the user is authenticated.
     * @param user The UserModel of the event creator.
     */
    public void setCreator(UserModel user) {
        _creatorInfo.setValue(user);
        EventModel currentEventData = _eventFormData.getValue();
        if (currentEventData != null && user != null) {
            currentEventData.setCreatedBy(user.getUserId());
            _eventFormData.setValue(currentEventData); // Notify observers about the change in form data
        }
    }

    /**
     * Updates the details of the event being created based on form input.
     */
    public void updateEventDetails(String title, String description, String location,
                                   Date startDate, Date endDate, int maxParticipants) {
        EventModel event = _eventFormData.getValue();
        if (event == null) {
            event = new EventModel(); // Should ideally not happen if initialized
        }
        event.setTitle(title);
        event.setDescription(description);
        event.setLocation(location);
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setMaxParticipants(maxParticipants);

        UserModel creator = _creatorInfo.getValue();
        if (creator != null) {
            event.setCreatedBy(creator.getUserId());
        }
        _eventFormData.setValue(event); // Update the LiveData holding the form state
    }


    /**
     * Attempts to create the new event using the data in _eventFormData.
     * The result of the operation can be observed on createEventOperationState.
     */
    public void submitCreateEvent() {
        EventModel eventToCreate = _eventFormData.getValue();
        UserModel currentCreator = _creatorInfo.getValue();

        if (eventToCreate == null) {
            // This case should be rare if _eventFormData is initialized.
            // If you want to directly update createEventOperationState with an error:
            // MutableLiveData<ResultWrapper<EventModel>> errorResult = new MutableLiveData<>();
            // errorResult.setValue(new ResultWrapper.Error<>("Event data is missing."));
            // createEventOperationState = errorResult; // This replaces the repo's LiveData, be careful.
            // Better to let the repo handle validation or have a separate local status.
            System.err.println("CreateEventViewModel: Event data is null before creation attempt.");
            return;
        }

        if (currentCreator == null || currentCreator.getUserId() == null || currentCreator.getUserId().isEmpty()) {
            System.err.println("CreateEventViewModel: Event creator information is missing.");
            // Post to a local LiveData<ResultWrapper<String>> for form validation errors if needed
            // Or the UI can observe createEventOperationState from repo which will show an error.
            // To directly set an error for the UI observing createEventOperationState:
            // This requires careful handling as createEventOperationState is usually from repo.
            // For now, we rely on repo's validation for creator ID.
            // eventHubRepository.singleEventOperationState.setValue(new ResultWrapper.Error<>("Creator info missing")); // Not ideal to set repo's state from VM.
            return;
        }
        // Ensure creator ID is set from our _creatorInfo
        eventToCreate.setCreatedBy(currentCreator.getUserId());

        // Basic client-side validation (repository also has some)
        if (eventToCreate.getTitle() == null || eventToCreate.getTitle().trim().isEmpty()) {
            System.err.println("CreateEventViewModel: Event title cannot be empty.");
            // Post to a local validation status LiveData for the form
            return;
        }
        if (eventToCreate.getStartDate() == null || eventToCreate.getEndDate() == null) {
            System.err.println("CreateEventViewModel: Event start and end dates must be set.");
            return;
        }
        if (eventToCreate.getEndDate().before(eventToCreate.getStartDate())) {
            System.err.println("CreateEventViewModel: Event end date cannot be before start date.");
            return;
        }
        // ID should be set by the repository during the creation process if not already set.
        // eventToCreate.setId(UUID.randomUUID().toString()); // Let repo handle ID for new events.

        System.out.println("CreateEventViewModel: Submitting event for creation: " + eventToCreate.toString());
        // The UI will observe createEventOperationState (which is eventHubRepository.singleEventOperationState)
        eventHubRepository.createEvent(eventToCreate);
    }

    /**
     * Resets the event form data to a new empty event.
     * Call this after a successful creation or if the user cancels.
     */
    public void resetForm() {
        EventModel newEvent = new EventModel();
        UserModel creator = _creatorInfo.getValue();
        if (creator != null) { // Preserve creator if already set
            newEvent.setCreatedBy(creator.getUserId());
        }
        _eventFormData.setValue(newEvent);
        // Also reset the operation state in the repository if it's for "single event"
        // This might need a specific method in the repo or careful state management.
        // For example, you might want singleEventOperationState to go back to Idle.
        // eventHubRepository.resetSingleEventOperationState(); // Hypothetical method
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        // No specific sources added directly from repo to MediatorLiveData in this ViewModel
        // as createEventOperationState directly points to repo's LiveData.
        // If MediatorLiveData were used, sources would be removed here.
        System.out.println("CreateEventViewModel: Cleared.");
    }
}