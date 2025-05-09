package com.example.event_hub.View;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.event_hub.Model.LocationData;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.R;
import com.example.event_hub.ViewModel.MapViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private MapViewModel mapViewModel;
    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;

    private ProgressBar pbMapLoading;
    private TextView tvMapErrorOrInfo;
    private MaterialButton btnOpenInMapsApp;

    private String eventIdArg;
    private String eventLocationArg;
    private LocationData currentLocationData; // To store resolved location

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        if (getArguments() != null) {
            // If using Safe Args:
            // eventIdArg = MapFragmentArgs.fromBundle(getArguments()).getEventId();
            // eventLocationArg = MapFragmentArgs.fromBundle(getArguments()).getEventLocation();
            eventIdArg = getArguments().getString("eventId");
            eventLocationArg = getArguments().getString("eventLocation");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pbMapLoading = view.findViewById(R.id.pb_map_loading);
        tvMapErrorOrInfo = view.findViewById(R.id.tv_map_error_or_info);
        btnOpenInMapsApp = view.findViewById(R.id.btn_open_in_maps_app);
        btnOpenInMapsApp.setEnabled(false); // Disable until location is ready

        // Initialize the SupportMapFragment
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            // Handle error: mapFragment not found
            tvMapErrorOrInfo.setText("Error initializing map.");
            tvMapErrorOrInfo.setVisibility(View.VISIBLE);
            return;
        }
        mapFragment.getMapAsync(this); // Triggers onMapReady

        setupClickListeners();
        observeViewModel();

        if (eventLocationArg != null && !eventLocationArg.isEmpty()) {
            // If EventModel.location (passed as eventLocationArg) is already "lat,lng" or address
            mapViewModel.loadLocationFromString(eventLocationArg, "Event Location"); // Pass a title if available
        } else if (eventIdArg != null) {
            // TODO: If only eventId is passed, MapViewModel needs to fetch event details
            // mapViewModel.fetchEventLocationById(eventIdArg);
            tvMapErrorOrInfo.setText("Fetching event location by ID not yet implemented.");
            tvMapErrorOrInfo.setVisibility(View.VISIBLE);
        } else {
            tvMapErrorOrInfo.setText("No location information provided.");
            tvMapErrorOrInfo.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        btnOpenInMapsApp.setOnClickListener(v -> openInExternalMaps());
    }

    private void observeViewModel() {
        mapViewModel.locationDataState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbMapLoading, result instanceof ResultWrapper.Loading);
            tvMapErrorOrInfo.setVisibility(View.GONE); // Hide error by default

            if (result instanceof ResultWrapper.Success) {
                @SuppressWarnings("unchecked")
                LocationData locationData = ((ResultWrapper.Success<LocationData>) result).getData();
                if (locationData != null) {
                    this.currentLocationData = locationData;
                    if (googleMap != null) {
                        updateMapLocation(locationData);
                    }
                    btnOpenInMapsApp.setEnabled(true);
                } else {
                    tvMapErrorOrInfo.setText("Location data is null.");
                    tvMapErrorOrInfo.setVisibility(View.VISIBLE);
                    btnOpenInMapsApp.setEnabled(false);
                }
            } else if (result instanceof ResultWrapper.Error) {
                @SuppressWarnings("unchecked")
                String error = ((ResultWrapper.Error<LocationData>) result).getMessage();
                tvMapErrorOrInfo.setText("Error: " + error);
                tvMapErrorOrInfo.setVisibility(View.VISIBLE);
                btnOpenInMapsApp.setEnabled(false);
                Toast.makeText(getContext(), "Error processing location: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        // Configure map settings if needed (e.g., UI controls, map type)
        // googleMap.getUiSettings().setZoomControlsEnabled(true);

        // If locationData was already loaded by ViewModel before map was ready, update map now
        if (currentLocationData != null) {
            updateMapLocation(currentLocationData);
        } else {
            // Show a default location or wait for ViewModel to provide data
            LatLng defaultLocation = new LatLng(52.2297, 21.0122); // Warsaw
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 6));
        }
    }

    private void updateMapLocation(LocationData locationData) {
        if (googleMap == null) return;

        if (locationData.getLatitude() != 0 && locationData.getLongitude() != 0) {
            LatLng position = new LatLng(locationData.getLatitude(), locationData.getLongitude());
            googleMap.clear(); // Clear previous markers
            googleMap.addMarker(new MarkerOptions().position(position).title(locationData.getEventTitle() != null ? locationData.getEventTitle() : "Event Location"));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15)); // Zoom level 15
            tvMapErrorOrInfo.setVisibility(View.GONE);
        } else if (locationData.getAddress() != null && !locationData.getAddress().isEmpty()) {
            // Address needs geocoding
            geocodeAddressAndShowOnMap(locationData.getAddress(), locationData.getEventTitle());
        } else {
            tvMapErrorOrInfo.setText("Insufficient location data to display on map.");
            tvMapErrorOrInfo.setVisibility(View.VISIBLE);
        }
    }

    private void geocodeAddressAndShowOnMap(String addressString, String title) {
        if (getContext() == null || googleMap == null) return;
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        handleVisibility(pbMapLoading, true); // Show loading during geocoding

        // Geocoding should be done in a background thread in a real app
        // For simplicity in this example, it's on the main thread (can cause ANR)
        // In a real app: use AsyncTask, Coroutines (Kotlin), or RxJava
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressString, 1);
            handleVisibility(pbMapLoading, false);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Update currentLocationData with resolved coordinates
                if (currentLocationData != null) {
                    currentLocationData.setLatitude(location.getLatitude());
                    currentLocationData.setLongitude(location.getLongitude());
                }

                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(latLng).title(title != null ? title : addressString));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                tvMapErrorOrInfo.setVisibility(View.GONE);
                btnOpenInMapsApp.setEnabled(true);
            } else {
                tvMapErrorOrInfo.setText("Could not find location for address: " + addressString);
                tvMapErrorOrInfo.setVisibility(View.VISIBLE);
                btnOpenInMapsApp.setEnabled(false);
            }
        } catch (IOException e) {
            handleVisibility(pbMapLoading, false);
            tvMapErrorOrInfo.setText("Geocoding failed: " + e.getMessage());
            tvMapErrorOrInfo.setVisibility(View.VISIBLE);
            btnOpenInMapsApp.setEnabled(false);
            e.printStackTrace();
        }
    }

    private void openInExternalMaps() {
        if (currentLocationData == null) {
            Toast.makeText(getContext(), "Location data not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri gmmIntentUri;
        if (currentLocationData.getLatitude() != 0 && currentLocationData.getLongitude() != 0) {
            // Using coordinates
            String titleEncoded = Uri.encode(currentLocationData.getEventTitle() != null ? currentLocationData.getEventTitle() : "Event Location");
            gmmIntentUri = Uri.parse("geo:" + currentLocationData.getLatitude() + "," + currentLocationData.getLongitude() + "?q=" + currentLocationData.getLatitude() + "," + currentLocationData.getLongitude() + "(" + titleEncoded + ")");
        } else if (currentLocationData.getAddress() != null && !currentLocationData.getAddress().isEmpty()) {
            // Using address string for query
            gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(currentLocationData.getAddress()));
        } else {
            Toast.makeText(getContext(), "No valid location to open.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps"); // Attempt to open Google Maps directly

        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            // Google Maps app is not installed, try generic intent
            Intent genericMapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            try {
                startActivity(genericMapIntent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(getContext(), "No map application found to handle this request.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
