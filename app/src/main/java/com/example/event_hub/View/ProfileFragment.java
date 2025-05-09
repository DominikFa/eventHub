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
// Import Glide if you plan to load profile images from URL
// import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ProfileViewModel profileViewModel;
    private AuthViewModel authViewModel;

    private ImageView ivProfileAvatar;
    private TextView tvFullName, tvUsername, tvEmail, tvBio, tvRole, tvStatus;
    private MaterialButton btnEditProfile, btnLogout, btnBanUser, btnDeleteAccount;
    private ProgressBar pbProfileLoading;
    private View nsvProfileContent; // To hide/show all content

    private String profileUserIdToDisplay; // User ID whose profile is being displayed
    private String loggedInUserId;
    private UserModel displayedUserModel;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (getArguments() != null) {
            // Check if a specific userId is passed to view another profile
            // If using Safe Args: ProfileFragmentArgs.fromBundle(getArguments()).getUserId();
            profileUserIdToDisplay = getArguments().getString("userId", null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupClickListeners();
        observeViewModels();
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
        nsvProfileContent = view.findViewById(R.id.nsv_profile_content); // ID of your NestedScrollView
    }

    private void setupClickListeners() {
        if (getView() == null) return;
        NavController navController = Navigation.findNavController(getView());

        btnEditProfile.setOnClickListener(v -> {
            if (displayedUserModel != null && loggedInUserId != null && loggedInUserId.equals(displayedUserModel.getUserId())) {
                // TODO: Navigate to EditProfileFragment, pass displayedUserModel.getUserId() or UserModel
                Toast.makeText(getContext(), "Navigate to Edit Profile", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "You can only edit your own profile.", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        btnBanUser.setOnClickListener(v -> {
            if (displayedUserModel != null) {
                showBanConfirmationDialog(displayedUserModel);
            }
        });

        btnDeleteAccount.setOnClickListener(v -> {
            if (displayedUserModel != null) {
                showDeleteConfirmationDialog(displayedUserModel);
            }
        });
    }

    private void observeViewModels() {
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), id -> {
            loggedInUserId = id;
            if (profileUserIdToDisplay == null && loggedInUserId != null) {
                // If no specific profile requested, load logged-in user's profile
                profileViewModel.loadUserProfile(loggedInUserId);
            } else if (profileUserIdToDisplay != null) {
                // Load the specified user's profile
                profileViewModel.loadUserProfile(profileUserIdToDisplay);
            } else if (loggedInUserId == null && profileUserIdToDisplay == null) {
                // Not logged in and no profile specified, navigate to login
                Toast.makeText(getContext(), "Please login to view profiles.", Toast.LENGTH_SHORT).show();
                if (getView() != null) {
                    // Navigation.findNavController(getView()).navigate(R.id.action_global_to_loginActivity); // Example
                }
            }
            updateButtonVisibility(); // Update buttons based on new loggedInUserId
        });

        profileViewModel.userProfileState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbProfileLoading, result instanceof ResultWrapper.Loading);
            nsvProfileContent.setVisibility(result instanceof ResultWrapper.Success ? View.VISIBLE : View.GONE);

            if (result instanceof ResultWrapper.Success) {
                @SuppressWarnings("unchecked")
                ResultWrapper.Success<UserModel> successResult = (ResultWrapper.Success<UserModel>) result;
                displayedUserModel = successResult.getData();
                if (displayedUserModel != null) {
                    populateProfileData(displayedUserModel);
                }
            } else if (result instanceof ResultWrapper.Error) {
                @SuppressWarnings("unchecked")
                ResultWrapper.Error<UserModel> errorResult = (ResultWrapper.Error<UserModel>) result;
                Toast.makeText(getContext(), getString(R.string.profile_loading_error) + ": " + errorResult.getMessage(), Toast.LENGTH_LONG).show();
                nsvProfileContent.setVisibility(View.GONE); // Hide content on error
            }
            updateButtonVisibility();
        });

        authViewModel.logoutState.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), R.string.action_logout_successful, Toast.LENGTH_SHORT).show();
                if (getView() != null) {
                    // Navigate to login screen after logout
                    // NavController navController = Navigation.findNavController(getView());
                    // navController.navigate(R.id.action_profileFragment_to_loginActivity); // Adjust action ID
                    // To clear backstack and go to login:
                    // navController.popBackStack(R.id.nav_graph, true); // Pop entire graph
                    // navController.navigate(R.id.loginActivity);
                    Toast.makeText(getContext(), "Logged out. Navigate to Login.", Toast.LENGTH_LONG).show();

                }
            } else if (result instanceof ResultWrapper.Error) {
                Toast.makeText(getContext(), R.string.action_logout_failed, Toast.LENGTH_SHORT).show();
            }
        });

        profileViewModel.userActionOperationState.observe(getViewLifecycleOwner(), result -> {
            // For Ban/Delete user actions
            if (result instanceof ResultWrapper.Loading) {
                // Could show a dialog loading state or disable buttons further
            } else if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), "User action successful.", Toast.LENGTH_SHORT).show();
                // If current user was deleted or banned, navigate away or refresh.
                if (displayedUserModel != null && loggedInUserId != null && displayedUserModel.getUserId().equals(loggedInUserId)) {
                    // If self-action led to this, might need to logout or navigate
                    if (displayedUserModel.getStatus().equals("banned") || result.toString().contains("delete")) { // Heuristic
                        authViewModel.logout(); // Force logout if self was banned/deleted
                    }
                } else if (displayedUserModel != null) {
                    profileViewModel.loadUserProfile(displayedUserModel.getUserId()); // Refresh the viewed profile
                }
            } else if (result instanceof ResultWrapper.Error) {
                String msg = ((ResultWrapper.Error<?>) result).getMessage();
                Toast.makeText(getContext(), "User action failed: " + msg, Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void populateProfileData(UserModel user) {
        UserDetails details = user.getUserDetails();
        if (details != null) {
            tvFullName.setText(details.getFullName() != null ? details.getFullName() : getString(R.string.not_available_placeholder));
            tvBio.setText(details.getDescription() != null ? details.getDescription() : getString(R.string.not_available_placeholder));
            // Load profile image using Glide
            // if (details.getProfileImageUrl() != null && !details.getProfileImageUrl().isEmpty() && getContext() != null) {
            // Glide.with(getContext()).load(details.getProfileImageUrl()).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(ivProfileAvatar);
            // } else {
            ivProfileAvatar.setImageResource(R.drawable.ic_profile_placeholder);
            // }
        } else {
            tvFullName.setText(R.string.not_available_placeholder);
            tvBio.setText(R.string.not_available_placeholder);
            ivProfileAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        }

        tvUsername.setText(user.getLogin() != null ? user.getLogin() : getString(R.string.not_available_placeholder));
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : getString(R.string.not_available_placeholder));
        tvRole.setText(user.getRole() != null ? user.getRole() : getString(R.string.not_available_placeholder));
        tvStatus.setText(user.getStatus() != null ? user.getStatus() : getString(R.string.not_available_placeholder));
    }

    private void updateButtonVisibility() {
        boolean isLoggedIn = loggedInUserId != null;
        boolean isViewingOwnProfile = displayedUserModel != null && isLoggedIn && loggedInUserId.equals(displayedUserModel.getUserId());

        btnEditProfile.setVisibility(isViewingOwnProfile ? View.VISIBLE : View.GONE);
        btnLogout.setVisibility(isViewingOwnProfile ? View.VISIBLE : View.GONE);

        // TODO: Implement role-based visibility for Ban/Delete
        // For now, show if viewing another user's profile and logged in (conceptual admin)
        // Or if viewing own profile for delete.
        boolean isAdminViewingOther = isLoggedIn && displayedUserModel != null && !isViewingOwnProfile /* && loggedInUserHasAdminRole() */;

        btnBanUser.setVisibility(isAdminViewingOther ? View.VISIBLE : View.GONE);
        btnDeleteAccount.setVisibility(isViewingOwnProfile || isAdminViewingOther ? View.VISIBLE : View.GONE);
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
        if (getContext() == null || userToBan == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_ban_confirm)
                .setMessage(getString(R.string.dialog_message_ban_confirm) + " (" + userToBan.getLogin() + ")?")
                .setPositiveButton(R.string.dialog_button_ban, (dialog, which) -> profileViewModel.banUser(userToBan.getUserId()))
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void showDeleteConfirmationDialog(UserModel userToDelete) {
        if (getContext() == null || userToDelete == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_delete_confirm)
                .setMessage(getString(R.string.dialog_message_delete_confirm) + " (" + userToDelete.getLogin() + ")?")
                .setPositiveButton(R.string.dialog_button_delete, (dialog, which) -> profileViewModel.deleteUser(userToDelete.getUserId()))
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }


    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
