// src/main/java/com/example/event_hub/View/InviteUsersFragment.java
package com.example.event_hub.View;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.R;
import com.example.event_hub.View.adapter.UserInviteAdapter;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.InvitationViewModel;
import com.example.event_hub.ViewModel.ProfileViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class InviteUsersFragment extends Fragment implements UserInviteAdapter.OnUserSelectionChangedListener {

    private InvitationViewModel invitationViewModel;
    private ProfileViewModel profileViewModel;
    private AuthViewModel authViewModel;

    private TextInputEditText etSearchByName, etSearchByLogin;
    private RecyclerView rvUsersToInvite;
    private UserInviteAdapter userInviteAdapter;
    private MaterialButton btnSendInvitations;
    private MaterialButton btnApplyUserFilters;
    private ProgressBar pbLoadingUsers;
    private TextView tvNoUsersFound;

    private long eventId;
    private String currentAuthToken;
    private final List<UserModel> allFetchedUsers = new ArrayList<>();

    private int usersCurrentPage = 0;
    private boolean usersIsLastPage = false;
    private boolean usersIsLoading = false;
    private final int USER_PAGE_SIZE = 5;

    // Counters for batch invitation sending
    private int pendingInvitationResponses = 0;
    private int successfulInvitationResponses = 0;
    private int failedInvitationResponses = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        invitationViewModel = new ViewModelProvider(this).get(InvitationViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (getArguments() != null) {
            eventId = getArguments().getLong("eventId");
        }
        userInviteAdapter = new UserInviteAdapter(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invite_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();
        setupUserFilterListeners();
        setupClickListeners();
        observeViewModels();

        if (eventId <= 0) {
            Toast.makeText(getContext(), R.string.error_event_id_missing, Toast.LENGTH_LONG).show();
            if(getView() != null) Navigation.findNavController(getView()).popBackStack();
            return;
        }

        tvNoUsersFound.setText(R.string.search_for_users_to_invite);
        tvNoUsersFound.setVisibility(View.VISIBLE);
        updateSendButtonState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear the accumulated list and adapter data to release memory
        allFetchedUsers.clear();
        userInviteAdapter.submitList(new ArrayList<>());
        // Reset pagination state variables
        usersCurrentPage = 0;
        usersIsLastPage = false;
        usersIsLoading = false;
    }

    private void bindViews(View view) {
        tvNoUsersFound = view.findViewById(R.id.tv_no_users_found_invite);
        etSearchByName = view.findViewById(R.id.et_search_by_name_invite);
        etSearchByLogin = view.findViewById(R.id.et_search_by_login_invite);
        btnApplyUserFilters = view.findViewById(R.id.btn_apply_user_filters);
        rvUsersToInvite = view.findViewById(R.id.rv_users_to_invite);
        btnSendInvitations = view.findViewById(R.id.btn_send_invitations);
        pbLoadingUsers = view.findViewById(R.id.pb_invite_users_loading);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvUsersToInvite.setLayoutManager(layoutManager);
        rvUsersToInvite.setAdapter(userInviteAdapter);

        rvUsersToInvite.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Only consider scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!usersIsLoading && !usersIsLastPage) {
                        // Check if we are at the end of the list and there are enough items to warrant loading more
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                                && totalItemCount >= USER_PAGE_SIZE) { // Only load if enough items to scroll initially
                            usersCurrentPage++;
                            loadUsers(); // Load the next page
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onUserSelectionChanged() {
        updateSendButtonState();
    }

    private void updateSendButtonState() {
        boolean enableButton = currentAuthToken != null && !userInviteAdapter.getSelectedUsers().isEmpty();
        btnSendInvitations.setEnabled(enableButton);
    }

    private void setupUserFilterListeners() {
        btnApplyUserFilters.setOnClickListener(v -> {
            // Apply filters resets the list and starts from page 0
            usersCurrentPage = 0;
            usersIsLastPage = false;
            // The list will be cleared by the observer when new data for page 0 arrives
            loadUsers(); // Trigger loading of the first page with new filters
        });

        TextWatcher filterTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Enable apply button if either search field has text
                boolean enableApplyButton = !etSearchByName.getText().toString().trim().isEmpty() ||
                        !etSearchByLogin.getText().toString().trim().isEmpty();
                btnApplyUserFilters.setEnabled(enableApplyButton);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etSearchByName.addTextChangedListener(filterTextWatcher);
        etSearchByLogin.addTextChangedListener(filterTextWatcher);

        btnApplyUserFilters.setEnabled(false); // Initially disable the apply filter button
    }


    private void loadUsers() {
        if (usersIsLoading || usersIsLastPage) return;

        usersIsLoading = true; // Use usersIsLoading here
        handleLoadingVisibility(true); // Show loading spinner before API call

        // Clear list and notify adapter immediately if loading first page/applying filters
        if (usersCurrentPage == 0) {
            allFetchedUsers.clear();
            userInviteAdapter.submitList(new ArrayList<>()); // Notify adapter that list is empty
        }

        String nameFilterInput = etSearchByName.getText().toString().trim();
        String loginFilterInput = etSearchByLogin.getText().toString().trim();

        String nameFilterForApi = null;
        String loginFilterForApi = null;

        // Logic to filter by only one field at a time (prioritize name over login)
        if (!nameFilterInput.isEmpty()) {
            nameFilterForApi = nameFilterInput;
            loginFilterForApi = null; // Ensure login filter is null if name filter is used
        } else if (!loginFilterInput.isEmpty()) {
            loginFilterForApi = loginFilterInput;
            nameFilterForApi = null; // Ensure name filter is null if login filter is used
        }
        // If both are empty, both nameFilterForApi and loginFilterForApi remain null,
        // leading to a fetch of all users (or the first page of all users).


        profileViewModel.fetchAllUsers(currentAuthToken, usersCurrentPage, USER_PAGE_SIZE,
                nameFilterForApi,
                loginFilterForApi,
                null); // No specific sort for users for now
    }


    private void setupClickListeners() {
        btnSendInvitations.setOnClickListener(v -> {
            if (currentAuthToken == null) {
                Toast.makeText(getContext(), R.string.auth_required_for_invites, Toast.LENGTH_SHORT).show();
                return;
            }
            List<UserModel> selectedUsers = userInviteAdapter.getSelectedUsers();
            if (selectedUsers.isEmpty()) {
                Toast.makeText(getContext(), R.string.no_users_selected, Toast.LENGTH_SHORT).show();
                return;
            }

            pendingInvitationResponses = selectedUsers.size();
            successfulInvitationResponses = 0;
            failedInvitationResponses = 0;

            Toast.makeText(getContext(), R.string.sending_invites, Toast.LENGTH_SHORT).show();
            for (UserModel user : selectedUsers) {
                invitationViewModel.sendInvitation(eventId, user.getId(), currentAuthToken);
            }
            userInviteAdapter.clearSelections();
        });
    }

    private void observeViewModels() {
        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            currentAuthToken = token;
            // Only load initially if token is available AND no users have been fetched yet
            // AND we are not already loading. This prevents redundant initial fetches.
            if (token != null && allFetchedUsers.isEmpty() && !usersIsLoading){
                loadUsers(); // Initial load of users
            }
            updateSendButtonState();
        });

        profileViewModel.allUserAccountsState.observe(getViewLifecycleOwner(), result -> {
            usersIsLoading = false; // Set loading to false regardless of success/error
            handleLoadingVisibility(false); // Hide loading spinner

            if (result instanceof ResultWrapper.Success) {
                PaginatedResponse<UserModel> response = ((ResultWrapper.Success<PaginatedResponse<UserModel>>) result).getData();
                if (response != null && response.getContent() != null) {
                    if (usersCurrentPage == 0) {
                        // allFetchedUsers.clear(); // This is now done in loadUsers()
                    }
                    allFetchedUsers.addAll(response.getContent()); // Add new items to the accumulated list
                    usersIsLastPage = response.getContent().size() < USER_PAGE_SIZE; // Determine if this is the last page
                    if (response.isLast()) { // Also check if the backend indicates it's the last page
                        usersIsLastPage = true;
                    }
                } else {
                    usersIsLastPage = true; // No content or null response means last page
                }

                // Submit a new ArrayList instance to ensure DiffUtil detects changes correctly
                userInviteAdapter.submitList(new ArrayList<>(allFetchedUsers));

                if (allFetchedUsers.isEmpty()) {
                    tvNoUsersFound.setText(R.string.no_users_found); // Use a more general "no users" message
                    tvNoUsersFound.setVisibility(View.VISIBLE);
                    rvUsersToInvite.setVisibility(View.GONE);
                } else {
                    tvNoUsersFound.setVisibility(View.GONE);
                    rvUsersToInvite.setVisibility(View.VISIBLE);
                }
            } else if (result instanceof ResultWrapper.Error) {
                String errorMessage = ((ResultWrapper.Error<?>) result).getMessage();
                tvNoUsersFound.setText(getString(R.string.error_fetching_users, errorMessage != null ? errorMessage : getString(R.string.unknown_error)));
                tvNoUsersFound.setVisibility(View.VISIBLE);
                rvUsersToInvite.setVisibility(View.GONE);
                allFetchedUsers.clear(); // Clear list on error
                userInviteAdapter.submitList(new ArrayList<>());
                usersIsLastPage = true; // No more data can be loaded on error
            } else if (result instanceof ResultWrapper.Idle) {
                tvNoUsersFound.setText(R.string.search_for_users_to_invite); // Initial message
                tvNoUsersFound.setVisibility(View.VISIBLE);
                rvUsersToInvite.setVisibility(View.GONE);
                allFetchedUsers.clear(); // Clear list on idle
                userInviteAdapter.submitList(new ArrayList<>());
                usersIsLastPage = true; // No data, so last page
            }
            // Always ensure the apply filters button is enabled/disabled correctly after loading.
            boolean enableApplyButton = !etSearchByName.getText().toString().trim().isEmpty() ||
                    !etSearchByLogin.getText().toString().trim().isEmpty();
            btnApplyUserFilters.setEnabled(enableApplyButton);
        });

        invitationViewModel.actionStatus.observe(getViewLifecycleOwner(), result -> {
            if (!(result instanceof ResultWrapper.Loading || result instanceof ResultWrapper.Idle)) {
                if (result instanceof ResultWrapper.Success) {
                    successfulInvitationResponses++;
                } else if (result instanceof ResultWrapper.Error) {
                    failedInvitationResponses++;
                }

                pendingInvitationResponses--; // Decrement the count of pending responses

                if (pendingInvitationResponses == 0) { // All responses for the current batch have been received
                    if (failedInvitationResponses == 0) {
                        Toast.makeText(getContext(), R.string.invitations_sent_success, Toast.LENGTH_SHORT).show();
                    } else if (successfulInvitationResponses > 0) {
                        Toast.makeText(getContext(), getString(R.string.invitations_sent_with_some_failures,
                                successfulInvitationResponses, failedInvitationResponses), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), R.string.invitations_sent_all_failed, Toast.LENGTH_LONG).show();
                    }
                    // Reset counters for the next batch
                    successfulInvitationResponses = 0;
                    failedInvitationResponses = 0;
                }
                // Reset action status to allow new actions to be observed.
                // This is crucial for the batch processing to work correctly for subsequent batches.
                invitationViewModel.resetActionStatus();
            }
        });
    }

    private void handleLoadingVisibility(boolean isLoading) {
        if (pbLoadingUsers != null) {
            pbLoadingUsers.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Optionally disable UI elements while loading
        btnSendInvitations.setEnabled(!isLoading);
        etSearchByName.setEnabled(!isLoading);
        etSearchByLogin.setEnabled(!isLoading);
        btnApplyUserFilters.setEnabled(!isLoading); // Control apply button during loading
        rvUsersToInvite.setEnabled(!isLoading);
    }
}