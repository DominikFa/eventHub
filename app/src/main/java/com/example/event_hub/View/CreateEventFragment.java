package com.example.event_hub.View;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.R;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.CreateEventViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial; // Import SwitchMaterial
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateEventFragment extends Fragment {

    private CreateEventViewModel createEventViewModel;
    private AuthViewModel authViewModel;

    private TextInputLayout tilEventName, tilEventDescription, tilEventLocation, tilMaxParticipants;
    private TextInputEditText etEventName, etEventDescription, etEventLocation, etMaxParticipants;
    private MaterialButton btnSelectEventStartDate, btnSelectEventStartTime;
    private MaterialButton btnSelectEventEndDate, btnSelectEventEndTime;
    private MaterialButton btnAddEventPhotos, btnCreateEventSubmit;
    private TextView tvSelectedEventStartDate, tvSelectedEventStartTime;
    private TextView tvSelectedEventEndDate, tvSelectedEventEndTime;
    private SwitchMaterial switchEventPublic; // Added Switch
    private ImageView ivUserIcon;
    private ProgressBar pbCreateEventLoading;

    private Calendar startDateTimeCalendar = Calendar.getInstance();
    private Calendar endDateTimeCalendar = Calendar.getInstance();
    private String currentAuthToken;
    private String loggedInUserId;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createEventViewModel = new ViewModelProvider(this).get(CreateEventViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Initialize start time to next hour, end time 2 hours after that.
        startDateTimeCalendar.add(Calendar.HOUR_OF_DAY, 1);
        startDateTimeCalendar.set(Calendar.MINUTE, 0);
        startDateTimeCalendar.set(Calendar.SECOND, 0);
        startDateTimeCalendar.set(Calendar.MILLISECOND, 0);

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
        createEventViewModel.resetFormAndState();
        bindViews(view);
        setupClickListeners();
        observeViewModels();
        updateSelectedDateTimeDisplay();
    }

    private void bindViews(View view) {
        tilEventName = view.findViewById(R.id.til_event_name_create);
        etEventName = view.findViewById(R.id.et_event_name_create);
        tilEventDescription = view.findViewById(R.id.til_event_description_create);
        etEventDescription = view.findViewById(R.id.et_event_description_create);
        tilEventLocation = view.findViewById(R.id.til_event_location_create);
        etEventLocation = view.findViewById(R.id.et_event_location_create);
        tilMaxParticipants = view.findViewById(R.id.til_event_max_participants_create);
        etMaxParticipants = view.findViewById(R.id.et_event_max_participants_create);

        btnSelectEventStartDate = view.findViewById(R.id.btn_select_event_start_date);
        tvSelectedEventStartDate = view.findViewById(R.id.tv_selected_event_start_date);
        btnSelectEventStartTime = view.findViewById(R.id.btn_select_event_start_time);
        tvSelectedEventStartTime = view.findViewById(R.id.tv_selected_event_start_time);

        btnSelectEventEndDate = view.findViewById(R.id.btn_select_event_end_date);
        tvSelectedEventEndDate = view.findViewById(R.id.tv_selected_event_end_date);
        btnSelectEventEndTime = view.findViewById(R.id.btn_select_event_end_time);
        tvSelectedEventEndTime = view.findViewById(R.id.tv_selected_event_end_time);

        switchEventPublic = view.findViewById(R.id.switch_event_public_create); // Bind the switch

        btnAddEventPhotos = view.findViewById(R.id.btn_add_event_photos);
        btnCreateEventSubmit = view.findViewById(R.id.btn_create_event_submit);
        ivUserIcon = view.findViewById(R.id.iv_user_icon_create_event);
        pbCreateEventLoading = view.findViewById(R.id.pb_create_event_loading);
    }

    private void setupClickListeners() {
        btnSelectEventStartDate.setOnClickListener(v -> showDatePickerDialog(startDateTimeCalendar, this::updateSelectedDateTimeDisplay));
        btnSelectEventStartTime.setOnClickListener(v -> showTimePickerDialog(startDateTimeCalendar, this::updateSelectedDateTimeDisplay));
        btnSelectEventEndDate.setOnClickListener(v -> showDatePickerDialog(endDateTimeCalendar, this::updateSelectedDateTimeDisplay));
        btnSelectEventEndTime.setOnClickListener(v -> showTimePickerDialog(endDateTimeCalendar, this::updateSelectedDateTimeDisplay));

        btnCreateEventSubmit.setOnClickListener(v -> attemptEventCreation());

        btnAddEventPhotos.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Add event photos - Not Implemented", Toast.LENGTH_SHORT).show();
        });

        ivUserIcon.setOnClickListener(v -> {
            if (getView() == null) return;
            NavController navController = Navigation.findNavController(getView());
            if (currentAuthToken != null && loggedInUserId != null) {
                Bundle profileArgs = new Bundle();
                profileArgs.putString("userId", loggedInUserId);
                // This action might not exist from CreateEventFragment, adjust nav graph or remove.
                // For now, assume it's a placeholder or you'll add the action.
                try {
                    // navController.navigate(R.id.action_createEventFragment_to_profileFragment, profileArgs);
                    Toast.makeText(getContext(), "Profile navigation from here is TBD.", Toast.LENGTH_SHORT).show();
                } catch (IllegalArgumentException e) {
                    Toast.makeText(getContext(), "Navigation to profile not set up from here.", Toast.LENGTH_SHORT).show();
                }
            } else {
                navController.navigate(R.id.loginActivity);
            }
        });
    }

    private void showDatePickerDialog(Calendar calendarToUpdate, Runnable onDateTimeUpdate) {
        if (getContext() == null) return;
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            calendarToUpdate.set(Calendar.YEAR, year);
            calendarToUpdate.set(Calendar.MONTH, month);
            calendarToUpdate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            if (onDateTimeUpdate != null) {
                onDateTimeUpdate.run();
            }
        }, calendarToUpdate.get(Calendar.YEAR), calendarToUpdate.get(Calendar.MONTH), calendarToUpdate.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showTimePickerDialog(Calendar calendarToUpdate, Runnable onDateTimeUpdate) {
        if (getContext() == null) return;
        new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            calendarToUpdate.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendarToUpdate.set(Calendar.MINUTE, minute);
            calendarToUpdate.set(Calendar.SECOND, 0);
            calendarToUpdate.set(Calendar.MILLISECOND, 0);
            if (onDateTimeUpdate != null) {
                onDateTimeUpdate.run();
            }
        }, calendarToUpdate.get(Calendar.HOUR_OF_DAY), calendarToUpdate.get(Calendar.MINUTE), true)
                .show();
    }

    private void updateSelectedDateTimeDisplay() {
        tvSelectedEventStartDate.setText(getString(R.string.selected_date_prefix_create) + dateFormat.format(startDateTimeCalendar.getTime()));
        tvSelectedEventStartTime.setText(getString(R.string.selected_time_prefix_create) + timeFormat.format(startDateTimeCalendar.getTime()));
        tvSelectedEventEndDate.setText(getString(R.string.selected_date_prefix_create) + dateFormat.format(endDateTimeCalendar.getTime()));
        tvSelectedEventEndTime.setText(getString(R.string.selected_time_prefix_create) + timeFormat.format(endDateTimeCalendar.getTime()));
    }

    private void attemptEventCreation() {
        tilEventName.setError(null);
        tilEventDescription.setError(null);
        tilEventLocation.setError(null);
        tilMaxParticipants.setError(null);

        String title = etEventName.getText().toString().trim();
        String description = etEventDescription.getText().toString().trim();
        String location = etEventLocation.getText().toString().trim();
        String maxParticipantsStr = etMaxParticipants.getText().toString().trim();

        boolean isValid = true;
        if (TextUtils.isEmpty(title)) {
            tilEventName.setError(getString(R.string.error_field_required));
            isValid = false;
        }
        if (TextUtils.isEmpty(description)) {
            tilEventDescription.setError(getString(R.string.error_field_required));
            isValid = false;
        }
        if (TextUtils.isEmpty(location)) {
            tilEventLocation.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        int maxParticipantsInt = 0;
        if (TextUtils.isEmpty(maxParticipantsStr)) {
            tilMaxParticipants.setError(getString(R.string.error_field_required));
            isValid = false;
        } else {
            try {
                maxParticipantsInt = Integer.parseInt(maxParticipantsStr);
                if (maxParticipantsInt <= 0) {
                    tilMaxParticipants.setError(getString(R.string.error_positive_number_required));
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                tilMaxParticipants.setError(getString(R.string.error_invalid_number_format));
                isValid = false;
            }
        }

        if (startDateTimeCalendar.getTime().after(endDateTimeCalendar.getTime())) {
            Toast.makeText(getContext(), R.string.toast_end_date_after_start, Toast.LENGTH_LONG).show();
            isValid = false;
        }
        Calendar now = Calendar.getInstance();
        if (startDateTimeCalendar.before(now)) {
            Toast.makeText(getContext(), "Start date and time cannot be in the past.", Toast.LENGTH_LONG).show();
            isValid = false;
        }


        if (!isValid) return;

        if (currentAuthToken == null) {
            Toast.makeText(getContext(), R.string.toast_login_to_create, Toast.LENGTH_LONG).show();
            if(getView() != null) Navigation.findNavController(getView()).navigate(R.id.loginActivity);
            return;
        }

        EventModel newEvent = new EventModel();
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setStartDate(startDateTimeCalendar.getTime());
        newEvent.setEndDate(endDateTimeCalendar.getTime());
        newEvent.setLocation(location);
        newEvent.setMaxParticipants(maxParticipantsInt);
        newEvent.setPublic(switchEventPublic.isChecked()); // Get the value from the switch

        createEventViewModel.submitCreateEvent(newEvent, currentAuthToken);
    }

    private void observeViewModels() {
        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            currentAuthToken = token;
            btnCreateEventSubmit.setEnabled(token != null && !(createEventViewModel.createEventOperationState.getValue() instanceof ResultWrapper.Loading));
        });
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), userId -> {
            loggedInUserId = userId;
            if (ivUserIcon != null) {
                if (userId != null) {
                    ivUserIcon.setImageResource(R.drawable.ic_profile);
                } else {
                    ivUserIcon.setImageResource(R.drawable.ic_login);
                }
            }
        });

        createEventViewModel.createEventOperationState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbCreateEventLoading, result instanceof ResultWrapper.Loading);
            btnCreateEventSubmit.setEnabled(!(result instanceof ResultWrapper.Loading) && currentAuthToken != null);

            if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), R.string.toast_event_created_successfully, Toast.LENGTH_LONG).show();
                if (getView() != null) {
                    Navigation.findNavController(getView()).navigate(R.id.action_createEventFragment_to_mainFragment);
                }
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                String errorMessage = errorResult.getMessage();
                Toast.makeText(getContext(), getString(R.string.toast_failed_to_create_event, (errorMessage != null ? errorMessage : getString(R.string.error_unknown))), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}