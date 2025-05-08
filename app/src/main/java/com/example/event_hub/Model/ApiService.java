package com.example.event_hub.Model; // Updated package name

// No longer need to import models if ApiService is in the same package
// import com.example.event_hub.Model.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
// If using Multipart for file uploads (like media):
// import okhttp3.MultipartBody;
// import okhttp3.RequestBody;
// import retrofit2.http.Multipart;
// import retrofit2.http.Part;



/**
 * Retrofit interface defining the API endpoints for the Event Hub application.
 * Each method corresponds to an API call.
 * Now located in the Model package.
 *
 * Note: Replace placeholder paths ("/api/..."), response types (Void, AuthResponse),
 * and parameter names with your actual API specification once available.
 * The 'Authorization' header is included for endpoints requiring authentication (JWT).
 */
public interface ApiService {

    // --- Authentication ---

    /**
     * Sends login credentials to the server.
     * Expects an AuthResponse containing user info and JWT upon success.
     * @param loginRequest Contains email and password.
     * @return Retrofit Call object wrapping the AuthResponse.
     */
    @POST("/api/auth/login") // Example endpoint path
    Call<AuthResponse> loginUser(@Body LoginRequest loginRequest);

    /**
     * Sends registration details to the server.
     * Expects a response indicating success or failure (e.g., a simple message or the created user ID).
     * Using AuthResponse here as a placeholder, adjust as needed.
     * @param registerRequest Contains username, email, and password.
     * @return Retrofit Call object wrapping the AuthResponse (or a different success/failure model).
     */
    @POST("/api/auth/register") // Example endpoint path
    Call<AuthResponse> registerUser(@Body RegisterRequest registerRequest);

    /**
     * Optional: Endpoint to explicitly logout/invalidate token on the server.
     * @param authToken The JWT token for authentication.
     * @return Retrofit Call object (e.g., wrapping Void or a simple status message).
     */
    @POST("/api/auth/logout") // Example endpoint path
    Call<Void> logoutUser(@Header("Authorization") String authToken);


    // --- User Profile ---

    /**
     * Fetches the profile details for a specific user.
     * @param authToken The JWT token for authentication.
     * @param userId The ID of the user whose profile to fetch.
     * @return Retrofit Call object wrapping the UserModel.
     */
    @GET("/api/users/{userId}/profile") // Example endpoint path
    Call<UserModel> getUserProfile(@Header("Authorization") String authToken, @Path("userId") String userId);

    /**
     * Updates the profile details (e.g., name, bio, image) for the authenticated user.
     * Uses PUT as it typically replaces the entire profile sub-resource or updates specific fields.
     * @param authToken The JWT token for authentication.
     * @param userId The ID of the user whose profile is being edited (often the authenticated user).
     * @param userDetails The updated UserDetails object.
     * @return Retrofit Call object (e.g., wrapping the updated UserModel or Void).
     */
    @PUT("/api/users/{userId}/profile") // Example endpoint path
    Call<UserModel> updateUserProfile(@Header("Authorization") String authToken, @Path("userId") String userId, @Body UserDetails userDetails);

    /**
     * Deletes a user account. (Requires appropriate permissions).
     * @param authToken The JWT token for authentication (likely admin).
     * @param userId The ID of the user to delete.
     * @return Retrofit Call object (e.g., wrapping Void or a status message).
     */
    @DELETE("/api/users/{userId}") // Example endpoint path
    Call<Void> deleteUser(@Header("Authorization") String authToken, @Path("userId") String userId);

    /**
     * Bans a user account. (Requires appropriate permissions).
     * Could be a PUT/POST to a specific ban endpoint or updating the user status.
     * @param authToken The JWT token for authentication (likely admin).
     * @param userId The ID of the user to ban.
     * @return Retrofit Call object (e.g., wrapping Void or the updated UserModel).
     */
    @POST("/api/users/{userId}/ban") // Example endpoint path
    Call<Void> banUser(@Header("Authorization") String authToken, @Path("userId") String userId);


    // --- Events ---

    /**
     * Fetches a list of public events. May support pagination/filtering via query parameters.
     * @return Retrofit Call object wrapping a List of EventModel.
     */
    @GET("/api/events/public") // Example endpoint path
    Call<List<EventModel>> getPublicEvents(/* @Query("page") int page, @Query("size") int size */);

    /**
     * Fetches a list of events the authenticated user is attending.
     * @param authToken The JWT token for authentication.
     * @return Retrofit Call object wrapping a List of EventModel.
     */
    @GET("/api/events/attended") // Example endpoint path
    Call<List<EventModel>> getAttendedEvents(@Header("Authorization") String authToken);

    /**
     * Fetches details for a single event.
     * @param eventId The ID of the event to fetch.
     * @return Retrofit Call object wrapping the EventModel.
     */
    @GET("/api/events/{eventId}") // Example endpoint path
    Call<EventModel> getEventDetails(@Path("eventId") String eventId);


