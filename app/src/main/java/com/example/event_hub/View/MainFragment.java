// src/main/java/com/example/event_hub/View/MainFragment.java
package com.example.event_hub.View;

import android.app.DatePickerDialog;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class MainFragment extends Fragment implements InvitationAdapter.OnInvitationActionListener {

    private MainViewModel mainViewModel;
    private AuthViewModel authViewModel;
    private InvitationViewModel invitationViewModel;
    private CreateEventViewModel createEventViewModel;

    private EventAdapter eventAdapter;
    private InvitationAdapter invitationAdapter;

    private RecyclerView rvItems;
    private TextView tvNoItems;

    private ProgressBar pbMainLoading;

    private TextInputLayout tilEventNameFilter, tilStartDateFilter, tilEndDateFilter;
    private TextInputEditText etEventNameFilter, etStartDateFilter, etEndDateFilter;
    private MaterialButton btnApplyFiltersMain;

    private final Calendar filterStartDateCalendar = Calendar.getInstance();
    private final Calendar filterEndDateCalendar = Calendar.getInstance();

    private Long loggedInUserId;
    private String currentAuthToken;
    private List<EventModel> allPublicEventsCacheForInvitations = new ArrayList<>(); // Still needed for invitation adapter

    // Pagination state variables for events
    private int currentPage = 0;
    private boolean isLastPage = false;
    private boolean isLoading = false;
    private List<EventModel> currentEventList = new ArrayList<>();

    // Pagination state variables for invitations
    private int invitationsCurrentPage = 0;
    private boolean invitationsIsLastPage = false;
    private boolean invitationsIsLoading = false;
    private List<InvitationModel> currentInvitationList = new ArrayList<>();

    private boolean isViewsBound = false; // New flag to track if views are bound

    private int currentSelectedTabId = R.id.navigation_events_public; // Default tab
    private final int PAGE_SIZE = 5; // Number of items per page, reduced for performance

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
                bundle.putLong("eventId", event.getId());
                Navigation.findNavController(getView()).navigate(R.id.action_mainFragment_to_eventDetailFragment, bundle);
            }
        });
        invitationAdapter = new InvitationAdapter(new ArrayList<>(), new ArrayList<>(), this);
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
        setupFilterListeners();
        observeViewModels();

        // Initial setup for filter UI visibility (hidden by default)
        tilEventNameFilter.setVisibility(View.GONE);
        tilStartDateFilter.setVisibility(View.GONE);
        tilEndDateFilter.setVisibility(View.GONE);
        btnApplyFiltersMain.setVisibility(View.GONE);

        // Initial state message for the default public events tab
        tvNoItems.setText(R.string.no_public_events_available);
        tvNoItems.setVisibility(View.VISIBLE);

        isViewsBound = true; // Set flag when views are successfully bound
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-load data for the currently selected tab on resume, resetting pagination
        handleTabSelection(currentSelectedTabId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewsBound = false; // Reset flag when views are destroyed
        // Clear accumulated lists and adapter data to release memory
        currentEventList.clear();
        currentInvitationList.clear();
        allPublicEventsCacheForInvitations.clear(); // Also clear this cache

        // Submit empty lists to adapters to clear their internal data
        if (eventAdapter != null) {
            eventAdapter.updateEvents(new ArrayList<>());
        }
        if (invitationAdapter != null) {
            invitationAdapter.updateInvitations(new ArrayList<>(), new ArrayList<>());
        }

        // Reset pagination state variables
        currentPage = 0;
        isLastPage = false;
        isLoading = false;
        invitationsCurrentPage = 0;
        invitationsIsLastPage = false;
        invitationsIsLoading = false;
    }

    private void bindViews(View view) {
        rvItems = view.findViewById(R.id.rv_items_main);
        tvNoItems = view.findViewById(R.id.tv_no_items_main);
        pbMainLoading = view.findViewById(R.id.pb_main_loading);

        tilEventNameFilter = view.findViewById(R.id.til_event_name_filter);
        etEventNameFilter = view.findViewById(R.id.et_event_name_filter);
        tilStartDateFilter = view.findViewById(R.id.til_start_date_filter);
        etStartDateFilter = view.findViewById(R.id.et_start_date_filter);
        tilEndDateFilter = view.findViewById(R.id.til_end_date_filter);
        etEndDateFilter = view.findViewById(R.id.et_end_date_filter);
        btnApplyFiltersMain = view.findViewById(R.id.btn_apply_filters_main);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvItems.setLayoutManager(layoutManager);
        rvItems.setAdapter(eventAdapter); // Default adapter for events

        rvItems.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Only consider scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (currentSelectedTabId == R.id.navigation_invitations) {
                        if (!invitationsIsLoading && !invitationsIsLastPage) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                    && firstVisibleItemPosition >= 0
                                    && totalItemCount >= PAGE_SIZE) { // Only load if enough items to scroll
                                invitationsCurrentPage++;
                                loadInvitationsForCurrentTab();
                            }
                        }
                    } else { // For event tabs
                        if (!isLoading && !isLastPage) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                    && firstVisibleItemPosition >= 0
                                    && totalItemCount >= PAGE_SIZE) { // Only load if enough items to scroll
                                currentPage++;
                                loadDataForCurrentTab();
                            }
                        }
                    }
                }
            }
        });
    }

    private void setupFilterListeners() {
        etStartDateFilter.setOnClickListener(v -> showDatePickerDialogForFilter(filterStartDateCalendar, etStartDateFilter));
        etEndDateFilter.setOnClickListener(v -> showDatePickerDialogForFilter(filterEndDateCalendar, etEndDateFilter));

        btnApplyFiltersMain.setOnClickListener(v -> {
            currentPage = 0; // Reset page for new filter application
            isLastPage = false;
            // The list will be cleared by loadDataForCurrentTab() before the call
            loadDataForCurrentTab(); // Load data with applied filters
        });
    }

    private void showDatePickerDialogForFilter(Calendar calendar, TextInputEditText targetEditText) {
        if (getContext() == null) return;
        new DatePickerDialog(getContext(), (view, year, month, day) -> {
            calendar.set(year, month, day);

            // Set time components explicitly for filtering to cover full day
            if (targetEditText.getId() == R.id.et_start_date_filter) {
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            } else if (targetEditText.getId() == R.id.et_end_date_filter) {
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
            }
            // Update the text field for user display (still "yyyy-MM-dd" format)
            targetEditText.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }


    private void observeViewModels() {
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), id -> {
            boolean changed = !Objects.equals(loggedInUserId, id);
            loggedInUserId = id;
            if (changed) {
                handleTabSelection(currentSelectedTabId); // Re-trigger load for current tab
            }
        });

        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            boolean changed = !Objects.equals(currentAuthToken, token);
            currentAuthToken = token;
            if (changed) {
                handleTabSelection(currentSelectedTabId); // Re-trigger load for current tab
            }
        });

        mainViewModel.publicEventsState.observe(getViewLifecycleOwner(), result -> {
            if (currentSelectedTabId == R.id.navigation_events_public) {
                handleEventListResult(result, R.string.no_public_events_available, R.string.error_loading_events_prefix);
            }
        });

        mainViewModel.attendedEventsState.observe(getViewLifecycleOwner(), result -> {
            if (currentSelectedTabId == R.id.navigation_events_known) {
                handleEventListResult(result, R.string.no_attended_events_found, R.string.error_loading_attended_events_prefix);
            }
        });

        mainViewModel.myCreatedEventsState.observe(getViewLifecycleOwner(), result -> {
            if (currentSelectedTabId == R.id.navigation_events_created) {
                handleEventListResult(result, R.string.no_created_events_found, R.string.error_loading_created_events_prefix);
            }
        });

        invitationViewModel.invitationsState.observe(getViewLifecycleOwner(), result -> {
            if (currentSelectedTabId == R.id.navigation_invitations) {
                handleInvitationListResult(result);
            }
        });

        invitationViewModel.actionStatus.observe(getViewLifecycleOwner(), result -> {
            if (!(result instanceof ResultWrapper.Loading || result instanceof ResultWrapper.Idle)) {
                if (result instanceof ResultWrapper.Success) {
                    Toast.makeText(getContext(), R.string.invitation_action_successful, Toast.LENGTH_SHORT).show();
                    if (isUserLoggedIn()) {
                        // Refresh invitations and attended events after action
                        invitationsCurrentPage = 0; // Reset pagination for invitations
                        invitationsIsLastPage = false;
                        currentInvitationList.clear();
                        loadInvitationsForCurrentTab();
                        mainViewModel.loadAttendedEvents(0, PAGE_SIZE, null, null, null, null); // Refresh attended events
                    }
                } else if (result instanceof ResultWrapper.Error) {
                    String msg = ((ResultWrapper.Error<?>) result).getMessage();
                    Toast.makeText(getContext(), getString(R.string.invitation_action_failed, (msg != null ? msg : getString(R.string.unknown_error))), Toast.LENGTH_SHORT).show();
                }
                invitationViewModel.resetActionStatus();
            }
        });
    }

    private void handleEventListResult(ResultWrapper<List<EventModel>> result, int noEventsMessageResId, int errorMessagePrefixResId) {
        isLoading = false;
        setLoadingState(false); // Hide loading after result

        if (result instanceof ResultWrapper.Success) {
            List<EventModel> newEvents = ((ResultWrapper.Success<List<EventModel>>) result).getData();
            // The list is already cleared in loadDataForCurrentTab() if currentPage == 0
            if (newEvents != null) {
                currentEventList.addAll(newEvents); // Append new data
                isLastPage = newEvents.size() < PAGE_SIZE; // Determine if this is the last page
            } else {
                isLastPage = true; // No new data, so it's the last page
            }

            eventAdapter.updateEvents(currentEventList); // Update adapter with accumulated list

            if (currentEventList.isEmpty()) {
                tvNoItems.setText(noEventsMessageResId);
                tvNoItems.setVisibility(View.VISIBLE);
                rvItems.setVisibility(View.GONE);
            } else {
                tvNoItems.setVisibility(View.GONE);
                rvItems.setVisibility(View.VISIBLE);
            }
        } else if (result instanceof ResultWrapper.Error) {
            String msg = ((ResultWrapper.Error<?>) result).getMessage();
            tvNoItems.setText(getString(errorMessagePrefixResId, (msg != null ? msg : getString(R.string.unknown_error))));
            tvNoItems.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            currentEventList.clear(); // Clear list on error
            eventAdapter.updateEvents(new ArrayList<>());
            isLastPage = true; // No more data can be loaded on error
        } else if (result instanceof ResultWrapper.Idle) {
            tvNoItems.setText(noEventsMessageResId);
            tvNoItems.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            currentEventList.clear(); // Clear list on idle
            eventAdapter.updateEvents(new ArrayList<>());
            isLastPage = true; // No data, so last page
        }
    }

    private void handleInvitationListResult(ResultWrapper<List<InvitationModel>> result) {
        invitationsIsLoading = false;
        setLoadingState(false); // Hide loading after result

        if (result instanceof ResultWrapper.Success) {
            List<InvitationModel> newInvitations = ((ResultWrapper.Success<List<InvitationModel>>) result).getData();
            // The list is already cleared in loadInvitationsForCurrentTab() if invitationsCurrentPage == 0
            if (newInvitations != null) {
                currentInvitationList.addAll(newInvitations); // Append new data
                invitationsIsLastPage = newInvitations.size() < PAGE_SIZE; // Determine if last page
            } else {
                invitationsIsLastPage = true;
            }

            invitationAdapter.updateInvitations(currentInvitationList, allPublicEventsCacheForInvitations);

            if (currentInvitationList.isEmpty()) {
                tvNoItems.setText(R.string.no_pending_invitations);
                tvNoItems.setVisibility(View.VISIBLE);
                rvItems.setVisibility(View.GONE);
            } else {
                tvNoItems.setVisibility(View.GONE);
                rvItems.setVisibility(View.VISIBLE);
            }
        } else if (result instanceof ResultWrapper.Error) {
            String msg = ((ResultWrapper.Error<?>) result).getMessage();
            tvNoItems.setText(getString(R.string.error_loading_invitations_prefix) + (msg != null ? msg : getString(R.string.unknown_error)));
            tvNoItems.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            currentInvitationList.clear();
            invitationAdapter.updateInvitations(new ArrayList<>(), allPublicEventsCacheForInvitations);
            invitationsIsLastPage = true;
        } else if (result instanceof ResultWrapper.Idle) {
            tvNoItems.setText(R.string.no_pending_invitations);
            tvNoItems.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            currentInvitationList.clear();
            invitationAdapter.updateInvitations(new ArrayList<>(), allPublicEventsCacheForInvitations);
            invitationsIsLastPage = true;
        }
    }


    public void handleTabSelection(int itemId) {
        // Always show loading initially for responsiveness
        setLoadingState(true);

        if (itemId == R.id.navigation_create_event) {
            setLoadingState(false);
            displayMessageAndHideLoading(R.string.login_to_create_event);
            return;
        }

        // Reset pagination and loading states only if tab actually changed
        if (itemId != currentSelectedTabId) {
            currentSelectedTabId = itemId;
            currentPage = 0;
            isLastPage = false;
            isLoading = false;
            currentEventList.clear(); // Clear local list
            eventAdapter.updateEvents(new ArrayList<>()); // Clear adapter immediately

            invitationsCurrentPage = 0;
            invitationsIsLastPage = false;
            invitationsIsLoading = false;
            currentInvitationList.clear(); // Clear local list
            invitationAdapter.updateInvitations(new ArrayList<>(), new ArrayList<>()); // Clear adapter immediately

            // Set the correct adapter based on the selected tab
            rvItems.setAdapter(itemId == R.id.navigation_invitations ? invitationAdapter : eventAdapter);
        }

        // Control visibility of filter UI ONLY if views are bound
        if (isViewsBound) { // New condition to prevent NullPointerException
            boolean isPublicEventsTab = (itemId == R.id.navigation_events_public);
            tilEventNameFilter.setVisibility(isPublicEventsTab ? View.VISIBLE : View.GONE);
            tilStartDateFilter.setVisibility(isPublicEventsTab ? View.VISIBLE : View.GONE);
            tilEndDateFilter.setVisibility(isPublicEventsTab ? View.VISIBLE : View.GONE);
            btnApplyFiltersMain.setVisibility(isPublicEventsTab ? View.VISIBLE : View.GONE);

            // Clear filter inputs when switching off public events tab
            if (!isPublicEventsTab) {
                etEventNameFilter.setText("");
                etStartDateFilter.setText("");
                etEndDateFilter.setText("");
                filterStartDateCalendar.clear();
                filterEndDateCalendar.clear();
            }
        }


        if (itemId == R.id.navigation_invitations) {
            loadInvitationsForCurrentTab();
        } else {
            loadDataForCurrentTab();
        }
    }


    private void loadDataForCurrentTab() {
        if (isLoading || isLastPage) return;

        isLoading = true;
        setLoadingState(true); // Show loading spinner before API call

        // Clear list and notify adapter immediately if loading first page/applying filters
        if (currentPage == 0) {
            currentEventList.clear();
            eventAdapter.updateEvents(new ArrayList<>()); // Notify adapter that list is empty
        }

        String eventNameFilterForApi = null;
        Date startDateFilterForApi = null;
        Date endDateFilterForApi = null;
        List<String> sortCriteriaForApi = new ArrayList<>();


        if (currentSelectedTabId == R.id.navigation_events_public || currentSelectedTabId == R.id.navigation_events_created) {
            // Apply filters only for Public and My Events tabs
            if (isViewsBound) { // Only access views if they are bound
                eventNameFilterForApi = etEventNameFilter.getText().toString().trim();
                startDateFilterForApi = !etStartDateFilter.getText().toString().isEmpty() ? filterStartDateCalendar.getTime() : null;
                endDateFilterForApi = !etEndDateFilter.getText().toString().isEmpty() ? filterEndDateCalendar.getTime() : null;
            } else { // If views are not bound, set filters to null to avoid crash
                eventNameFilterForApi = null;
                startDateFilterForApi = null;
                endDateFilterForApi = null;
            }
            sortCriteriaForApi = Arrays.asList("startDate,asc");
        }


        if (currentSelectedTabId == R.id.navigation_events_public) {
            mainViewModel.loadPublicEvents(currentPage, PAGE_SIZE, eventNameFilterForApi, startDateFilterForApi, endDateFilterForApi, sortCriteriaForApi);
        } else if (currentSelectedTabId == R.id.navigation_events_known) {
            if (isUserLoggedIn()) {
                mainViewModel.loadAttendedEvents(currentPage, PAGE_SIZE, null, null, null, null);
            } else {
                displayMessageAndHideLoading(R.string.login_to_see_attended_events_detail);
                isLoading = false;
                isLastPage = true;
                currentEventList.clear();
                eventAdapter.updateEvents(new ArrayList<>());
            }
        } else if (currentSelectedTabId == R.id.navigation_events_created) {
            if (isUserLoggedIn()) {
                mainViewModel.loadCreatedEvents(currentPage, PAGE_SIZE, eventNameFilterForApi, startDateFilterForApi, endDateFilterForApi, sortCriteriaForApi);
            } else {
                displayMessageAndHideLoading(R.string.login_to_see_created_events_detail);
                isLoading = false;
                isLastPage = true;
                currentEventList.clear();
                eventAdapter.updateEvents(new ArrayList<>());
            }
        }
    }

    private void loadInvitationsForCurrentTab() {
        if (invitationsIsLoading || invitationsIsLastPage) return;

        invitationsIsLoading = true;
        setLoadingState(true); // Show loading spinner before API call

        // Clear list and notify adapter immediately if loading first page
        if (invitationsCurrentPage == 0) {
            currentInvitationList.clear();
            invitationAdapter.updateInvitations(new ArrayList<>(), allPublicEventsCacheForInvitations); // Clear adapter immediately
        }

        if (isUserLoggedIn()) {
            invitationViewModel.fetchInvitations(invitationsCurrentPage, PAGE_SIZE);
        } else {
            displayMessageAndHideLoading(R.string.login_to_view_invitations);
            invitationsIsLoading = false;
            invitationsIsLastPage = true;
            currentInvitationList.clear();
            invitationAdapter.updateInvitations(new ArrayList<>(), allPublicEventsCacheForInvitations);
        }
    }


    private boolean isUserLoggedIn() {
        return currentAuthToken != null && loggedInUserId != null;
    }

    private void setLoadingState(boolean isLoading) {
        if (pbMainLoading != null) {
            pbMainLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Add null checks for filter UI elements, as they might not be initialized yet when this method is called early
        if (etEventNameFilter != null) {
            etEventNameFilter.setEnabled(!isLoading);
        }
        if (etStartDateFilter != null) {
            etStartDateFilter.setEnabled(!isLoading);
        }
        if (etEndDateFilter != null) {
            etEndDateFilter.setEnabled(!isLoading);
        }
        if (btnApplyFiltersMain != null) {
            btnApplyFiltersMain.setEnabled(!isLoading);
        }
        if (rvItems != null) {
            rvItems.setEnabled(!isLoading);
        }
    }

    private void displayMessageAndHideLoading(int messageResId) {
        setLoadingState(false);
        if (tvNoItems != null) {
            tvNoItems.setText(messageResId);
            tvNoItems.setVisibility(View.VISIBLE);
        }
        if (rvItems != null) {
            rvItems.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAcceptInvitation(InvitationModel invitation) {
        if (currentAuthToken != null) {
            invitationViewModel.acceptInvitation(invitation.getInvitationId(), currentAuthToken);
        } else {
            Toast.makeText(getContext(), R.string.toast_login_required_for_action, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeclineInvitation(InvitationModel invitation) {
        if (currentAuthToken != null) {
            invitationViewModel.declineInvitation(invitation.getInvitationId(), currentAuthToken);
        } else {
            Toast.makeText(getContext(), R.string.toast_login_required_for_action, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInvitationClick(InvitationModel invitation) {
        if (getView() != null && invitation.getEvent() != null && invitation.getEvent().getId() != null) {
            Bundle bundle = new Bundle();
            bundle.putLong("eventId", invitation.getEvent().getId());
            Navigation.findNavController(getView()).navigate(R.id.action_mainFragment_to_eventDetailFragment, bundle);
        }
    }
}