package com.example.event_hub.ViewModel;

import android.app.Application;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer; // Import Observer

import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.LocationData;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.R;

import java.util.Locale;

public class MapViewModel extends AndroidViewModel {

    private static final String TAG = "MapViewModel";

    private final EventHubRepository eventHubRepository;
    private final Application application;

    private final MutableLiveData<ResultWrapper<String>> _staticMapImageUrlState = new MutableLiveData<>();
    public LiveData<ResultWrapper<String>> staticMapImageUrlState = _staticMapImageUrlState;

    private final MutableLiveData<ResultWrapper<LocationData>> _locationDataForExternalApp = new MutableLiveData<>();
    public LiveData<ResultWrapper<LocationData>> locationDataForExternalApp = _locationDataForExternalApp;

    // Store the observer instance to be able to remove it later
    private Observer<ResultWrapper<EventModel>> eventDetailObserver;


    public MapViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        this.eventHubRepository = EventHubRepository.getInstance();
    }

    /**
     * Helper to generate OpenStreetMap Static Map URL from provided latitude, longitude, and zoom level.
     * Uses staticmap.openstreetmap.de service.
     * @param latitude The latitude of the map center/marker.
     * @param longitude The longitude of the map center/marker.
     * @param zoomLevel The desired zoom level for the map.
     * @param markerTitle A title for the marker (may not be displayed by all static map services).
     * @return The URL for the static map image.
     */
    private String generateStaticMapUrl(double latitude, double longitude, int zoomLevel, String markerTitle) {
        // Format for staticmap.openstreetmap.de:
        // https://staticmap.openstreetmap.de/staticmap.php?center={lat},{lon}&zoom={zoom}&size={width}x{height}&markers={lat},{lon},red-pushpin
        // Size 600x300 pixels.
        // Marker format: {latitude},{longitude},[color-name|color-hex][icon-name]
        // Using 'red-pushpin' as a simple marker style.

        return String.format(Locale.US,
                "https://staticmap.openstreetmap.de/staticmap.php?center=%f,%f&zoom=%d&size=600x300&markers=%f,%f,red-pushpin",
                latitude, longitude, zoomLevel, latitude, longitude);
    }

    /**
     * Loads location data from an EventModel. Strictly uses latitude and longitude if available.
     * If coordinates are missing, it signals an error.
     * @param event The EventModel containing location data.
     */
    public void loadLocationForEvent(@Nullable EventModel event) {
        Log.d(TAG, "loadLocationForEvent: Processing event location from EventModel.");
        if (event == null) {
            _staticMapImageUrlState.postValue(new ResultWrapper.Error<>("Event data is null."));
            _locationDataForExternalApp.postValue(new ResultWrapper.Error<>("Event data is null."));
            Log.e(TAG, "loadLocationForEvent: Event data is null.");
            return;
        }
        LocationData locationData = event.getLocation();
        // Strict check: locationData must exist AND have valid non-zero latitude/longitude
        if (locationData == null || (locationData.getLatitude() == 0.0 && locationData.getLongitude() == 0.0)) {
            String errorMsg = application.getString(R.string.map_invalid_coordinates_format);
            _staticMapImageUrlState.postValue(new ResultWrapper.Error<>(errorMsg));
            _locationDataForExternalApp.postValue(new ResultWrapper.Error<>(errorMsg));
            Log.e(TAG, "loadLocationForEvent: Location data is null or has zero coordinates. Error: " + errorMsg);
            return;
        }

        _staticMapImageUrlState.postValue(new ResultWrapper.Loading<>());
        _locationDataForExternalApp.postValue(new ResultWrapper.Loading<>());

        // Use existing coordinates to generate static map URL with a predetermined zoom (e.g., 15)
        int defaultZoom = 15; // Set your desired default zoom level here
        String imageUrl = generateStaticMapUrl(locationData.getLatitude(), locationData.getLongitude(), defaultZoom, event.getName());
        Log.d(TAG, "loadLocationForEvent: Using existing coordinates. Image URL: " + imageUrl);
        _staticMapImageUrlState.postValue(new ResultWrapper.Success<>(imageUrl));
        _locationDataForExternalApp.postValue(new ResultWrapper.Success<>(locationData)); // Pass full LocationData
    }

    /**
     * Fetches event details by ID and then attempts to load its location.
     * Strictly uses latitude and longitude from the fetched event's location.
     * @param eventId The ID of the event to fetch.
     */
    public void fetchEventLocationDetailsById(Long eventId) {
        Log.d(TAG, "fetchEventLocationDetailsById: Fetching event details for ID: " + eventId);
        if (eventId == null || eventId == 0L) {
            String errorMsg = "Event ID is missing.";
            _staticMapImageUrlState.postValue(new ResultWrapper.Error<>(errorMsg));
            _locationDataForExternalApp.postValue(new ResultWrapper.Error<>(errorMsg));
            Log.e(TAG, "fetchEventLocationDetailsById: Event ID is null or 0.");
            return;
        }
        _staticMapImageUrlState.postValue(new ResultWrapper.Loading<>());
        _locationDataForExternalApp.postValue(new ResultWrapper.Loading<>());

        // Initialize the observer if it's null or we need a new one for a new fetch
        if (eventDetailObserver == null) {
            eventDetailObserver = new Observer<ResultWrapper<EventModel>>() {
                @Override
                public void onChanged(ResultWrapper<EventModel> eventResult) {
                    if (eventResult instanceof ResultWrapper.Success) {
                        EventModel event = ((ResultWrapper.Success<EventModel>) eventResult).getData();
                        if (event != null && event.getId().equals(eventId)) {
                            Log.d(TAG, "fetchEventLocationDetailsById: Event details fetched for ID: " + eventId);
                            // Delegate to loadLocationForEvent which strictly checks coordinates
                            loadLocationForEvent(event);
                            eventHubRepository.singleEventOperationState.removeObserver(this); // Remove observer after first result
                        }
                    } else if (eventResult instanceof ResultWrapper.Error) {
                        String errorMessage = "Failed to fetch event details for map: " + ((ResultWrapper.Error<?>) eventResult).getMessage();
                        _staticMapImageUrlState.postValue(new ResultWrapper.Error<>(errorMessage));
                        _locationDataForExternalApp.postValue(new ResultWrapper.Error<>(errorMessage));
                        Log.e(TAG, "fetchEventLocationDetailsById: " + errorMessage);
                        eventHubRepository.singleEventOperationState.removeObserver(this); // Remove observer on error as well
                    }
                }
            };
        } else {
            // If observer already exists, ensure it's not currently observing a different request.
            // Or, for simplicity, always remove and re-add to ensure a fresh observation for each call.
            // However, since it removes itself, this might not be strictly necessary, but good for robustness.
            eventHubRepository.singleEventOperationState.removeObserver(eventDetailObserver);
        }

        eventHubRepository.singleEventOperationState.observeForever(eventDetailObserver);
        eventHubRepository.fetchEventDetails(eventId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "MapViewModel: Cleared.");
        // Crucially, remove the observer when the ViewModel is cleared
        if (eventDetailObserver != null) {
            eventHubRepository.singleEventOperationState.removeObserver(eventDetailObserver);
        }
    }
}