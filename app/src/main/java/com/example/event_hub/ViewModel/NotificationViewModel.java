package com.example.event_hub.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.Model.NotificationModel;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.ResultWrapper;

public class NotificationViewModel extends ViewModel {

    private final EventHubRepository eventHubRepository;

    private final MediatorLiveData<ResultWrapper<PaginatedResponse<NotificationModel>>> _myNotificationsState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<PaginatedResponse<NotificationModel>>> myNotificationsState = _myNotificationsState;

    private final MediatorLiveData<ResultWrapper<NotificationModel>> _notificationActionState = new MediatorLiveData<>();
    public LiveData<ResultWrapper<NotificationModel>> notificationActionState = _notificationActionState;


    public NotificationViewModel() {
        eventHubRepository = EventHubRepository.getInstance();
        _myNotificationsState.setValue(new ResultWrapper.Idle<>());
        _notificationActionState.setValue(new ResultWrapper.Idle<>());

        _myNotificationsState.addSource(eventHubRepository.myNotificationsState, _myNotificationsState::setValue);
        _notificationActionState.addSource(eventHubRepository.singleNotificationOperationState, _notificationActionState::setValue);
    }

    public void fetchMyNotifications(String authToken, int page, int size) {
        if (authToken == null || authToken.isEmpty()) {
            _myNotificationsState.setValue(new ResultWrapper.Error<>("Authentication token missing for notifications."));
            return;
        }
        eventHubRepository.getMyNotifications(authToken, page, size);
    }

    public void updateNotificationStatus(Long notificationId, String newStatus, String authToken) {
        if (notificationId == null || newStatus == null || authToken == null || authToken.isEmpty()) {
            _notificationActionState.setValue(new ResultWrapper.Error<>("Missing data for updating notification status."));
            return;
        }
        eventHubRepository.updateNotificationStatus(notificationId, newStatus, authToken);
    }

    public void clearState() {
        _myNotificationsState.postValue(new ResultWrapper.Idle<>());
        _notificationActionState.postValue(new ResultWrapper.Idle<>());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _myNotificationsState.removeSource(eventHubRepository.myNotificationsState);
        _notificationActionState.removeSource(eventHubRepository.singleNotificationOperationState);
    }
}