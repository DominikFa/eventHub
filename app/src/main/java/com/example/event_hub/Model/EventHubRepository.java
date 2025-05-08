package com.example.event_hub.Model;

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
import java.util.stream.Collectors;

public class EventHubRepository {

    private static volatile EventHubRepository INSTANCE;
    private final ExecutorService executorService;

    // --- Placeholder Data Stores ---
    private final List<UserModel> simulatedUsers = new ArrayList<>();
    private final List<EventModel> simulatedEvents = new ArrayList<>();
    private final List<ParticipantModel> simulatedParticipation = new ArrayList<>();
    private final List<InvitationModel> simulatedInvitations = new ArrayList<>();
    private final List<MediaModel> simulatedMedia = new ArrayList<>();

    // --- Central LiveData for entity lists and states ---
    private final MutableLiveData<ResultWrapper<List<EventModel>>> _publicEventsState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<List<EventModel>>> publicEventsState = _publicEventsState;

    private final MutableLiveData<ResultWrapper<List<InvitationModel>>> _userInvitationsState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<List<InvitationModel>>> userInvitationsState = _userInvitationsState;

    private final MutableLiveData<ResultWrapper<List<UserModel>>> _eventParticipantsState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<List<UserModel>>> eventParticipantsState = _eventParticipantsState;

    private final MutableLiveData<ResultWrapper<List<MediaModel>>> _eventMediaState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<List<MediaModel>>> eventMediaState = _eventMediaState;

    // --- LiveData for specific operation outcomes ---
    private final MutableLiveData<ResultWrapper<UserModel>> _userProfileOperationState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<UserModel>> userProfileOperationState = _userProfileOperationState; // For fetch, edit (returns updated user)

    private final MutableLiveData<ResultWrapper<EventModel>> _singleEventOperationState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<EventModel>> singleEventOperationState = _singleEventOperationState; // For create, fetch single

    private final MutableLiveData<ResultWrapper<Void>> _voidOperationState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<Void>> voidOperationState = _voidOperationState; // For join, delete, ban, simple updates

    private final MutableLiveData<ResultWrapper<MediaModel>> _mediaUploadOperationState = new MutableLiveData<>(new ResultWrapper.Idle<>());
    public LiveData<ResultWrapper<MediaModel>> mediaUploadOperationState = _mediaUploadOperationState;


