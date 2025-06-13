// src/androidTest/java/com/example/event_hub/EventHubUiTest.java
package com.example.event_hub;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import android.content.Intent;
import android.os.IBinder;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.espresso.Root;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.rule.IntentsRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.event_hub.Repositiry.AuthRepository;
import com.example.event_hub.View.LoginActivity;
import com.example.event_hub.MainActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventHubUiTest {

    // Rule for launching MainActivity
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    // Rule for verifying intents (e.g., opening external maps)
    @Rule
    public IntentsRule intentsRule = new IntentsRule();

    // Test credentials (should match backend test data)
    private static final String TEST_USER_EMAIL = "test1@t.t";
    private static final String TEST_USER_PASSWORD = "password";
    private static final String TEST_ORGANIZER_EMAIL = "test2@t.t";
    private static final String TEST_ORGANIZER_PASSWORD = "password";
    private static final String TEST_ADMIN_EMAIL = "test3@t.t";
    private static final String TEST_ADMIN_PASSWORD = "password";

    // Helper for a short delay to allow UI to settle or network calls to complete (NOT IDEAL FOR PRODUCTION)
    private void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Helper method to navigate to LoginActivity from MainActivity
    private void navigateToLoginScreen() {
        // Ensure we are on MainActivity first
        onView(withId(R.id.tv_app_title_main)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_profile_or_login_main)).perform(click()); // Click profile/login button
        waitFor(500); // Wait for navigation
        onView(withId(R.id.tv_login_title)).check(matches(isDisplayed())); // Verify on login screen
    }

    // Helper method to perform login steps assuming already on LoginActivity
    private void performLoginSteps(String email, String password) {
        onView(withId(R.id.et_username_login)).perform(replaceText(email), closeSoftKeyboard());
        onView(withId(R.id.et_password_login)).perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.btn_login)).perform(click());
        waitFor(3000); // Wait for login API call and navigation
    }

    // Combined Login Helper: Navigates to login screen, performs login, and verifies success
    private void loginAs(String email, String password) {
        navigateToLoginScreen();
        performLoginSteps(email, password);
        // Add a waitFor here to ensure MainActivity is fully resumed and drawn
        waitFor(1000); // Increased wait for stability
        onView(withId(R.id.tv_app_title_main)).check(matches(isDisplayed())); // Verify back on MainActivity
        onView(withId(R.id.btn_profile_or_login_main)).check(matches(allOf(isDisplayed(), withContentDescription(R.string.content_desc_profile))));
    }

    // Helper method to navigate to ProfileFragment from MainActivity
    private void navigateToProfileScreen() {
        // Ensure we are on MainActivity first
        onView(withId(R.id.tv_app_title_main)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_profile_or_login_main)).perform(click()); // Click profile/login button
        waitFor(1000); // Wait for navigation
        onView(withId(R.id.tv_profile_screen_title)).check(matches(isDisplayed())); // Verify on profile screen
        // IMPORTANT: Wait for profile data to load and buttons to become visible
        onView(withId(R.id.tv_profile_full_name)).check(matches(isDisplayed())); // Assert profile data loaded
        waitFor(500); // Small wait after data is displayed, before interacting with buttons
    }

    // Combined Logout Helper: Navigates to profile screen, performs logout, and verifies success
    private void logoutUser() {
        navigateToProfileScreen(); // This will now wait for profile data to load
        onView(withId(R.id.btn_logout)).check(matches(isDisplayed())); // Explicitly check logout button is displayed
        onView(withId(R.id.btn_logout)).perform(scrollTo(), click());
        waitFor(1000); // Wait for dialog to appear
        onView(withText(R.string.dialog_button_logout)).perform(click());
        waitFor(2000); // Wait for logout API call and navigation
        onView(withId(R.id.tv_login_title)).check(matches(isDisplayed())); // Verify on login screen
    }


    @Before
    public void setup() {
        // At the start of each test, MainActivity is launched by ActivityScenarioRule.
        // We will perform a cleanup if the user is logged in from a previous test.
        if (AuthRepository.getInstance().isLoggedInSynchronous()) {
            // Need to be on MainActivity to initiate logout via UI.
            // This check ensures we are in a state where UI logout can be performed.
            // If the app is not in MainActivity, the test might need to handle this differently
            // (e.g., by skipping logout or explicitly restarting the app).
            try {
                onView(withId(R.id.tv_app_title_main)).check(matches(isDisplayed()));
                logoutUser(); // Log out if already logged in
            } catch (Exception e) {
                // If tv_app_title_main is not found, it means MainActivity is not currently visible.
                // This can happen if a previous test navigated away and didn't return, or if it crashed.
                // For a robust setup, we should ensure app is in MainActivity.
                // However, for this context, if it's not on MainActivity, we assume it's on LoginActivity
                // (e.g., from a redirect test) and skip the logout, letting the test itself handle login.
                System.err.println("Skipping logout in @Before: MainActivity not found. Assuming app is on LoginActivity or unexpected state.");
                return;
            }
        }
    }

    @After
    public void tearDown() {
        // After each test, log out to ensure clean state for the next test.
        // This ensures the next @Before setup starts cleanly.
        if (AuthRepository.getInstance().isLoggedInSynchronous()) {
            // Perform basic check if MainActivity is still in hierarchy before attempting logout UI
            // This avoids issues if the test crashed mid-navigation.
            try {
                onView(withId(R.id.main_activity_container)).check(matches(isDisplayed()));
                logoutUser();
            } catch (Exception e) {
                // If main_activity_container is not displayed, it means the app is probably not in a state
                // where UI logout can be performed (e.g., crashed, on login screen).
                // Log the exception but let the test finish.
                System.err.println("Error during tearDown logout: " + e.getMessage());
            }
        }
    }

    // --- I. Authentication (Login & Register) ---

    @Test
    public void testSuccessfulLogin() {
        loginAs(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        // Assertions are already inside loginAs helper
    }

    @Test
    public void testFailedLogin() {
        navigateToLoginScreen();
        performLoginSteps("nonexistent@example.com", "wrongpassword");
        onView(withId(R.id.til_password_login)).check(matches(hasDescendant(withText(containsString("Login Failed")))));
        onView(withId(R.id.btn_login)).check(matches(isDisplayed()));
    }

    @Test
    public void testSuccessfulRegistration() {
        navigateToLoginScreen(); // Go to login screen first
        onView(withId(R.id.tv_register_prompt)).perform(click()); // Navigate to Register screen
        waitFor(500); // Wait for navigation

        String uniqueEmail = "test_reg_" + System.currentTimeMillis() + "@test.com";
        String uniqueUsername = "testuser_" + System.currentTimeMillis();
        String password = "testpassword123";

        onView(withId(R.id.et_username_register)).perform(replaceText(uniqueUsername), closeSoftKeyboard());
        onView(withId(R.id.et_email_register)).perform(replaceText(uniqueEmail), closeSoftKeyboard());
        onView(withId(R.id.et_password_register)).perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.btn_register)).perform(click());
        waitFor(5000); // Wait for registration API call and navigation

        onView(withText(R.string.toast_event_created_successfully)).inRoot(new ToastMatcher()).check(matches(isDisplayed())); // Verify toast
        onView(withId(R.id.tv_login_title)).check(matches(isDisplayed())); // Verify navigated back to Login
        onView(withId(R.id.et_username_login)).check(matches(withText(uniqueEmail))); // Email pre-filled
    }

    @Test
    public void testFailedRegistration_existingEmail() {
        navigateToLoginScreen(); // Go to login screen first
        onView(withId(R.id.tv_register_prompt)).perform(click()); // Navigate to Register screen
        waitFor(500);

        onView(withId(R.id.et_username_register)).perform(replaceText("Existing User"), closeSoftKeyboard());
        onView(withId(R.id.et_email_register)).perform(replaceText(TEST_USER_EMAIL), closeSoftKeyboard()); // Use existing email
        onView(withId(R.id.et_password_register)).perform(replaceText("newpassword"), closeSoftKeyboard());
        onView(withId(R.id.btn_register)).perform(click());
        waitFor(3000);

        onView(withId(R.id.til_email_register)).check(matches(hasDescendant(withText(containsString("email"))))); // Error on email field
    }

    // --- II. Event Management ---

    @Test
    public void testCreateEventNewLocation() {
        loginAs(TEST_ORGANIZER_EMAIL, TEST_ORGANIZER_PASSWORD);
        onView(withId(R.id.navigation_create_event)).perform(click()); // Navigate to Create Event tab
        waitFor(1000);

        String eventName = "UI Test Event " + System.currentTimeMillis();
        String eventDescription = "Description for UI Test Event";

        // Fill event details
        onView(withId(R.id.et_event_name_create)).perform(replaceText(eventName), closeSoftKeyboard());
        onView(withId(R.id.et_event_description_create)).perform(replaceText(eventDescription), closeSoftKeyboard());
        onView(withId(R.id.et_event_max_participants_create)).perform(replaceText("50"), closeSoftKeyboard());

        // Set future dates (adjust as needed based on current date)
        Calendar futureStartDate = Calendar.getInstance();
        futureStartDate.add(Calendar.DAY_OF_YEAR, 7); // 7 days from now
        onView(withId(R.id.btn_select_event_start_date)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(
                futureStartDate.get(Calendar.YEAR), futureStartDate.get(Calendar.MONTH) + 1, futureStartDate.get(Calendar.DAY_OF_MONTH)));
        onView(withId(android.R.id.button1)).perform(click()); // Click OK

        onView(withId(R.id.btn_select_event_start_time)).perform(click());
        onView(withClassName(Matchers.equalTo(TimePicker.class.getName()))).perform(PickerActions.setTime(10, 0));
        onView(withId(android.R.id.button1)).perform(click()); // Click OK

        Calendar futureEndDate = (Calendar) futureStartDate.clone();
        futureEndDate.add(Calendar.HOUR_OF_DAY, 2); // 2 hours after start
        onView(withId(R.id.btn_select_event_end_date)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(
                futureEndDate.get(Calendar.YEAR), futureEndDate.get(Calendar.MONTH) + 1, futureEndDate.get(Calendar.DAY_OF_MONTH)));
        onView(withId(android.R.id.button1)).perform(click()); // Click OK

        onView(withId(R.id.btn_select_event_end_time)).perform(click());
        onView(withClassName(Matchers.equalTo(TimePicker.class.getName()))).perform(PickerActions.setTime(12, 0));
        onView(withId(android.R.id.button1)).perform(click()); // Click OK

        // Select new location type (already default in current UI state)
        // onView(withId(R.id.rb_new_location)).perform(click()); // Ensure New Location is selected

        // Fill new location details
        onView(withId(R.id.et_street_name_create)).perform(replaceText("Test Street"), closeSoftKeyboard());
        onView(withId(R.id.et_street_number_create)).perform(replaceText("123"), closeSoftKeyboard());
        onView(withId(R.id.et_postal_code_create)).perform(replaceText("12345"), closeSoftKeyboard());
        onView(withId(R.id.et_city_create)).perform(replaceText("Test City"), closeSoftKeyboard());
        onView(withId(R.id.et_region_create)).perform(replaceText("TR"), closeSoftKeyboard());
        onView(withId(R.id.et_country_iso_create)).perform(replaceText("PL"), closeSoftKeyboard());

        // Submit event
        onView(withId(R.id.btn_create_event_submit)).perform(click());
        waitFor(5000); // Wait for event creation API call and navigation

        onView(withText(R.string.toast_event_created_successfully)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));

        // Verify event appears in "My Events" tab
        onView(withId(R.id.navigation_events_created)).perform(click());
        waitFor(2000); // Wait for list to load
        onView(withText(eventName)).check(matches(isDisplayed()));
        onView(withText(eventDescription)).check(matches(isDisplayed()));
    }

    @Test
    public void testViewPublicEvents() {
        // Already on public events tab by default after launching MainActivity
        onView(withId(R.id.navigation_events_public)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_items_main)).check(matches(isDisplayed()));
        waitFor(2000); // Wait for events to load
        // Verify at least one event card is displayed (if data exists)
        // This assumes your public events list is not empty for testing
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).check(matches(isDisplayed()));
    }

    @Test
    public void testViewAttendedEvents_LoggedIn() {
        loginAs(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        onView(withId(R.id.navigation_events_known)).perform(click()); // Navigate to Attended Events tab
        waitFor(2000); // Wait for events to load

        // Assuming there are some attended events for TEST_USER_EMAIL
        // If not, test should adapt or add test data first
        onView(withId(R.id.rv_items_main)).check(matches(isDisplayed()));
        // Check for specific content if known, or just that it's not the "no items" message initially
        onView(withId(R.id.tv_no_items_main)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testViewAttendedEvents_LoggedOut() {
        // Test setup ensures logged out
        onView(withId(R.id.navigation_events_known)).perform(click());
        waitFor(1000);
        onView(withId(R.id.tv_no_items_main)).check(matches(withText(R.string.login_to_see_attended_events_detail)));
    }

    @Test
    public void testEventDetailView() {
        // Ensure public events are loaded
        onView(withId(R.id.navigation_events_public)).perform(click());
        waitFor(2000);

        // Click on the first event in the list (assuming at least one exists)
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).perform(click());
        waitFor(2000); // Wait for detail screen to load

        onView(withId(R.id.tv_event_name_detail)).check(matches(isDisplayed()));
        onView(withId(R.id.tv_event_description_detail)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_join_leave_event_detail)).check(matches(isDisplayed()));
    }

    @Test
    public void testJoinLeaveEvent() {
        loginAs(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        onView(withId(R.id.navigation_events_public)).perform(click());
        waitFor(2000);

        // Click on an event to go to details (choose one that the user is not yet participating in)
        // For simplicity, click the first event and assume it's joinable.
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).perform(click());
        waitFor(2000);

        // Check if button says "Join Event" or "Leave Event"
        onView(withId(R.id.btn_join_leave_event_detail)).check(matches(isDisplayed()));

        // Perform Join (if applicable)
        onView(withId(R.id.btn_join_leave_event_detail)).perform(click());
        waitFor(3000); // Wait for API call and UI update
        onView(withText(R.string.event_action_successful)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
        onView(withId(R.id.btn_join_leave_event_detail)).check(matches(withText(R.string.btn_leave_event)));

        // Perform Leave
        onView(withId(R.id.btn_join_leave_event_detail)).perform(click());
        waitFor(3000); // Wait for API call and UI update
        onView(withText(R.string.event_action_successful)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
        onView(withId(R.id.btn_join_leave_event_detail)).check(matches(withText(R.string.btn_join_event)));
    }


    @Test
    public void testDeleteEvent_Organizer() {
        loginAs(TEST_ORGANIZER_EMAIL, TEST_ORGANIZER_PASSWORD);
        onView(withId(R.id.navigation_events_created)).perform(click()); // Go to created events
        waitFor(2000);

        // Assuming there's at least one created event to delete.
        // For simplicity, find the first created event
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).check(matches(isDisplayed())); // Added check
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed()))
                .perform(click()); // Click on the first event
        waitFor(2000);

        onView(withId(R.id.btn_delete_event_detail)).check(matches(isDisplayed())); // Added check
        onView(withId(R.id.btn_delete_event_detail)).perform(scrollTo(), click());
        waitFor(1000); // Wait for dialog
        onView(withText(R.string.dialog_button_delete)).perform(click());
        waitFor(3000); // Wait for delete API call and navigation back

        onView(withText(R.string.event_action_successful)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
        onView(withId(R.id.tv_app_title_main)).check(matches(isDisplayed())); // Verify back on main screen
        // Could add a check that the deleted event is no longer in "My Events" list.
    }

    @Test
    public void testDeleteEvent_NonOrganizer() {
        loginAs(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        onView(withId(R.id.navigation_events_public)).perform(click());
        waitFor(2000);

        // Click on any public event
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).check(matches(isDisplayed())); // Added check
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed()))
                .perform(click());
        waitFor(2000);

        // Verify delete button is not visible
        onView(withId(R.id.btn_delete_event_detail)).check(matches(not(isDisplayed())));
    }


    // --- III. User & Profile Management ---

    @Test
    public void testViewOwnProfile_LoggedIn() {
        loginAs(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        navigateToProfileScreen(); // Click profile icon
        waitFor(2000);

        onView(withId(R.id.tv_profile_full_name)).check(matches(isDisplayed()));
        onView(withId(R.id.tv_profile_email)).check(matches(withText(TEST_USER_EMAIL)));
        onView(withId(R.id.tv_profile_role)).check(matches(withText("USER")));
        onView(withId(R.id.btn_logout)).check(matches(isDisplayed())); // Logout button should be visible for own profile
    }

    @Test
    public void testViewAnotherUserProfile_FromParticipants() {
        loginAs(TEST_ORGANIZER_EMAIL, TEST_ORGANIZER_PASSWORD);
        onView(withId(R.id.navigation_events_public)).perform(click());
        waitFor(2000);

        // Click on the first public event (assuming it has participants)
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).check(matches(isDisplayed())); // Added check
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).perform(click());
        waitFor(2000);

        // Scroll to participants list and click on the first participant (assuming there is one)
        onView(withId(R.id.rv_event_participants)).perform(scrollTo());
        waitFor(1000);
        // Ensure participant item is displayed before clicking
        onView(allOf(withId(R.id.tv_participant_name), isDisplayed())).check(matches(isDisplayed())); // Added check
        onView(withId(R.id.rv_event_participants))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click())); // Corrected usage of atPosition
        waitFor(2000);

        onView(withId(R.id.tv_profile_full_name)).check(matches(isDisplayed()));
        // Check if the displayed profile is NOT the logged-in user's profile
        onView(withId(R.id.tv_profile_email)).check(matches(not(withText(TEST_ORGANIZER_EMAIL))));
        onView(withId(R.id.btn_logout)).check(matches(not(isDisplayed()))); // Logout button should not be visible for other profiles
    }

    @Test
    public void testViewProfile_LoggedOutRedirectsToLogin() {
        // Test setup ensures logged out
        navigateToProfileScreen(); // Try to go to profile when not logged in
        waitFor(1000);

        onView(withId(R.id.tv_login_title)).check(matches(isDisplayed())); // Should navigate to LoginActivity
        onView(withText(R.string.login_to_view_profiles)).inRoot(new ToastMatcher()).check(matches(isDisplayed())); // Verify toast
    }

    @Test
    public void testLogout() {
        loginAs(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        logoutUser(); // Use the helper
        // Assertions are already inside logoutUser helper
    }

    // --- IV. Invitations ---

    @Test
    public void testSendAndAcceptInvitation() {
        // 1. Organizer sends invitation
        loginAs(TEST_ORGANIZER_EMAIL, TEST_ORGANIZER_PASSWORD);
        onView(withId(R.id.navigation_events_created)).perform(click());
        waitFor(2000);

        // Assuming an event exists, click on it (first one)
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).check(matches(isDisplayed())); // Added check
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).perform(click());
        waitFor(2000);

        onView(withId(R.id.btn_invite_detail)).check(matches(isDisplayed())); // Added check
        onView(withId(R.id.btn_invite_detail)).perform(scrollTo(), click());
        waitFor(1000);

        // Find the user to invite (e.g., TEST_USER_EMAIL) and select them
        onView(withId(R.id.et_search_users_invite)).perform(replaceText(TEST_USER_EMAIL), closeSoftKeyboard()); // Corrected to use TEST_USER_EMAIL constant
        waitFor(1000); // Wait for filter to apply
        // Ensure user list item is displayed
        onView(allOf(withId(R.id.tv_user_login_invite_item), withText(TEST_USER_EMAIL))).check(matches(isDisplayed())); // Added check
        onView(allOf(withId(R.id.tv_user_login_invite_item), withText(TEST_USER_EMAIL))).perform(click());
        waitFor(500);
        onView(withId(R.id.btn_send_invitations)).check(matches(isDisplayed())); // Added check
        onView(withId(R.id.btn_send_invitations)).perform(click());
        waitFor(3000);
        onView(withText(R.string.invitations_sent_success)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
        onView(withId(R.id.tv_event_name_detail)).check(matches(isDisplayed())); // Verify back on event detail

        // 2. Invited user accepts invitation
        logoutUser();
        loginAs(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        onView(withId(R.id.navigation_invitations)).perform(click()); // Navigate to Invitations tab
        waitFor(3000); // Longer wait for invitation list to load

        // Assuming the sent invitation is the first one, accept it
        onView(allOf(withId(R.id.btn_accept_invitation), isDisplayed())).check(matches(isDisplayed())); // Added check
        onView(allOf(withId(R.id.btn_accept_invitation), isDisplayed())).perform(click());
        waitFor(3000);
        onView(withText(R.string.invitation_action_successful)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));

        // Verify it's no longer in invitations, and appears in "Attended Events"
        onView(withId(R.id.tv_no_items_main)).check(matches(withText(R.string.no_pending_invitations))); // No more invitations
        onView(withId(R.id.navigation_events_known)).perform(click()); // Go to Attended Events
        waitFor(2000);
        // This assertion needs to know the event name, but for simplicity, just check list is not empty
        onView(withId(R.id.rv_items_main)).check(matches(isDisplayed()));
    }

    // --- V. Map Functionality ---

    @Test
    public void testViewEventLocationOnMap() {
        loginAs(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        onView(withId(R.id.navigation_events_public)).perform(click());
        waitFor(2000);

        // Click on an event (must have location data for this test)
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).check(matches(isDisplayed())); // Added check
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).perform(click());
        waitFor(2000);

        onView(withId(R.id.btn_get_directions_detail)).check(matches(isDisplayed())); // Added check
        onView(withId(R.id.btn_get_directions_detail)).perform(scrollTo(), click());
        waitFor(3000); // Wait for map to load

        // Verify map fragment is displayed
        onView(withId(R.id.map_container)).check(matches(isDisplayed()));
        // Check for map elements (e.g., zoom controls, if present)
        // onView(withId(R.id.google_map)).check(matches(isDisplayed())); // This depends on how GoogleMap views are exposed
    }

    @Test
    public void testOpenInExternalMaps() {
        loginAs(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        onView(withId(R.id.navigation_events_public)).perform(click());
        waitFor(2000);

        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).check(matches(isDisplayed())); // Added check
        onView(allOf(withId(R.id.tv_event_title_card), isDisplayed())).perform(click());
        waitFor(2000);

        onView(withId(R.id.btn_get_directions_detail)).check(matches(isDisplayed())); // Added check
        onView(withId(R.id.btn_get_directions_detail)).perform(scrollTo(), click());
        waitFor(3000);

        onView(withId(R.id.btn_open_in_maps_app)).check(matches(isDisplayed())); // Added check
        // Click the "Open in Maps App" button
        onView(withId(R.id.btn_open_in_maps_app)).perform(click());

        // Verify an external map intent was fired
        intended(allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(Matchers.hasToString(startsWith("geo:"))) // Corrected: Use hasToString for Uri Matcher
        ));
    }


    // --- Custom ToastMatcher for verifying Toast messages ---
    // This is necessary because Toasts are not part of the view hierarchy managed by ActivityScenarioRule
    public static class ToastMatcher extends TypeSafeMatcher<Root> {

        @Override
        public void describeTo(Description description) {
            description.appendText("is toast");
        }

        @Override
        public boolean matchesSafely(Root root) {
            // Correctly access WindowManager.LayoutParams
            int type = root.getWindowLayoutParams().get().type;
            if ((type == WindowManager.LayoutParams.TYPE_TOAST)) {
                // Get the window token for the toast and the application
                IBinder windowToken = root.getDecorView().getWindowToken();
                IBinder appToken = root.getDecorView().getApplicationWindowToken();
                if (windowToken == appToken) {
                    //means this window isn't a toast (it's the app window)
                    return false;
                }
                return true;
            }
            return false;
        }
    }
}