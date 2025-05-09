package com.example.event_hub.View;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.R;
import com.example.event_hub.View.adapter.EventAdapter;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.MainViewModel;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainFragment extends Fragment {

    private MainViewModel mainViewModel;
    private AuthViewModel authViewModel;
    private EventAdapter eventAdapter;
    private RecyclerView rvEvents;
    private TextView tvNoEvents;
    private MaterialButton btnSelectDate;
    private ImageButton btnProfileOrLogin;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar pbMainLoading;

    private Calendar selectedDateCalendar = Calendar.getInstance();
    private String loggedInUserId;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        eventAdapter = new EventAdapter(new ArrayList<>(), event -> {
            if (getView() != null && event.getId() != null) {
                // MODIFIED: Using Bundle to pass eventId
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getId()); // Ensure "eventId" matches argument name in nav_graph
                Navigation.findNavController(getView()).navigate(R.id.action_mainFragment_to_eventDetailFragment, bundle);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupRecyclerView();
        setupBottomNavigation();
        updateDateButtonText();

        btnSelectDate.setOnClickListener(v -> showDatePickerDialog());

        btnProfileOrLogin.setOnClickListener(v -> {
            if (getView() == null) return;
            NavController navController = Navigation.findNavController(getView());
            if (authViewModel.isLoggedIn()) {
                Bundle profileArgs = new Bundle();
                if (loggedInUserId != null) {
                    profileArgs.putString("userId", loggedInUserId); // Pass loggedInUserId to ProfileFragment
                }
                // Make sure R.id.action_mainFragment_to_profileFragment exists and ProfileFragment accepts "userId"
                // navController.navigate(R.id.action_mainFragment_to_profileFragment, profileArgs);
                Toast.makeText(getContext(), getString(R.string.navigate_to_profile_toast), Toast.LENGTH_SHORT).show();
            } else {
                // navController.navigate(R.id.action_mainFragment_to_loginActivity);
                Toast.makeText(getContext(), getString(R.string.navigate_to_login_toast), Toast.LENGTH_SHORT).show();
            }
        });

        observeViewModels();
        // Load public events by default when the fragment is created or bottom nav selects it
        if (bottomNavigationView.getSelectedItemId() == R.id.navigation_events_public ||
                bottomNavigationView.getSelectedItemId() == 0) { // 0 if no item is selected initially
            mainViewModel.loadPublicEvents();
        } else if (bottomNavigationView.getSelectedItemId() == R.id.navigation_events_known) {
            loadAttendedEventsForCurrentUser();
        }
    }

    private void bindViews(View view) {
        rvEvents = view.findViewById(R.id.rv_events_main);
        tvNoEvents = view.findViewById(R.id.tv_no_events_main);
        btnSelectDate = view.findViewById(R.id.btn_select_date_main);
        btnProfileOrLogin = view.findViewById(R.id.btn_profile_or_login_main);
        bottomNavigationView = view.findViewById(R.id.bottom_navigation_main);
        pbMainLoading = view.findViewById(R.id.pb_main_loading); // Ensure this ID exists in fragment_main.xml
    }

    private void setupRecyclerView() {
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEvents.setAdapter(eventAdapter);
    }

    private void updateDateButtonText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()); // Corrected yyyy
        btnSelectDate.setText(dateFormat.format(selectedDateCalendar.getTime()));
    }

    private void showDatePickerDialog() {
        if (getContext() == null) return;
        DatePickerDialog.OnDateSetListener dateSetListener = (datePicker, year, month, dayOfMonth) -> {
            selectedDateCalendar.set(Calendar.YEAR, year);
            selectedDateCalendar.set(Calendar.MONTH, month);
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateButtonText();
            // TODO: Implement filtering events based on selectedDateCalendar
            Toast.makeText(getContext(), "Date selected: " + dayOfMonth + "/" + (month + 1) + "/" + year, Toast.LENGTH_SHORT).show();
        };

        new DatePickerDialog(requireContext(), dateSetListener,
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void observeViewModels() {
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), userId -> {
            loggedInUserId = userId;
            if (btnProfileOrLogin != null) {
                if (userId != null) {
                    btnProfileOrLogin.setImageResource(R.drawable.ic_profile);
                    btnProfileOrLogin.setContentDescription(getString(R.string.content_desc_profile));
                } else {
                    btnProfileOrLogin.setImageResource(R.drawable.ic_login);
                    btnProfileOrLogin.setContentDescription(getString(R.string.content_desc_login));
                }
            }
            if (bottomNavigationView.getSelectedItemId() == R.id.navigation_events_known) {
                loadAttendedEventsForCurrentUser();
            }
        });

        mainViewModel.publicEventsState.observe(getViewLifecycleOwner(), result -> {
            // Only update if "Publiczne" tab is selected or no specific tab implies public
            if (bottomNavigationView.getSelectedItemId() == R.id.navigation_events_public ||
                    bottomNavigationView.getSelectedItemId() == 0 ) { // 0 can be initial state
                handleEventListResult(result, getString(R.string.no_public_events_available), getString(R.string.error_loading_events_prefix));
            }
        });

        mainViewModel.attendedEventsState.observe(getViewLifecycleOwner(), result -> {
            // Only update if "Znam" tab is selected
            if (bottomNavigationView.getSelectedItemId() == R.id.navigation_events_known) {
                handleEventListResult(result, getString(R.string.no_attended_events_found), getString(R.string.error_loading_attended_events_prefix));
            }
        });
    }

    private void handleEventListResult(ResultWrapper<List<EventModel>> result, String noEventsMessage, String errorMessagePrefix) {
        handleVisibility(pbMainLoading, result instanceof ResultWrapper.Loading);
        if (result instanceof ResultWrapper.Success) {
            @SuppressWarnings("unchecked")
            ResultWrapper.Success<List<EventModel>> successResult = (ResultWrapper.Success<List<EventModel>>) result;
            List<EventModel> events = successResult.getData();
            if (events != null && !events.isEmpty()) {
                eventAdapter.updateEvents(events);
                rvEvents.setVisibility(View.VISIBLE);
                tvNoEvents.setVisibility(View.GONE);
            } else {
                eventAdapter.updateEvents(new ArrayList<>());
                rvEvents.setVisibility(View.GONE);
                tvNoEvents.setVisibility(View.VISIBLE);
                tvNoEvents.setText(noEventsMessage);
            }
        } else if (result instanceof ResultWrapper.Error) {
            @SuppressWarnings("unchecked")
            ResultWrapper.Error<List<EventModel>> errorResult = (ResultWrapper.Error<List<EventModel>>) result;
            String errorMessage = errorResult.getMessage();
            rvEvents.setVisibility(View.GONE);
            tvNoEvents.setVisibility(View.VISIBLE);
            tvNoEvents.setText(errorMessagePrefix + errorMessage);
            if (getContext() != null) { // Check context for Toast
                Toast.makeText(getContext(), errorMessagePrefix + errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }


    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (getView() == null) return false;
            // NavController navController = Navigation.findNavController(getView()); // Not used for tab switching here
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_events_known) {
                loadAttendedEventsForCurrentUser();
                return true;
            } else if (itemId == R.id.navigation_events_public) {
                mainViewModel.loadPublicEvents();
                return true;
            } else if (itemId == R.id.navigation_create_event) {
                // Navigation.findNavController(getView()).navigate(R.id.action_mainFragment_to_createEventFragment);
                Toast.makeText(getContext(), "Navigate to Create Event", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        // Set default selection if needed, e.g., public events
        // This will also trigger the initial load in observeViewModels if not already loaded
        if(bottomNavigationView.getSelectedItemId() == 0 && mainViewModel.publicEventsState.getValue() instanceof ResultWrapper.Idle) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_events_public);
        }
    }

    private void loadAttendedEventsForCurrentUser() {
        if (loggedInUserId != null) {
            mainViewModel.loadAttendedEvents(loggedInUserId);
        } else {
            if (getContext() != null) { // Check context for Toast
                Toast.makeText(getContext(), R.string.login_to_see_attended_events, Toast.LENGTH_SHORT).show();
            }
            eventAdapter.updateEvents(new ArrayList<>());
            if (rvEvents != null) rvEvents.setVisibility(View.GONE);
            if (tvNoEvents != null) {
                tvNoEvents.setVisibility(View.VISIBLE);
                tvNoEvents.setText(R.string.login_to_see_attended_events_detail);
            }
        }
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
