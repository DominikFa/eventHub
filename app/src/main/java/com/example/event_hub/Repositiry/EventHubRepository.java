// src/main/java/com/example/event_hub/Repositiry/EventHubRepository.java
package com.example.event_hub.Repositiry;

import androidx.lifecycle.MutableLiveData;

import com.example.event_hub.Model.request.EventCreationRequest;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.EventSummary;
import com.example.event_hub.Model.InvitationModel;
import com.example.event_hub.Model.request.LocationCreationRequest;
import com.example.event_hub.Model.LocationData;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.Model.NotificationModel;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.ParticipantModel;
import com.example.event_hub.Model.ParticipantStatus;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.request.SendInvitationRequest;
import com.example.event_hub.Model.request.UpdateProfileRequest;
import com.example.event_hub.Model.request.UpdateRoleRequest;
import com.example.event_hub.Model.request.UpdateStatusRequest;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.UserSummary;

import java.io.File;
import java.text.SimpleDateFormat; // Import SimpleDateFormat
import java.util.Date;
import java.util.List;
import java.util.Locale; // Import Locale
import java.util.TimeZone; // Import TimeZone
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody; // Explicitly import okhttp3.ResponseBody
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException; // Import IOException
import java.time.Instant;
// import java.util.Date; // Duplicate import, already present


public class EventHubRepository {

    private static volatile EventHubRepository INSTANCE;
    private final ApiService apiService;
    public final AuthRepository authRepository;

    // LiveData states
    public final MutableLiveData<ResultWrapper<PaginatedResponse<EventSummary>>> publicEventsState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<PaginatedResponse<EventSummary>>> allEventsState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<EventModel>> singleEventOperationState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<Void>> voidOperationState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<PaginatedResponse<ParticipantModel>>> eventParticipantsState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<PaginatedResponse<InvitationModel>>> userInvitationsState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<MediaModel>> mediaUploadOperationState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<List<MediaModel>>> eventMediaState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<PaginatedResponse<EventModel>>> attendedEventsState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<PaginatedResponse<EventModel>>> myCreatedEventsState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<PaginatedResponse<LocationData>>> allLocationsState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<LocationData>> singleLocationOperationState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<PaginatedResponse<NotificationModel>>> myNotificationsState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<NotificationModel>> singleNotificationOperationState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<UserModel>> userProfileOperationState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<UserSummary>> userSummaryState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<PaginatedResponse<UserSummary>>> allUserSummaryState = new MutableLiveData<>();

    // Corrected type: Should be UserSummary as per API, not UserModel
    public final MutableLiveData<ResultWrapper<PaginatedResponse<UserModel>>> allUserAccountsState = new MutableLiveData<>();
    public final MutableLiveData<ResultWrapper<ResponseBody>> fileDownloadState = new MutableLiveData<>(); // Using okhttp3.ResponseBody
    public final MutableLiveData<ResultWrapper<ParticipantStatus>> participantStatusState = new MutableLiveData<>();



