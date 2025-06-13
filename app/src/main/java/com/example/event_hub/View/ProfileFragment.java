package com.example.event_hub.View;

import android.app.AlertDialog;
import android.content.Intent; // Import Intent
import android.os.Bundle;
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

import com.bumptech.glide.Glide;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.R;
import com.example.event_hub.Repositiry.ApiClient; // Import ApiClient
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.ProfileViewModel;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {


    private ProfileViewModel profileViewModel;
    private AuthViewModel authViewModel;

    private ImageView ivProfileAvatar;
    private TextView tvFullName, tvUsername, tvEmail, tvBio, tvRole, tvStatus;
    private MaterialButton btnEditProfile, btnLogout, btnBanUser, btnDeleteAccount;
    private ProgressBar pbProfileLoading;
    private View nsvProfileContent;

    private Long profileUserIdToDisplay;
    private Long loggedInUserId;
    private String currentAuthToken; // Still used for button visibility and other actions
    private UserModel displayedUserModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (getArguments() != null) {
            long userIdArg = getArguments().getLong("userId", 0L);
            if (userIdArg > 0) {
                profileUserIdToDisplay = userIdArg;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupClickListeners();
        observeViewModels();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        profileViewModel.clearProfileState();
    }

    private void bindViews(View view) {
        ivProfileAvatar = view.findViewById(R.id.iv_profile_avatar);
        tvFullName = view.findViewById(R.id.tv_profile_full_name);
        tvUsername = view.findViewById(R.id.tv_profile_username);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvBio = view.findViewById(R.id.tv_profile_bio);
        tvRole = view.findViewById(R.id.tv_profile_role);
        tvStatus = view.findViewById(R.id.tv_profile_status);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnBanUser = view.findViewById(R.id.btn_ban_user_profile);
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account_profile);
        pbProfileLoading = view.findViewById(R.id.pb_profile_loading);
        nsvProfileContent = view.findViewById(R.id.nsv_profile_content);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), R.string.edit_profile_not_implemented, Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        btnBanUser.setOnClickListener(v -> {
            if (displayedUserModel != null && currentAuthToken != null) {
                showBanConfirmationDialog(displayedUserModel);
            } else {
                Toast.makeText(getContext(), R.string.profile_or_auth_missing, Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteAccount.setOnClickListener(v -> {
            if (displayedUserModel != null && currentAuthToken != null) {
                showDeleteConfirmationDialog(displayedUserModel);
            } else {
                Toast.makeText(getContext(), R.string.profile_or_auth_missing, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModels() {
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), id -> {
            loggedInUserId = id;
            // Centralize profile loading logic to ensure proper ID and token handling
            loadProfileBasedOnState();
            updateButtonVisibility();
        });

        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            currentAuthToken = token;
            // Centralize profile loading logic to ensure proper ID and token handling
            loadProfileBasedOnState();
            updateButtonVisibility();
        });

        profileViewModel.userProfileState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbProfileLoading, result instanceof ResultWrapper.Loading);
            nsvProfileContent.setVisibility(result instanceof ResultWrapper.Success ? View.VISIBLE : View.GONE);

            if (result instanceof ResultWrapper.Success) {
                displayedUserModel = ((ResultWrapper.Success<UserModel>) result).getData();
                if (displayedUserModel != null) {
                    populateProfileData(displayedUserModel);
                } else {
                    nsvProfileContent.setVisibility(View.GONE);
                    if (isAdded()) Toast.makeText(getContext(), R.string.profile_data_not_loaded, Toast.LENGTH_SHORT).show();
                }
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                if (isAdded()) Toast.makeText(getContext(), getString(R.string.error_loading_profile_prefix) + errorResult.getMessage(), Toast.LENGTH_LONG).show();
                nsvProfileContent.setVisibility(View.GONE);
            }
            updateButtonVisibility();
        });

        profileViewModel.userActionState.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), R.string.user_action_successful, Toast.LENGTH_SHORT).show();
                if (displayedUserModel != null && loggedInUserId != null && !displayedUserModel.getId().equals(loggedInUserId)) {
                    profileViewModel.loadUserProfile(displayedUserModel.getId()); // No token param here, gets from ViewModel
                }
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                Toast.makeText(getContext(), getString(R.string.user_action_failed_prefix) + errorResult.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        authViewModel.logoutState.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), R.string.action_logout_successful, Toast.LENGTH_SHORT).show();
                navigateToLogin(); // Direct navigation after successful logout
            } else if (result instanceof ResultWrapper.Error) {
                Toast.makeText(getContext(), R.string.action_logout_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // New helper method to encapsulate profile loading logic based on current state
    private void loadProfileBasedOnState() {
        if (loggedInUserId != null && loggedInUserId != 0L) {
            // User is logged in
            if (profileUserIdToDisplay != null && profileUserIdToDisplay != 0L) {
                // If a specific userId is passed, load that profile.
                // ProfileViewModel will decide which API (current user or by ID) to call.
                profileViewModel.loadUserProfile(profileUserIdToDisplay);
            } else {
                // If no specific userId is passed, load the logged-in user's own profile.
                profileViewModel.loadUserProfile(loggedInUserId);
            }
        } else {
            // User is not logged in.
            // If profileUserIdToDisplay was set, this implies an attempt to view someone else's profile while not logged in.
            // This should redirect to login.
            // If profileUserIdToDisplay was not set, it's a direct navigation to "my profile" while not logged in.
            // This also redirects to login.
            handleNotLoggedInState();
        }
    }


    private void handleNotLoggedInState() {
        nsvProfileContent.setVisibility(View.GONE);
        pbProfileLoading.setVisibility(View.GONE);
        if (isAdded()) {
            Toast.makeText(getContext(), R.string.login_to_view_profiles, Toast.LENGTH_LONG).show();
            // Do NOT navigate from here directly, as it might cause a loop or unexpected behavior
            // Let the MainActivity (or parent Activity) handle the ultimate redirection if needed.
            // For now, just show the message and ensure the UI is not showing profile data.
            // If the user tries to interact with profile functionality without being logged in,
            // they will be redirected to LoginActivity by the MainActivity's bottom navigation
            // or profile/login button handling.
        }
    }

    /**
     * Navigates to the LoginActivity and clears the activity stack.
     * This ensures a clean slate after logout.
     */
    private void navigateToLogin() {
        if (isAdded()) { // Ensure fragment is attached
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            // These flags clear all previous activities and start LoginActivity as a new task.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finishAffinity(); // Finish MainActivity and all its tasks
        }
    }

    private void populateProfileData(UserModel user) {
        if (getContext() == null || user == null) return;

        tvFullName.setText(user.getName() != null ? user.getName() : getString(R.string.not_available_placeholder));
        tvBio.setText(user.getDescription() != null && !user.getDescription().isEmpty() ? user.getDescription() : getString(R.string.not_available_placeholder));
        tvUsername.setText(user.getLogin() != null ? "@" + user.getLogin() : getString(R.string.not_available_placeholder));
        tvEmail.setText(user.getLogin() != null ? user.getLogin() : getString(R.string.not_available_placeholder));
        tvRole.setText(user.getRole() != null ? user.getRole().toUpperCase() : getString(R.string.not_available_placeholder));
        tvStatus.setText(user.getStatus() != null ? user.getStatus().toUpperCase() : getString(R.string.not_available_placeholder));


        String imageUrl = null;
        if (user.getId() != null) {
            imageUrl = ApiClient.BASE_URL + "api/users/" + user.getId() + "/profile-image";
        }

        if (imageUrl != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder) // Placeholder image while loading
                    .error(R.drawable.ic_profile_placeholder)      // Image to display on error
                    .into(ivProfileAvatar);
        } else {
            // Fallback to placeholder if no valid URL can be constructed
            ivProfileAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void updateButtonVisibility() {
        boolean isLoggedIn = currentAuthToken != null && loggedInUserId != null;
        if (!isLoggedIn || displayedUserModel == null) {
            btnEditProfile.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
            btnBanUser.setVisibility(View.GONE);
            btnDeleteAccount.setVisibility(View.GONE);
            return;
        }

        boolean isViewingOwnProfile = loggedInUserId.equals(displayedUserModel.getId());
        btnEditProfile.setVisibility(isViewingOwnProfile ? View.VISIBLE : View.GONE);
        btnLogout.setVisibility(isViewingOwnProfile ? View.VISIBLE : View.GONE);

        UserModel loggedInUserDetails = authViewModel.getCurrentUserSynchronous();
        boolean isAdmin = loggedInUserDetails != null && "ADMIN".equalsIgnoreCase(loggedInUserDetails.getRole());

        if (isViewingOwnProfile) {
            btnBanUser.setVisibility(View.GONE);
            btnDeleteAccount.setVisibility(View.VISIBLE);
        } else {
            boolean displayedUserIsAdmin = "ADMIN".equalsIgnoreCase(displayedUserModel.getRole());
            int visibility = isAdmin && !displayedUserIsAdmin ? View.VISIBLE : View.GONE;
            btnBanUser.setVisibility(visibility);
            btnDeleteAccount.setVisibility(visibility);
        }
    }

    private void showLogoutConfirmationDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_logout_confirm)
                .setMessage(R.string.dialog_message_logout_confirm)
                .setPositiveButton(R.string.dialog_button_logout, (dialog, which) -> authViewModel.logout())
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void showBanConfirmationDialog(UserModel userToBan) {
        if (getContext() == null || currentAuthToken == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_ban_confirm)
                .setMessage(getString(R.string.dialog_message_ban_confirm_user, userToBan.getLogin()))
                .setPositiveButton(R.string.dialog_button_ban, (dialog, which) -> profileViewModel.banUser(userToBan.getId(), currentAuthToken))
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void showDeleteConfirmationDialog(UserModel userToDelete) {
        if (getContext() == null || currentAuthToken == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_delete_confirm)
                .setMessage(getString(R.string.dialog_message_delete_confirm_user, userToDelete.getLogin()))
                .setPositiveButton(R.string.dialog_button_delete, (dialog, which) -> profileViewModel.deleteUser(userToDelete.getId(), currentAuthToken))
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}