package com.example.event_hub.View;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.event_hub.Model.request.EventCreationRequest;
import com.example.event_hub.Model.request.LocationCreationRequest;
import com.example.event_hub.Model.LocationData;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.R;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.CreateEventViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateEventFragment extends Fragment {

    private CreateEventViewModel createEventViewModel;
    private AuthViewModel authViewModel;

    private TextInputLayout tilEventName, tilEventDescription, tilMaxParticipants;
    private TextInputEditText etEventName, etEventDescription, etMaxParticipants;

    private RadioGroup rgLocationType;
    private LinearLayout llNewLocationInputs, llExistingLocationSelection;
    private TextInputLayout tilStreetName, tilStreetNumber, tilApartment, tilPostalCode, tilCity, tilRegion, tilCountryIso;
    private TextInputEditText etStreetName, etStreetNumber, etApartment, etPostalCode, etCity, etRegion, etCountryIso;

    // New fields for latitude and longitude
    private TextInputLayout tilLatitude, tilLongitude;
    private TextInputEditText etLatitude, etLongitude;

    private Spinner spinnerExistingLocations;
    private ProgressBar pbLocationsLoading;
    private TextView tvNoExistingLocations;
    private ArrayAdapter<String> existingLocationsAdapter;
    private List<LocationData> availableLocations = new ArrayList<>();

    private MaterialButton btnSelectEventStartDate, btnSelectEventStartTime;
    private MaterialButton btnSelectEventEndDate, btnSelectEventEndTime;
    private MaterialButton btnCreateEventSubmit;
    private TextView tvSelectedEventStartDate, tvSelectedEventStartTime;
    private TextView tvSelectedEventEndDate, tvSelectedEventEndTime;
    private SwitchMaterial switchEventPublic;
    // Removed: private ImageView ivUserIcon;
    private ProgressBar pbCreateEventLoading;

    private Calendar startDateTimeCalendar = Calendar.getInstance();
    private Calendar endDateTimeCalendar = Calendar.getInstance();
    private String currentAuthToken;
    private Long loggedInUserId;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM,EEEE", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createEventViewModel = new ViewModelProvider(this).get(CreateEventViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Ustawienie domyślnych dat i godzin
        startDateTimeCalendar.add(Calendar.HOUR_OF_DAY, 1);
        startDateTimeCalendar.set(Calendar.MINUTE, 0);
        endDateTimeCalendar.setTime(startDateTimeCalendar.getTime());
        endDateTimeCalendar.add(Calendar.HOUR_OF_DAY, 2);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupClickListeners();
        setupLocationSpinner();
        observeViewModels();
        updateSelectedDateTimeDisplay();
    }

    private void bindViews(View view) {
        tilEventName = view.findViewById(R.id.til_event_name_create);
        etEventName = view.findViewById(R.id.et_event_name_create);
        tilEventDescription = view.findViewById(R.id.til_event_description_create);
        etEventDescription = view.findViewById(R.id.et_event_description_create);
        tilMaxParticipants = view.findViewById(R.id.til_event_max_participants_create);
        etMaxParticipants = view.findViewById(R.id.et_event_max_participants_create);
        rgLocationType = view.findViewById(R.id.rg_location_type);
        llNewLocationInputs = view.findViewById(R.id.ll_new_location_inputs);
        llExistingLocationSelection = view.findViewById(R.id.ll_existing_location_selection);
        tilStreetName = view.findViewById(R.id.til_street_name_create);
        etStreetName = view.findViewById(R.id.et_street_name_create);
        tilStreetNumber = view.findViewById(R.id.til_street_number_create);
        etStreetNumber = view.findViewById(R.id.et_street_number_create);
        tilApartment = view.findViewById(R.id.til_apartment_create);
        etApartment = view.findViewById(R.id.et_apartment_create);
        tilPostalCode = view.findViewById(R.id.til_postal_code_create);
        etPostalCode = view.findViewById(R.id.et_postal_code_create);
        tilCity = view.findViewById(R.id.til_city_create);
        etCity = view.findViewById(R.id.et_city_create);
        tilRegion = view.findViewById(R.id.til_region_create);
        etRegion = view.findViewById(R.id.et_region_create);
        tilCountryIso = view.findViewById(R.id.til_country_iso_create);
        etCountryIso = view.findViewById(R.id.et_country_iso_create);

        // Bind new latitude and longitude fields
        tilLatitude = view.findViewById(R.id.til_latitude_create);
        etLatitude = view.findViewById(R.id.et_latitude_create);
        tilLongitude = view.findViewById(R.id.til_longitude_create);
        etLongitude = view.findViewById(R.id.et_longitude_create);

        spinnerExistingLocations = view.findViewById(R.id.spinner_existing_locations);
        pbLocationsLoading = view.findViewById(R.id.pb_locations_loading);
        tvNoExistingLocations = view.findViewById(R.id.tv_no_existing_locations);
        btnSelectEventStartDate = view.findViewById(R.id.btn_select_event_start_date);
        tvSelectedEventStartDate = view.findViewById(R.id.tv_selected_event_start_date);
        btnSelectEventStartTime = view.findViewById(R.id.btn_select_event_start_time);
        tvSelectedEventStartTime = view.findViewById(R.id.tv_selected_event_start_time);
        btnSelectEventEndDate = view.findViewById(R.id.btn_select_event_end_date);
        tvSelectedEventEndDate = view.findViewById(R.id.tv_selected_event_end_date);
        btnSelectEventEndTime = view.findViewById(R.id.btn_select_event_end_time);
        tvSelectedEventEndTime = view.findViewById(R.id.tv_selected_event_end_time);
        switchEventPublic = view.findViewById(R.id.switch_event_public_create);
        btnCreateEventSubmit = view.findViewById(R.id.btn_create_event_submit);
        // Removed: ivUserIcon = view.findViewById(R.id.iv_user_icon_create_event);
        pbCreateEventLoading = view.findViewById(R.id.pb_create_event_loading);
    }

    private void setupClickListeners() {
        btnSelectEventStartDate.setOnClickListener(v -> showDatePickerDialog(startDateTimeCalendar, this::updateSelectedDateTimeDisplay));
        btnSelectEventStartTime.setOnClickListener(v -> showTimePickerDialog(startDateTimeCalendar, this::updateSelectedDateTimeDisplay));
        btnSelectEventEndDate.setOnClickListener(v -> showDatePickerDialog(endDateTimeCalendar, this::updateSelectedDateTimeDisplay));
        btnSelectEventEndTime.setOnClickListener(v -> showTimePickerDialog(endDateTimeCalendar, this::updateSelectedDateTimeDisplay));
        btnCreateEventSubmit.setOnClickListener(v -> attemptEventCreation());

        // Removed: ivUserIcon.setOnClickListener(v -> { ... });

        rgLocationType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isNewLocation = checkedId == R.id.rb_new_location;
            llNewLocationInputs.setVisibility(isNewLocation ? View.VISIBLE : View.GONE);
            llExistingLocationSelection.setVisibility(isNewLocation ? View.GONE : View.VISIBLE);
            if (!isNewLocation && currentAuthToken != null) {
                createEventViewModel.fetchLocationsForSelection(currentAuthToken);
            }
        });
    }

    private void setupLocationSpinner() {
        existingLocationsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        existingLocationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExistingLocations.setAdapter(existingLocationsAdapter);
        spinnerExistingLocations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                createEventViewModel.setSelectedLocationPosition(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                createEventViewModel.setSelectedLocationPosition(-1);
            }
        });
    }

    private void observeViewModels() {
        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            currentAuthToken = token;
            if (token != null && rgLocationType.getCheckedRadioButtonId() == R.id.rb_existing_location) {
                createEventViewModel.fetchLocationsForSelection(token);
            }
        });

        authViewModel.currentUserId.observe(getViewLifecycleOwner(), userId -> {
            loggedInUserId = userId;
            // Removed: ivUserIcon.setImageResource(userId != null ? R.drawable.ic_profile : R.drawable.ic_login);
        });

        createEventViewModel.availableLocationsState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbLocationsLoading, result instanceof ResultWrapper.Loading);
            if (result instanceof ResultWrapper.Success) {
                List<LocationData> locations = ((ResultWrapper.Success<List<LocationData>>) result).getData();
                if (locations != null && !locations.isEmpty()) {
                    availableLocations = locations;
                    List<String> locationNames = new ArrayList<>();
                    for (LocationData loc : locations) {
                        locationNames.add(loc.getFullAddress() != null ? loc.getFullAddress() : "Unnamed Location");
                    }
                    existingLocationsAdapter.clear();
                    existingLocationsAdapter.addAll(locationNames);
                    existingLocationsAdapter.notifyDataSetChanged();
                    spinnerExistingLocations.setVisibility(View.VISIBLE);
                    tvNoExistingLocations.setVisibility(View.GONE);
                } else {
                    spinnerExistingLocations.setVisibility(View.GONE);
                    tvNoExistingLocations.setVisibility(View.VISIBLE);
                }
            } else if (result instanceof ResultWrapper.Error) {
                spinnerExistingLocations.setVisibility(View.GONE);
                tvNoExistingLocations.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Error fetching locations", Toast.LENGTH_SHORT).show();
            }
        });

        createEventViewModel.createEventOperationState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbCreateEventLoading, result instanceof ResultWrapper.Loading);
            btnCreateEventSubmit.setEnabled(!(result instanceof ResultWrapper.Loading));
            if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), R.string.toast_event_created_successfully, Toast.LENGTH_LONG).show();
                if (getView() != null) {
                    Navigation.findNavController(getView()).navigate(R.id.action_createEventFragment_to_mainFragment);
                }
            } else if (result instanceof ResultWrapper.Error) {
                Toast.makeText(getContext(), "Failed to create event: " + ((ResultWrapper.Error<?>) result).getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void attemptEventCreation() {
        // Clear previous errors
        tilEventName.setError(null);
        tilEventDescription.setError(null);
        tilMaxParticipants.setError(null);
        tilStreetName.setError(null);
        tilStreetNumber.setError(null);
        tilPostalCode.setError(null);
        tilCity.setError(null);
        tilRegion.setError(null);
        tilCountryIso.setError(null);
        tilLatitude.setError(null);
        tilLongitude.setError(null);

        if (!validateInputs()) return;

        String name = etEventName.getText().toString().trim();
        String description = etEventDescription.getText().toString().trim();
        Integer maxParticipantsInt = null;
        try {
            if (!etMaxParticipants.getText().toString().isEmpty()) {
                maxParticipantsInt = Integer.parseInt(etMaxParticipants.getText().toString());
                if (maxParticipantsInt <= 0) {
                    tilMaxParticipants.setError(getString(R.string.error_positive_number_required));
                    return;
                }
            }
        } catch (NumberFormatException e) {
            tilMaxParticipants.setError(getString(R.string.error_invalid_number_format));
            return;
        }

        EventCreationRequest newEventRequest;
        if (rgLocationType.getCheckedRadioButtonId() == R.id.rb_new_location) {
            Double latitude = null;
            Double longitude = null;

            if (!etLatitude.getText().toString().isEmpty()) {
                try {
                    latitude = Double.parseDouble(etLatitude.getText().toString());
                } catch (NumberFormatException e) {
                    tilLatitude.setError("Invalid latitude format");
                    return;
                }
            }
            if (!etLongitude.getText().toString().isEmpty()) {
                try {
                    longitude = Double.parseDouble(etLongitude.getText().toString());
                } catch (NumberFormatException e) {
                    tilLongitude.setError("Invalid longitude format");
                    return;
                }
            }


            LocationCreationRequest newLocationRequest = new LocationCreationRequest(
                    etStreetName.getText().toString().trim(),
                    etStreetNumber.getText().toString().trim(),
                    etApartment.getText().toString().trim(),
                    etPostalCode.getText().toString().trim(),
                    etCity.getText().toString().trim(),
                    etRegion.getText().toString().trim(),
                    etCountryIso.getText().toString().trim(),
                    latitude != null ? latitude : 0.0,
                    longitude != null ? longitude : 0.0
            );
            newEventRequest = new EventCreationRequest(name, description, startDateTimeCalendar.getTime(), endDateTimeCalendar.getTime(), switchEventPublic.isChecked(), maxParticipantsInt, newLocationRequest);
        } else {
            LocationData selectedLocation = createEventViewModel.getSelectedLocation();
            if (selectedLocation == null) {
                Toast.makeText(getContext(), R.string.no_existing_location_selected, Toast.LENGTH_SHORT).show();
                return;
            }
            newEventRequest = new EventCreationRequest(name, description, startDateTimeCalendar.getTime(), endDateTimeCalendar.getTime(), switchEventPublic.isChecked(), maxParticipantsInt, selectedLocation.getId());
        }

        createEventViewModel.submitCreateEvent(newEventRequest, currentAuthToken);
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (etEventName.getText().toString().trim().isEmpty()) {
            tilEventName.setError(getString(R.string.error_field_required));
            isValid = false;
        }
        if (etEventDescription.getText().toString().trim().isEmpty()) {
            tilEventDescription.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (rgLocationType.getCheckedRadioButtonId() == R.id.rb_new_location) {
            if (etStreetName.getText().toString().trim().isEmpty()) {
                tilStreetName.setError(getString(R.string.error_field_required));
                isValid = false;
            }
            if (etStreetNumber.getText().toString().trim().isEmpty()) {
                tilStreetNumber.setError(getString(R.string.error_field_required));
                isValid = false;
            }
            if (etPostalCode.getText().toString().trim().isEmpty()) {
                tilPostalCode.setError(getString(R.string.error_field_required));
                isValid = false;
            }
            if (etCity.getText().toString().trim().isEmpty()) {
                tilCity.setError(getString(R.string.error_field_required));
                isValid = false;
            }
            if (etRegion.getText().toString().trim().isEmpty()) {
                tilRegion.setError(getString(R.string.error_field_required));
                isValid = false;
            }
            if (etCountryIso.getText().toString().trim().isEmpty()) {
                tilCountryIso.setError(getString(R.string.error_field_required));
                isValid = false;
            }
        } else if (createEventViewModel.getSelectedLocation() == null) {
            Toast.makeText(getContext(), R.string.no_existing_location_selected, Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (startDateTimeCalendar.after(endDateTimeCalendar)) {
            Toast.makeText(getContext(), R.string.toast_end_date_after_start, Toast.LENGTH_LONG).show();
            isValid = false;
        }
        return isValid;
    }

    private void updateSelectedDateTimeDisplay() {
        tvSelectedEventStartDate.setText(dateFormat.format(startDateTimeCalendar.getTime()));
        tvSelectedEventStartTime.setText(timeFormat.format(startDateTimeCalendar.getTime()));
        tvSelectedEventEndDate.setText(dateFormat.format(endDateTimeCalendar.getTime()));
        tvSelectedEventEndTime.setText(timeFormat.format(endDateTimeCalendar.getTime()));
    }

    private void showDatePickerDialog(Calendar calendar, Runnable onUpdate) {
        if (getContext() == null) return;
        new DatePickerDialog(getContext(), (view, year, month, day) -> {
            calendar.set(year, month, day);
            onUpdate.run();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePickerDialog(Calendar calendar, Runnable onUpdate) {
        if (getContext() == null) return;
        new TimePickerDialog(getContext(), (view, hour, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            onUpdate.run();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void handleVisibility(View view, boolean isVisible) {
        if (view != null) {
            view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }
}