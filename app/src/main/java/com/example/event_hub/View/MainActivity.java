package com.example.event_hub.View;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI; // If using Toolbar/ActionBar

import com.example.event_hub.R;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the NavHostFragment and get the NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Define top-level destinations for AppBarConfiguration if you have an ActionBar/Toolbar
            // This prevents the Up button from showing on these screens.
            // Example: If MainFragment, ProfileFragment, etc., are top-level.
            // appBarConfiguration = new AppBarConfiguration.Builder(
            // R.id.mainFragment, R.id.profileFragment /*, other top-level destinations */
            // ).build();

            // If you add a Toolbar to activity_main.xml:
            // androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
            // setSupportActionBar(toolbar);
            // NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        } else {
            // Handle error: NavHostFragment not found
            // This usually indicates an issue with your activity_main.xml or FragmentContainerView setup.
            throw new IllegalStateException("NavHostFragment not found. Check your activity_main.xml layout.");
        }
    }

    // If using an ActionBar/Toolbar managed by Navigation Component:
    // @Override
    // public boolean onSupportNavigateUp() {
    //    // Ensure appBarConfiguration is initialized
    //    if (navController == null || appBarConfiguration == null) {
    //        return super.onSupportNavigateUp();
    //    }
    //    return NavigationUI.navigateUp(navController, appBarConfiguration)
    //            || super.onSupportNavigateUp();
    // }

    // You might also handle deep linking or specific intent actions here later.
}
