package com.example.event_hub.ViewModel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository; // If needed to fetch event by ID
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.LocationData; // Ensure this model exists
import com.example.event_hub.Model.ResultWrapper; // Ensure this import is correct

public class MapViewModel extends ViewModel {

    // private final EventHubRepository eventHubRepository; // Uncomment if fetching event by ID

    private final MutableLiveData<ResultWrapper<LocationData>> _locationDataState = new MutableLiveData<>();
    public LiveData<ResultWrapper<LocationData>> locationDataState = _locationDataState;

    // To store the event title if location is derived from an EventModel
    private String currentEventTitleForMap;


    public MapViewModel() {
        // this.eventHubRepository = EventHubRepository.getInstance(); // Uncomment if used
    }

    /**
     * Prepares location data from an EventModel.
     * Tries to parse latitude/longitude from the event's location string or uses it as an address.
     * @param event The EventModel containing location information.
     */
    public void loadLocationForEvent(@Nullable EventModel event) {
        if (event == null) {
            _locationDataState.postValue(new ResultWrapper.Error<>("Event data is null. Cannot determine location."));
            return;
        }
        if (event.getLocation() == null || event.getLocation().isEmpty()) {
            _locationDataState.postValue(new ResultWrapper.Error<>("Location information is missing for the event."));
            return;
        }
        _locationDataState.postValue(new ResultWrapper.Loading<>());

        String locationString = event.getLocation();
        this.currentEventTitleForMap = event.getTitle(); // Store title

        processLocationString(locationString, this.currentEventTitleForMap);
    }

    /**
     * Prepares location data directly from a location string (address or "lat,long").
     * @param locationString The location string.
     * @param eventTitle Optional title for the map marker.
     */
    public void loadLocationFromString(@Nullable String locationString, @Nullable String eventTitle) {
        if (locationString == null || locationString.isEmpty()) {
            _locationDataState.postValue(new ResultWrapper.Error<>("Location string is missing."));
            return;
        }
        _locationDataState.postValue(new ResultWrapper.Loading<>());
        this.currentEventTitleForMap = eventTitle; // Store title

        processLocationString(locationString, this.currentEventTitleForMap);
    }

    private void processLocationString(String locationString, String title) {
        // Attempt to parse as "latitude,longitude"
        try {
            if (locationString.contains(",")) {
                String[] parts = locationString.split(",");
                if (parts.length == 2) {
                    double lat = Double.parseDouble(parts[0].trim());
                    double lon = Double.parseDouble(parts[1].trim());
                    // Basic validation for lat/lon ranges
                    if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                        _locationDataState.postValue(new ResultWrapper.Success<>(new LocationData(lat, lon, locationString, title)));
                        return;
                    } else {
                        System.err.println("MapViewModel: Parsed Lat/Lng out of valid range: " + locationString);
                        // Fall through to treat as address if range is invalid
                    }
                }
            }
        } catch (NumberFormatException e) {
            // Not a valid lat,long string, treat as address
            System.err.println("MapViewModel: Location string not parsable as lat,long: " + locationString);
        }

        // If not parsable as valid lat,long, treat as an address.
        // The Fragment will be responsible for geocoding this address.
        _locationDataState.postValue(new ResultWrapper.Success<>(new LocationData(locationString, title)));
    }

    /**
     * Call this method if the MapViewModel needs to fetch event details by ID
     * to then extract the location. This requires EventHubRepository to be set up.
     *
     * public void fetchEventLocationDetailsById(String eventId) {
     * if (eventId == null || eventId.isEmpty()) {
     * _locationDataState.postValue(new ResultWrapper.Error<>("Event ID is missing."));
     * return;
     * }
     * _locationDataState.postValue(new ResultWrapper.Loading<>());
     *
     * // Assuming eventHubRepository.singleEventOperationState is observed
     * // and will trigger an update here or this method adds a one-time observer.
     * // For simplicity, if this ViewModel is responsible, it should observe:
     * // eventHubRepository.singleEventOperationState.observeForever(new Observer<ResultWrapper<EventModel>>() {
     * //     @Override
     * //     public void onChanged(ResultWrapper<EventModel> eventResult) {
     * //         if (eventResult instanceof ResultWrapper.Success) {
     * //             EventModel event = ((ResultWrapper.Success<EventModel>) eventResult).getData();
     * //             if (event != null && event.getId().equals(eventId)) { // Ensure it's for the requested event
     * //                 loadLocationForEvent(event);
     * //                 eventHubRepository.singleEventOperationState.removeObserver(this); // Clean up
     * //             }
     * //         } else if (eventResult instanceof ResultWrapper.Error) {
     * //             _locationDataState.postValue(new ResultWrapper.Error<>("Failed to fetch event details for map: " + ((ResultWrapper.Error<?>) eventResult).getMessage()));
     * //             eventHubRepository.singleEventOperationState.removeObserver(this); // Clean up
     * //         }
     * //         // Ignore Loading state here, already handled by _locationDataState
     * //     }
     * // });
     * // eventHubRepository.fetchEventDetails(eventId);
     * System.out.println("MapViewModel: fetchEventLocationDetailsById - Not fully implemented without direct repo observation setup.");
     * _locationDataState.postValue(new ResultWrapper.Error<>("Fetching event by ID for map not fully implemented in VM."));
     * }
     */

    @Override
    protected void onCleared() {
        super.onCleared();
        System.out.println("MapViewModel: Cleared.");
    }
}
