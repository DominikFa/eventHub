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
// import com.example.event_hub.ViewModel.ProfileViewModel; // For fetching users to invite
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class InviteUsersFragment extends Fragment {

    private InvitationViewModel invitationViewModel;
    // private ProfileViewModel profileViewModel; // To search/fetch users
    private AuthViewModel authViewModel;

    private TextInputEditText etSearchUsers;
    private RecyclerView rvUsersToInvite;
    private UserInviteAdapter userInviteAdapter;
    private MaterialButton btnSendInvitations;
    private ProgressBar pbLoadingUsers;
    private TextView tvNoUsersFound;

    private String eventId;
    private String currentAuthToken;
    private List<UserModel> allFetchedUsers = new ArrayList<>(); // Placeholder for user search results

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        invitationViewModel = new ViewModelProvider(this).get(InvitationViewModel.class);
        // profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
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
        setupSearch();
        setupClickListeners();
        observeViewModels();

        if (eventId == null) { /* ... handle error ... */ }
        // TODO: Implement fetching users to invite using profileViewModel.searchUsers() or similar
        // For now, it will be an empty list or manually populated allFetchedUsers
        // For example: profileViewModel.fetchAllUsersForInviteList();
        // Then observe profileViewModel.userListForInvite.observe... to populate allFetchedUsers and update adapter.
        tvNoUsersFound.setText(R.string.search_to_find_users_for_invite); // Initial message
        updateSendButtonState();
    }

    private void bindViews(View view) { /* ... as before ... */ }
    private void setupRecyclerView() { /* ... as before ... */ }
    private void updateSendButtonState() { /* ... as before ... */ }
    private void setupSearch() { /* ... as before, ideally calls a VM method ... */ }
    private void filterLocalUsers(String query) { /* ... as before, for client-side filtering example ... */ }


    private void setupClickListeners() {
        btnSendInvitations.setOnClickListener(v -> {
            if (currentAuthToken == null) {
                Toast.makeText(getContext(), "Authentication required to send invitations.", Toast.LENGTH_SHORT).show();
                // Optionally navigate to login
                return;
            }
            List<UserModel> selectedUsers = userInviteAdapter.getSelectedUsers();
            if (selectedUsers.isEmpty()) {
                Toast.makeText(getContext(), R.string.no_users_selected_for_invite, Toast.LENGTH_SHORT).show();
                return;
            }
            for (UserModel user : selectedUsers) {
                invitationViewModel.sendInvitation(eventId, user.getUserId(), currentAuthToken);
                // Observe actionStatus for individual send results if needed, or batch them.
                // For now, this sends one by one.
            }
            // After loop, can show a general "Invitations sending..." message
            Toast.makeText(getContext(), "Sending invitations...", Toast.LENGTH_SHORT).show();
            userInviteAdapter.clearSelections(); // Clear selection after sending
            updateSendButtonState();
        });
    }

    private void observeViewModels() {
        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            currentAuthToken = token;
            btnSendInvitations.setEnabled(token != null && !userInviteAdapter.getSelectedUsers().isEmpty());
        });

        // TODO: Observer for profileViewModel.userListForInvite (or search results)
        // to populate allFetchedUsers and call filterLocalUsers("").

        invitationViewModel.actionStatus.observe(getViewLifecycleOwner(), result -> {
            // This will observe the status of the *last* sendInvitation call due to its current setup.
            // For multiple invitations, a batch processing status or individual feedback per invitation is better.
            if (result instanceof ResultWrapper.Success) {
                // Toast.makeText(getContext(), "Invitation request processed.", Toast.LENGTH_SHORT).show();
                // A general success/error after all are sent might be better than per-invitation.
            } else if (result instanceof ResultWrapper.Error) {
                Toast.makeText(getContext(), "Error processing an invitation: " + ((ResultWrapper.Error<?>) result).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void handleVisibility(View view, boolean isLoading) { /* ... as before ... */ }
}