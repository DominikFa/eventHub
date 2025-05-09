package com.example.event_hub.View;

import android.app.AlertDialog;
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
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.UserDetails;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.R;
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
    private View nsvProfileContent; // NestedScrollView or main content view

    private String profileUserIdToDisplay; // User ID whose profile is being displayed
    private String loggedInUserId; // Currently authenticated user's ID
    private String currentAuthToken;
    private UserModel displayedUserModel; // The UserModel object currently being displayed

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (getArguments() != null) {
            profileUserIdToDisplay = getArguments().getString("userId", null);
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
            if (displayedUserModel != null && currentAuthToken != null) {
                Toast.makeText(getContext(), "Edit Profile UI not implemented.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Profile data or authentication missing.", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        btnBanUser.setOnClickListener(v -> {
            if (displayedUserModel != null && currentAuthToken != null) {
                showBanConfirmationDialog(displayedUserModel);
            } else {
                Toast.makeText(getContext(), "Profile data or authentication missing.", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteAccount.setOnClickListener(v -> {
            if (displayedUserModel != null && currentAuthToken != null) {
                showDeleteConfirmationDialog(displayedUserModel);
            } else {
                Toast.makeText(getContext(), "Profile data or authentication missing.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModels() {
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), id -> {
            loggedInUserId = id;
            if (profileUserIdToDisplay != null) {
                profileViewModel.loadUserProfile(profileUserIdToDisplay);
            } else if (loggedInUserId != null) {
                profileViewModel.loadUserProfile(loggedInUserId);
            } else {
                nsvProfileContent.setVisibility(View.GONE);
                pbProfileLoading.setVisibility(View.GONE);
                if (isAdded() && getView() != null && Navigation.findNavController(getView()).getCurrentDestination().getId() == R.id.profileFragment) {
                    Toast.makeText(getContext(), R.string.login_to_view_profiles, Toast.LENGTH_LONG).show();
                }
            }
            updateButtonVisibility();
        });

        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            currentAuthToken = token;
            updateButtonVisibility();
        });

        profileViewModel.userProfileState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbProfileLoading, result instanceof ResultWrapper.Loading);
            if (result instanceof ResultWrapper.Loading) {
                nsvProfileContent.setVisibility(View.GONE);
            }

            if (result instanceof ResultWrapper.Success) {
                ResultWrapper.Success<UserModel> successResult = (ResultWrapper.Success<UserModel>) result;
                displayedUserModel = successResult.getData();
                if (displayedUserModel != null) {
                    populateProfileData(displayedUserModel);
                    nsvProfileContent.setVisibility(View.VISIBLE);
                } else {
                    nsvProfileContent.setVisibility(View.GONE);
                    if (isAdded() && (profileUserIdToDisplay != null || loggedInUserId != null))
                        Toast.makeText(getContext(), R.string.profile_data_not_loaded, Toast.LENGTH_SHORT).show();
                }
            } else if (result instanceof ResultWrapper.Error) {
                nsvProfileContent.setVisibility(View.GONE);
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                if (isAdded() && (profileUserIdToDisplay != null || loggedInUserId != null))
                    Toast.makeText(getContext(), getString(R.string.error_loading_profile_prefix) + errorResult.getMessage(), Toast.LENGTH_LONG).show();
            }
            updateButtonVisibility();
        });

        profileViewModel.profileEditState.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ResultWrapper.Loading){
                // Handled by userProfileState or specific button disabling
            } else if (result instanceof ResultWrapper.Success) {
                ResultWrapper.Success<UserModel> successResult = (ResultWrapper.Success<UserModel>) result;
                UserModel updatedUser = successResult.getData();
                if (updatedUser != null && displayedUserModel != null && updatedUser.getUserId().equals(displayedUserModel.getUserId())) {
                    Toast.makeText(getContext(), R.string.profile_updated_successfully, Toast.LENGTH_SHORT).show();
                }
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                Toast.makeText(getContext(), getString(R.string.profile_edit_failed_prefix) + errorResult.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        profileViewModel.userActionState.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ResultWrapper.Loading){
                // Disable relevant button e.g. btnBanUser.setEnabled(false);
            } else if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), R.string.user_action_successful, Toast.LENGTH_SHORT).show();
                if (displayedUserModel != null && loggedInUserId != null && !displayedUserModel.getUserId().equals(loggedInUserId)) {
                    profileViewModel.loadUserProfile(displayedUserModel.getUserId());
                }
                // Re-enable button if it was disabled for loading
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                Toast.makeText(getContext(), getString(R.string.user_action_failed_prefix) + errorResult.getMessage(), Toast.LENGTH_LONG).show();
                // Re-enable button
            }
        });

        authViewModel.logoutState.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), R.string.action_logout_successful, Toast.LENGTH_SHORT).show();
                if (getView() != null && isAdded()) {
                    NavController navController = Navigation.findNavController(getView());
                    navController.navigate(R.id.loginActivity, null, new androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build());
                }
            } else if (result instanceof ResultWrapper.Error) {
                Toast.makeText(getContext(), R.string.action_logout_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateProfileData(UserModel user) {
        if (getContext() == null || user == null) return;
        UserDetails details = user.getUserDetails();
        if (details != null) {
            tvFullName.setText(details.getFullName() != null ? details.getFullName() : getString(R.string.not_available_placeholder));
            tvBio.setText(details.getDescription() != null ? details.getDescription() : getString(R.string.not_available_placeholder));
            ivProfileAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        } else {
            tvFullName.setText(getString(R.string.not_available_placeholder));
            tvBio.setText(getString(R.string.not_available_placeholder));
            ivProfileAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        }
        tvUsername.setText(user.getLogin() != null ? user.getLogin() : getString(R.string.not_available_placeholder));
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : getString(R.string.not_available_placeholder));
        tvRole.setText(user.getRole() != null ? user.getRole().toUpperCase() : getString(R.string.not_available_placeholder));
        tvStatus.setText(user.getStatus() != null ? user.getStatus().toUpperCase() : getString(R.string.not_available_placeholder));
    }

    private void updateButtonVisibility() {
        boolean isLoggedIn = currentAuthToken != null && loggedInUserId != null;
        boolean isViewingOwnProfile = displayedUserModel != null && isLoggedIn && loggedInUserId.equals(displayedUserModel.getUserId());

        btnEditProfile.setVisibility(isViewingOwnProfile ? View.VISIBLE : View.GONE);
        btnLogout.setVisibility(isViewingOwnProfile ? View.VISIBLE : View.GONE);

        UserModel currentlyLoggedInUserDetails = authViewModel.getCurrentUserSynchronous();
        boolean loggedInUserIsAdmin = currentlyLoggedInUserDetails != null && "admin".equalsIgnoreCase(currentlyLoggedInUserDetails.getRole());

        if (displayedUserModel != null && isLoggedIn) {
            if (isViewingOwnProfile) {
                btnBanUser.setVisibility(View.GONE);
                btnDeleteAccount.setVisibility(View.VISIBLE);
            } else {
                boolean displayedUserIsAdmin = "admin".equalsIgnoreCase(displayedUserModel.getRole());
                btnBanUser.setVisibility(loggedInUserIsAdmin && !displayedUserIsAdmin ? View.VISIBLE : View.GONE);
                btnDeleteAccount.setVisibility(loggedInUserIsAdmin && !displayedUserIsAdmin ? View.VISIBLE : View.GONE);
            }
        } else {
            btnBanUser.setVisibility(View.GONE);
            btnDeleteAccount.setVisibility(View.GONE);
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
        if (getContext() == null || userToBan == null || currentAuthToken == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_ban_confirm)
                .setMessage(getString(R.string.dialog_message_ban_confirm_user, userToBan.getLogin()))
                .setPositiveButton(R.string.dialog_button_ban, (dialog, which) -> profileViewModel.banUser(userToBan.getUserId(), currentAuthToken))
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void showDeleteConfirmationDialog(UserModel userToDelete) {
        if (getContext() == null || userToDelete == null || currentAuthToken == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_delete_confirm)
                .setMessage(getString(R.string.dialog_message_delete_confirm_user, userToDelete.getLogin()))
                .setPositiveButton(R.string.dialog_button_delete, (dialog, which) -> profileViewModel.deleteUser(userToDelete.getUserId(), currentAuthToken))
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}