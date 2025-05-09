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
import com.example.event_hub.Model.InvitationModel;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.R;
import com.example.event_hub.View.adapter.EventAdapter;
import com.example.event_hub.View.adapter.InvitationAdapter;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.CreateEventViewModel;
import com.example.event_hub.ViewModel.InvitationViewModel;
import com.example.event_hub.ViewModel.MainViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public class MainFragment extends Fragment implements InvitationAdapter.OnInvitationActionListener {

    private MainViewModel mainViewModel;
    private AuthViewModel authViewModel;
    private InvitationViewModel invitationViewModel;
    private CreateEventViewModel createEventViewModel;

    private EventAdapter eventAdapter;
    private InvitationAdapter invitationAdapter;

    private RecyclerView rvItems;
    private TextView tvNoItems;
    private MaterialButton btnSelectDate;
    private ImageButton btnProfileOrLogin;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar pbMainLoading;

    private Calendar selectedDateCalendar = Calendar.getInstance();
    private String loggedInUserId;
    private String currentAuthToken;
    private List<EventModel> currentEventListCache = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        invitationViewModel = new ViewModelProvider(this).get(InvitationViewModel.class);
        createEventViewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);

        eventAdapter = new EventAdapter(new ArrayList<>(), event -> {
            if (getView() != null && event.getId() != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getId());
                Navigation.findNavController(getView()).navigate(R.id.action_mainFragment_to_eventDetailFragment, bundle);
            }
        });
        invitationAdapter = new InvitationAdapter(new ArrayList<>(), currentEventListCache, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();
        setupBottomNavigation(); // This will trigger initial tab selection and data load
        updateDateButtonText(); // Set initial text for date button

        btnSelectDate.setOnClickListener(v -> showDatePickerDialog());

        btnProfileOrLogin.setOnClickListener(v -> {
            if (getView() == null) return;
            NavController navController = Navigation.findNavController(getView());
            if (currentAuthToken != null && loggedInUserId != null) {
                Bundle profileArgs = new Bundle();
                profileArgs.putString("userId", loggedInUserId);
                navController.navigate(R.id.action_mainFragment_to_profileFragment, profileArgs);
            } else {
                navController.navigate(R.id.action_mainFragment_to_loginActivity);
            }
        });

        observeViewModels();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data for the current tab if it might be stale
        if (bottomNavigationView != null && loggedInUserId != null) { // Check if logged in for relevant tabs
            int selectedItemId = bottomNavigationView.getSelectedItemId();
            if (selectedItemId == R.id.navigation_events_known) {
                mainViewModel.loadAttendedEvents(loggedInUserId);
            } else if (selectedItemId == R.id.navigation_invitations) {
                loadInvitationsForCurrentUser();
            }
        }
        // Public events refresh might also be useful or handled by pull-to-refresh if implemented
    }


    private void bindViews(View view) {
        rvItems = view.findViewById(R.id.rv_items_main);
        tvNoItems = view.findViewById(R.id.tv_no_items_main);
        btnSelectDate = view.findViewById(R.id.btn_select_date_main);
        btnProfileOrLogin = view.findViewById(R.id.btn_profile_or_login_main);
        bottomNavigationView = view.findViewById(R.id.bottom_navigation_main);
        pbMainLoading = view.findViewById(R.id.pb_main_loading);
    }

    private void setupRecyclerView() {
        rvItems.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void updateDateButtonText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        btnSelectDate.setText(getString(R.string.selected_date_prefix) + dateFormat.format(selectedDateCalendar.getTime()));
    }

    private void showDatePickerDialog() {
        if (getContext() == null) return;
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateCalendar.set(Calendar.YEAR, year);
                    selectedDateCalendar.set(Calendar.MONTH, month);
                    selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateButtonText();
                    // Re-filter or re-fetch data based on the new date and current tab
                    if (bottomNavigationView != null) {
                        handleTabSelection(bottomNavigationView.getSelectedItemId());
                    }
                },
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void observeViewModels() {
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), id -> {
            boolean becameNull = loggedInUserId != null && id == null; // Check if user logged out
            loggedInUserId = id;
            updateProfileLoginButtonIcon();
            if (bottomNavigationView != null) {
                // If user logged out, some tabs might need to clear their data or show a login prompt.
                // handleTabSelection will be called and can manage this.
                if (becameNull && (bottomNavigationView.getSelectedItemId() == R.id.navigation_events_known || bottomNavigationView.getSelectedItemId() == R.id.navigation_invitations)) {
                    handleTabSelection(bottomNavigationView.getSelectedItemId()); // Re-evaluate tab content
                } else if (id != null){ // If user logged in, refresh relevant tabs
                    handleTabSelection(bottomNavigationView.getSelectedItemId());
                }
            }
        });

        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            currentAuthToken = token;
            updateProfileLoginButtonIcon();
            // Potentially re-trigger tab selection if token change affects data visibility
            if (bottomNavigationView != null) {
                handleTabSelection(bottomNavigationView.getSelectedItemId());
            }
        });

        mainViewModel.publicEventsState.observe(getViewLifecycleOwner(), result -> {
            if (bottomNavigationView.getSelectedItemId() == R.id.navigation_events_public) {
                if (result instanceof ResultWrapper.Success) {
                    currentEventListCache = ((ResultWrapper.Success<List<EventModel>>) result).getData();
                    if (currentEventListCache == null) currentEventListCache = new ArrayList<>();
                } else if (result instanceof ResultWrapper.Error || result instanceof ResultWrapper.Idle){
                    currentEventListCache.clear(); // Clear cache on error/idle for safety
                }
                handleEventListResult(result, getString(R.string.no_public_events_available), getString(R.string.error_loading_events_prefix));
            }
        });

        mainViewModel.attendedEventsState.observe(getViewLifecycleOwner(), result -> {
            if (bottomNavigationView.getSelectedItemId() == R.id.navigation_events_known) {
                if (result instanceof ResultWrapper.Success) {
                    currentEventListCache = ((ResultWrapper.Success<List<EventModel>>) result).getData();
                    if (currentEventListCache == null) currentEventListCache = new ArrayList<>();
                } else if (result instanceof ResultWrapper.Error || result instanceof ResultWrapper.Idle){
                    currentEventListCache.clear();
                }
                handleEventListResult(result, getString(R.string.no_attended_events_found), getString(R.string.error_loading_attended_events_prefix));
            }
        });

        invitationViewModel.invitationsState.observe(getViewLifecycleOwner(), result -> {
            if (bottomNavigationView.getSelectedItemId() == R.id.navigation_invitations) {
                handleInvitationListResult(result);
            }
        });

        invitationViewModel.actionStatus.observe(getViewLifecycleOwner(), result -> {
            if (!(result instanceof ResultWrapper.Loading || result instanceof ResultWrapper.Idle)) {
                if (result instanceof ResultWrapper.Success) {
                    Toast.makeText(getContext(), "Invitation action successful!", Toast.LENGTH_SHORT).show();
                    if (loggedInUserId != null && bottomNavigationView.getSelectedItemId() == R.id.navigation_invitations) {
                        loadInvitationsForCurrentUser(); // Refresh invitations
                    }
                    // If accepting an invitation makes you an attendee, refresh attended events too
                    if (loggedInUserId != null && bottomNavigationView.getSelectedItemId() == R.id.navigation_events_known) {
                        mainViewModel.loadAttendedEvents(loggedInUserId);
                    }

                } else if (result instanceof ResultWrapper.Error) {
                    String msg = ((ResultWrapper.Error<Void>) result).getMessage();
                    Toast.makeText(getContext(), "Invitation action failed: " + (msg != null ? msg : "Unknown error"), Toast.LENGTH_SHORT).show();
                }
                invitationViewModel.resetActionStatus();
            }
        });
    }

    private void updateProfileLoginButtonIcon() {
        if (btnProfileOrLogin != null) {
            if (currentAuthToken != null && loggedInUserId != null) {
                btnProfileOrLogin.setImageResource(R.drawable.ic_profile);
                btnProfileOrLogin.setContentDescription(getString(R.string.content_desc_profile));
            } else {
                btnProfileOrLogin.setImageResource(R.drawable.ic_login);
                btnProfileOrLogin.setContentDescription(getString(R.string.content_desc_login));
            }
        }
    }

    private void handleEventListResult(ResultWrapper<List<EventModel>> result, String noEventsMessage, String errorMessagePrefix) {
        handleVisibility(pbMainLoading, result instanceof ResultWrapper.Loading);
        rvItems.setVisibility(View.GONE);
        tvNoItems.setVisibility(View.GONE);

        if (result instanceof ResultWrapper.Success) {
            List<EventModel> events = ((ResultWrapper.Success<List<EventModel>>) result).getData();
            if (events != null && !events.isEmpty()) {
                List<EventModel> filteredEvents = events.stream()
                        .filter(event -> event.getStartDate() != null && isSameDay(event.getStartDate(), selectedDateCalendar.getTime()))
                        .collect(Collectors.toList());

                if (!filteredEvents.isEmpty()) {
                    eventAdapter.updateEvents(filteredEvents);
                    rvItems.setVisibility(View.VISIBLE);
                } else {
                    tvNoItems.setText(getString(R.string.no_events_found_for_date));
                    tvNoItems.setVisibility(View.VISIBLE);
                    eventAdapter.updateEvents(new ArrayList<>()); // Clear adapter if no events for date
                }
            } else {
                tvNoItems.setText(noEventsMessage);
                tvNoItems.setVisibility(View.VISIBLE);
                eventAdapter.updateEvents(new ArrayList<>()); // Clear adapter if no events at all
            }
        } else if (result instanceof ResultWrapper.Error) {
            tvNoItems.setText(errorMessagePrefix + ((ResultWrapper.Error<?>) result).getMessage());
            tvNoItems.setVisibility(View.VISIBLE);
            eventAdapter.updateEvents(new ArrayList<>());
        } else if (result instanceof ResultWrapper.Idle){
            tvNoItems.setText(noEventsMessage);
            tvNoItems.setVisibility(View.VISIBLE);
            eventAdapter.updateEvents(new ArrayList<>());
        }
    }


    private void handleInvitationListResult(ResultWrapper<List<InvitationModel>> result) {
        handleVisibility(pbMainLoading, result instanceof ResultWrapper.Loading);
        rvItems.setVisibility(View.GONE);
        tvNoItems.setVisibility(View.GONE);

        if (result instanceof ResultWrapper.Success) {
            List<InvitationModel> invitations = ((ResultWrapper.Success<List<InvitationModel>>) result).getData();
            // Pass currentEventListCache for title lookup
            invitationAdapter.updateInvitations(invitations != null ? invitations : new ArrayList<>(), currentEventListCache);
            if (invitations != null && !invitations.isEmpty()) {
                rvItems.setVisibility(View.VISIBLE);
            } else {
                tvNoItems.setText(R.string.no_pending_invitations); // Use string resource
                tvNoItems.setVisibility(View.VISIBLE);
            }
        } else if (result instanceof ResultWrapper.Error) {
            tvNoItems.setText(getString(R.string.error_loading_invitations_prefix) + ((ResultWrapper.Error<?>) result).getMessage()); // Use string resource
            tvNoItems.setVisibility(View.VISIBLE);
            invitationAdapter.updateInvitations(new ArrayList<>(), currentEventListCache); // Clear adapter
        } else if (result instanceof ResultWrapper.Idle){
            tvNoItems.setText(R.string.no_pending_invitations);
            tvNoItems.setVisibility(View.VISIBLE);
            invitationAdapter.updateInvitations(new ArrayList<>(), currentEventListCache);
        }
    }

    private boolean isSameDay(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) return false;
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void setupBottomNavigation() {
        // Set listener first
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_create_event) {
                if (currentAuthToken == null) { // Check if user is logged in
                    Toast.makeText(getContext(), "Please login to create an event.", Toast.LENGTH_SHORT).show();
                    if(getView()!=null) Navigation.findNavController(getView()).navigate(R.id.action_mainFragment_to_loginActivity);
                    return false; // Prevent navigation to create event if not logged in
                }
                if (getView() != null) {
                    createEventViewModel.resetFormAndState(); // Reset ViewModel before navigating
                    Navigation.findNavController(getView()).navigate(R.id.action_mainFragment_to_createEventFragment);
                }
                return true; // Consume selection
            }
            // For other items, just handle the tab selection for data loading
            handleTabSelection(itemId);
            return true;
        });

        // Then set the selected item, which will trigger the listener if it's a new selection
        // or if reselection is enabled (which it is by default with setOnItemSelectedListener).
        if (bottomNavigationView.getSelectedItemId() == 0 ) { // If no item is selected (first launch)
            bottomNavigationView.setSelectedItemId(R.id.navigation_events_public);
        } else {
            // If an item is already selected (e.g. on configuration change), re-trigger its logic
            handleTabSelection(bottomNavigationView.getSelectedItemId());
        }
    }


    private void handleTabSelection(int itemId) {
        btnSelectDate.setVisibility(View.GONE); // Hide date filter by default

        if (rvItems.getAdapter() instanceof EventAdapter) {
            ((EventAdapter) rvItems.getAdapter()).updateEvents(new ArrayList<>());
        } else if (rvItems.getAdapter() instanceof InvitationAdapter) {
            ((InvitationAdapter) rvItems.getAdapter()).updateInvitations(new ArrayList<>(), new ArrayList<>());
        }
        tvNoItems.setVisibility(View.GONE);
        pbMainLoading.setVisibility(View.VISIBLE);


        if (itemId == R.id.navigation_events_known) {
            rvItems.setAdapter(eventAdapter);
            btnSelectDate.setVisibility(View.VISIBLE);
            if (currentAuthToken != null && loggedInUserId != null) {
                mainViewModel.loadAttendedEvents(loggedInUserId);
            } else {
                handleVisibility(pbMainLoading, false);
                tvNoItems.setText(R.string.login_to_see_attended_events_detail);
                tvNoItems.setVisibility(View.VISIBLE);
            }
        } else if (itemId == R.id.navigation_events_public) {
            rvItems.setAdapter(eventAdapter);
            btnSelectDate.setVisibility(View.VISIBLE);
            mainViewModel.loadPublicEvents(); // This will eventually update currentEventListCache via its observer
        } else if (itemId == R.id.navigation_invitations) {
            rvItems.setAdapter(invitationAdapter);
            if (currentAuthToken != null && loggedInUserId != null) {
                loadInvitationsForCurrentUser();
            } else {
                handleVisibility(pbMainLoading, false);
                tvNoItems.setText(R.string.login_to_view_invitations); // Use string resource
                tvNoItems.setVisibility(View.VISIBLE);
            }
        } else if (itemId == R.id.navigation_create_event) {
            // Navigation and ViewModel reset is handled in setOnItemSelectedListener
            // No specific data loading needed here FOR MainFragment itself
            handleVisibility(pbMainLoading, false); // Hide loading as we are navigating away or have shown login prompt
        }
    }

    private void loadInvitationsForCurrentUser() {
        if (loggedInUserId != null && currentAuthToken != null) {
            // Check if public events (for titles) are already loaded or being loaded
            ResultWrapper<List<EventModel>> publicEventsResult = mainViewModel.publicEventsState.getValue();
            if (publicEventsResult instanceof ResultWrapper.Success) {
                currentEventListCache = ((ResultWrapper.Success<List<EventModel>>) publicEventsResult).getData();
                if (currentEventListCache == null) currentEventListCache = new ArrayList<>();
                invitationViewModel.fetchInvitations(loggedInUserId, currentEventListCache);
            } else if (publicEventsResult instanceof ResultWrapper.Loading) {
                // Wait for public events to load, then fetch invitations
                mainViewModel.publicEventsState.observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<ResultWrapper<List<EventModel>>>() {
                    @Override
                    public void onChanged(ResultWrapper<List<EventModel>> updatedResult) {
                        if (updatedResult instanceof ResultWrapper.Success) {
                            currentEventListCache = ((ResultWrapper.Success<List<EventModel>>) updatedResult).getData();
                            if (currentEventListCache == null) currentEventListCache = new ArrayList<>();
                            invitationViewModel.fetchInvitations(loggedInUserId, currentEventListCache);
                            mainViewModel.publicEventsState.removeObserver(this); // Clean up observer
                        } else if (updatedResult instanceof ResultWrapper.Error) {
                            invitationViewModel.fetchInvitations(loggedInUserId, new ArrayList<>()); // Fetch with empty cache
                            mainViewModel.publicEventsState.removeObserver(this);
                        }
                        // If still loading, this observer will be called again.
                    }
                });
            } else { // Idle or Error for public events
                mainViewModel.loadPublicEvents(); // Trigger load for public events
                // And then observe it as above to fetch invitations once public events are available
                mainViewModel.publicEventsState.observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<ResultWrapper<List<EventModel>>>() {
                    @Override
                    public void onChanged(ResultWrapper<List<EventModel>> updatedResult) {
                        if (updatedResult instanceof ResultWrapper.Success) {
                            currentEventListCache = ((ResultWrapper.Success<List<EventModel>>) updatedResult).getData();
                            if (currentEventListCache == null) currentEventListCache = new ArrayList<>();
                            invitationViewModel.fetchInvitations(loggedInUserId, currentEventListCache);
                            mainViewModel.publicEventsState.removeObserver(this);
                        } else if (updatedResult instanceof ResultWrapper.Error) {
                            invitationViewModel.fetchInvitations(loggedInUserId, new ArrayList<>());
                            mainViewModel.publicEventsState.removeObserver(this);
                        }
                    }
                });
            }
        } else {
            handleVisibility(pbMainLoading, false);
            tvNoItems.setText(R.string.login_to_view_invitations);
            tvNoItems.setVisibility(View.VISIBLE);
            if (invitationAdapter != null) {
                invitationAdapter.updateInvitations(new ArrayList<>(), new ArrayList<>());
            }
        }
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onAcceptInvitation(InvitationModel invitation) {
        if (currentAuthToken != null) {
            invitationViewModel.acceptInvitation(invitation.getInvitationId(), currentAuthToken);
        } else {
            Toast.makeText(getContext(), "Please login to respond.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeclineInvitation(InvitationModel invitation) {
        if (currentAuthToken != null) {
            invitationViewModel.declineInvitation(invitation.getInvitationId(), currentAuthToken);
        } else {
            Toast.makeText(getContext(), "Please login to respond.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInvitationClick(InvitationModel invitation) {
        if (getView() != null && invitation.getEventId() != null) {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", invitation.getEventId());
            Navigation.findNavController(getView()).navigate(R.id.action_mainFragment_to_eventDetailFragment, bundle);
        }
    }

    // Add string resources to strings.xml:
    // <string name="no_pending_invitations">No pending invitations.</string>
    // <string name="error_loading_invitations_prefix">Error loading invitations: </string>
    // <string name="login_to_view_invitations">Login to view invitations.</string>
}