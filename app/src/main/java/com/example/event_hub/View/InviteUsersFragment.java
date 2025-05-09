package com.example.event_hub.View;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.EditText; // Using TextInputEditText
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
import java.util.stream.Collectors;

public class InviteUsersFragment extends Fragment {

    private InvitationViewModel invitationViewModel;
    private ProfileViewModel profileViewModel;
    private AuthViewModel authViewModel;

    private TextInputEditText etSearchUsers;
    private RecyclerView rvUsersToInvite;
    private UserInviteAdapter userInviteAdapter;
    private MaterialButton btnSendInvitations;
    private ProgressBar pbLoadingUsers;
    private TextView tvNoUsersFound;

    private String eventId;
    private List<UserModel> allFetchedUsers = new ArrayList<>(); // This should be populated by ProfileViewModel
    // private List<UserModel> selectedUsersToInvite = new ArrayList<>(); // Adapter will manage selected users

    public InviteUsersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        invitationViewModel = new ViewModelProvider(this).get(InvitationViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (getArguments() != null) {
            // If using Safe Args: InviteUsersFragmentArgs.fromBundle(getArguments()).getEventId();
            eventId = getArguments().getString("eventId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invite_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();
        setupSearch();
        setupClickListeners();
        observeViewModels();

        if (eventId == null) {
            Toast.makeText(getContext(), R.string.error_event_id_not_found, Toast.LENGTH_LONG).show();
            if (getView() != null) Navigation.findNavController(getView()).popBackStack();
            return;
        }

        // Initial state for the list
        tvNoUsersFound.setText(R.string.search_to_find_users_for_invite);
        tvNoUsersFound.setVisibility(View.VISIBLE);
        rvUsersToInvite.setVisibility(View.GONE);
        updateSendButtonState(); // Initial button state
    }

    private void bindViews(View view) {
        etSearchUsers = view.findViewById(R.id.et_search_users_invite);
        rvUsersToInvite = view.findViewById(R.id.rv_users_to_invite);
        btnSendInvitations = view.findViewById(R.id.btn_send_invitations);
        pbLoadingUsers = view.findViewById(R.id.pb_invite_users_loading);
        tvNoUsersFound = view.findViewById(R.id.tv_no_users_found_invite);
    }

    private void setupRecyclerView() {
        userInviteAdapter = new UserInviteAdapter(new ArrayList<>(), (user, isSelected) -> {
            // This callback is invoked when a checkbox state changes in the adapter
            updateSendButtonState();
        });
        rvUsersToInvite.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsersToInvite.setAdapter(userInviteAdapter);
    }

    private void updateSendButtonState() {
        if (userInviteAdapter != null) {
            btnSendInvitations.setEnabled(!userInviteAdapter.getSelectedUsers().isEmpty());
        } else {
            btnSendInvitations.setEnabled(false);
        }
    }

    private void setupSearch() {
        etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO: Call profileViewModel.searchUsers(s.toString());
                // For now, client-side filtering if allFetchedUsers is populated
                filterLocalUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Placeholder for client-side filtering. Real implementation should use ViewModel for search.
    private void filterLocalUsers(String query) {
        if (allFetchedUsers.isEmpty() && query.isEmpty()) {
            userInviteAdapter.updateUsers(new ArrayList<>());
            tvNoUsersFound.setText(R.string.search_to_find_users_for_invite);
            tvNoUsersFound.setVisibility(View.VISIBLE);
            rvUsersToInvite.setVisibility(View.GONE);
            updateSendButtonState();
            return;
        }

        List<UserModel> filteredList = new ArrayList<>();
        if (!query.isEmpty()) {
            for (UserModel user : allFetchedUsers) { // allFetchedUsers should be populated from ViewModel
                if ((user.getLogin() != null && user.getLogin().toLowerCase().contains(query.toLowerCase())) ||
                        (user.getUserDetails() != null && user.getUserDetails().getFullName() != null && user.getUserDetails().getFullName().toLowerCase().contains(query.toLowerCase()))) {
                    filteredList.add(user);
                }
            }
        } else {
            // If query is empty, show all fetched users (if any were fetched)
            // Or, require search to show any users. For now, let's assume we show all if query is empty and list isn't.
            // filteredList.addAll(allFetchedUsers);
        }

        if (!filteredList.isEmpty()){
            userInviteAdapter.updateUsers(filteredList);
            tvNoUsersFound.setVisibility(View.GONE);
            rvUsersToInvite.setVisibility(View.VISIBLE);
        } else if (!query.isEmpty()) { // Searched but no results
            userInviteAdapter.updateUsers(new ArrayList<>());
            tvNoUsersFound.setText(R.string.no_users_found_for_invite);
            tvNoUsersFound.setVisibility(View.VISIBLE);
            rvUsersToInvite.setVisibility(View.GONE);
        } else { // Query is empty and no base list to show (e.g. allFetchedUsers is empty)
            userInviteAdapter.updateUsers(new ArrayList<>());
            tvNoUsersFound.setText(R.string.search_to_find_users_for_invite);
            tvNoUsersFound.setVisibility(View.VISIBLE);
            rvUsersToInvite.setVisibility(View.GONE);
        }
        updateSendButtonState();
    }


    private void setupClickListeners() {
        btnSendInvitations.setOnClickListener(v -> {
            List<UserModel> selectedUsers = userInviteAdapter.getSelectedUsers();
            if (selectedUsers.isEmpty()) {
                Toast.makeText(getContext(), R.string.no_users_selected_for_invite, Toast.LENGTH_SHORT).show();
                return;
            }
            // List<String> userIdsToInvite = selectedUsers.stream()
            //                                 .map(UserModel::getUserId)
            //                                 .collect(Collectors.toList());
            // TODO: Call invitationViewModel.sendInvitations(eventId, userIdsToInvite);
            // This method needs to be created in InvitationViewModel and EventHubRepository
            Toast.makeText(getContext(), "Sending " + selectedUsers.size() + " invitations for event " + eventId + " (TBD)", Toast.LENGTH_LONG).show();
        });
    }

    private void observeViewModels() {
        // TODO: Observe user search results from ProfileViewModel
        // profileViewModel.userSearchResults.observe(getViewLifecycleOwner(), result -> {
        //     handleVisibility(pbLoadingUsers, result instanceof ResultWrapper.Loading);
        //     if (result instanceof ResultWrapper.Success) {
        //         allFetchedUsers.clear();
        //         List<UserModel> users = ((ResultWrapper.Success<List<UserModel>>) result).getData();
        //         if (users != null) {
        //             allFetchedUsers.addAll(users);
        //         }
        //         filterLocalUsers(etSearchUsers.getText().toString()); // Re-filter with current query
        //     } else if (result instanceof ResultWrapper.Error) {
        //         Toast.makeText(getContext(), "Error searching users: " + ((ResultWrapper.Error<?>) result).getMessage(), Toast.LENGTH_SHORT).show();
        //         tvNoUsersFound.setText("Error searching users.");
        //         tvNoUsersFound.setVisibility(View.VISIBLE);
        //         rvUsersToInvite.setVisibility(View.GONE);
        //     }
        // });

        // TODO: Observe invitation sending status from InvitationViewModel
        // invitationViewModel.sendInvitationBatchStatus.observe(getViewLifecycleOwner(), result -> { ... });
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
