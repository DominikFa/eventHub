package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.Model.ResultWrapper;

import java.util.List;

public class MediaViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository; // Should be correctly initialized
    private String currentObservingEventIdForMedia = null;

    private final MediatorLiveData<ResultWrapper<List<MediaModel>>> _mediaListState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<MediaModel>>> mediaListState = _mediaListState;

    public LiveData<ResultWrapper<MediaModel>> mediaUploadOperationState;
    public LiveData<ResultWrapper<Void>> mediaDeleteOperationState;

    public MediaViewModel() {
        eventHubRepository = EventHubRepository.getInstance(); // Correct instantiation
        _mediaListState.setValue(new ResultWrapper.Idle<>());

        mediaUploadOperationState = eventHubRepository.mediaUploadOperationState;
        mediaDeleteOperationState = eventHubRepository.voidOperationState;

        _mediaListState.addSource(eventHubRepository.eventMediaState, resultWrapper -> {
            _mediaListState.setValue(resultWrapper);
        });
    }

    public void loadMediaForEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _mediaListState.setValue(new ResultWrapper.Error<>("Event ID is missing for fetching media."));
            currentObservingEventIdForMedia = null;
            return;
        }
        if (!eventId.equals(currentObservingEventIdForMedia) || !(_mediaListState.getValue() instanceof ResultWrapper.Loading)) {
            _mediaListState.setValue(new ResultWrapper.Loading<>());
        }
        currentObservingEventIdForMedia = eventId;
        // This line should now work if EventHubRepository.java is the latest version
        eventHubRepository.fetchMediaForEvent(eventId);
    }

    // uploadMedia and deleteMedia should take authToken as per new Repository design
    public void uploadMedia(MediaModel mediaToUpload, String authTokenUploader) {
        if (mediaToUpload == null || authTokenUploader == null) {
            System.err.println("MediaViewModel: MediaModel or authToken for upload is null.");
            // If mediaUploadOperationState was a MutableLiveData in this VM:
            // _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Client Error: Missing data."));
            return;
        }
        if (currentObservingEventIdForMedia == null || currentObservingEventIdForMedia.isEmpty()) {
            System.err.println("MediaViewModel: Event context not set for media upload.");
            // If mediaUploadOperationState was a MutableLiveData in this VM:
            // _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Client Error: Event context not set."));
            return;
        }
        mediaToUpload.setEventId(currentObservingEventIdForMedia);
        eventHubRepository.uploadMedia(mediaToUpload, authTokenUploader);
    }

    public void deleteMedia(String mediaId, String authTokenActingUser, String eventIdForRefreshContext) {
        if (mediaId == null || authTokenActingUser == null || eventIdForRefreshContext == null) {
            System.err.println("MediaViewModel: Missing info for media deletion.");
            return;
        }
        eventHubRepository.deleteMedia(mediaId, eventIdForRefreshContext, authTokenActingUser);
    }

    public void clearState() {
        currentObservingEventIdForMedia = null;
        _mediaListState.setValue(new ResultWrapper.Idle<>());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _mediaListState.removeSource(eventHubRepository.eventMediaState);
    }
}