    private EventHubRepository() {
        executorService = Executors.newFixedThreadPool(2);
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

    private void initializePlaceholderData() {
        simulatedUsers.clear();
        simulatedEvents.clear();
        simulatedParticipation.clear();
        simulatedInvitations.clear();
        simulatedMedia.clear();

        // --- Users ---
        UserDetails details1 = new UserDetails("Test User FullName", "Bio for the main test user.", "test_avatar.png");
        simulatedUsers.add(new UserModel("user_jwt_123", "test@example.com", "test@example.com", "user", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10)), "active", details1));
        UserDetails details2 = new UserDetails("Another User", "Organizer of the Tech Conference.", "another_avatar.png");
        simulatedUsers.add(new UserModel("another_user_456", "another_user", "another@test.com", "organizer", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)), "active", details2));
        UserDetails details3 = new UserDetails("Banned User", "This user is currently banned.", "banned_avatar.png");
        simulatedUsers.add(new UserModel("user_banned_789", "banned_user", "banned@test.com", "user", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(20)), "banned", details3));
        UserDetails details4 = new UserDetails("Artist Creator", "Leads the art workshop.", "artist_avatar.png");
        simulatedUsers.add(new UserModel("user_artist_03", "artist_user", "artist@test.com", "user", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(15)), "active", details4));

        // --- Events ---
        long currentTime = System.currentTimeMillis();
        simulatedEvents.add(new EventModel("event101", "Tech Conference 2025", "Annual technology conference with guest speakers.", "Convention Center Hall A", new Date(currentTime + TimeUnit.DAYS.toMillis(30)), new Date(currentTime + TimeUnit.DAYS.toMillis(32)), 500, "another_user_456"));
        simulatedEvents.add(new EventModel("event103", "Art Workshop", "Hands-on workshop for aspiring artists.", "Community Art Studio", new Date(currentTime + TimeUnit.DAYS.toMillis(7)), new Date(currentTime + TimeUnit.DAYS.toMillis(7) + TimeUnit.HOURS.toMillis(3)), 25, "user_artist_03"));
        simulatedEvents.add(new EventModel("event_past_009", "Past Coding Bootcamp", "Intensive coding bootcamp.", "Online", new Date(currentTime - TimeUnit.DAYS.toMillis(90)), new Date(currentTime - TimeUnit.DAYS.toMillis(60)), 30, "another_user_456"));
        simulatedEvents.add(new EventModel("event_declined_010", "Declined Invitation Event", "Event that was declined by user_jwt_123.", "Venue X", new Date(currentTime + TimeUnit.DAYS.toMillis(5)), new Date(currentTime + TimeUnit.DAYS.toMillis(5)), 10, "another_user_456"));
        simulatedEvents.add(new EventModel("event_expired_011", "Expired Invitation Event", "Event with an expired invitation for user_jwt_123.", "Venue Y", new Date(currentTime + TimeUnit.DAYS.toMillis(1)), new Date(currentTime + TimeUnit.DAYS.toMillis(1)), 50, "another_user_456"));
        _publicEventsState.postValue(new ResultWrapper.Success<>(new ArrayList<>(simulatedEvents)));


        // --- Participation ---
        simulatedParticipation.add(new ParticipantModel("event101", "user_jwt_123", "attending", "participant"));
        simulatedParticipation.add(new ParticipantModel("event101", "another_user_456", "attending", "organizer"));
        simulatedParticipation.add(new ParticipantModel("event101", "user_banned_789", "banned", "participant"));
        simulatedParticipation.add(new ParticipantModel("event103", "user_jwt_123", "attending", "participant"));
        simulatedParticipation.add(new ParticipantModel("event103", "user_artist_03", "attending", "organizer"));
        simulatedParticipation.add(new ParticipantModel("event_past_009", "user_jwt_123", "attending", "participant"));

        // --- Invitations ---
        simulatedInvitations.add(new InvitationModel("inv_001", "event101", "user_jwt_123", "sent", new Date(currentTime - TimeUnit.DAYS.toMillis(1)), null));
        simulatedInvitations.add(new InvitationModel("inv_002", "event103", "user_jwt_123", "sent", new Date(currentTime - TimeUnit.HOURS.toMillis(5)), null));
        simulatedInvitations.add(new InvitationModel("inv_003", "event_past_009", "user_jwt_123", "accepted", new Date(currentTime - TimeUnit.DAYS.toMillis(60)), new Date(currentTime - TimeUnit.DAYS.toMillis(59))));
        simulatedInvitations.add(new InvitationModel("inv_004", "event_declined_010", "user_jwt_123", "declined", new Date(currentTime - TimeUnit.DAYS.toMillis(2)), new Date(currentTime - TimeUnit.DAYS.toMillis(1))));
        simulatedInvitations.add(new InvitationModel("inv_005", "event_expired_011", "user_jwt_123", "expired", new Date(currentTime - TimeUnit.DAYS.toMillis(10)), null));

        // --- Media ---
        simulatedMedia.add(new MediaModel("media_001", "event101", "another_user_456", "simulated/path/event101_logo.png", "image/png", new Date(), "logo", "event101_logo.png", "Official Tech Conference Logo"));
        simulatedMedia.add(new MediaModel("media_002", "event103", "user_artist_03", "simulated/path/workshop_photo1.jpg", "image/jpeg", new Date(), "gallery", "workshop_photo1.jpg", "Photo from the art workshop"));
        simulatedMedia.add(new MediaModel("media_003", "event101", "user_jwt_123", "simulated/path/tech_pic.jpg", "image/jpeg", new Date(currentTime - TimeUnit.HOURS.toMillis(2)), "gallery", "tech_pic.jpg", "Picture taken at the conference"));

        System.out.println("EventHubRepository: Placeholder data initialized.");
    }

    // --- Profile Methods ---
    public void fetchUserProfile(String userId) {
        _userProfileOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(800);
                Optional<UserModel> userOpt = simulatedUsers.stream()
                        .filter(u -> userId.equals(u.getUserId()))
                        .findFirst();
                if (userOpt.isPresent()) {
                    _userProfileOperationState.postValue(new ResultWrapper.Success<>(userOpt.get()));
                } else {
                    _userProfileOperationState.postValue(new ResultWrapper.Error<>("Profile not found for user ID: " + userId));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _userProfileOperationState.postValue(new ResultWrapper.Error<>("Profile fetch interrupted."));
            }
        });
    }

    public void editProfile(String userId, UserDetails updatedDetails) {
        _userProfileOperationState.postValue(new ResultWrapper.Loading<>()); // Indicate loading for the profile itself
        executorService.submit(() -> {
            try {
                Thread.sleep(900);
                Optional<UserModel> userOpt = simulatedUsers.stream()
                        .filter(u -> userId.equals(u.getUserId()))
                        .findFirst();

                if (userOpt.isPresent()) {
                    UserModel user = userOpt.get();
                    user.setUserDetails(updatedDetails); // Update the UserDetails within the UserModel
                    // Simulate saving the whole UserModel or just the UserDetails part
                    // For consistency, we post the updated UserModel
                    _userProfileOperationState.postValue(new ResultWrapper.Success<>(user));
                    System.out.println("EventHubRepository: Profile updated for user " + userId);
                } else {
                    _userProfileOperationState.postValue(new ResultWrapper.Error<>("Profile update failed: User not found."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _userProfileOperationState.postValue(new ResultWrapper.Error<>("Profile update interrupted."));
            }
        });
    }

    public void deleteUser(String userId) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(1500);
                boolean removedUser = simulatedUsers.removeIf(u -> userId.equals(u.getUserId()));
                if (removedUser) {
                    simulatedParticipation.removeIf(p -> userId.equals(p.getAccountId()));
                    simulatedInvitations.removeIf(i -> userId.equals(i.getAccountId()));
                    simulatedMedia.removeIf(m -> userId.equals(m.getAccountId()));
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null)); // Success
                    System.out.println("EventHubRepository: User " + userId + " deleted.");
                    // Potentially refresh any lists that might be affected if they don't auto-update
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("User deletion failed: User not found."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("User deletion interrupted."));
            }
        });
    }

    public void banUser(String userId) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(700);
                Optional<UserModel> userOpt = simulatedUsers.stream()
                        .filter(u -> userId.equals(u.getUserId()))
                        .findFirst();
                if (userOpt.isPresent()) {
                    UserModel user = userOpt.get();
                    user.setStatus("banned");
                    // Also update participant status for this user in all events to 'banned'
                    simulatedParticipation.forEach(p -> {
                        if (userId.equals(p.getAccountId())) {
                            p.setStatus("banned");
                        }
                    });
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null)); // Success
                    fetchUserProfile(userId); // Refresh the user profile state
                    System.out.println("EventHubRepository: User " + userId + " banned.");
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Ban user failed: User not found."));
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
                Thread.sleep(1000);
                _publicEventsState.postValue(new ResultWrapper.Success<>(new ArrayList<>(simulatedEvents)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _publicEventsState.postValue(new ResultWrapper.Error<>("Public event fetch interrupted."));
            }
        });
    }

    public LiveData<ResultWrapper<List<EventModel>>> fetchAttendedEvents(String userId) {
        MutableLiveData<ResultWrapper<List<EventModel>>> specificUserAttendedEvents = new MutableLiveData<>(new ResultWrapper.Idle<>());
        specificUserAttendedEvents.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(900);
                List<String> attendedEventIds = simulatedParticipation.stream()
                        .filter(p -> userId.equals(p.getAccountId()) && "attending".equals(p.getStatus()))
                        .map(ParticipantModel::getEventId)
                        .distinct()
                        .collect(Collectors.toList());
                List<EventModel> attendedEvents = simulatedEvents.stream()
                        .filter(e -> attendedEventIds.contains(e.getId()))
                        .collect(Collectors.toList());
                specificUserAttendedEvents.postValue(new ResultWrapper.Success<>(attendedEvents));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                specificUserAttendedEvents.postValue(new ResultWrapper.Error<>("Attended event fetch interrupted."));
            }
        });
        return specificUserAttendedEvents;
    }

    public void fetchEventDetails(String eventId) {
        _singleEventOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(700);
                Optional<EventModel> eventOpt = simulatedEvents.stream()
                        .filter(e -> eventId.equals(e.getId()))
                        .findFirst();
                if(eventOpt.isPresent()) {
                    _singleEventOperationState.postValue(new ResultWrapper.Success<>(eventOpt.get()));
                } else {
                    _singleEventOperationState.postValue(new ResultWrapper.Error<>("Event not found with ID: " + eventId));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _singleEventOperationState.postValue(new ResultWrapper.Error<>("Fetch event details interrupted."));
            }
        });
    }


    public void joinPublicEvent(String eventId, String userId) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(600);
                Optional<EventModel> eventOpt = simulatedEvents.stream().filter(e -> eventId.equals(e.getId())).findFirst();
                Optional<UserModel> userOpt = simulatedUsers.stream().filter(u -> userId.equals(u.getUserId())).findFirst();

                if (!eventOpt.isPresent()) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Join failed: Event not found."));
                    return;
                }
                if (!userOpt.isPresent()) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Join failed: User not found."));
                    return;
                }
                if (!"active".equals(userOpt.get().getStatus())) {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Join failed: User account is not active."));
                    return;
                }

                Optional<ParticipantModel> existingParticipation = simulatedParticipation.stream()
                        .filter(p -> eventId.equals(p.getEventId()) && userId.equals(p.getAccountId()))
                        .findFirst();

                if (existingParticipation.isPresent()) {
                    if ("banned".equals(existingParticipation.get().getStatus())) {
                        _voidOperationState.postValue(new ResultWrapper.Error<>("Cannot join: User is banned from this event."));
                    } else if ("attending".equals(existingParticipation.get().getStatus())) {
                        _voidOperationState.postValue(new ResultWrapper.Error<>("Already attending this event."));
                    } else { // e.g. cancelled, can re-join by setting to attending
                        existingParticipation.get().setStatus("attending");
                        existingParticipation.get().setEventRole("participant"); // Reset role
                        _voidOperationState.postValue(new ResultWrapper.Success<>(null)); // Rejoined
                        System.out.println("EventHubRepository: User " + userId + " re-joined event " + eventId);
                    }
                } else {
                    simulatedParticipation.add(new ParticipantModel(eventId, userId, "attending", "participant"));
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    System.out.println("EventHubRepository: User " + userId + " joined event " + eventId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Join event interrupted."));
            }
        });
    }

    public void createEvent(EventModel eventToCreate) {
        _singleEventOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(1300);
                if (eventToCreate == null || eventToCreate.getCreatedBy() == null || eventToCreate.getCreatedBy().isEmpty()) {
                    _singleEventOperationState.postValue(new ResultWrapper.Error<>("Create event failed: Creator ID missing."));
                    return;
                }
                if (eventToCreate.getTitle() == null || eventToCreate.getTitle().trim().isEmpty()) {
                    _singleEventOperationState.postValue(new ResultWrapper.Error<>("Create event failed: Title is required."));
                    return;
                }
                if (eventToCreate.getId() == null || eventToCreate.getId().isEmpty()) { // Ensure ID is set
                    eventToCreate.setId("event_new_" + UUID.randomUUID().toString().substring(0, 8));
                }

                simulatedEvents.add(eventToCreate);
                // Add organizer as participant
                simulatedParticipation.removeIf(p -> p.getEventId().equals(eventToCreate.getId()) && p.getAccountId().equals(eventToCreate.getCreatedBy())); // Remove if any prior, then add
                simulatedParticipation.add(new ParticipantModel(eventToCreate.getId(), eventToCreate.getCreatedBy(), "attending", "organizer"));

                _singleEventOperationState.postValue(new ResultWrapper.Success<>(eventToCreate));
                System.out.println("EventHubRepository: Created event " + eventToCreate.getId());
                fetchPublicEvents(); // Refresh the public list
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _singleEventOperationState.postValue(new ResultWrapper.Error<>("Create event interrupted."));
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
                        .filter(p -> eventId.equals(p.getEventId()) && !"banned".equals(p.getStatus())) // Exclude banned from active list display maybe?
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

    public void deleteParticipant(String eventId, String participantAccountId) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(500);
                boolean removed = simulatedParticipation.removeIf(p ->
                        eventId.equals(p.getEventId()) && participantAccountId.equals(p.getAccountId()));

                if (removed) {
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    System.out.println("EventHubRepository: Removed participant " + participantAccountId + " from event " + eventId);
                    fetchEventParticipants(eventId); // Refresh participant list for the event
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Remove participant failed: Not found."));
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
                Thread.sleep(650);
                // Filter out invitations for events that no longer exist (optional cleanup)
                List<String> existingEventIds = simulatedEvents.stream().map(EventModel::getId).collect(Collectors.toList());
                simulatedInvitations.removeIf(inv -> !existingEventIds.contains(inv.getEventId()));

                List<InvitationModel> userInvitations = simulatedInvitations.stream()
                        .filter(inv -> accountId.equals(inv.getAccountId()))
                        .collect(Collectors.toList());
                _userInvitationsState.postValue(new ResultWrapper.Success<>(userInvitations));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _userInvitationsState.postValue(new ResultWrapper.Error<>("Invitation fetch interrupted."));
            }
        });
    }

    public void updateInvitationStatus(String invitationId, String newStatus, String accountIdToRefresh) {
        _voidOperationState.postValue(new ResultWrapper.Loading<>()); // Use void for general success/fail of this action
        executorService.submit(() -> {
            try {
                Thread.sleep(400);
                Optional<InvitationModel> invOpt = simulatedInvitations.stream()
                        .filter(inv -> inv.getInvitationId().equals(invitationId))
                        .findFirst();

                if (invOpt.isPresent()) {
                    InvitationModel inv = invOpt.get();
                    String currentStatus = inv.getInvitationStatus();

                    if (("revoked".equals(currentStatus) || "expired".equals(currentStatus)) && !"sent".equals(newStatus)) { // allow re-sending a revoked/expired?
                        _voidOperationState.postValue(new ResultWrapper.Error<>("Cannot update invitation: Already " + currentStatus));
                        return;
                    }
                    if (currentStatus.equals(newStatus)) {
                        _voidOperationState.postValue(new ResultWrapper.Error<>("Invitation status already set to " + newStatus));
                        return;
                    }

                    inv.setInvitationStatus(newStatus);
                    if ("accepted".equals(newStatus) || "declined".equals(newStatus) || "revoked".equals(newStatus)) { // Revoked by user action could be a response
                        inv.setRespondedAt(new Date());
                    } else { // e.g. "sent" status might clear respondedAt
                        inv.setRespondedAt(null);
                    }

                    if ("accepted".equals(newStatus)) {
                        // Add or update participation record
                        Optional<ParticipantModel> participantOpt = simulatedParticipation.stream()
                                .filter(p -> p.getEventId().equals(inv.getEventId()) && p.getAccountId().equals(inv.getAccountId()))
                                .findFirst();
                        if (participantOpt.isPresent()) {
                            participantOpt.get().setStatus("attending"); // Ensure they are attending
                            participantOpt.get().setEventRole("participant"); // Ensure correct role
                        } else {
                            simulatedParticipation.add(new ParticipantModel(inv.getEventId(), inv.getAccountId(), "attending", "participant"));
                        }
                        System.out.println("EventHubRepository: Added/Updated participant " + inv.getAccountId() + " for event " + inv.getEventId());
                    } else if ("declined".equals(newStatus) || "revoked".equals(newStatus) || "expired".equals(newStatus)) { // "expired" handled by cron usually
                        // If user declines/revokes, set participation status to cancelled
                        simulatedParticipation.stream()
                                .filter(p -> inv.getEventId().equals(p.getEventId()) && inv.getAccountId().equals(p.getAccountId()))
                                .findFirst().ifPresent(p -> p.setStatus("cancelled"));
                    }

                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    System.out.println("EventHubRepository: Updated invitation " + invitationId + " to status " + newStatus);
                    fetchInvitationsForUser(accountIdToRefresh); // Refresh the user's invitation list
                    if ("accepted".equals(newStatus) || "declined".equals(newStatus)) { // If status change affects participation list, refresh it.
                        fetchEventParticipants(inv.getEventId());
                    }
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Invitation status update failed: Not found."));
                }
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
                Thread.sleep(550);
                List<MediaModel> eventMedia = simulatedMedia.stream()
                        .filter(m -> eventId.equals(m.getEventId()))
                        .collect(Collectors.toList());
                _eventMediaState.postValue(new ResultWrapper.Success<>(eventMedia));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _eventMediaState.postValue(new ResultWrapper.Error<>("Media fetch interrupted."));
            }
        });
    }

    public void uploadMedia(MediaModel mediaToUpload) {
        _mediaUploadOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(2000);
                if (mediaToUpload == null || mediaToUpload.getEventId() == null || mediaToUpload.getMediaType() == null || mediaToUpload.getUsage() == null) {
                    _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Media upload failed: Missing required fields."));
                    return;
                }
                if (mediaToUpload.getMediaId() == null || mediaToUpload.getMediaId().isEmpty()) {
                    mediaToUpload.setMediaId("media_new_" + UUID.randomUUID().toString().substring(0, 8));
                }
                mediaToUpload.setUploadedAt(new Date());
                if (mediaToUpload.getMediaFileReference() == null || mediaToUpload.getMediaFileReference().isEmpty()) {
                    String simulatedFileName = (mediaToUpload.getFileName() != null ? mediaToUpload.getFileName() : "uploaded_file_" + mediaToUpload.getMediaId());
                    mediaToUpload.setMediaFileReference("simulated/storage/path/" + simulatedFileName);
                }

                simulatedMedia.add(mediaToUpload);
                _mediaUploadOperationState.postValue(new ResultWrapper.Success<>(mediaToUpload));
                System.out.println("EventHubRepository: Media uploaded: " + mediaToUpload.getMediaId());
                fetchMediaForEvent(mediaToUpload.getEventId()); // Refresh media list for the event
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Media upload interrupted."));
            }
        });
    }

    public void deleteMedia(String mediaId, String eventIdToRefresh) { // Added eventIdToRefresh
        _voidOperationState.postValue(new ResultWrapper.Loading<>());
        executorService.submit(() -> {
            try {
                Thread.sleep(450);
                boolean removed = simulatedMedia.removeIf(m -> mediaId.equals(m.getMediaId()));
                if (removed) {
                    _voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    System.out.println("EventHubRepository: Media deleted: " + mediaId);
                    if (eventIdToRefresh != null) {
                        fetchMediaForEvent(eventIdToRefresh); // Refresh media list
                    }
                } else {
                    _voidOperationState.postValue(new ResultWrapper.Error<>("Delete media failed: Not found."));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _voidOperationState.postValue(new ResultWrapper.Error<>("Media deletion interrupted."));
            }
        });
    }
}