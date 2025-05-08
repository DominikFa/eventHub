package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.Model.UserModel; // For uploader info if needed
import com.example.event_hub.Model.ResultWrapper; // Ensure this import path is correct

import java.util.List;

public class MediaViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private String currentObservingEventIdForMedia = null;

    // LiveData for the list of media items for the currently loaded event
    private final MediatorLiveData<ResultWrapper<List<MediaModel>>> _mediaListState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<MediaModel>>> mediaListState = _mediaListState;

    // LiveData for the status of media upload operations
    public LiveData<ResultWrapper<MediaModel>> mediaUploadOperationState;

    // LiveData for the status of media delete operations
    public LiveData<ResultWrapper<Void>> mediaDeleteOperationState;


    public MediaViewModel() {
        eventHubRepository = EventHubRepository.getInstance();

        // Directly observe repository states for upload and delete operations
        mediaUploadOperationState = eventHubRepository.mediaUploadOperationState;
        mediaDeleteOperationState = eventHubRepository.voidOperationState; // Assuming delete uses the generic void state
    }

    /**
     * Fetches all media items for a specific event.
     * The result is observed on mediaListState.
     * @param eventId The ID of the event whose media are to be fetched.
     */
    public void loadMediaForEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _mediaListState.setValue(new ResultWrapper.Error<>("Event ID is missing for fetching media."));
            return;
        }

        if (currentObservingEventIdForMedia != null) {
            _mediaListState.removeSource(eventHubRepository.eventMediaState);
        }
        currentObservingEventIdForMedia = eventId;

        _mediaListState.setValue(new ResultWrapper.Loading<>());
        System.out.println("MediaViewModel: Fetching media for eventId: " + eventId);

        _mediaListState.addSource(eventHubRepository.eventMediaState, mediaListWrapper -> {
            // Assuming eventMediaState in repo is updated by fetchMediaForEvent(eventId)
            _mediaListState.setValue(mediaListWrapper);
        });
        eventHubRepository.fetchMediaForEvent(eventId);
    }

    /**
     * Uploads a new media item for the current event.
     * The current event context is taken from currentObservingEventIdForMedia.
     * @param mediaToUpload The MediaModel object representing the file to upload.
     * It should have eventId, accountId (uploader), mediaType, usage, etc., set by the UI/caller.
     * The repository will handle setting the mediaId and mediaFileReference upon successful simulation.
     */
    public void uploadMedia(MediaModel mediaToUpload) {
        if (mediaToUpload == null) {
            // Handle error: post to a local status or rely on repo's mediaUploadOperationState
            System.err.println("MediaViewModel: MediaModel for upload is null.");
            // If mediaUploadOperationState is observed by UI, this error might not be directly shown
            // unless repo call fails early. Consider a local MutableLiveData for pre-call validation errors.
            return;
        }

        if (currentObservingEventIdForMedia == null || currentObservingEventIdForMedia.isEmpty()) {
            System.err.println("MediaViewModel: Event context not set for media upload.");
            // Update mediaUploadOperationState with an error if it's used for pre-validation
            // This is tricky as mediaUploadOperationState is directly from repo.
            // A local action status LiveData might be better for such immediate client-side validation issues.
            return;
        }
        // Ensure the media is associated with the currently viewed event.
        // The uploader (accountId) should be set before calling this, typically from the logged-in user.
        mediaToUpload.setEventId(currentObservingEventIdForMedia);


        System.out.println("MediaViewModel: Attempting to upload media for event " + currentObservingEventIdForMedia);
        // The UI will observe mediaUploadOperationState (which is eventHubRepository.mediaUploadOperationState)
        eventHubRepository.uploadMedia(mediaToUpload);
        // After successful upload, the repository's uploadMedia calls fetchMediaForEvent,
        // which updates eventMediaState, and thus _mediaListState will be updated.
    }

    /**
     * Deletes a media item.
     * @param mediaId The ID of the media item to delete.
     */
    public void deleteMedia(String mediaId) {
        if (mediaId == null || mediaId.isEmpty()) {
            System.err.println("MediaViewModel: Media ID is missing for deletion.");
            // Post to a local action status if needed
            return;
        }
        if (currentObservingEventIdForMedia == null) {
            System.err.println("MediaViewModel: Event context not available for refreshing media list after deletion.");
            // Post to a local action status if needed
            return;
        }

        System.out.println("MediaViewModel: Attempting to delete media: " + mediaId);
        // The UI will observe mediaDeleteOperationState (which is eventHubRepository.voidOperationState)
        // Pass the eventId so the repo can refresh the correct media list upon successful deletion
        eventHubRepository.deleteMedia(mediaId, currentObservingEventIdForMedia);
        // After successful deletion, the repository's deleteMedia calls fetchMediaForEvent,
        // which updates eventMediaState, and thus _mediaListState will be updated.
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentObservingEventIdForMedia != null) {
            _mediaListState.removeSource(eventHubRepository.eventMediaState);
        }
        currentObservingEventIdForMedia = null;
        // No need to remove sources for mediaUploadOperationState or mediaDeleteOperationState
        // as they are direct references to repository LiveData, not MediatorLiveData sources added here.
        System.out.println("MediaViewModel: Cleared.");
    }
}