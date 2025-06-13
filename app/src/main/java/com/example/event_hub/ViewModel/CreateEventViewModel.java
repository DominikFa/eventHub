package com.example.event_hub.ViewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.example.event_hub.Model.request.EventCreationRequest;
import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.LocationData;
import com.example.event_hub.Model.ResultWrapper;
import java.util.List;
import java.util.ArrayList; // Import ArrayList for empty list


public class CreateEventViewModel extends AndroidViewModel {

    private final EventHubRepository eventHubRepository;
    private final LocationSelectionViewModel locationSelectionViewModel;

    private final MediatorLiveData<ResultWrapper<EventModel>> _operationState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<EventModel>> createEventOperationState = _operationState;

    public LiveData<ResultWrapper<List<LocationData>>> availableLocationsState;
    public LiveData<Integer> selectedLocationPosition;

    private String lastSubmittedEventTitleForComparison = null;

    public CreateEventViewModel(@NonNull Application application) {
        super(application);
        eventHubRepository = EventHubRepository.getInstance();
        locationSelectionViewModel = new LocationSelectionViewModel();
        availableLocationsState = locationSelectionViewModel.availableLocationsState;
        selectedLocationPosition = locationSelectionViewModel.selectedLocationPosition;

        resetFormAndState();

        _operationState.addSource(eventHubRepository.singleEventOperationState, repoResult -> {
            if (repoResult instanceof ResultWrapper.Success) {
                EventModel createdEvent = ((ResultWrapper.Success<EventModel>) repoResult).getData();
                // ZMIANA: Użycie getName() zamiast getTitle()
                if (createdEvent != null && createdEvent.getName() != null && createdEvent.getName().equals(lastSubmittedEventTitleForComparison)) {
                    _operationState.setValue(repoResult);
                    lastSubmittedEventTitleForComparison = null;
                }
            } else if (repoResult instanceof ResultWrapper.Error) {
                _operationState.setValue(repoResult);
                lastSubmittedEventTitleForComparison = null;
            }
        });
    }

    public void fetchLocationsForSelection(String authToken) {
        // Corrected: Pass null for cityFilter and an empty ArrayList for sort
        locationSelectionViewModel.fetchAllLocations(authToken, 0, 100, null, new ArrayList<>());
    }

    public void setSelectedLocationPosition(int position) {
        locationSelectionViewModel.setSelectedLocationPosition(position);
    }

    public LocationData getSelectedLocation() {
        return locationSelectionViewModel.getSelectedLocation();
    }

    public void submitCreateEvent(EventCreationRequest eventToCreate, String authToken) {
        if (eventToCreate == null || authToken == null || authToken.isEmpty()) {
            _operationState.setValue(new ResultWrapper.Error<>("Invalid data or user not authenticated."));
            return;
        }

        if (eventToCreate.getLocationId() == null && eventToCreate.getLocation() == null) {
            _operationState.setValue(new ResultWrapper.Error<>("Location must be specified."));
            return;
        }

        _operationState.setValue(new ResultWrapper.Loading<>());
        lastSubmittedEventTitleForComparison = eventToCreate.getName();
        eventHubRepository.createEvent(eventToCreate, authToken);
    }

    public void resetFormAndState() {
        _operationState.postValue(new ResultWrapper.Idle<>());
        lastSubmittedEventTitleForComparison = null;
        eventHubRepository.resetSingleEventOperationState();
        locationSelectionViewModel.clearState();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _operationState.removeSource(eventHubRepository.singleEventOperationState);
    }
}