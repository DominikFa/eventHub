package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.ResultWrapper; // Ensure this import is correct

import java.util.Date;
// import java.util.UUID; // UUID generation is better handled by Repository

public class CreateEventViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;

    private final MutableLiveData<EventModel> _eventFormData = new MutableLiveData<>();
    public LiveData<EventModel> eventFormData = _eventFormData;

    private final MutableLiveData<UserModel> _creatorInfo = new MutableLiveData<>();
    public LiveData<UserModel> creatorInfo = _creatorInfo;

    public LiveData<ResultWrapper<EventModel>> createEventOperationState;


    public CreateEventViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        _eventFormData.setValue(new EventModel());

        createEventOperationState = eventHubRepository.singleEventOperationState;
    }

    public void setCreator(UserModel user) {
        _creatorInfo.setValue(user);
        EventModel currentEventData = _eventFormData.getValue();
        if (currentEventData != null && user != null) {
            currentEventData.setCreatedBy(user.getUserId());
            _eventFormData.setValue(currentEventData);
        }
    }

    public void updateEventDetailsInForm(String title, String description, Date startDate, Date endDate, String location, int maxParticipants, String creatorId) {
        EventModel event = _eventFormData.getValue();
        if (event == null) {
            event = new EventModel();
        }
        event.setTitle(title);
        event.setDescription(description);
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setLocation(location);
        event.setMaxParticipants(maxParticipants);
        if (creatorId != null) {
            event.setCreatedBy(creatorId);
        }
        _eventFormData.setValue(event);
    }

    /**
     * Submits the event to be created via the repository.
     * @param eventToCreate The EventModel object to be created.
     */
    public void submitCreateEvent(EventModel eventToCreate) { // Accepts EventModel
        if (eventToCreate == null) {
            System.err.println("CreateEventViewModel: Attempted to submit null event.");
            // Repository will post an error to singleEventOperationState if it receives null
            return;
        }
        System.out.println("CreateEventViewModel: Submitting event for creation: " + eventToCreate.toString());
        eventHubRepository.createEvent(eventToCreate);
    }

    public void resetForm() {
        EventModel newEvent = new EventModel();
        UserModel creator = _creatorInfo.getValue();
        if (creator != null) {
            newEvent.setCreatedBy(creator.getUserId());
        }
        _eventFormData.setValue(newEvent);
        // Consider resetting repository's singleEventOperationState to Idle if needed
        // eventHubRepository.resetSingleEventOperationState(); // (This method would need to be created in repo)
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        System.out.println("CreateEventViewModel: Cleared.");
    }
}