    /**
     * Creates a new event.
     * @param authToken The JWT token for authentication.
     * @param eventModel The EventModel object containing details for the new event.
     * @return Retrofit Call object wrapping the created EventModel (including its new ID).
     */
    @POST("/api/events") // Example endpoint path
    Call<EventModel> createEvent(@Header("Authorization") String authToken, @Body EventModel eventModel);

    /**
     * Allows the authenticated user to join a public event.
     * @param authToken The JWT token for authentication.
     * @param eventId The ID of the event to join.
     * @return Retrofit Call object (e.g., wrapping Void or a ParticipantModel).
     */
    @POST("/api/events/{eventId}/join") // Example endpoint path
    Call<Void> joinEvent(@Header("Authorization") String authToken, @Path("eventId") String eventId);


    // --- Participants ---

    /**
     * Fetches the list of participants (as UserModels) for a specific event.
     * @param authToken The JWT token for authentication (may be needed depending on event privacy).
     * @param eventId The ID of the event.
     * @return Retrofit Call object wrapping a List of UserModel.
     */
    @GET("/api/events/{eventId}/participants") // Example endpoint path
    Call<List<UserModel>> getEventParticipants(@Header("Authorization") String authToken, @Path("eventId") String eventId);

    /**
     * Removes a participant from an event. (Requires permissions).
     * @param authToken The JWT token for authentication (organizer/admin).
     * @param eventId The ID of the event.
     * @param accountId The ID of the participant to remove.
     * @return Retrofit Call object (e.g., wrapping Void or status message).
     */
    @DELETE("/api/events/{eventId}/participants/{accountId}") // Example endpoint path
    Call<Void> removeParticipant(@Header("Authorization") String authToken, @Path("eventId") String eventId, @Path("accountId") String accountId);


    // --- Invitations ---

    /**
     * Fetches invitations for the authenticated user.
     * @param authToken The JWT token for authentication.
     * @return Retrofit Call object wrapping a List of InvitationModel.
     */
    @GET("/api/invitations/my") // Example endpoint path
    Call<List<InvitationModel>> getMyInvitations(@Header("Authorization") String authToken);

    /**
     * Accepts an invitation.
     * @param authToken The JWT token for authentication.
     * @param invitationId The ID of the invitation to accept.
     * @return Retrofit Call object (e.g., wrapping the updated InvitationModel or Void).
     */
    @POST("/api/invitations/{invitationId}/accept") // Example endpoint path
    Call<InvitationModel> acceptInvitation(@Header("Authorization") String authToken, @Path("invitationId") String invitationId);

    /**
     * Declines an invitation.
     * @param authToken The JWT token for authentication.
     * @param invitationId The ID of the invitation to decline.
     * @return Retrofit Call object (e.g., wrapping the updated InvitationModel or Void).
     */
    @POST("/api/invitations/{invitationId}/decline") // Example endpoint path
    Call<InvitationModel> declineInvitation(@Header("Authorization") String authToken, @Path("invitationId") String invitationId);

    /**
     * Cancels/Revokes an invitation (sender action).
     * @param authToken The JWT token for authentication (sender/organizer).
     * @param invitationId The ID of the invitation to cancel.
     * @return Retrofit Call object (e.g., wrapping the updated InvitationModel or Void).
     */
    @POST("/api/invitations/{invitationId}/cancel") // Example endpoint path
    Call<InvitationModel> cancelInvitation(@Header("Authorization") String authToken, @Path("invitationId") String invitationId);

    /**
     * Sends an invitation from an event organizer to a user.
     * @param authToken The JWT token for authentication (organizer).
     * @param eventId The ID of the event.
     * @param recipientAccountId The ID of the user to invite.
     * @return Retrofit Call object wrapping the newly created InvitationModel.
     */
    @POST("/api/events/{eventId}/invitations") // Example endpoint path
    Call<InvitationModel> sendInvitation(@Header("Authorization") String authToken, @Path("eventId") String eventId, @Query("recipientId") String recipientAccountId);


    // --- Media ---

    /**
     * Fetches media items associated with an event.
     * @param eventId The ID of the event.
     * @return Retrofit Call object wrapping a List of MediaModel.
     */
    @GET("/api/events/{eventId}/media") // Example endpoint path
    Call<List<MediaModel>> getEventMedia(@Path("eventId") String eventId);

    /**
     * Deletes a specific media item. (Requires permissions).
     * @param authToken The JWT token for authentication (uploader/organizer/admin).
     * @param mediaId The ID of the media item to delete.
     * @return Retrofit Call object (e.g., wrapping Void).
     */
    @DELETE("/api/media/{mediaId}") // Example endpoint path
    Call<Void> deleteMedia(@Header("Authorization") String authToken, @Path("mediaId") String mediaId);

    /*
    // Example for Uploading Media using Multipart
    @Multipart
    @POST("/api/events/{eventId}/media") // Example endpoint path
    Call<MediaModel> uploadEventMedia(
            @Header("Authorization") String authToken,
            @Path("eventId") String eventId,
            @Part("usage") RequestBody usage, // e.g., "gallery", "logo" passed as RequestBody
            @Part("description") RequestBody description, // Optional description
            @Part MultipartBody.Part file // The actual file data
    );
    */

}
