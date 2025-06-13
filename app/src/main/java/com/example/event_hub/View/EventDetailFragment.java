// src/main/java/com/example/event_hub/View/EventDetailFragment.java
package com.example.event_hub.View;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_hub.Model.EventModel;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.Model.PaginatedResponse;
import com.example.event_hub.Model.ParticipantModel;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.R;
import com.example.event_hub.View.adapter.EventPhotoAdapter;
import com.example.event_hub.View.adapter.ParticipantAdapter;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.EventDetailViewModel;
import com.example.event_hub.ViewModel.MediaViewModel;
import com.example.event_hub.ViewModel.ParticipantViewModel;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class EventDetailFragment extends Fragment implements ParticipantAdapter.OnParticipantClickListener, ParticipantAdapter.OnRemoveParticipantClickListener, EventPhotoAdapter.OnPhotoClickListener {

    private EventDetailViewModel eventDetailViewModel;
    private AuthViewModel authViewModel;
    private MediaViewModel mediaViewModel;
    private ParticipantViewModel participantViewModel;

    private TextView tvEventName, tvEventDescription, tvEventDateTime, tvEventLocation,
            tvEventScreenTitle, tvPhotosTitle, tvParticipantsTitle, tvNoPhotos, tvNoParticipants;

    private MaterialButton btnJoinLeaveEvent, btnGetDirections, btnInvite, btnUploadPhoto, btnDeleteEvent;

    private ImageView ivUserIcon;
    private ProgressBar pbEventDetailsLoading, pbPhotosLoading, pbParticipantsLoading;

    private RecyclerView rvEventPhotos, rvEventParticipants;


    private ParticipantAdapter participantAdapter;
    private EventPhotoAdapter eventPhotoAdapter;

    private long eventId;
    private String currentAuthToken;
    private Long loggedInUserId;
    private EventModel currentEvent;

    // Added flag to track if a delete action is pending
    private boolean isDeleteActionPending = false;

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormatOnly = new SimpleDateFormat("dd MMM,EEEE", Locale.getDefault());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventDetailViewModel = new ViewModelProvider(this).get(EventDetailViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        mediaViewModel = new ViewModelProvider(this).get(MediaViewModel.class);
        participantViewModel = new ViewModelProvider(this).get(ParticipantViewModel.class);

        if (getArguments() != null) {
            eventId = getArguments().getLong("eventId");
        }

        participantAdapter = new ParticipantAdapter(new ArrayList<>(), this, this, false);
        eventPhotoAdapter = new EventPhotoAdapter(new ArrayList<>(), this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupClickListeners();
        setupRecyclerViews();
        observeViewModels();
        updateProfileLoginButtonIcon(view);

        if (eventId > 0) {
            eventDetailViewModel.loadEventAllDetails(eventId);
            participantViewModel.loadParticipants(eventId);
            // The backend API does not currently support fetching a list of media for an event.
            // This call will result in an empty or error state for the media list.
            mediaViewModel.loadMediaForEvent(eventId);
        } else {
            Toast.makeText(getContext(), R.string.error_event_id_not_found, Toast.LENGTH_SHORT).show();
            if (getView() != null) Navigation.findNavController(getView()).popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        eventDetailViewModel.clearStates();
    }

    private void bindViews(View view) {
        ivUserIcon = view.findViewById(R.id.iv_user_icon_event_detail);

        pbEventDetailsLoading = view.findViewById(R.id.pb_event_details_loading);
        tvEventScreenTitle = view.findViewById(R.id.tv_event_screen_title);

        tvEventName = view.findViewById(R.id.tv_event_name_detail);
        tvEventDescription = view.findViewById(R.id.tv_event_description_detail);
        tvEventDateTime = view.findViewById(R.id.tv_event_date_time_detail);
        tvEventLocation = view.findViewById(R.id.tv_event_location_detail);

        tvPhotosTitle = view.findViewById(R.id.tv_photos_title);
        pbPhotosLoading = view.findViewById(R.id.pb_photos_loading);
        rvEventPhotos = view.findViewById(R.id.rv_event_photos);
        tvNoPhotos = view.findViewById(R.id.tv_no_photos_detail);

        tvParticipantsTitle = view.findViewById(R.id.tv_participants_title);
        pbParticipantsLoading = view.findViewById(R.id.pb_participants_loading);
        rvEventParticipants = view.findViewById(R.id.rv_event_participants);
        tvNoParticipants = view.findViewById(R.id.tv_no_participants_detail);


        btnJoinLeaveEvent = view.findViewById(R.id.btn_join_leave_event_detail);
        btnGetDirections = view.findViewById(R.id.btn_get_directions_detail);
        btnInvite = view.findViewById(R.id.btn_invite_detail);
        btnUploadPhoto = view.findViewById(R.id.btn_upload_photo_detail);
        btnDeleteEvent = view.findViewById(R.id.btn_delete_event_detail);
    }

    private void setupClickListeners() {
        ivUserIcon.setOnClickListener(v -> {
            if (getView() == null) return;
            NavController navController = Navigation.findNavController(getView());
            if (currentAuthToken != null && loggedInUserId != null) {
                Bundle profileArgs = new Bundle();
                profileArgs.putLong("userId", loggedInUserId);
                navController.navigate(R.id.action_eventDetailFragment_to_profileFragment, profileArgs);
            } else {
                navController.navigate(R.id.loginActivity);
            }
        });

        btnJoinLeaveEvent.setOnClickListener(v -> {

            btnJoinLeaveEvent.setEnabled(false);

            if (currentEvent != null && currentAuthToken != null && loggedInUserId != null) {
                boolean isParticipant = participantAdapter.getParticipants().stream()
                        .anyMatch(p -> Objects.equals(loggedInUserId, p.getId()));

                if (isParticipant) {
                    eventDetailViewModel.leaveCurrentEvent(currentAuthToken);
                } else {
                    eventDetailViewModel.joinCurrentEvent(currentAuthToken);
                }
            } else {
                Toast.makeText(getContext(), R.string.toast_login_to_join_leave, Toast.LENGTH_SHORT).show();

                btnJoinLeaveEvent.setEnabled(true);
            }
        });
        btnDeleteEvent.setOnClickListener(v -> {
            if (currentEvent != null && currentAuthToken != null) {
                showDeleteEventConfirmationDialog();
            }
        });

        btnInvite.setOnClickListener(v -> {
            if (currentEvent != null && currentAuthToken != null) {
                if (isOrganizer(currentEvent, loggedInUserId)) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("eventId", currentEvent.getId());
                    if (getView() != null) Navigation.findNavController(getView()).navigate(R.id.action_eventDetailFragment_to_inviteUsersFragment, bundle);
                } else {
                    Toast.makeText(getContext(), R.string.organizer_only_invite, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), R.string.auth_required_to_send_invites, Toast.LENGTH_SHORT).show();
            }
        });

        btnGetDirections.setOnClickListener(v -> {
            if (currentEvent != null && currentEvent.getLocation() != null) {
                Bundle bundle = new Bundle();
                // Pass only eventId and eventName, MapFragment will fetch full location details
                bundle.putLong("eventId", currentEvent.getId());
                bundle.putString("eventName", currentEvent.getName());
                if (getView() != null) Navigation.findNavController(getView()).navigate(R.id.action_eventDetailFragment_to_mapFragment, bundle);
            } else {
                Toast.makeText(getContext(), R.string.event_location_not_available, Toast.LENGTH_SHORT).show();
            }
        });

        btnUploadPhoto.setOnClickListener(v -> {
            Toast.makeText(getContext(), R.string.upload_photo_not_implemented, Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeleteEventConfirmationDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_delete_event_confirm)
                .setMessage(getString(R.string.dialog_message_delete_event_confirm, currentEvent.getName()))
                .setPositiveButton(R.string.dialog_button_delete, (dialog, which) -> {
                    isDeleteActionPending = true; // Set flag when delete action is initiated
                    eventDetailViewModel.deleteCurrentEvent(currentAuthToken);
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }


    private void setupRecyclerViews() {
        rvEventParticipants.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvEventParticipants.setAdapter(participantAdapter);

        rvEventPhotos.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvEventPhotos.setAdapter(eventPhotoAdapter);
    }

    private void observeViewModels() {
        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            currentAuthToken = token;
            updateButtonStates();
            updateProfileLoginButtonIcon(getView());
        });

        authViewModel.currentUserId.observe(getViewLifecycleOwner(), userId -> {
            loggedInUserId = userId;
            updateButtonStates();
            updateProfileLoginButtonIcon(getView());
        });

        eventDetailViewModel.eventDetailState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbEventDetailsLoading, result instanceof ResultWrapper.Loading);
            if (result instanceof ResultWrapper.Success) {
                currentEvent = ((ResultWrapper.Success<EventModel>) result).getData();
                if (currentEvent != null) {
                    populateEventDetails(currentEvent);
                    updateButtonStates();
                } else {
                    Toast.makeText(getContext(), R.string.error_event_not_found, Toast.LENGTH_SHORT).show();
                    if (getView() != null) Navigation.findNavController(getView()).popBackStack();
                }
            } else if (result instanceof ResultWrapper.Error) {
                String errorMessage = ((ResultWrapper.Error<?>) result).getMessage();
                Toast.makeText(getContext(), getString(R.string.error_loading_event_details) + (errorMessage != null ? ": " + errorMessage : ""), Toast.LENGTH_LONG).show();
                if (getView() != null) Navigation.findNavController(getView()).popBackStack();
            }
        });

        eventDetailViewModel.eventActionState.observe(getViewLifecycleOwner(), result -> {
            if (!(result instanceof ResultWrapper.Loading || result instanceof ResultWrapper.Idle)) {
                if (result instanceof ResultWrapper.Success) {
                    Toast.makeText(getContext(), R.string.event_action_successful, Toast.LENGTH_SHORT).show();
                    if (isDeleteActionPending) {
                        // If delete action was pending and succeeded, navigate back
                        isDeleteActionPending = false; // Reset flag
                        if (getView() != null) {
                            Navigation.findNavController(getView()).popBackStack();
                        }
                    } else {
                        // For join/leave, refresh event details and participants
                        if (eventId > 0) {
                            eventDetailViewModel.loadEventAllDetails(eventId);
                            participantViewModel.loadParticipants(eventId);
                        }
                    }
                } else if (result instanceof ResultWrapper.Error) {
                    String msg = ((ResultWrapper.Error<Void>) result).getMessage();
                    Toast.makeText(getContext(), getString(R.string.event_action_failed, (msg != null ? msg : getString(R.string.unknown_error))), Toast.LENGTH_SHORT).show();
                    isDeleteActionPending = false; // Reset flag on error too
                }
            }
            updateButtonStates();
        });

        participantViewModel.participantsState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbParticipantsLoading, result instanceof ResultWrapper.Loading);
            handleVisibility(rvEventParticipants, !(result instanceof ResultWrapper.Loading));
            handleVisibility(tvNoParticipants, false);

            if (result instanceof ResultWrapper.Success) {
                PaginatedResponse<ParticipantModel> paginatedParticipants = ((ResultWrapper.Success<PaginatedResponse<ParticipantModel>>) result).getData();
                List<ParticipantModel> participants = paginatedParticipants != null ? paginatedParticipants.getContent() : new ArrayList<>();

                if (!participants.isEmpty()) {
                    // Map ParticipantModel to UserSummary, then UserSummary to UserModel
                    List<UserModel> userModels = participants.stream()
                            .map(ParticipantModel::getUser) // This gives a Stream<UserSummary>
                            .filter(Objects::nonNull)
                            .map(summary -> {
                                // Manually convert UserSummary to UserModel
                                UserModel user = new UserModel();
                                user.setId(summary.getId());
                                user.setName(summary.getName());
                                user.setProfileImageUrl(summary.getProfileImageUrl());
                                // Note: Other fields from UserModel like login, role, etc., will be null
                                // as they are not available in UserSummary. The adapter must handle this.
                                return user;
                            })
                            .collect(Collectors.toList());

                    boolean canRemove = isOrganizer(currentEvent, loggedInUserId);
                    participantAdapter.updateParticipants(userModels, canRemove);
                    tvNoParticipants.setVisibility(View.GONE);
                    rvEventParticipants.setVisibility(View.VISIBLE);
                } else {
                    participantAdapter.updateParticipants(new ArrayList<>(), false);
                    tvNoParticipants.setVisibility(View.VISIBLE);
                    tvNoParticipants.setText(R.string.no_participants_yet);
                }
            } else if (result instanceof ResultWrapper.Error) {
                participantAdapter.updateParticipants(new ArrayList<>(), false);
                tvNoParticipants.setText(getString(R.string.error_loading_participants_prefix) + ((ResultWrapper.Error<?>) result).getMessage());
                tvNoParticipants.setVisibility(View.VISIBLE);
            } else if (result instanceof ResultWrapper.Idle) {
                participantAdapter.updateParticipants(new ArrayList<>(), false);
                tvNoParticipants.setVisibility(View.VISIBLE);
                tvNoParticipants.setText(R.string.no_participants_yet);
            }
            updateButtonStates();
        });

        mediaViewModel.mediaListState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbPhotosLoading, result instanceof ResultWrapper.Loading);
            handleVisibility(rvEventPhotos, !(result instanceof ResultWrapper.Loading));
            handleVisibility(tvNoPhotos, false);

            if (result instanceof ResultWrapper.Success) {
                List<MediaModel> media = ((ResultWrapper.Success<List<MediaModel>>) result).getData();
                if (media != null && !media.isEmpty()) {
                    eventPhotoAdapter.updateMedia(media);
                    tvNoPhotos.setVisibility(View.GONE);
                    rvEventPhotos.setVisibility(View.VISIBLE);
                } else {
                    eventPhotoAdapter.updateMedia(new ArrayList<>());
                    tvNoPhotos.setVisibility(View.VISIBLE);
                    tvNoPhotos.setText(R.string.no_media_yet);
                }
            } else if (result instanceof ResultWrapper.Error) {
                eventPhotoAdapter.updateMedia(new ArrayList<>());
                tvNoPhotos.setText(getString(R.string.error_loading_media_prefix) + ((ResultWrapper.Error<?>) result).getMessage());
                tvNoPhotos.setVisibility(View.VISIBLE);
            } else if (result instanceof ResultWrapper.Idle) {
                eventPhotoAdapter.updateMedia(new ArrayList<>());
                tvNoPhotos.setVisibility(View.VISIBLE);
                tvNoPhotos.setText(R.string.no_media_yet);
            }
        });

        participantViewModel.navigateToParticipantProfileId.observe(getViewLifecycleOwner(), userId -> {
            if (userId != null && getView() != null) {
                Bundle bundle = new Bundle();
                bundle.putLong("userId", userId);
                Navigation.findNavController(getView()).navigate(R.id.action_eventDetailFragment_to_profileFragment, bundle);
            }
        });
    }

    private void populateEventDetails(EventModel event) {
        tvEventName.setText(event.getName());
        tvEventDescription.setText(event.getDescription());

        String dateRange = "";
        if (event.getStartDate() != null) {
            dateRange = dateTimeFormat.format(event.getStartDate());
            if (event.getEndDate() != null) {
                if (!isSameDay(event.getStartDate(), event.getEndDate())) {
                    dateRange = dateFormatOnly.format(event.getStartDate()) + " - " + dateFormatOnly.format(event.getEndDate());
                } else {
                    dateRange = dateTimeFormat.format(event.getStartDate()) + " - " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.getEndDate());
                }
            }
        } else {
            dateRange = getString(R.string.not_available_placeholder);
        }
        tvEventDateTime.setText(dateRange);

        if (event.getLocation() != null && event.getLocation().getFullAddress() != null && !event.getLocation().getFullAddress().isEmpty()) {
            tvEventLocation.setText(event.getLocation().getFullAddress());
            tvEventLocation.setVisibility(View.VISIBLE);
        } else {
            tvEventLocation.setText(R.string.not_available_placeholder);
            tvEventLocation.setVisibility(View.GONE);
        }
    }

    private void updateProfileLoginButtonIcon(View view) {
        if (ivUserIcon != null) {
            if (currentAuthToken != null && loggedInUserId != null) {
                ivUserIcon.setImageResource(R.drawable.ic_profile);
                ivUserIcon.setContentDescription(getString(R.string.content_desc_profile));
            } else {
                ivUserIcon.setImageResource(R.drawable.ic_login);
                ivUserIcon.setContentDescription(getString(R.string.content_desc_login));
            }
        }
    }


    private void updateButtonStates() {
        if (currentEvent == null) {
            btnJoinLeaveEvent.setVisibility(View.GONE);
            btnGetDirections.setVisibility(View.GONE);
            btnInvite.setVisibility(View.GONE);
            btnUploadPhoto.setVisibility(View.GONE);
            btnDeleteEvent.setVisibility(View.GONE);
            return;
        }

        boolean isLoggedIn = loggedInUserId != null && currentAuthToken != null;
        boolean isOrganizer = isOrganizer(currentEvent, loggedInUserId);

        boolean isParticipant = participantAdapter.getParticipants().stream()
                .anyMatch(p -> Objects.equals(loggedInUserId, p.getId()));

        btnGetDirections.setVisibility(currentEvent.getLocation() != null ? View.VISIBLE : View.GONE);

        if (isLoggedIn) {
            btnJoinLeaveEvent.setVisibility(View.VISIBLE);
            if (isParticipant) {
                btnJoinLeaveEvent.setText(R.string.btn_leave_event);
            } else {
                btnJoinLeaveEvent.setText(R.string.btn_join_event);
            }
        } else {
            btnJoinLeaveEvent.setVisibility(View.GONE);
        }

        if (isOrganizer) {
            btnInvite.setVisibility(View.VISIBLE);
            btnUploadPhoto.setVisibility(View.VISIBLE);
            btnDeleteEvent.setVisibility(View.VISIBLE);
        } else {
            btnInvite.setVisibility(View.GONE);
            btnUploadPhoto.setVisibility(View.GONE);
            btnDeleteEvent.setVisibility(View.GONE);
        }

        boolean isLoadingAction = eventDetailViewModel.eventActionState.getValue() instanceof ResultWrapper.Loading;
        btnJoinLeaveEvent.setEnabled(!isLoadingAction);
        btnGetDirections.setEnabled(!isLoadingAction);
        btnInvite.setEnabled(!isLoadingAction);
        btnUploadPhoto.setEnabled(!isLoadingAction);
        btnDeleteEvent.setEnabled(!isLoadingAction);
    }

    private boolean isOrganizer(EventModel event, Long userId) {
        return event != null && event.getOrganizer() != null && userId != null && userId.equals(event.getOrganizer().getId());
    }


    private void handleVisibility(View view, boolean isVisible) {
        if (view != null) {
            view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    @Override
    public void onParticipantClick(UserModel participant) {
        if (participant != null && participant.getId() != null) {
            participantViewModel.viewParticipantProfile(participant.getId());
        }
    }

    @Override
    public void onRemoveClick(UserModel participant) {
        if (participant != null && participant.getId() != null && currentAuthToken != null) {
            if (isOrganizer(currentEvent, loggedInUserId)) {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.dialog_title_remove_participant)
                        .setMessage(getString(R.string.dialog_message_remove_participant_confirm, participant.getName() != null ? participant.getName() : participant.getLogin()))
                        .setPositiveButton(R.string.dialog_button_remove, (dialog, which) ->
                                eventDetailViewModel.deleteParticipant(participant.getId(), currentAuthToken))
                        .setNegativeButton(R.string.dialog_button_cancel, null)
                        .show();
            } else {
                Toast.makeText(getContext(), R.string.organizer_only_remove_participants, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), R.string.cannot_remove_participant_missing_data, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPhotoClick(MediaModel mediaItem, View sharedImageView) {
        if (getContext() != null) {
            Toast.makeText(getContext(), getString(R.string.viewing_photo_not_implemented, mediaItem.getMediaId().toString()), Toast.LENGTH_SHORT).show();
        }
    }
}