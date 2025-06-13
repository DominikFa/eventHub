// src/main/java/com/example/event_hub/View/MapFragment.java
package com.example.event_hub.View;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.event_hub.Model.LocationData;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.R;
import com.example.event_hub.ViewModel.MapViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class MapFragment extends Fragment { // Does NOT implement OnMapReadyCallback anymore

    private static final String TAG = "MapFragment";

    private MapViewModel mapViewModel;

    private ProgressBar pbMapLoading;
    private TextView tvMapErrorOrInfo;
    private MaterialButton btnOpenInMapsApp;
    private ImageView ivStaticMap; // ImageView for static map

    private long eventIdArg;
    private String eventNameArg; // Keep eventName for map marker title

    private LocationData currentLocationData; // Stores the location data for external map app button

    public MapFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())).get(MapViewModel.class);

        if (getArguments() != null) {
            eventIdArg = getArguments().getLong("eventId", 0L); // Default to 0L if not found
            eventNameArg = getArguments().getString("eventName");
            Log.d(TAG, "onCreate: Received arguments - eventId: " + eventIdArg + ", eventName: " + eventNameArg);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: MapFragment creating view.");
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: MapFragment view created.");

        pbMapLoading = view.findViewById(R.id.pb_map_loading);
        tvMapErrorOrInfo = view.findViewById(R.id.tv_map_error_or_info);
        btnOpenInMapsApp = view.findViewById(R.id.btn_open_in_maps_app);
        btnOpenInMapsApp.setEnabled(false); // Disabled by default
        ivStaticMap = view.findViewById(R.id.iv_static_map); // Initialize ImageView
        ivStaticMap.setVisibility(View.GONE); // Hide initially

        setupClickListeners();
        observeViewModel();

        // Always attempt to fetch location details by event ID
        if (eventIdArg > 0) {
            Log.d(TAG, "onViewCreated: Fetching location by event ID: " + eventIdArg);
            mapViewModel.fetchEventLocationDetailsById(eventIdArg);
        } else {
            // If no valid event ID, display an error message
            tvMapErrorOrInfo.setText(R.string.map_no_location_provided);
            tvMapErrorOrInfo.setVisibility(View.VISIBLE);
            Log.e(TAG, "onViewCreated: No valid event ID provided.");
        }
    }

    private void setupClickListeners() {
        btnOpenInMapsApp.setOnClickListener(v -> openInExternalMaps());
        Log.d(TAG, "setupClickListeners: External Maps button listener set.");
    }

    private void observeViewModel() {
        // Observe staticMapImageUrlState for the image URL to display static map
        mapViewModel.staticMapImageUrlState.observe(getViewLifecycleOwner(), result -> {
            Log.d(TAG, "observeViewModel: StaticMapImageUrlState changed. Result type: " + result.getClass().getSimpleName());
            handleVisibility(pbMapLoading, result instanceof ResultWrapper.Loading);
            tvMapErrorOrInfo.setVisibility(View.GONE); // Hide previous errors when new result comes

            if (result instanceof ResultWrapper.Success) {
                String imageUrl = ((ResultWrapper.Success<String>) result).getData();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Log.d(TAG, "observeViewModel: Received successful image URL: " + imageUrl);
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_placeholder_image) // Placeholder image
                            .error(R.drawable.ic_placeholder_image_error) // Image to display on error
                            .into(ivStaticMap);
                    ivStaticMap.setVisibility(View.VISIBLE); // Make ImageView visible
                } else {
                    tvMapErrorOrInfo.setText(R.string.map_location_data_null); // Or specific error
                    tvMapErrorOrInfo.setVisibility(View.VISIBLE);
                    ivStaticMap.setVisibility(View.GONE); // Hide ImageView if no valid URL
                    Log.e(TAG, "observeViewModel: Static Map URL is null or empty despite success state.");
                }
            } else if (result instanceof ResultWrapper.Error) {
                String error = ((ResultWrapper.Error<String>) result).getMessage();
                tvMapErrorOrInfo.setText(getString(R.string.map_error_processing_location, error));
                tvMapErrorOrInfo.setVisibility(View.VISIBLE);
                ivStaticMap.setVisibility(View.GONE); // Hide ImageView on error
                Toast.makeText(getContext(), getString(R.string.map_error_processing_location, error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "observeViewModel: Error in StaticMapImageUrlState: " + error);
            }
        });

        // Observe locationDataForExternalApp for external app launch button enablement
        mapViewModel.locationDataForExternalApp.observe(getViewLifecycleOwner(), result -> {
            Log.d(TAG, "observeViewModel: LocationDataForExternalAppState changed. Result type: " + result.getClass().getSimpleName());
            if (result instanceof ResultWrapper.Success) {
                LocationData locationData = ((ResultWrapper.Success<LocationData>) result).getData();
                // Ensure LocationData has valid non-zero latitude AND longitude for external app launch
                if (locationData != null && locationData.getLatitude() != 0.0 && locationData.getLongitude() != 0.0) {
                    this.currentLocationData = locationData; // Store valid location data for external app
                    btnOpenInMapsApp.setEnabled(true);
                    Log.d(TAG, "observeViewModel: External map button enabled with valid coordinates.");
                } else {
                    btnOpenInMapsApp.setEnabled(false);
                    Log.e(TAG, "observeViewModel: External map button disabled due to invalid or zero coordinates.");
                }
            } else if (result instanceof ResultWrapper.Error) {
                btnOpenInMapsApp.setEnabled(false);
                Log.e(TAG, "observeViewModel: External map button disabled due to error in location data for external app: " + ((ResultWrapper.Error<?>) result).getMessage());
            } else if (result instanceof ResultWrapper.Loading) {
                btnOpenInMapsApp.setEnabled(false); // Disable while loading
            }
        });
    }

    private void openInExternalMaps() {
        Log.d(TAG, "openInExternalMaps: Attempting to open in external maps.");
        // Strictly check if currentLocationData exists and has valid non-zero coordinates
        if (currentLocationData == null || currentLocationData.getLatitude() == 0.0 || currentLocationData.getLongitude() == 0.0) {
            Toast.makeText(getContext(), R.string.map_invalid_coordinates_format, Toast.LENGTH_LONG).show();
            Log.e(TAG, "openInExternalMaps: Location data (coordinates) not available or invalid for external maps.");
            return;
        }

        Uri gmmIntentUri;
        String titleForMap = eventNameArg != null ? eventNameArg : getString(R.string.map_event_location_title_default);
        String titleEncoded = Uri.encode(titleForMap);

        // Always use lat/lng if available for external maps
        gmmIntentUri = Uri.parse("geo:" + currentLocationData.getLatitude() + "," + currentLocationData.getLongitude() + "?q=" + currentLocationData.getLatitude() + "," + currentLocationData.getLongitude() + "(" + titleEncoded + ")");

        Log.d(TAG, "openInExternalMaps: Constructed URI: " + gmmIntentUri.toString());

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(mapIntent);
            Log.d(TAG, "openInExternalMaps: Launched Google Maps app intent.");
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "openInExternalMaps: Google Maps app not found, falling back to generic map intent.");
            // Fallback to a generic map intent if Google Maps is not installed
            Intent genericMapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            try {
                startActivity(genericMapIntent);
                Log.d(TAG, "openInExternalMaps: Launched generic map intent.");
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(getContext(), R.string.map_no_app_found, Toast.LENGTH_LONG).show();
                Log.e(TAG, "openInExternalMaps: No map application found to handle request.");
            }
        }
    }

    private void handleVisibility(View view, boolean isVisible) {
        if (view != null) {
            view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }
}