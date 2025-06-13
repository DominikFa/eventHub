// src/main/java/com/example/event_hub/MainActivity.java
package com.example.event_hub;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.navigation.ui.NavigationUI;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.CreateEventViewModel;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.View.MainFragment; // Import MainFragment
import androidx.fragment.app.Fragment; // Import Fragment
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    private BottomNavigationView bottomNavigationView;
    private ImageButton btnProfileOrLogin;
    private AuthViewModel authViewModel;
    private CreateEventViewModel createEventViewModel;
    private Long loggedInUserId;
    private String currentAuthToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModels
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        createEventViewModel = new ViewModelProvider(this).get(CreateEventViewModel.class);

        // Find views from MainActivity's layout
        bottomNavigationView = findViewById(R.id.bottom_navigation_main);
        btnProfileOrLogin = findViewById(R.id.btn_profile_or_login_main);


        // Find the NavHostFragment and get the NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // DO NOT use NavigationUI.setupWithNavController for the bottom nav if MainFragment manages internal tabs.
            // Removed: NavigationUI.setupWithNavController(bottomNavigationView, navController);

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_create_event) { // Handle "Create Event" tab
                    if (loggedInUserId == null) {
                        Toast.makeText(this, R.string.login_to_create_event, Toast.LENGTH_SHORT).show();
                        navController.navigate(R.id.loginActivity);
                        return false; // Prevent default navigation to create event fragment
                    }
                    createEventViewModel.resetFormAndState();
                    navController.navigate(R.id.createEventFragment);
                    return true; // Consume the event as we handled it
                } else {
                    // For other tabs (Public, Attended, Invitations, My Events),
                    // we want to stay in MainFragment and just tell it to switch its internal content.

                    // Ensure MainFragment is the current destination if it's not already.
                    if (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != R.id.mainFragment) {
                        navController.navigate(R.id.mainFragment); // Navigate back to mainFragment
                    }

                    // Get a reference to the currently displayed fragment in the NavHostFragment.
                    Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                    if (currentFragment instanceof MainFragment) {
                        MainFragment mainFragment = (MainFragment) currentFragment;
                        // Tell MainFragment to handle the tab selection internally
                        mainFragment.handleTabSelection(itemId);
                        return true; // Consume the event
                    }
                }
                return false; // Fallback (should not be reached if all items handled)
            });


            // Setup click listener for the profile/login button
            btnProfileOrLogin.setOnClickListener(v -> {
                if (loggedInUserId != null) {
                    // Navigate to profile if logged in
                    Bundle profileArgs = new Bundle();
                    profileArgs.putLong("userId", loggedInUserId);
                    navController.navigate(R.id.profileFragment, profileArgs); // Navigate to ProfileFragment
                } else {
                    // Navigate to login if not logged in
                    navController.navigate(R.id.loginActivity); // Navigate to LoginActivity
                }
            });

            // Observe AuthViewModel for login status changes
            authViewModel.currentUserId.observe(this, id -> {
                boolean changed = !Objects.equals(loggedInUserId, id);
                loggedInUserId = id;
                updateProfileLoginButtonIcon(); // Update button appearance based on login state
                // When login state changes, re-select the current tab to refresh its content
                // (MainFragment's onResume or tab change will handle data loading)
                if (changed) {
                    bottomNavigationView.setSelectedItemId(bottomNavigationView.getSelectedItemId());
                }
            });

            authViewModel.currentJwtToken.observe(this, token -> {
                boolean changed = !Objects.equals(currentAuthToken, token);
                currentAuthToken = token;
                updateProfileLoginButtonIcon(); // Update button appearance based on token state
                if (changed) {
                    bottomNavigationView.setSelectedItemId(bottomNavigationView.getSelectedItemId());
                }
            });

            // Set Publicne tab as default on launch
            bottomNavigationView.setSelectedItemId(R.id.navigation_events_public);

        } else {
            throw new IllegalStateException("NavHostFragment not found. Check your activity_main.xml layout.");
        }
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
}