// src/main/java/com/example/event_hub/ViewModel/MainViewModel.java
package com.example.event_hub.ViewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData; // Import MediatorLiveData
import com.example.event_hub.Repositiry.AuthRepository;
import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.EventSummary;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.ResultWrapper;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MainViewModel extends AndroidViewModel {

    private final EventHubRepository eventHubRepository;
    private final AuthRepository authRepository; // Added AuthRepository

    // Changed to MediatorLiveData for transformation
    public final MediatorLiveData<ResultWrapper<List<EventModel>>> publicEventsState = new MediatorLiveData<>();
    public final MediatorLiveData<ResultWrapper<List<EventModel>>> attendedEventsState = new MediatorLiveData<>();
    public final MediatorLiveData<ResultWrapper<List<EventModel>>> myCreatedEventsState = new MediatorLiveData<>();


    public MainViewModel(@NonNull Application application) {
        super(application);
        eventHubRepository = EventHubRepository.getInstance();
        authRepository = AuthRepository.getInstance(); // Initialize AuthRepository

        // Source from repository's publicEventsState and map to EventModel list
        publicEventsState.addSource(eventHubRepository.publicEventsState, resultWrapper -> {
            if (resultWrapper instanceof ResultWrapper.Success) {
                PaginatedResponse<EventSummary> response = ((ResultWrapper.Success<PaginatedResponse<EventSummary>>) resultWrapper).getData();
                List<EventSummary> summaries = (response != null && response.getContent() != null) ? response.getContent() : null;

                List<EventModel> eventModels = null;
                if (summaries != null) {
                    eventModels = summaries.stream().map(summary -> {
                        EventModel model = new EventModel();
                        model.setId(summary.getId());
                        model.setName(summary.getName());
                        model.setStartDate(summary.getStartDate());
                        model.setEndDate(summary.getEndDate());
                        return model;
                    }).collect(Collectors.toList());
                }
                publicEventsState.setValue(new ResultWrapper.Success<>(eventModels));
            } else if (resultWrapper instanceof ResultWrapper.Error) {
                publicEventsState.setValue(new ResultWrapper.Error<>(((ResultWrapper.Error<?>) resultWrapper).getMessage()));
            } else if (resultWrapper instanceof ResultWrapper.Loading) {
                publicEventsState.setValue(new ResultWrapper.Loading<>());
            } else if (resultWrapper instanceof ResultWrapper.Idle) {
                publicEventsState.setValue(new ResultWrapper.Idle<>());
            }
        });

        // Source from repository's attendedEventsState
        attendedEventsState.addSource(eventHubRepository.attendedEventsState, resultWrapper -> {
            if (resultWrapper instanceof ResultWrapper.Success) {
                PaginatedResponse<EventModel> response = ((ResultWrapper.Success<PaginatedResponse<EventModel>>) resultWrapper).getData();
                attendedEventsState.setValue(new ResultWrapper.Success<>((response != null) ? response.getContent() : null));
            } else if (resultWrapper instanceof ResultWrapper.Error) {
                attendedEventsState.setValue(new ResultWrapper.Error<>(((ResultWrapper.Error<?>) resultWrapper).getMessage()));
            } else if (resultWrapper instanceof ResultWrapper.Loading) {
                attendedEventsState.setValue(new ResultWrapper.Loading<>());
            } else if (resultWrapper instanceof ResultWrapper.Idle) {
                attendedEventsState.setValue(new ResultWrapper.Idle<>());
            }
        });

        // Source from repository's myCreatedEventsState
        myCreatedEventsState.addSource(eventHubRepository.myCreatedEventsState, resultWrapper -> {
            if (resultWrapper instanceof ResultWrapper.Success) {
                PaginatedResponse<EventModel> response = ((ResultWrapper.Success<PaginatedResponse<EventModel>>) resultWrapper).getData();
                myCreatedEventsState.setValue(new ResultWrapper.Success<>((response != null) ? response.getContent() : null));
            } else if (resultWrapper instanceof ResultWrapper.Error) {
                myCreatedEventsState.setValue(new ResultWrapper.Error<>(((ResultWrapper.Error<?>) resultWrapper).getMessage()));
            } else if (resultWrapper instanceof ResultWrapper.Loading) {
                myCreatedEventsState.setValue(new ResultWrapper.Loading<>());
            } else if (resultWrapper instanceof ResultWrapper.Idle) {
                myCreatedEventsState.setValue(new ResultWrapper.Idle<>());
            }
        });
    }

    /**
     * Fetches public events with pagination, filtering, and sorting.
     * @param page The page number (0-indexed).
     * @param size The number of items per page.
     * @param name Optional: Filter by event name.
     * @param startDate Optional: Filter by events starting on or after this date.
     * @param endDate Optional: Filter by events ending on or before this date.
     * @param sort Optional: List of sorting criteria (e.g., "name,asc", "startDate,desc").
     */
    public void loadPublicEvents(int page, int size, String name, Date startDate, Date endDate, List<String> sort) {
        eventHubRepository.fetchPublicEvents(page, size, name, startDate, endDate, sort);
    }

    /**
     * Fetches events the current user is participating in with pagination, filtering, and sorting.
     * The user is identified by the JWT provided by the AuthRepository.
     * @param page The page number (0-indexed).
     * @param size The number of items per page.
     * @param name Optional: Filter by event name.
     * @param startDate Optional: Filter by events starting on or after this date.
     * @param endDate Optional: Filter by events ending on or before this date.
     * @param sort Optional: List of sorting criteria (e.g., "name,asc", "startDate,desc").
     */
    public void loadAttendedEvents(int page, int size, String name, Date startDate, Date endDate, List<String> sort) {
        String authToken = authRepository.getCurrentTokenSynchronous();
        if (authToken != null) {
            eventHubRepository.fetchMyParticipatedEvents(authToken, page, size, name, startDate, endDate, sort);
        } else {
            attendedEventsState.postValue(new ResultWrapper.Error<>("Authentication required to fetch attended events."));
        }
    }

    /**
     * Fetches events created by the current user with pagination, filtering, and sorting.
     * The user is identified by the JWT provided by the AuthRepository.
     * @param page The page number (0-indexed).
     * @param size The number of items per page.
     * @param name Optional: Filter by event name.
     * @param startDate Optional: Filter by events starting on or after this date.
     * @param endDate Optional: Filter by events ending on or before this date.
     * @param sort Optional: List of sorting criteria (e.g., "name,asc", "startDate,desc").
     */
    public void loadCreatedEvents(int page, int size, String name, Date startDate, Date endDate, List<String> sort) {
        String authToken = authRepository.getCurrentTokenSynchronous();
        if (authToken != null) {
            eventHubRepository.fetchMyCreatedEvents(authToken, page, size, name, startDate, endDate, sort);
        } else {
            myCreatedEventsState.postValue(new ResultWrapper.Error<>("Authentication required to fetch created events."));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove sources to prevent memory leaks when ViewModel is cleared
        publicEventsState.removeSource(eventHubRepository.publicEventsState);
        attendedEventsState.removeSource(eventHubRepository.attendedEventsState);
        myCreatedEventsState.removeSource(eventHubRepository.myCreatedEventsState);
    }
}