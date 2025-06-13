package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.LocationData;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.ResultWrapper;
import java.util.List;

public class LocationSelectionViewModel extends ViewModel {

    private final MutableLiveData<Integer> _selectedLocationPosition = new MutableLiveData<>(-1);
    public LiveData<Integer> selectedLocationPosition = _selectedLocationPosition;

    private final MediatorLiveData<ResultWrapper<List<LocationData>>> _availableLocationsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<LocationData>>> availableLocationsState = _availableLocationsState;

    private final EventHubRepository eventHubRepository;
    private List<LocationData> lastFetchedLocations;

    public LocationSelectionViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        _availableLocationsState.setValue(new ResultWrapper.Idle<>());

        _availableLocationsState.addSource(eventHubRepository.allLocationsState, result -> {
            if (result instanceof ResultWrapper.Success) {
                PaginatedResponse<LocationData> paginatedResponse = ((ResultWrapper.Success<PaginatedResponse<LocationData>>) result).getData();
                lastFetchedLocations = paginatedResponse != null ? paginatedResponse.getContent() : null;
                _availableLocationsState.setValue(new ResultWrapper.Success<>(lastFetchedLocations));
            } else if (result instanceof ResultWrapper.Error) {
                _availableLocationsState.setValue(new ResultWrapper.Error<>(((ResultWrapper.Error<?>) result).getMessage()));
            } else if (result instanceof ResultWrapper.Loading) {
                _availableLocationsState.setValue(new ResultWrapper.Loading<>());
            }
        });
    }


    public void fetchAllLocations(String authToken, int page, int size, String cityFilter, List<String> sort) {
        if (!(_availableLocationsState.getValue() instanceof ResultWrapper.Loading)) {
            eventHubRepository.getAllLocations(authToken, page, size, cityFilter, sort);
        }
    }

    public void setSelectedLocationPosition(int position) {
        _selectedLocationPosition.setValue(position);
    }

    public LocationData getSelectedLocation() {
        Integer pos = _selectedLocationPosition.getValue();
        if (pos != null && pos >= 0 && lastFetchedLocations != null && pos < lastFetchedLocations.size()) {
            return lastFetchedLocations.get(pos);
        }
        return null;
    }

    public void clearState() {
        _availableLocationsState.postValue(new ResultWrapper.Idle<>());
        _selectedLocationPosition.postValue(-1);
        lastFetchedLocations = null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _availableLocationsState.removeSource(eventHubRepository.allLocationsState);
    }
}