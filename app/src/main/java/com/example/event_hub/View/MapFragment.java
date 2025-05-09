package com.example.event_hub.View;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.lang.ref.WeakReference;
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
        btnOpenInMapsApp.setEnabled(false);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            tvMapErrorOrInfo.setText("Error initializing map.");
            tvMapErrorOrInfo.setVisibility(View.VISIBLE);
            return;
        }
        mapFragment.getMapAsync(this);

        setupClickListeners();
        observeViewModel();

        if (eventLocationArg != null && !eventLocationArg.isEmpty()) {
            mapViewModel.loadLocationFromString(eventLocationArg, "Event Location");
        } else if (eventIdArg != null) {
            tvMapErrorOrInfo.setText("Fetching event location by ID not yet implemented in ViewModel.");
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
            tvMapErrorOrInfo.setVisibility(View.GONE);

            if (result instanceof ResultWrapper.Success) {
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
        if (currentLocationData != null) {
            updateMapLocation(currentLocationData);
        } else {
            LatLng defaultLocation = new LatLng(52.2297, 21.0122); // Warsaw
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 6));
        }
    }

    private void updateMapLocation(LocationData locationData) {
        if (googleMap == null) return;

        if (locationData.getLatitude() != 0 && locationData.getLongitude() != 0) {
            LatLng position = new LatLng(locationData.getLatitude(), locationData.getLongitude());
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(position).title(locationData.getEventTitle() != null ? locationData.getEventTitle() : "Event Location"));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
            tvMapErrorOrInfo.setVisibility(View.GONE);
        } else if (locationData.getAddress() != null && !locationData.getAddress().isEmpty()) {
            // Address needs geocoding - initiate background task
            new GeocodeAddressTask(this).execute(locationData.getAddress(), locationData.getEventTitle());
        } else {
            tvMapErrorOrInfo.setText("Insufficient location data to display on map.");
            tvMapErrorOrInfo.setVisibility(View.VISIBLE);
        }
    }

    // Called by GeocodeAddressTask onPostExecute
    private void onGeocodingResult(LatLng latLng, String addressString, String title, String errorMessage) {
        handleVisibility(pbMapLoading, false);
        if (latLng != null && googleMap != null) {
            if (currentLocationData != null && addressString.equals(currentLocationData.getAddress())) {
                currentLocationData.setLatitude(latLng.latitude);
                currentLocationData.setLongitude(latLng.longitude);
            }
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(latLng).title(title != null ? title : addressString));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            tvMapErrorOrInfo.setVisibility(View.GONE);
            btnOpenInMapsApp.setEnabled(true);
        } else {
            tvMapErrorOrInfo.setText(errorMessage != null ? errorMessage : "Could not find location for address: " + addressString);
            tvMapErrorOrInfo.setVisibility(View.VISIBLE);
            btnOpenInMapsApp.setEnabled(false);
        }
    }


    private void openInExternalMaps() {
        if (currentLocationData == null) {
            Toast.makeText(getContext(), "Location data not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri gmmIntentUri;
        if (currentLocationData.getLatitude() != 0 && currentLocationData.getLongitude() != 0) {
            String titleEncoded = Uri.encode(currentLocationData.getEventTitle() != null ? currentLocationData.getEventTitle() : "Event Location");
            gmmIntentUri = Uri.parse("geo:" + currentLocationData.getLatitude() + "," + currentLocationData.getLongitude() + "?q=" + currentLocationData.getLatitude() + "," + currentLocationData.getLongitude() + "(" + titleEncoded + ")");
        } else if (currentLocationData.getAddress() != null && !currentLocationData.getAddress().isEmpty()) {
            gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(currentLocationData.getAddress()));
        } else {
            Toast.makeText(getContext(), "No valid location to open.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            Intent genericMapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            try {
                startActivity(genericMapIntent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(getContext(), "No map application found.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    // Static inner class for AsyncTask to avoid memory leaks
    private static class GeocodeAddressTask extends AsyncTask<String, Void, GeocodeAddressTask.GeocodeResult> {
        private final WeakReference<MapFragment> fragmentReference;

        static class GeocodeResult {
            LatLng latLng;
            String addressString; // Original address for context
            String title;         // Original title for context
            String errorMessage;

            GeocodeResult(LatLng latLng, String addressString, String title) {
                this.latLng = latLng;
                this.addressString = addressString;
                this.title = title;
            }

            GeocodeResult(String errorMessage, String addressString, String title) {
                this.errorMessage = errorMessage;
                this.addressString = addressString;
                this.title = title;
            }
        }

        GeocodeAddressTask(MapFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            MapFragment fragment = fragmentReference.get();
            if (fragment != null && fragment.getContext() != null) {
                fragment.handleVisibility(fragment.pbMapLoading, true);
            }
        }

        @Override
        protected GeocodeResult doInBackground(String... params) {
            MapFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.getContext() == null) {
                return new GeocodeResult("Context lost during geocoding.", params[0], params.length > 1 ? params[1] : null);
            }
            Geocoder geocoder = new Geocoder(fragment.getContext(), Locale.getDefault());
            String addressString = params[0];
            String title = params.length > 1 ? params[1] : addressString;
            try {
                List<Address> addresses = geocoder.getFromLocationName(addressString, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address location = addresses.get(0);
                    return new GeocodeResult(new LatLng(location.getLatitude(), location.getLongitude()), addressString, title);
                } else {
                    return new GeocodeResult("Address not found: " + addressString, addressString, title);
                }
            } catch (IOException e) {
                return new GeocodeResult("Geocoding failed: " + e.getMessage(), addressString, title);
            }
        }

        @Override
        protected void onPostExecute(GeocodeResult result) {
            MapFragment fragment = fragmentReference.get();
            if (fragment != null && fragment.isAdded()) { // Check if fragment is still added
                fragment.onGeocodingResult(result.latLng, result.addressString, result.title, result.errorMessage);
            }
        }
    }
}