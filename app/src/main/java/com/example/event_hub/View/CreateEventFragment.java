package com.example.event_hub.View;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.R;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.CreateEventViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateEventFragment extends Fragment {

    private CreateEventViewModel createEventViewModel;
    private AuthViewModel authViewModel;

    private TextInputLayout tilEventName, tilEventDescription;
    private TextInputEditText etEventName, etEventDescription;
    private MaterialButton btnSelectEventDate, btnSelectEventTime, btnAddEventPhotos, btnCreateEventSubmit;
    private TextView tvSelectedEventStartDate, tvSelectedEventStartTime;
    private ImageView ivUserIcon;
    private ProgressBar pbCreateEventLoading;

    private Calendar startDateTimeCalendar = Calendar.getInstance();
    // private Calendar endDateTimeCalendar = Calendar.getInstance(); // If you add end date/time pickers

    public CreateEventFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createEventViewModel = new ViewModelProvider(this).get(CreateEventViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupClickListeners();
        observeViewModels();

        // Set current user as creator
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), userId -> {
            if (userId != null) {
                // Fetch UserModel to pass to setCreator or just pass userId
                // For simplicity, if CreateEventViewModel can take just userId, that's easier.
                // Let's assume CreateEventViewModel's setCreator needs UserModel.
                // This requires ProfileViewModel or another way to get UserModel from userId.
                // For now, we'll pass a placeholder UserModel if CreateEventVM is adapted,
                // or just set the creatorId in the EventModel directly.
                // createEventViewModel.setCreator(userModel); // Needs userModel
            }
        });
        updateSelectedDateTimeDisplay();
    }

    private void bindViews(View view) {
        tilEventName = view.findViewById(R.id.til_event_name_create);
        etEventName = view.findViewById(R.id.et_event_name_create);
        tilEventDescription = view.findViewById(R.id.til_event_description_create);
        etEventDescription = view.findViewById(R.id.et_event_description_create);
        btnSelectEventDate = view.findViewById(R.id.btn_select_event_date);
        tvSelectedEventStartDate = view.findViewById(R.id.tv_selected_event_start_date);
        btnSelectEventTime = view.findViewById(R.id.btn_select_event_time);
        tvSelectedEventStartTime = view.findViewById(R.id.tv_selected_event_start_time);
        btnAddEventPhotos = view.findViewById(R.id.btn_add_event_photos);
        btnCreateEventSubmit = view.findViewById(R.id.btn_create_event_submit);
        ivUserIcon = view.findViewById(R.id.iv_user_icon_create_event);
        pbCreateEventLoading = view.findViewById(R.id.pb_create_event_loading);
    }

    private void setupClickListeners() {
        btnSelectEventDate.setOnClickListener(v -> showDatePickerDialog());
        btnSelectEventTime.setOnClickListener(v -> showTimePickerDialog());

        btnAddEventPhotos.setOnClickListener(v -> {
            // TODO: Implement image picker logic
            Toast.makeText(getContext(), "Add photos clicked - TBD", Toast.LENGTH_SHORT).show();
        });

        btnCreateEventSubmit.setOnClickListener(v -> attemptEventCreation());

        ivUserIcon.setOnClickListener(v -> {
            if (getView() != null) {
                NavController navController = Navigation.findNavController(getView());
                if (authViewModel.isLoggedIn()) {
                    // TODO: Navigate to Profile
                    Toast.makeText(getContext(), R.string.navigate_to_profile_toast, Toast.LENGTH_SHORT).show();
                } else {
                    // TODO: Navigate to Login
                    Toast.makeText(getContext(), R.string.navigate_to_login_toast, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDatePickerDialog() {
        if (getContext() == null) return;
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            startDateTimeCalendar.set(Calendar.YEAR, year);
            startDateTimeCalendar.set(Calendar.MONTH, month);
            startDateTimeCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateSelectedDateTimeDisplay();
        };
        new DatePickerDialog(requireContext(), dateSetListener,
                startDateTimeCalendar.get(Calendar.YEAR),
                startDateTimeCalendar.get(Calendar.MONTH),
                startDateTimeCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showTimePickerDialog() {
        if (getContext() == null) return;
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            startDateTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            startDateTimeCalendar.set(Calendar.MINUTE, minute);
            updateSelectedDateTimeDisplay();
        };
        new TimePickerDialog(requireContext(), timeSetListener,
                startDateTimeCalendar.get(Calendar.HOUR_OF_DAY),
                startDateTimeCalendar.get(Calendar.MINUTE),
                true) // true for 24-hour view
                .show();
    }

    private void updateSelectedDateTimeDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvSelectedEventStartDate.setText(getString(R.string.selected_date_prefix) + dateFormat.format(startDateTimeCalendar.getTime()));
        tvSelectedEventStartTime.setText(getString(R.string.selected_time_prefix) + timeFormat.format(startDateTimeCalendar.getTime()));
    }

    private void attemptEventCreation() {
        String title = etEventName.getText().toString().trim();
        String description = etEventDescription.getText().toString().trim();
        // TODO: Get location, max participants from other input fields if you add them

        tilEventName.setError(null);
        tilEventDescription.setError(null);

        boolean isValid = true;
        if (TextUtils.isEmpty(title)) {
            tilEventName.setError("Event name cannot be empty");
            isValid = false;
        }
        if (TextUtils.isEmpty(description)) {
            tilEventDescription.setError("Event description cannot be empty");
            isValid = false;
        }
        // Validate date/time (e.g., ensure it's in the future)
        if (startDateTimeCalendar.getTime().before(new Date())) {
            Toast.makeText(getContext(), "Event date/time must be in the future.", Toast.LENGTH_LONG).show();
            isValid = false;
        }

        if (!isValid) return;

        String creatorId = authViewModel.currentUserId.getValue(); // Or from a synchronously fetched UserModel
        if (creatorId == null) {
            Toast.makeText(getContext(), "You must be logged in to create an event.", Toast.LENGTH_LONG).show();
            // TODO: Navigate to login
            return;
        }

        EventModel newEvent = new EventModel();
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setStartDate(startDateTimeCalendar.getTime());
        // For simplicity, setting end date same as start date + 2 hours. Add an end date picker for more control.
        Calendar endCalendar = (Calendar) startDateTimeCalendar.clone();
        endCalendar.add(Calendar.HOUR_OF_DAY, 2);
        newEvent.setEndDate(endCalendar.getTime());
        newEvent.setCreatedBy(creatorId);
        newEvent.setLocation("Default Location - Add Input"); // Placeholder
        newEvent.setMaxParticipants(50); // Placeholder

        createEventViewModel.submitCreateEvent(newEvent); // ViewModel method to call repository
    }

    private void observeViewModels() {
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), userId -> {
            if (ivUserIcon != null) {
                if (userId != null) {
                    ivUserIcon.setImageResource(R.drawable.ic_profile);
                    ivUserIcon.setContentDescription(getString(R.string.content_desc_profile));
                } else {
                    ivUserIcon.setImageResource(R.drawable.ic_login);
                    ivUserIcon.setContentDescription(getString(R.string.content_desc_login));
                }
            }
        });

        createEventViewModel.createEventOperationState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbCreateEventLoading, result instanceof ResultWrapper.Loading);
            btnCreateEventSubmit.setEnabled(!(result instanceof ResultWrapper.Loading));

            if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_LONG).show();
                if (getView() != null) {
                    // Navigate back to main screen or event detail screen
                    // CreateEventFragmentDirections.ActionCreateEventFragmentToMainFragment action =
                    //    CreateEventFragmentDirections.actionCreateEventFragmentToMainFragment();
                    // Navigation.findNavController(getView()).navigate(action);
                    Navigation.findNavController(getView()).popBackStack(); // Simple pop back
                }
            } else if (result instanceof ResultWrapper.Error) {
                String errorMessage = ((ResultWrapper.Error<?>) result).getMessage();
                Toast.makeText(getContext(), "Failed to create event: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