    private EventHubRepository() {
        this.apiService = ApiClient.getApiService();
        this.authRepository = AuthRepository.getInstance();



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



    // --- Generic Callback ---
    private <T> Callback<T> createCallback(MutableLiveData<ResultWrapper<T>> liveData) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful()) {
                    liveData.postValue(new ResultWrapper.Success<>(response.body()));
                } else {
                    String errorMessage = "API Error: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            if (!errorBody.isEmpty()) {
                                errorMessage += " - " + errorBody;
                            }
                        } catch (IOException e) {
                            errorMessage += " - Could not read error body.";
                            e.printStackTrace(); // Log the stack trace for debugging
                        }
                    }
                    liveData.postValue(new ResultWrapper.Error<>(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                liveData.postValue(new ResultWrapper.Error<>(t.getMessage() != null ? t.getMessage() : "Network error."));
            }
        };
    }

    // --- User Profile & Account Methods ---
    public void fetchUserProfile(Long userId, String authToken) {
        userProfileOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.getUserProfile("Bearer " + authToken, userId).enqueue(createCallback(userProfileOperationState));
    }

    public void fetchUserSummary(Long userId) {
        userSummaryState.postValue(new ResultWrapper.Loading<>());
        apiService.getUserSummary(userId).enqueue(createCallback(userSummaryState));
    }

    public void fetchCurrentUserProfile(String authToken) {
        userProfileOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.getCurrentUserProfile("Bearer " + authToken).enqueue(createCallback(userProfileOperationState));
    }

    public void editProfile(UpdateProfileRequest request, String authToken) {
        userProfileOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.updateUserProfile("Bearer " + authToken, request).enqueue(createCallback(userProfileOperationState));
    }

    public void deleteUser(Long userId, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        Long currentUserId = authRepository.getCurrentUserIdSynchronous();
        if (userId.equals(currentUserId)) {
            apiService.deleteUser("Bearer " + authToken).enqueue(createCallback(voidOperationState));
        } else {
            apiService.deleteUserByAdmin("Bearer " + authToken, userId).enqueue(createCallback(voidOperationState));
        }
    }

    public void banUser(Long userId, String authToken) {
        userProfileOperationState.postValue(new ResultWrapper.Loading<>());
        UpdateStatusRequest request = new UpdateStatusRequest("banned");
        apiService.updateUserStatus("Bearer " + authToken, userId, request).enqueue(createCallback(userProfileOperationState));
    }

    public void changeUserRole(Long userId, String roleName, String authToken) {
        userProfileOperationState.postValue(new ResultWrapper.Loading<>());
        UpdateRoleRequest request = new UpdateRoleRequest(roleName);
        apiService.updateUserRole("Bearer " + authToken, userId, request).enqueue(createCallback(userProfileOperationState));
    }

    public void fetchAllUserSummaries(String authToken, int page, int size, String nameFilter, String loginFilter, List<String> sort) {
        allUserSummaryState.postValue(new ResultWrapper.Loading<>());
        apiService.getAllUserAccounts("Bearer " + authToken, page, size, nameFilter, loginFilter, sort).enqueue(createCallback(allUserSummaryState));
    }

    public void getProfileImage(Long userId) {
        fileDownloadState.postValue(new ResultWrapper.Loading<>());
        apiService.getProfileImage(userId).enqueue(createCallback(fileDownloadState));
    }

    // --- Event Methods ---
    public void fetchPublicEvents(int page, int size, String name, Date startDate, Date endDate, List<String> sort) {
        publicEventsState.postValue(new ResultWrapper.Loading<>());
        // Use the new date-only formatter for startDate and endDate parameters in the query
        String formattedStartDate = formatDateToISO8601(startDate);
        String formattedEndDate = formatDateToISO8601(endDate);

        apiService.getPublicEvents(page, size, name, formattedStartDate, formattedEndDate, sort).enqueue(createCallback(publicEventsState));
    }
    private String formatDateToISO8601(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().toString(); // This should be the ONLY line here
    }

    public void fetchAllEvents(String authToken, int page, int size) {
        allEventsState.postValue(new ResultWrapper.Loading<>());
        apiService.getAllEvents("Bearer " + authToken, page, size).enqueue(createCallback(allEventsState));
    }

    public void fetchEventDetails(Long eventId) {
        singleEventOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.getEventDetails(eventId).enqueue(createCallback(singleEventOperationState));
    }

    public void createEvent(EventCreationRequest request, String authToken) {
        singleEventOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.createEvent("Bearer " + authToken, request).enqueue(createCallback(singleEventOperationState));
    }

    public void updateEvent(Long eventId, EventCreationRequest request, String authToken) {
        singleEventOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.updateEvent("Bearer " + authToken, eventId, request).enqueue(createCallback(singleEventOperationState));
    }

    public void updateAnyEvent(Long eventId, EventCreationRequest request, String authToken) {
        singleEventOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.updateAnyEvent("Bearer " + authToken, eventId, request).enqueue(createCallback(singleEventOperationState));
    }

    public void fetchMyParticipatedEvents(String authToken, int page, int size, String name, Date startDate, Date endDate, List<String> sort) {
        attendedEventsState.postValue(new ResultWrapper.Loading<>());
        String formattedStartDate = formatDateToISO8601(startDate);
        String formattedEndDate = formatDateToISO8601(endDate);
        apiService.getMyParticipatedEvents("Bearer " + authToken, page, size, name, formattedStartDate, formattedEndDate, sort).enqueue(createCallback(attendedEventsState));
    }

    public void fetchMyCreatedEvents(String authToken, int page, int size, String name, Date startDate, Date endDate, List<String> sort) {
        myCreatedEventsState.postValue(new ResultWrapper.Loading<>());
        String formattedStartDate = formatDateToISO8601(startDate);
        String formattedEndDate = formatDateToISO8601(endDate);
        apiService.getMyCreatedEvents("Bearer " + authToken, page, size, name, formattedStartDate, formattedEndDate, sort).enqueue(createCallback(myCreatedEventsState));
    }

    public void deleteEvent(Long eventId, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.deleteEvent("Bearer " + authToken, eventId).enqueue(createCallback(voidOperationState));
    }

    public void deleteAnyEvent(Long eventId, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.deleteAnyEvent("Bearer " + authToken, eventId).enqueue(createCallback(voidOperationState));
    }

    // --- Participant Methods ---
    public void fetchEventParticipants(Long eventId, int page, int size) {
        eventParticipantsState.postValue(new ResultWrapper.Loading<>());
        String authToken = authRepository.getCurrentTokenSynchronous();
        if (authToken != null && !authToken.isEmpty()) {
            apiService.getEventParticipantsList("Bearer " + authToken, eventId, page, size).enqueue(createCallback(eventParticipantsState));
        } else {
            eventParticipantsState.postValue(new ResultWrapper.Error<>("Authentication required to fetch participants."));
        }
    }

    public void joinEvent(Long eventId, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.joinEvent("Bearer " + authToken, eventId).enqueue(createVoidCallbackWithRefresh(() -> fetchEventParticipants(eventId, 0, 50)));
    }

    public void leaveEvent(Long eventId, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.leaveEvent("Bearer " + authToken, eventId).enqueue(createVoidCallbackWithRefresh(() -> fetchEventParticipants(eventId, 0, 50)));
    }

    public void updateParticipantStatus(Long eventId, Long participantUserId, String status, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        UpdateStatusRequest request = new UpdateStatusRequest(status);
        apiService.updateParticipantStatus("Bearer " + authToken, eventId, participantUserId, request).enqueue(createVoidCallbackWithRefresh(() -> fetchEventParticipants(eventId, 0, 50)));
    }

    public void getMyParticipantStatus(Long eventId, String authToken) {
        participantStatusState.postValue(new ResultWrapper.Loading<>());
        apiService.getMyParticipantStatus("Bearer " + authToken, eventId).enqueue(createCallback(participantStatusState));
    }

    public void getUserParticipantStatus(Long eventId, Long userId, String authToken) {
        participantStatusState.postValue(new ResultWrapper.Loading<>());
        apiService.getUserParticipantStatus("Bearer " + authToken, eventId, userId).enqueue(createCallback(participantStatusState));
    }

    // --- Invitation Methods ---
    public void sendInvitation(Long eventId, Long recipientUserId, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        SendInvitationRequest request = new SendInvitationRequest(eventId, recipientUserId);

        apiService.sendInvitation("Bearer " + authToken, request).enqueue(new Callback<InvitationModel>() {
            @Override
            public void onResponse(Call<InvitationModel> call, Response<InvitationModel> response) {
                if (response.isSuccessful()) {
                    voidOperationState.postValue(new ResultWrapper.Success<>(null));
                } else {
                    String errorMessage = "API Error: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            if (!errorBody.isEmpty()) {
                                errorMessage += " - " + errorBody;
                            }
                        } catch (IOException e) {
                            errorMessage += " - Could not read error body.";
                            e.printStackTrace(); // Log the stack trace for debugging
                        }
                    }
                    voidOperationState.postValue(new ResultWrapper.Error<>(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<InvitationModel> call, Throwable t) {
                voidOperationState.postValue(new ResultWrapper.Error<>(t.getMessage() != null ? t.getMessage() : "Network error."));
            }
        });
    }

    public void fetchInvitationsForUser(String authToken, int page, int size) {
        userInvitationsState.postValue(new ResultWrapper.Loading<>());
        apiService.getMyInvitations("Bearer " + authToken, page, size).enqueue(createCallback(userInvitationsState));
    }

    public void updateInvitationStatus(Long invitationId, String status, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        Call<InvitationModel> call;
        String formattedAuthToken = "Bearer " + authToken;
        if ("ACCEPTED".equalsIgnoreCase(status)) call = apiService.acceptInvitation(formattedAuthToken, invitationId);
        else if ("DECLINED".equalsIgnoreCase(status)) call = apiService.declineInvitation(formattedAuthToken, invitationId);
        else call = apiService.revokeInvitation(formattedAuthToken, invitationId);
        call.enqueue(createVoidCallbackWithRefresh(() -> fetchInvitationsForUser(authToken, 0, 50)));
    }

    // --- Media Methods ---
    public void uploadEventGalleryImage(Long eventId, File file, String authToken) {
        mediaUploadOperationState.postValue(new ResultWrapper.Loading<>());
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        apiService.uploadEventGalleryImage("Bearer " + authToken, eventId, body).enqueue(createCallback(mediaUploadOperationState));
    }

    public void adminUploadEventGalleryImage(Long eventId, File file, String authToken) {
        mediaUploadOperationState.postValue(new ResultWrapper.Loading<>());
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        apiService.adminUploadEventGalleryImage("Bearer " + authToken, eventId, body).enqueue(createCallback(mediaUploadOperationState));
    }

    public void uploadEventLogo(Long eventId, File file, String authToken) {
        mediaUploadOperationState.postValue(new ResultWrapper.Loading<>());
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        apiService.uploadEventLogo("Bearer " + authToken, eventId, body).enqueue(createCallback(mediaUploadOperationState));
    }

    public void uploadEventSchedule(Long eventId, File file, String authToken) {
        mediaUploadOperationState.postValue(new ResultWrapper.Loading<>());
        RequestBody requestFile = RequestBody.create(MediaType.parse("application/pdf"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        apiService.uploadEventSchedule("Bearer " + authToken, eventId, body).enqueue(createCallback(mediaUploadOperationState));
    }

    public void downloadGalleryMedia(Long fileId) {
        fileDownloadState.postValue(new ResultWrapper.Loading<>());
        apiService.downloadGalleryMedia(fileId).enqueue(createCallback(fileDownloadState));
    }

    public void downloadScheduleMedia(Long fileId, String authToken) {
        fileDownloadState.postValue(new ResultWrapper.Loading<>());
        apiService.downloadScheduleMedia("Bearer " + authToken, fileId).enqueue(createCallback(fileDownloadState));
    }

    public void deleteGalleryMedia(Long fileId, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.deleteGalleryMedia("Bearer " + authToken, fileId).enqueue(createCallback(voidOperationState));
    }

    public void organizerDeleteGalleryMedia(Long eventId, Long fileId, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.organizerDeleteGalleryMedia("Bearer " + authToken, eventId, fileId).enqueue(createCallback(voidOperationState));
    }

    public void deleteAnyMedia(Long fileId, String authToken) {
        voidOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.deleteAnyMedia("Bearer " + authToken, fileId).enqueue(createCallback(voidOperationState));
    }

    // --- Location Methods ---
    public void createLocation(LocationCreationRequest request, String authToken) {
        singleLocationOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.createLocation("Bearer " + authToken, request).enqueue(createCallback(singleLocationOperationState));
    }

    public void getAllLocations(String authToken, int page, int size, String cityFilter, List<String> sort) {
        allLocationsState.postValue(new ResultWrapper.Loading<>());
        apiService.getAllLocations("Bearer " + authToken, page, size, cityFilter, sort).enqueue(createCallback(allLocationsState));
    }

    public void getLocationById(Long locationId) {
        singleLocationOperationState.postValue(new ResultWrapper.Loading<>());
        apiService.getLocationById(locationId).enqueue(createCallback(singleLocationOperationState));
    }

    // --- Notification Methods ---
    public void getMyNotifications(String authToken, int page, int size) {
        myNotificationsState.postValue(new ResultWrapper.Loading<>());
        apiService.getMyNotifications("Bearer " + authToken, page, size).enqueue(createCallback(myNotificationsState));
    }

    public void updateNotificationStatus(Long notificationId, String status, String authToken) {
        singleNotificationOperationState.postValue(new ResultWrapper.Loading<>());
        UpdateStatusRequest request = new UpdateStatusRequest(status);
        apiService.updateNotificationStatus("Bearer " + authToken, notificationId, request).enqueue(createCallback(singleNotificationOperationState));
    }

    // --- Helper for callbacks that need to refresh data ---
    private <T> Callback<T> createVoidCallbackWithRefresh(Runnable refresher) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful()) {
                    voidOperationState.postValue(new ResultWrapper.Success<>(null));
                    refresher.run();
                } else {
                    String errorMessage = "API Error: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            if (!errorBody.isEmpty()) {
                                errorMessage += " - " + errorBody;
                            }
                        } catch (IOException e) {
                            errorMessage += " - Could not read error body.";
                            e.printStackTrace(); // Log the stack trace for debugging
                        }
                    }
                    voidOperationState.postValue(new ResultWrapper.Error<>(errorMessage));
                }
            }
            @Override
            public void onFailure(Call<T> call, Throwable t) {
                voidOperationState.postValue(new ResultWrapper.Error<>(t.getMessage() != null ? t.getMessage() : "Network error."));
            }
        };
    }

    public void resetSingleEventOperationState() {
        singleEventOperationState.postValue(new ResultWrapper.Idle<>());
    }

    public void resetVoidOperationState() {
        voidOperationState.postValue(new ResultWrapper.Idle<>());
    }

}