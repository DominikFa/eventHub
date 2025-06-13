package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.Model.ResultWrapper;
import java.io.File;
import java.util.List;

public class MediaViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private Long currentObservingEventIdForMedia = null;

    private final MediatorLiveData<ResultWrapper<List<MediaModel>>> _mediaListState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<MediaModel>>> mediaListState = _mediaListState;

    // ZMIANA: ViewModel zarządza własnym stanem operacji na mediach
    private final MediatorLiveData<ResultWrapper<MediaModel>> _mediaUploadOperationState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<MediaModel>> mediaUploadOperationState = _mediaUploadOperationState;

    private final MediatorLiveData<ResultWrapper<Void>> _mediaDeleteOperationState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<Void>> mediaDeleteOperationState = _mediaDeleteOperationState;

    public MediaViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        _mediaListState.setValue(new ResultWrapper.Idle<>());
        _mediaUploadOperationState.setValue(new ResultWrapper.Idle<>());
        _mediaDeleteOperationState.setValue(new ResultWrapper.Idle<>());

        // Nasłuchiwanie na zmiany z repozytorium i aktualizacja lokalnego stanu
        _mediaListState.addSource(eventHubRepository.eventMediaState, _mediaListState::setValue);
        _mediaUploadOperationState.addSource(eventHubRepository.mediaUploadOperationState, _mediaUploadOperationState::setValue);
        _mediaDeleteOperationState.addSource(eventHubRepository.voidOperationState, _mediaDeleteOperationState::setValue);
    }

    public void loadMediaForEvent(Long eventId) {
        if (eventId == null || eventId == 0L) {
            _mediaListState.setValue(new ResultWrapper.Error<>("Event ID is missing for fetching media."));
            currentObservingEventIdForMedia = null;
            return;
        }
        if (!eventId.equals(currentObservingEventIdForMedia) || !(_mediaListState.getValue() instanceof ResultWrapper.Loading)) {
            _mediaListState.setValue(new ResultWrapper.Loading<>());
        }
        currentObservingEventIdForMedia = eventId;
        // eventHubRepository.fetchMediaForEvent(eventId); // Ta metoda nie jest zaimplementowana w repozytorium
    }

    public void uploadMedia(File file, String authTokenUploader) {
        if (file == null || authTokenUploader == null) {
            System.err.println("MediaViewModel: File or authToken for upload is null.");
            // Błąd walidacji - ustawiamy stan w ViewModelu, nie w repozytorium
            _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("File or authentication token is missing."));
            return;
        }
        if (currentObservingEventIdForMedia == null) {
            System.err.println("MediaViewModel: Event context not set for media upload.");
            // Błąd walidacji - ustawiamy stan w ViewModelu, nie w repozytorium
            _mediaUploadOperationState.postValue(new ResultWrapper.Error<>("Event ID not set."));
            return;
        }
        // Wywołanie akcji w repozytorium
        eventHubRepository.uploadEventGalleryImage(currentObservingEventIdForMedia, file, authTokenUploader);
    }

    public void deleteMedia(Long mediaId, String authTokenActingUser) {
        if (mediaId == null || authTokenActingUser == null) {
            _mediaDeleteOperationState.postValue(new ResultWrapper.Error<>("Missing info for media deletion."));
            return;
        }
        eventHubRepository.deleteGalleryMedia(mediaId, authTokenActingUser);
    }

    public void clearState() {
        currentObservingEventIdForMedia = null;
        _mediaListState.postValue(new ResultWrapper.Idle<>());
        _mediaUploadOperationState.postValue(new ResultWrapper.Idle<>());
        _mediaDeleteOperationState.postValue(new ResultWrapper.Idle<>());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _mediaListState.removeSource(eventHubRepository.eventMediaState);
        _mediaUploadOperationState.removeSource(eventHubRepository.mediaUploadOperationState);
        _mediaDeleteOperationState.removeSource(eventHubRepository.voidOperationState);
    }
}