package com.example.event_hub.ViewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.ResultWrapper;

public class EventDetailViewModel extends AndroidViewModel {

    private final EventHubRepository eventHubRepository;
    private Long currentEventId; // Zmieniono na Long

    public LiveData<ResultWrapper<EventModel>> eventDetailState;
    public LiveData<ResultWrapper<Void>> eventActionState;

    public EventDetailViewModel(@NonNull Application application) {
        super(application);
        this.eventHubRepository = EventHubRepository.getInstance();
        this.eventDetailState = eventHubRepository.singleEventOperationState;
        this.eventActionState = eventHubRepository.voidOperationState;
    }

    public void loadEventAllDetails(Long eventId) {
        this.currentEventId = eventId;
        eventHubRepository.fetchEventDetails(eventId);
    }

    public void joinCurrentEvent(String authToken) {
        if (currentEventId == null || authToken == null) return;
        eventHubRepository.joinEvent(currentEventId, authToken);
    }

    public void leaveCurrentEvent(String authToken) {
        if (currentEventId == null || authToken == null) return;
        eventHubRepository.leaveEvent(currentEventId, authToken);
    }

    public void deleteCurrentEvent(String authToken) {
        if (currentEventId == null || authToken == null) return;
        eventHubRepository.deleteEvent(currentEventId, authToken);
    }

    public void deleteParticipant(Long participantId, String authToken) {
        if (currentEventId == null || participantId == null || authToken == null) return;
        eventHubRepository.updateParticipantStatus(currentEventId, participantId, "cancelled", authToken);
    }

    public void clearStates() {
        currentEventId = null;
        eventHubRepository.resetSingleEventOperationState();
        eventHubRepository.resetVoidOperationState();
    }
}