package com.example.event_hub.Model;

import androidx.core.util.Pair; // For Pair from AuthRepository
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EventHubRepository {

    private static volatile EventHubRepository INSTANCE;
    private final ExecutorService executorService;
    private final AuthRepository authRepository;

    // Data Stores
    private final List<UserModel> simulatedUsers = new ArrayList<>();
    private final List<EventModel> simulatedEvents = new ArrayList<>();
    private final List<ParticipantModel> simulatedParticipation = new ArrayList<>();
    private final List<InvitationModel> simulatedInvitations = new ArrayList<>();
    private final List<MediaModel> simulatedMedia = new ArrayList<>();

    // LiveData
    private final MutableLiveData<ResultWrapper<List<EventModel>>> _publicEventsState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<List<EventModel>>> publicEventsState = _publicEventsState;

    private final MutableLiveData<ResultWrapper<List<InvitationModel>>> _userInvitationsState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<List<InvitationModel>>> userInvitationsState = _userInvitationsState;

    private final MutableLiveData<ResultWrapper<List<UserModel>>> _eventParticipantsState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<List<UserModel>>> eventParticipantsState = _eventParticipantsState;

    private final MutableLiveData<ResultWrapper<List<MediaModel>>> _eventMediaState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<List<MediaModel>>> eventMediaState = _eventMediaState;

    private final MutableLiveData<ResultWrapper<UserModel>> _userProfileOperationState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<UserModel>> userProfileOperationState = _userProfileOperationState;

    private final MutableLiveData<ResultWrapper<EventModel>> _singleEventOperationState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<EventModel>> singleEventOperationState = _singleEventOperationState;

    private final MutableLiveData<ResultWrapper<Void>> _voidOperationState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<Void>> voidOperationState = _voidOperationState;

    private final MutableLiveData<ResultWrapper<MediaModel>> _mediaUploadOperationState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<MediaModel>> mediaUploadOperationState = _mediaUploadOperationState;


    private EventHubRepository() {
        executorService = Executors.newFixedThreadPool(3); // Increased pool size for concurrent operations
        authRepository = AuthRepository.getInstance();
        initializePlaceholderData();
    }

    public static EventHubRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (EventHubRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EventHubRepository();
                }
            }
        }
        return INSTANCE;
    }

    private boolean isPrincipalAdmin(Pair<String, String> principal) {
        return principal != null && "admin".equalsIgnoreCase(principal.second);
    }

    private Optional<UserModel> findUserById(String userId) {
        if (userId == null) return Optional.empty();
        return simulatedUsers.stream().filter(u -> userId.equals(u.getUserId())).findFirst();
    }

    private Optional<EventModel> findEventById(String eventId) {
        if (eventId == null) return Optional.empty();
        return simulatedEvents.stream().filter(e -> eventId.equals(e.getId())).findFirst();
    }

    private void initializePlaceholderData() {
        simulatedUsers.clear();
        simulatedEvents.clear();
        simulatedParticipation.clear();
        simulatedInvitations.clear();
        simulatedMedia.clear();

        UserDetails detailsAdmin = new UserDetails("Admin FullName", "I am the administrator.", "admin_avatar.png");
        simulatedUsers.add(new UserModel("admin_user_007", "admin@example.com", "admin@example.com", "admin", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)), "active", detailsAdmin));
        UserDetails details1 = new UserDetails("Test User FullName", "Bio for the main test user.", "test_avatar.png");
        simulatedUsers.add(new UserModel("user_jwt_123", "test@example.com", "test@example.com", "user", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10)), "active", details1)); // User for test@example.com
        UserDetails details2 = new UserDetails("Another User", "Organizer of the Tech Conference.", "another_avatar.png");
        simulatedUsers.add(new UserModel("another_user_456", "another_user_login", "another@test.com", "organizer", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)), "active", details2));

        long currentTime = System.currentTimeMillis();
        // Event in 30 days
        simulatedEvents.add(new EventModel("event101", "Tech Conference 2025", "Annual technology conference.", "Convention Center", new Date(currentTime + TimeUnit.DAYS.toMillis(30)), new Date(currentTime + TimeUnit.DAYS.toMillis(32)), 500, "another_user_456"));
        // Event in 15 days
        simulatedEvents.add(new EventModel("event102", "Admin's Grand Meetup", "A special event by admin.", "Secret HQ", new Date(currentTime + TimeUnit.DAYS.toMillis(15)), new Date(currentTime + TimeUnit.DAYS.toMillis(15) + TimeUnit.HOURS.toMillis(5)), 100, "admin_user_007"));
        // Event for "today" to test default view in MainFragment
        // Calendar todayCal = Calendar.getInstance();
        // simulatedEvents.add(new EventModel("event_today_01", "Today's Local Market", "Fresh produce and crafts.", "Town Square", todayCal.getTime(), new Date(todayCal.getTimeInMillis() + TimeUnit.HOURS.toMillis(4)), 50, "user_jwt_123"));


        _publicEventsState.postValue(new ResultWrapper.Success<>(new ArrayList<>(simulatedEvents))); // Initialize with all events

        simulatedParticipation.add(new ParticipantModel("event101", "user_jwt_123", "attending", "participant"));
        simulatedParticipation.add(new ParticipantModel("event101", "another_user_456", "attending", "organizer"));
        simulatedParticipation.add(new ParticipantModel("event102", "admin_user_007", "attending", "organizer"));

        simulatedInvitations.add(new InvitationModel("inv_001", "event101", "user_jwt_123", "sent", new Date(currentTime - TimeUnit.DAYS.toMillis(1)), null));
        simulatedInvitations.add(new InvitationModel("inv_admin_001", "event102", "user_jwt_123", "sent", new Date(currentTime - TimeUnit.DAYS.toMillis(2)), null));

        simulatedMedia.add(new MediaModel("media_001", "event101", "another_user_456", "https://via.placeholder.com/300.png/09f/fff?Text=Event101Logo", "image/png", new Date(), "logo", "event101_logo.png", "Tech Conference Logo"));
        simulatedMedia.add(new MediaModel("media_admin_001", "event102", "admin_user_007", "https://via.placeholder.com/300.png/f90/fff?Text=AdminEventLogo", "image/png", new Date(), "logo", "admin_event_logo.png", "Admin Event Logo"));
        System.out.println("EventHubRepository: Placeholder data initialized.");
    }

    // --- Profile Methods ---
    public void fetchUserProfile(String userId) {
        _userProfileOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(200); // Simulate network delay
                Optional<UserModel> userOpt = findUserById(userId);
                if (userOpt.isPresent()) {
                    _userProfileOperationState.postValue(new ResultWrapper.Success<>(userOpt.get()));
                } else {
                    _userProfileOperationState.postValue(new ResultWrapper.Error<>("Profile not found: " + userId));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _userProfileOperationState.postValue(new ResultWrapper.Error<>("Profile fetch interrupted."));
            }
        });
    }

    public void editProfile(String userIdToEdit, UserDetails updatedDetails, String authToken) {
        _userProfileOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> principalOpt = authRepository.validateTokenAndExtractPrincipal(authToken);
            if (!principalOpt.isPresent()) {
                _userProfileOperationState.postValue(new ResultWrapper.Error<>("Unauthorized: Invalid token."));
                return;
            }
            Pair<String, String> principal = principalOpt.get();
            try {
                Thread.sleep(500);
                if (!userIdToEdit.equals(principal.first) && !isPrincipalAdmin(principal)) {
                    _userProfileOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Cannot edit another user's profile."));
                    return;
                }
                Optional<UserModel> userOpt = findUserById(userIdToEdit);
                if (userOpt.isPresent()) {
                    UserModel user = userOpt.get();
                    user.setUserDetails(updatedDetails); // Update the details
                    _userProfileOperationState.postValue(new ResultWrapper.Success<>(user)); // Post success with updated user
                } else {
                    _userProfileOperationState.postValue(new ResultWrapper.Error<>("Update failed: User to edit not found."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _userProfileOperationState.postValue(new ResultWrapper.Error<>("Edit profile interrupted."));
            }
        });
    }
    public void deleteUser(String userIdToDelete, String authToken) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> principalOpt = authRepository.validateTokenAndExtractPrincipal(authToken);
            if (!principalOpt.isPresent()) {
                _voidOperationState.postValue(new ResultWrapper.Error<>("Unauthorized: Invalid token."));
                return;
            }
            Pair<String, String> principal = principalOpt.get();
            try {
                Thread.sleep(1000);
                if (!userIdToDelete.equals(principal.first) && !isPrincipalAdmin(principal)) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Insufficient rights to delete user."));
                    return;
                }

                // Design Consideration: Prevent deletion of the last admin.
                Optional<UserModel> userToDeleteOpt = findUserById(userIdToDelete);
                if (userToDeleteOpt.isPresent() && "admin".equalsIgnoreCase(userToDeleteOpt.get().getRole())) {
                    long adminCount = simulatedUsers.stream().filter(u -> "admin".equalsIgnoreCase(u.getRole()) && "active".equals(u.getStatus())).count();
                    if (adminCount <= 1) {
                        _voidOperationState.postValue(new ResultWrapper.Error<>("Cannot delete the last active admin account."));
                        return;
                    }
                }

                boolean removed = simulatedUsers.removeIf(u -> userIdToDelete.equals(u.getUserId()));
                if (removed) {
                    // Cascade deletes for related data
                    simulatedParticipation.removeIf(p -> userIdToDelete.equals(p.getAccountId()));
                    simulatedInvitations.removeIf(i -> userIdToDelete.equals(i.getAccountId()));
                    simulatedMedia.removeIf(m -> userIdToDelete.equals(m.getAccountId()));
                    // Also remove events created by this user
                    simulatedEvents.removeIf(e -> userIdToDelete.equals(e.getCreatedBy()));

                    // If the deleted user is the currently logged-in user in AuthRepository, log them out
                    if (userIdToDelete.equals(authRepository.getCurrentUserIdSynchronous())) {
                        authRepository.logout(); // This will update AuthRepository's LiveData
                    }

                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    fetchPublicEvents(); // Refresh public events list as some might have been deleted
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Deletion failed: User not found."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Delete user interrupted."));
            }
        });
    }


    public void banUser(String userIdToBan, String authToken) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> principalOpt = authRepository.validateTokenAndExtractPrincipal(authToken);
            if (!principalOpt.isPresent() || !isPrincipalAdmin(principalOpt.get())) {
                _voidOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Only admins can ban users."));
                return;
            }
            Pair<String, String> principal = principalOpt.get(); // Current admin
            try {
                Thread.sleep(500);
                if (userIdToBan.equals(principal.first)) { // Admin trying to ban self
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Admins cannot ban themselves."));
                    return;
                }

                Optional<UserModel> userOpt = findUserById(userIdToBan);
                if (userOpt.isPresent()) {
                    UserModel user = userOpt.get();
                    if ("admin".equalsIgnoreCase(user.getRole())) { // Admin trying to ban another admin
                        _voidOperationState.postValue(new ResultWrapper.Error<>("Cannot ban another admin."));
                        return;
                    }
                    user.setStatus("banned");
                    // Update participation status for the banned user
                    simulatedParticipation.forEach(p -> {
                        if (userIdToBan.equals(p.getAccountId())) {
                            p.setStatus("banned"); // Or "cancelled" if more appropriate
                        }
                    });
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    // Optionally refresh user profile if it's being observed elsewhere
                    fetchUserProfile(userIdToBan); // This updates _userProfileOperationState
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Ban failed: User not found."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Ban user interrupted."));
            }
        });
    }


    // --- Event Methods ---
    public void fetchPublicEvents() {
        _publicEventsState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(700); // Simulate network delay
                // In a real app, this would be a network call. Here, just return the simulated list.
                // The list might be filtered by backend for "public" status, date, etc.
                // For simulation, we return all events.
                _publicEventsState.postValue(new ResultWrapper.Success<>(new ArrayList<>(simulatedEvents)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _publicEventsState.postValue(new ResultWrapper.Error<>("Public events fetch interrupted."));
            }
        });
    }

    public LiveData<ResultWrapper<List<EventModel>>> fetchAttendedEvents(String userId) {
        MutableLiveData<ResultWrapper<List<EventModel>>> specificUserAttendedEvents = new MutableLiveData<>(new ResultWrapper.Idle<>());
        if (userId == null || userId.isEmpty()) {
            specificUserAttendedEvents.postValue(new ResultWrapper.Error<>("User ID required to fetch attended events."));
            return specificUserAttendedEvents;
        }
        specificUserAttendedEvents.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(700); // Simulate delay
                List<String> attendedEventIds = simulatedParticipation.stream()
                        .filter(p -> userId.equals(p.getAccountId()) && "attending".equals(p.getStatus()))
                        .map(ParticipantModel::getEventId)
                        .distinct()
                        .collect(Collectors.toList());

                List<EventModel> attendedEvents = simulatedEvents.stream()
                        .filter(event -> attendedEventIds.contains(event.getId()))
                        .collect(Collectors.toList());
                specificUserAttendedEvents.postValue(new ResultWrapper.Success<>(attendedEvents));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                specificUserAttendedEvents.postValue(new ResultWrapper.Error<>("Attended events fetch interrupted."));
            }
        });
        return specificUserAttendedEvents;
    }

    public void fetchEventDetails(String eventId) {
        _singleEventOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(400); // Simulate delay
                Optional<EventModel> eventOpt = findEventById(eventId);
                if (eventOpt.isPresent()) {
                    _singleEventOperationState.postValue(new ResultWrapper.Success<>(eventOpt.get()));
                } else {
                    _singleEventOperationState.postValue(new ResultWrapper.Error<>("Event not found: " + eventId));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _singleEventOperationState.postValue(new ResultWrapper.Error<>("Event details fetch interrupted."));
            }
        });
    }


    public void createEvent(EventModel eventToCreate, String authToken) {
        _singleEventOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> principalOpt = authRepository.validateTokenAndExtractPrincipal(authToken);
            if (!principalOpt.isPresent()) {
                _singleEventOperationState.postValue(new ResultWrapper.Error<>("Unauthorized: Login to create event."));
                return;
            }
            Pair<String, String> principal = principalOpt.get();
            try {
                Thread.sleep(1000);
                eventToCreate.setCreatedBy(principal.first); // Set creator from validated token
                if (eventToCreate.getTitle() == null || eventToCreate.getTitle().trim().isEmpty()) {
                    _singleEventOperationState.postValue(new ResultWrapper.Error<>("Event title is required.")); return;
                }
                if (eventToCreate.getStartDate() == null || eventToCreate.getEndDate() == null || eventToCreate.getEndDate().before(eventToCreate.getStartDate())) {
                    _singleEventOperationState.postValue(new ResultWrapper.Error<>("Valid start and end dates are required for event. End date must be after start date.")); return;
                }
                if (eventToCreate.getId() == null || eventToCreate.getId().isEmpty()) {
                    eventToCreate.setId("event_new_" + UUID.randomUUID().toString().substring(0, 8));
                }
                simulatedEvents.add(eventToCreate);
                // Automatically add creator as organizer participant
                simulatedParticipation.add(new ParticipantModel(eventToCreate.getId(), principal.first, "attending", "organizer"));
                _singleEventOperationState.postValue(new ResultWrapper.Success<>(eventToCreate));
                fetchPublicEvents(); // Refresh public events list
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _singleEventOperationState.postValue(new ResultWrapper.Error<>("Create event interrupted."));
            }
        });
    }

    public void deleteEvent(String eventId, String authToken) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> principalOpt = authRepository.validateTokenAndExtractPrincipal(authToken);
            if (!principalOpt.isPresent()) {
                _voidOperationState.postValue(new ResultWrapper.Error<>("Unauthorized.")); return;
            }
            Pair<String, String> principal = principalOpt.get();
            try {
                Thread.sleep(1000);
                Optional<EventModel> eventOpt = findEventById(eventId);
                if (!eventOpt.isPresent()) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Delete event failed: Event not found.")); return;
                }
                EventModel event = eventOpt.get();
                if (!principal.first.equals(event.getCreatedBy()) && !isPrincipalAdmin(principal)) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Only the event creator or an admin can delete this event.")); return;
                }
                boolean removed = simulatedEvents.removeIf(e -> e.getId().equals(eventId));
                if (removed) {
                    simulatedParticipation.removeIf(p -> p.getEventId().equals(eventId));
                    simulatedInvitations.removeIf(i -> i.getEventId().equals(eventId));
                    simulatedMedia.removeIf(m -> m.getEventId().equals(eventId));
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    fetchPublicEvents(); // Refresh public events list
                } else {
                    // This case should ideally not happen if eventOpt.isPresent() was true
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Delete event failed unexpectedly after finding event."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Delete event interrupted."));
            }
        });
    }

    public void joinPublicEvent(String eventId, String authToken) { // Changed userId to authToken
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> principalOpt = authRepository.validateTokenAndExtractPrincipal(authToken);
            if (!principalOpt.isPresent()) {
                _voidOperationState.postValue(new ResultWrapper.Error<>("Unauthorized: Please login to join.")); return;
            }
            String userId = principalOpt.get().first; // Get userId from token
            try {
                Thread.sleep(600);
                Optional<EventModel> eventOpt = findEventById(eventId);
                if (!eventOpt.isPresent()) { _voidOperationState.postValue(new ResultWrapper.Error<>("Join failed: Event not found.")); return; }

                Optional<ParticipantModel> existingParticipation = simulatedParticipation.stream()
                        .filter(p -> eventId.equals(p.getEventId()) && userId.equals(p.getAccountId()))
                        .findFirst();

                if (existingParticipation.isPresent()) {
                    ParticipantModel participation = existingParticipation.get();
                    if ("banned".equals(participation.getStatus())) { _voidOperationState.postValue(new ResultWrapper.Error<>("Cannot join: User is banned from this event.")); return; }
                    else if ("attending".equals(participation.getStatus())) { _voidOperationState.postValue(new ResultWrapper.Success<>(null)); return; } // Already attending
                    else { // e.g., was "cancelled", now rejoining
                        participation.setStatus("attending");
                        participation.setEventRole("participant"); // Reset role to participant
                        _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    }
                } else {
                    simulatedParticipation.add(new ParticipantModel(eventId, userId, "attending", "participant"));
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                }
                fetchEventParticipants(eventId); // Refresh participants for this event
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Join event interrupted."));
            }
        });
    }

    public void leaveEvent(String eventId, String authToken) { // Changed userId to authToken
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> principalOpt = authRepository.validateTokenAndExtractPrincipal(authToken);
            if (!principalOpt.isPresent()) {
                _voidOperationState.postValue(new ResultWrapper.Error<>("Unauthorized: Please login.")); return;
            }
            String userId = principalOpt.get().first; // Get userId from token
            try {
                Thread.sleep(600);
                // Remove if user is an active participant. Don't remove if they are, e.g., an organizer who "cancelled" their own participation.
                boolean removed = simulatedParticipation.removeIf(p -> eventId.equals(p.getEventId()) && userId.equals(p.getAccountId()) && "attending".equals(p.getStatus()));
                if(removed) {
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    fetchEventParticipants(eventId); // Refresh participants for this event
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Failed to leave: Not currently an active participant or participation record not found."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Leave event interrupted."));
            }
        });
    }


    // --- Participant Methods ---
    public void fetchEventParticipants(String eventId) {
        _eventParticipantsState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(750);
                List<String> accountIds = simulatedParticipation.stream()
                        .filter(p -> eventId.equals(p.getEventId()) && "attending".equals(p.getStatus())) // Only show attending
                        .map(ParticipantModel::getAccountId)
                        .distinct()
                        .collect(Collectors.toList());
                List<UserModel> participants = simulatedUsers.stream()
                        .filter(u -> accountIds.contains(u.getUserId()))
                        .collect(Collectors.toList());
                _eventParticipantsState.postValue(new ResultWrapper.Success<>(participants));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _eventParticipantsState.postValue(new ResultWrapper.Error<>("Participant fetch interrupted."));
            }
        });
    }

    public void deleteParticipant(String eventId, String participantAccountId, String authToken) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> principalOpt = authRepository.validateTokenAndExtractPrincipal(authToken);
            if (!principalOpt.isPresent()) {
                _voidOperationState.postValue(new ResultWrapper.Error<>("Unauthorized.")); return;
            }
            Pair<String, String> principal = principalOpt.get();
            try {
                Thread.sleep(500);
                Optional<EventModel> eventOpt = findEventById(eventId);
                if (!eventOpt.isPresent()) { _voidOperationState.postValue(new ResultWrapper.Error<>("Remove failed: Event not found.")); return; }
                EventModel event = eventOpt.get();

                // Permission check: Event creator or Admin can remove participants
                if (!principal.first.equals(event.getCreatedBy()) && !isPrincipalAdmin(principal)) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Only event creator or admin can remove participants.")); return;
                }

                // Prevent event creator from being removed as a participant through this action
                // (they are implicitly an organizer). They should delete the event if they want to remove themselves.
                if (participantAccountId.equals(event.getCreatedBy())) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Event creator cannot be removed as participant via this action. Consider deleting the event or transferring ownership.")); return;
                }

                boolean removed = simulatedParticipation.removeIf(p -> eventId.equals(p.getEventId()) && participantAccountId.equals(p.getAccountId()));
                if (removed) {
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    fetchEventParticipants(eventId); // Refresh participant list
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Remove failed: Participant not found in this event."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Remove participant interrupted."));
            }
        });
    }


    // --- Invitation Methods ---
    public void fetchInvitationsForUser(String accountId) {
        _userInvitationsState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(300);
                List<InvitationModel> userInvitations = simulatedInvitations.stream()
                        .filter(inv -> accountId.equals(inv.getAccountId()))
                        .collect(Collectors.toList());
                _userInvitationsState.postValue(new ResultWrapper.Success<>(userInvitations));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _userInvitationsState.postValue(new ResultWrapper.Error<>("Invitations fetch interrupted."));
            }
        });
    }


    public void sendInvitation(String eventId, String recipientAccountId, String authTokenSender) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> senderPrincipalOpt = authRepository.validateTokenAndExtractPrincipal(authTokenSender);
            if (!senderPrincipalOpt.isPresent()) {
                _voidOperationState.postValue(new ResultWrapper.Error<>("Unauthorized: Invalid sender token.")); return;
            }
            Pair<String, String> senderPrincipal = senderPrincipalOpt.get();
            try {
                Thread.sleep(500);
                Optional<EventModel> eventOpt = findEventById(eventId);
                if (!eventOpt.isPresent()) { _voidOperationState.postValue(new ResultWrapper.Error<>("Send failed: Event not found.")); return; }
                EventModel event = eventOpt.get();

                // Permission: Only event creator or an admin can send invitations for this event
                if (!senderPrincipal.first.equals(event.getCreatedBy()) && !isPrincipalAdmin(senderPrincipal)) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Only event creator or admin can send invitations for this event.")); return;
                }

                Optional<UserModel> recipientOpt = findUserById(recipientAccountId);
                if (!recipientOpt.isPresent() || !"active".equals(recipientOpt.get().getStatus())) { // Check if recipient is active
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Send invitation failed: Recipient not found or not active.")); return;
                }

                // Cannot send invitation to self
                if (recipientAccountId.equals(senderPrincipal.first)) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Cannot send invitation to self.")); return;
                }

                // Check if user is already a participant
                boolean alreadyParticipant = simulatedParticipation.stream().anyMatch(p -> eventId.equals(p.getEventId()) && recipientAccountId.equals(p.getAccountId()) && "attending".equals(p.getStatus()));
                if (alreadyParticipant) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("User is already a participant in this event.")); return;
                }

                // Check if user already has a pending or accepted invitation
                boolean alreadyInvited = simulatedInvitations.stream().anyMatch(i -> eventId.equals(i.getEventId()) && recipientAccountId.equals(i.getAccountId()) && ("sent".equals(i.getInvitationStatus()) || "accepted".equals(i.getInvitationStatus())));
                if (alreadyInvited) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("User already has an active or accepted invitation for this event.")); return;
                }

                String newInvitationId = "inv_new_" + UUID.randomUUID().toString().substring(0, 8);
                InvitationModel newInvitation = new InvitationModel(newInvitationId, eventId, recipientAccountId, "sent", new Date(), null);
                simulatedInvitations.add(newInvitation);
                _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                // Optionally, refresh invitations for the sender if they are viewing their sent invitations (not currently a feature)
                // or for the recipient if they are actively polling (handled by fetchInvitationsForUser when recipient views)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Send invitation interrupted."));
            }
        });
    }

    public void updateInvitationStatus(String invitationId, String newStatus, String authTokenActingUser) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> actingPrincipalOpt = authRepository.validateTokenAndExtractPrincipal(authTokenActingUser);
            if (!actingPrincipalOpt.isPresent()) {
                _voidOperationState.postValue(new ResultWrapper.Error<>("Unauthorized.")); return;
            }
            Pair<String, String> actingPrincipal = actingPrincipalOpt.get();
            String actingUserId = actingPrincipal.first;

            try {
                Thread.sleep(400);
                Optional<InvitationModel> invOpt = simulatedInvitations.stream().filter(inv -> inv.getInvitationId().equals(invitationId)).findFirst();
                if (!invOpt.isPresent()) { _voidOperationState.postValue(new ResultWrapper.Error<>("Update failed: Invitation not found.")); return; }

                InvitationModel inv = invOpt.get();
                Optional<EventModel> eventOpt = findEventById(inv.getEventId()); // Needed for revoke permission check

                // Permission checks
                if (("accepted".equals(newStatus) || "declined".equals(newStatus))) {
                    // Only the recipient of the invitation can accept or decline it.
                    if (!actingUserId.equals(inv.getAccountId())) {
                        _voidOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Only the recipient can accept or decline an invitation.")); return;
                    }
                } else if ("revoked".equals(newStatus)) {
                    // Only the event creator or an admin can revoke an invitation.
                    if (!eventOpt.isPresent() || (!actingUserId.equals(eventOpt.get().getCreatedBy()) && !isPrincipalAdmin(actingPrincipal))) {
                        _voidOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Only the event creator or an admin can revoke an invitation.")); return;
                    }
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Invalid status update: " + newStatus)); return;
                }

                inv.setInvitationStatus(newStatus);
                inv.setRespondedAt(new Date());

                if ("accepted".equals(newStatus)) {
                    // Add user to participants list if not already there (or update status if was e.g. "cancelled")
                    Optional<ParticipantModel> existingPart = simulatedParticipation.stream()
                            .filter(p -> inv.getEventId().equals(p.getEventId()) && inv.getAccountId().equals(p.getAccountId()))
                            .findFirst();
                    if (existingPart.isPresent()) {
                        existingPart.get().setStatus("attending");
                        existingPart.get().setEventRole("participant");
                    } else {
                        simulatedParticipation.add(new ParticipantModel(inv.getEventId(), inv.getAccountId(), "attending", "participant"));
                    }
                    fetchEventParticipants(inv.getEventId()); // Refresh participants list
                } else if ("declined".equals(newStatus) || "revoked".equals(newStatus)) {
                    // Remove user from participants if they were there due to a previous acceptance that is now declined/revoked
                    simulatedParticipation.removeIf(p -> inv.getEventId().equals(p.getEventId()) && inv.getAccountId().equals(p.getAccountId()));
                    fetchEventParticipants(inv.getEventId()); // Refresh participants list
                }

                _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                fetchInvitationsForUser(inv.getAccountId()); // Refresh invitations for the user whose invitation changed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Invitation update interrupted."));
            }
        });
    }


    // --- Media Methods ---
    public void fetchMediaForEvent(String eventId) {
        _eventMediaState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(250);
                List<MediaModel> eventMedia = simulatedMedia.stream()
                        .filter(media -> eventId.equals(media.getEventId()))
                        .collect(Collectors.toList());
                _eventMediaState.postValue(new ResultWrapper.Success<>(eventMedia));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _eventMediaState.postValue(new ResultWrapper.Error<>("Media fetch interrupted for event " + eventId));
            }
        });
    }


    public void uploadMedia(MediaModel mediaToUpload, String authTokenUploader) {
        _mediaUploadOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> uploaderPrincipalOpt = authRepository.validateTokenAndExtractPrincipal(authTokenUploader);
            if (!uploaderPrincipalOpt.isPresent()) {
                _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Unauthorized: Invalid token for upload.")); return;
            }
            Pair<String, String> uploaderPrincipal = uploaderPrincipalOpt.get();
            String uploaderUserId = uploaderPrincipal.first;

            try {
                Thread.sleep(1000); // Simulate upload time
                if (mediaToUpload == null || mediaToUpload.getEventId() == null) { _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Upload failed: Media or Event ID missing.")); return; }

                Optional<EventModel> eventOpt = findEventById(mediaToUpload.getEventId());
                if (!eventOpt.isPresent()) { _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Upload failed: Event not found.")); return; }

                // Permission: Uploader must be attending participant, event creator, or admin
                boolean isAttendingParticipant = simulatedParticipation.stream()
                        .anyMatch(p -> mediaToUpload.getEventId().equals(p.getEventId()) &&
                                uploaderUserId.equals(p.getAccountId()) &&
                                "attending".equals(p.getStatus()));
                boolean isEventCreator = uploaderUserId.equals(eventOpt.get().getCreatedBy());

                if (!isAttendingParticipant && !isEventCreator && !isPrincipalAdmin(uploaderPrincipal)) {
                    _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Only attending participants, the event creator, or an admin can upload media.")); return;
                }

                mediaToUpload.setAccountId(uploaderUserId); // Set uploader ID
                if (mediaToUpload.getMediaId() == null || mediaToUpload.getMediaId().isEmpty()) {
                    mediaToUpload.setMediaId("media_new_" + UUID.randomUUID().toString().substring(0, 8));
                }
                mediaToUpload.setUploadedAt(new Date());

                // Simulate generating a URL if one isn't provided (e.g. for a local file path)
                if (mediaToUpload.getMediaFileReference() == null || mediaToUpload.getMediaFileReference().isEmpty() || !mediaToUpload.getMediaFileReference().startsWith("http")) {
                    mediaToUpload.setMediaFileReference("https://via.placeholder.com/300x200/EFEFEF/000000?Text=Img-" + mediaToUpload.getMediaId().substring(0,Math.min(mediaToUpload.getMediaId().length(), 4)));
                }

                simulatedMedia.add(mediaToUpload);
                _mediaUploadOperationState.postValue(new ResultWrapper.Success<>(mediaToUpload));
                fetchMediaForEvent(mediaToUpload.getEventId()); // Refresh media list for the event
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Media upload interrupted."));
            }
        });
    }

    public void deleteMedia(String mediaId, String eventIdToRefreshContext, String authTokenActingUser) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            Optional<Pair<String, String>> actingPrincipalOpt = authRepository.validateTokenAndExtractPrincipal(authTokenActingUser);
            if (!actingPrincipalOpt.isPresent()) {
                _voidOperationState.postValue(new ResultWrapper.Error<>("Unauthorized.")); return;
            }
            Pair<String, String> actingPrincipal = actingPrincipalOpt.get();
            String actingUserId = actingPrincipal.first;

            try {
                Thread.sleep(500);
                Optional<MediaModel> mediaOpt = simulatedMedia.stream().filter(m -> mediaId.equals(m.getMediaId())).findFirst();
                if (!mediaOpt.isPresent()) { _voidOperationState.postValue(new ResultWrapper.Error<>("Delete failed: Media not found.")); return; }

                MediaModel media = mediaOpt.get();
                Optional<EventModel> eventOpt = findEventById(media.getEventId()); // Event context for creator check

                // Permission: Media uploader, event creator, or admin can delete
                boolean isUploader = actingUserId.equals(media.getAccountId());
                boolean isEventCreator = eventOpt.isPresent() && actingUserId.equals(eventOpt.get().getCreatedBy());

                if (!isUploader && !isEventCreator && !isPrincipalAdmin(actingPrincipal)) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Permission Denied: Only the uploader, event creator, or an admin can delete this media.")); return;
                }

                boolean removed = simulatedMedia.removeIf(m -> mediaId.equals(m.getMediaId()));
                if (removed) {
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    if (eventIdToRefreshContext != null) { // Refresh media for the event context
                        fetchMediaForEvent(eventIdToRefreshContext);
                    }
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Delete failed unexpectedly after finding media."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Media deletion interrupted."));
            }
        });
    }


    // --- Utility / Reset ---
    public void resetSingleEventOperationState() {
        _singleEventOperationState.postValue(new ResultWrapper.Idle<>());
    }
    public void resetVoidOperationState() {
        _voidOperationState.postValue(new ResultWrapper.Idle<>());
    }
    public void resetUserProfileOperationState() {
        _userProfileOperationState.postValue(new ResultWrapper.Idle<>());
    }
    public void resetMediaUploadOperationState() {
        _mediaUploadOperationState.postValue(new ResultWrapper.Idle<>());
    }

}