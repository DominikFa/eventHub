package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.ResultWrapper;

public class CreateEventViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private final MutableLiveData<EventModel> _eventFormData = new MutableLiveData<>();
    public LiveData<EventModel> eventFormData = _eventFormData;

    private final MediatorLiveData<ResultWrapper<EventModel>> _operationState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<EventModel>> createEventOperationState = _operationState;

    private String lastSubmittedEventTitleForComparison = null;

    public CreateEventViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        resetFormAndState();
    }

    public void submitCreateEvent(EventModel eventToCreate, String authToken) { // Changed actingUserId to authToken
        if (eventToCreate == null) {
            _operationState.setValue(new ResultWrapper.Error<>("Event data is null."));
            return;
        }
        if (authToken == null || authToken.isEmpty()) {
            _operationState.setValue(new ResultWrapper.Error<>("User not authenticated. Cannot create event."));
            return;
        }
        // eventToCreate.setCreatedBy() will be handled by repository using validated token

        _operationState.setValue(new ResultWrapper.Loading<>());
        lastSubmittedEventTitleForComparison = eventToCreate.getTitle();

        _operationState.removeSource(eventHubRepository.singleEventOperationState);
        _operationState.addSource(eventHubRepository.singleEventOperationState, new Observer<ResultWrapper<EventModel>>() {
            @Override
            public void onChanged(ResultWrapper<EventModel> repoResult) {
                if (repoResult instanceof ResultWrapper.Success) {
                    EventModel createdEvent = ((ResultWrapper.Success<EventModel>) repoResult).getData();
                    if (createdEvent != null && createdEvent.getTitle().equals(lastSubmittedEventTitleForComparison)) {
                        _operationState.setValue(repoResult);
                        _operationState.removeSource(eventHubRepository.singleEventOperationState);
                        lastSubmittedEventTitleForComparison = null;
                    }
                } else if (repoResult instanceof ResultWrapper.Error) {
                    _operationState.setValue(repoResult);
                    _operationState.removeSource(eventHubRepository.singleEventOperationState);
                    lastSubmittedEventTitleForComparison = null;
                }
            }
        });
        eventHubRepository.createEvent(eventToCreate, authToken);
    }

    public void resetFormAndState() {
        _eventFormData.setValue(new EventModel());
        _operationState.setValue(new ResultWrapper.Idle<>());
        lastSubmittedEventTitleForComparison = null;
        eventHubRepository.resetSingleEventOperationState();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _operationState.removeSource(eventHubRepository.singleEventOperationState);
    }
}