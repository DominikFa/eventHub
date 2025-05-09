package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.event_hub.Model.EventHubRepository;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.Model.ResultWrapper;

import java.util.List;

public class ParticipantViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;
    private String currentObservingEventIdForParticipants = null;

    private final MediatorLiveData<ResultWrapper<List<UserModel>>> _participantsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<List<UserModel>>> participantsState = _participantsState;

    private final MediatorLiveData<ResultWrapper<Void>> _participantActionStatus = new MediatorLiveData<>();
    public LiveData<ResultWrapper<Void>> participantActionStatus = _participantActionStatus;

    private final MutableLiveData<String> _navigateToParticipantProfileId = new MutableLiveData<>();
    public LiveData<String> navigateToParticipantProfileId = _navigateToParticipantProfileId;

    private Observer<ResultWrapper<Void>> voidOperationObserver;

    public ParticipantViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        _participantsState.setValue(new ResultWrapper.Idle<>());
        _participantActionStatus.setValue(new ResultWrapper.Idle<>());

        voidOperationObserver = voidResultWrapper -> {
            if (!(voidResultWrapper instanceof ResultWrapper.Loading)) {
                _participantActionStatus.setValue(voidResultWrapper);
                _participantActionStatus.removeSource(eventHubRepository.voidOperationState);
            } else {
                _participantActionStatus.setValue(new ResultWrapper.Loading<>());
            }
        };
    }

    public void loadParticipants(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _participantsState.setValue(new ResultWrapper.Error<>("Event ID is missing."));
            currentObservingEventIdForParticipants = null;
            return;
        }
        if (!eventId.equals(currentObservingEventIdForParticipants) || !(_participantsState.getValue() instanceof ResultWrapper.Loading)) {
            _participantsState.setValue(new ResultWrapper.Loading<>());
        }
        currentObservingEventIdForParticipants = eventId;
        _participantsState.removeSource(eventHubRepository.eventParticipantsState);
        _participantsState.addSource(eventHubRepository.eventParticipantsState, _participantsState::setValue);
        eventHubRepository.fetchEventParticipants(eventId);
    }

    public void viewParticipantProfile(String participantAccountId) {
        _navigateToParticipantProfileId.setValue(participantAccountId);
        _navigateToParticipantProfileId.postValue(null);
    }

    public void deleteParticipant(String eventId, String participantAccountId, String authToken) { // authToken
        if (eventId == null || participantAccountId == null || authToken == null) {
            _participantActionStatus.setValue(new ResultWrapper.Error<>("Missing info for participant deletion."));
            return;
        }
        _participantActionStatus.setValue(new ResultWrapper.Loading<>());
        _participantActionStatus.removeSource(eventHubRepository.voidOperationState);
        _participantActionStatus.addSource(eventHubRepository.voidOperationState, voidOperationObserver);
        eventHubRepository.deleteParticipant(eventId, participantAccountId, authToken);
    }

    public void clearState() {
        currentObservingEventIdForParticipants = null;
        _participantsState.setValue(new ResultWrapper.Idle<>());
        _participantActionStatus.setValue(new ResultWrapper.Idle<>());
        _navigateToParticipantProfileId.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentObservingEventIdForParticipants != null) {
            _participantsState.removeSource(eventHubRepository.eventParticipantsState);
        }
        _participantActionStatus.removeSource(eventHubRepository.voidOperationState);
    }
}