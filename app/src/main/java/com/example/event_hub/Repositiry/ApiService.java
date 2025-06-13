// src/main/java/com/example/event_hub/Repositiry/ApiService.java
package com.example.event_hub.Repositiry;

import com.example.event_hub.Model.AuthResponse;
import com.example.event_hub.Model.request.ChangePasswordRequest;
import com.example.event_hub.Model.request.EventCreationRequest;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.EventSummary;
import com.example.event_hub.Model.InvitationModel;
import com.example.event_hub.Model.request.LocationCreationRequest;
import com.example.event_hub.Model.LocationData;
import com.example.event_hub.Model.LoginRequest;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.Model.NotificationModel;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.ParticipantModel;
import com.example.event_hub.Model.ParticipantStatus;
import com.example.event_hub.Model.RegisterRequest;
import com.example.event_hub.Model.request.SendInvitationRequest;
import com.example.event_hub.Model.request.UpdateProfileRequest;
import com.example.event_hub.Model.request.UpdateRoleRequest;
import com.example.event_hub.Model.request.UpdateStatusRequest;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.UserSummary;

import java.util.Date; // Keep Date import if still used for @Body or other non-query params
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- Authentication ---
    @POST("api/auth/login")
    Call<AuthResponse> loginUser(@Body LoginRequest loginRequest);

    @POST("api/auth/register")
    Call<UserModel> registerUser(@Body RegisterRequest registerRequest);

    // --- User Profile & Account ---
    @GET("api/account/me")
    Call<UserModel> getCurrentUserProfile(@Header("Authorization") String authToken);

    @GET("api/accounts/{id}")
    Call<UserModel> getUserProfile(@Header("Authorization") String authToken, @Path("id") Long userId);

    @GET("api/accounts/{id}/summary")
    Call<UserSummary> getUserSummary(@Path("id") Long userId);

    @PUT("api/account/profile")
    Call<UserModel> updateUserProfile(@Header("Authorization") String authToken, @Body UpdateProfileRequest updateProfileRequest);

    @PUT("api/account/password")
    Call<String> changePassword(@Header("Authorization") String authToken, @Body ChangePasswordRequest changePasswordRequest);

    @Multipart
    @POST("api/account/profile-image")
    Call<String> uploadProfileImage(@Header("Authorization") String authToken, @Part MultipartBody.Part file);

    @GET("api/users/{id}/profile-image")
    Call<ResponseBody> getProfileImage(@Path("id") Long userId);

    @DELETE("api/account")
    Call<Void> deleteUser(@Header("Authorization") String authToken);

    @GET("api/accounts/summary/all")
    Call<PaginatedResponse<UserSummary>> getAllUserAccounts(@Header("Authorization") String authToken,
                                                          @Query("page") int page,
                                                          @Query("size") int size,
                                                          @Query("name") String name,
                                                          @Query("login") String login,
                                                          @Query("sort") List<String> sort);

    @PATCH("api/admin/accounts/{id}/status")
    Call<UserModel> updateUserStatus(@Header("Authorization") String authToken, @Path("id") Long userId, @Body UpdateStatusRequest statusRequest);

    @PATCH("api/admin/accounts/{id}/role")
    Call<UserModel> updateUserRole(@Header("Authorization") String authToken, @Path("id") Long userId, @Body UpdateRoleRequest roleRequest);

    @DELETE("api/admin/accounts/{id}")
    Call<Void> deleteUserByAdmin(@Header("Authorization") String authToken, @Path("id") Long userId);

    // --- Events ---
    @POST("api/events")
    Call<EventModel> createEvent(@Header("Authorization") String authToken, @Body EventCreationRequest eventCreationRequest);

    // Changed Instant parameters to String for query parameters
    @GET("api/events/public")
    Call<PaginatedResponse<EventSummary>> getPublicEvents(@Query("page") int page,
                                                          @Query("size") int size,
                                                          @Query("name") String name,
                                                          @Query("startDate") String startDate, // Changed to String
                                                          @Query("endDate") String endDate,   // Changed to String
                                                          @Query("sort") List<String> sort);

    @GET("api/events/all")
    Call<PaginatedResponse<EventSummary>> getAllEvents(@Header("Authorization") String authToken,
                                                       @Query("page") int page,
                                                       @Query("size") int size);

    @GET("api/events/{id}")
    Call<EventModel> getEventDetails(@Path("id") Long eventId);

    @PUT("api/events/{id}")
    Call<EventModel> updateEvent(@Header("Authorization") String authToken, @Path("id") Long eventId, @Body EventCreationRequest eventCreationRequest);

    @DELETE("api/events/{id}")
    Call<Void> deleteEvent(@Header("Authorization") String authToken, @Path("id") Long eventId);

    // Changed Instant parameters to String for query parameters
    @GET("api/events/my-participated")
    Call<PaginatedResponse<EventModel>> getMyParticipatedEvents(@Header("Authorization") String authToken,
                                                                @Query("page") int page,
                                                                @Query("size") int size,
                                                                @Query("name") String name,
                                                                @Query("startDate") String startDate, // Changed to String
                                                                @Query("endDate") String endDate,   // Changed to String
                                                                @Query("sort") List<String> sort);

    // Changed Instant parameters to String for query parameters
    @GET("api/events/my-created")
    Call<PaginatedResponse<EventModel>> getMyCreatedEvents(@Header("Authorization") String authToken,
                                                           @Query("page") int page,
                                                           @Query("size") int size,
                                                           @Query("name") String name,
                                                           @Query("startDate") String startDate, // Changed to String
                                                           @Query("endDate") String endDate,   // Changed to String
                                                           @Query("sort") List<String> sort);

    @PUT("api/admin/events/{id}")
    Call<EventModel> updateAnyEvent(@Header("Authorization") String authToken, @Path("id") Long eventId, @Body EventCreationRequest eventCreationRequest);

    @DELETE("api/admin/events/{id}")
    Call<Void> deleteAnyEvent(@Header("Authorization") String authToken, @Path("id") Long eventId);

    // --- Participants ---
    @POST("api/events/{eventId}/participants")
    Call<ParticipantModel> joinEvent(@Header("Authorization") String authToken, @Path("eventId") Long eventId);

    @GET("api/events/{eventId}/participants/me")
    Call<ParticipantStatus> getMyParticipantStatus(@Header("Authorization") String authToken, @Path("eventId") Long eventId);

    @GET("api/events/{eventId}/participants/{userId}")
    Call<ParticipantStatus> getUserParticipantStatus(@Header("Authorization") String authToken, @Path("eventId") Long eventId, @Path("userId") Long userId);

    @GET("api/events/{eventId}/participants")
    Call<PaginatedResponse<ParticipantModel>> getEventParticipantsList(@Header("Authorization") String authToken, @Path("eventId") Long eventId, @Query("page") int page, @Query("size") int size);

    @DELETE("api/events/{eventId}/participants/me")
    Call<Void> leaveEvent(@Header("Authorization") String authToken, @Path("eventId") Long eventId);

    @PATCH("api/events/{eventId}/participants/{userId}/status")
    Call<ParticipantModel> updateParticipantStatus(@Header("Authorization") String authToken, @Path("eventId") Long eventId, @Path("userId") Long userId, @Body UpdateStatusRequest statusRequest);

    // --- Invitations ---
    @POST("api/invitations")
    Call<InvitationModel> sendInvitation(@Header("Authorization") String authToken, @Body SendInvitationRequest invitationRequest);

    @GET("api/invitations/my")
    Call<PaginatedResponse<InvitationModel>> getMyInvitations(@Header("Authorization") String authToken, @Query("page") int page, @Query("size") int size);

    @POST("api/invitations/{invitationId}/accept")
    Call<InvitationModel> acceptInvitation(@Header("Authorization") String authToken, @Path("invitationId") Long invitationId);

    @POST("api/invitations/{invitationId}/decline")
    Call<InvitationModel> declineInvitation(@Header("Authorization") String authToken, @Path("invitationId") Long invitationId);

    @POST("api/invitations/{invitationId}/revoke")
    Call<InvitationModel> revokeInvitation(@Header("Authorization") String authToken, @Path("invitationId") Long invitationId);

    // --- Media ---
    @Multipart
    @POST("api/events/{id}/media/gallery")
    Call<MediaModel> uploadEventGalleryImage(@Header("Authorization") String authToken, @Path("id") Long eventId, @Part MultipartBody.Part file);

    @Multipart
    @POST("api/admin/events/{id}/media/gallery")
    Call<MediaModel> adminUploadEventGalleryImage(@Header("Authorization") String authToken, @Path("id") Long eventId, @Part MultipartBody.Part file);

    @Multipart
    @POST("api/events/{id}/media/logo")
    Call<MediaModel> uploadEventLogo(@Header("Authorization") String authToken, @Path("id") Long eventId, @Part MultipartBody.Part file);

    @Multipart
    @POST("api/events/{id}/media/schedule")
    Call<MediaModel> uploadEventSchedule(@Header("Authorization") String authToken, @Path("id") Long eventId, @Part MultipartBody.Part file);

    @GET("api/media/gallery/{fileId}")
    Call<ResponseBody> downloadGalleryMedia(@Path("fileId") Long fileId);

    @GET("api/media/schedule/{fileId}")
    Call<ResponseBody> downloadScheduleMedia(@Header("Authorization") String authToken, @Path("fileId") Long fileId);

    @DELETE("api/media/gallery/{fileId}")
    Call<Void> deleteGalleryMedia(@Header("Authorization") String authToken, @Path("fileId") Long fileId);

    @DELETE("api/events/{id}/media/gallery/{fileId}")
    Call<Void> organizerDeleteGalleryMedia(@Header("Authorization") String authToken, @Path("id") Long eventId, @Path("fileId") Long fileId);

    @DELETE("api/admin/media/{fileId}")
    Call<Void> deleteAnyMedia(@Header("Authorization") String authToken, @Path("fileId") Long fileId);

    // --- Location ---
    @POST("api/locations")
    Call<LocationData> createLocation(@Header("Authorization") String authToken, @Body LocationCreationRequest location);

    @GET("api/locations")
    Call<PaginatedResponse<LocationData>> getAllLocations(@Header("Authorization") String authToken,
                                                          @Query("page") int page,
                                                          @Query("size") int size,
                                                          @Query("city") String city,
                                                          @Query("sort") List<String> sort);

    @GET("api/locations/{locationId}")
    Call<LocationData> getLocationById(@Path("locationId") Long locationId);

    // --- Notification ---
    @GET("api/notifications")
    Call<PaginatedResponse<NotificationModel>> getMyNotifications(@Header("Authorization") String authToken, @Query("page") int page, @Query("size") int size);

    @PATCH("api/notifications/{id}/status")
    Call<NotificationModel> updateNotificationStatus(@Header("Authorization") String authToken, @Path("id") Long notificationId, @Body UpdateStatusRequest statusRequest);
}
