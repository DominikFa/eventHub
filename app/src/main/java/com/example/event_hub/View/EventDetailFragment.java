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
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.R;
import com.example.event_hub.View.adapter.EventPhotoAdapter;
import com.example.event_hub.View.adapter.ParticipantAdapter;
import com.example.event_hub.ViewModel.AuthViewModel;
import com.example.event_hub.ViewModel.EventDetailViewModel;
// If you are using Safe Args and it's set up correctly, uncomment this:
// import com.example.event_hub.EventDetailFragmentArgs;


import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

public class EventDetailFragment extends Fragment {

    private EventDetailViewModel eventDetailViewModel;
    private AuthViewModel authViewModel;

    private TextView tvEventName, tvEventDescription, tvEventScreenTitle;
    private TextView tvEventDateTime, tvEventLocation;
    private RecyclerView rvEventPhotos, rvEventParticipants;
    private TextView tvNoPhotos, tvNoParticipants;
    private MaterialButton btnGetDirections, btnInvite, btnJoinLeaveEvent;
    private ImageView ivUserIcon;
    private ProgressBar pbEventDetailsLoading, pbPhotosLoading, pbParticipantsLoading;

    private EventPhotoAdapter photoAdapter;
    private ParticipantAdapter participantAdapter;

    private String currentEventId;
    private EventModel currentEvent;
    private boolean isCurrentUserParticipant = false;
    private String loggedInUserId;

    public EventDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventDetailViewModel = new ViewModelProvider(this).get(EventDetailViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (getArguments() != null) {
            // Reverted to getArguments().getString("eventId") for broader compatibility.
            // Ensure "eventId" is the key used when passing the argument.
            // If Safe Args (EventDetailFragmentArgs) is correctly configured and built, you can use:
            // currentEventId = EventDetailFragmentArgs.fromBundle(getArguments()).getEventId();
            currentEventId = getArguments().getString("eventId");
        }

        photoAdapter = new EventPhotoAdapter(new ArrayList<>(), (mediaItem, sharedImageView) -> {
            Toast.makeText(getContext(), "Clicked photo: " + mediaItem.getMediaFileReference(), Toast.LENGTH_SHORT).show();
            // TODO: Implement full-screen image view with shared element transition
        });

        participantAdapter = new ParticipantAdapter(
                new ArrayList<>(),
                participant -> { // onParticipantClick
                    if (getView() != null && participant.getUserId() != null) {
                        Toast.makeText(getContext(), "View profile of: " + participant.getLogin(), Toast.LENGTH_SHORT).show();
                        // TODO: Implement navigation to ProfileFragment (pass participant.getUserId())
                        // Example:
                        // NavController navController = Navigation.findNavController(getView());
                        // EventDetailFragmentDirections.ActionGlobalToProfileFragment action =
                        //     EventDetailFragmentDirections.actionGlobalToProfileFragment(participant.getUserId());
                        // navController.navigate(action);
                    }
                },
                participant -> { // onRemoveClick
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.dialog_title_remove_participant)
                            .setMessage(getString(R.string.dialog_message_remove_participant_confirm,
                                    (participant.getUserDetails() != null && participant.getUserDetails().getFullName() != null && !participant.getUserDetails().getFullName().isEmpty()
                                            ? participant.getUserDetails().getFullName() : participant.getLogin())))
                            .setPositiveButton(R.string.dialog_button_remove, (dialog, which) -> {
                                if (currentEventId != null && participant.getUserId() != null) {
                                    eventDetailViewModel.deleteParticipant(participant.getUserId());
                                }
                            })
                            .setNegativeButton(R.string.dialog_button_cancel, null)
                            .show();
                },
                false // Initial canRemove state
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerViews();
        setupClickListeners();
        observeViewModels();

        if (currentEventId != null) {
            eventDetailViewModel.loadEventAllDetails(currentEventId);
        } else {
            Toast.makeText(getContext(), R.string.error_event_id_not_found, Toast.LENGTH_LONG).show();
            if (getView() != null) Navigation.findNavController(getView()).popBackStack();
        }
    }

    private void bindViews(View view) {
        tvEventScreenTitle = view.findViewById(R.id.tv_event_screen_title);
        tvEventName = view.findViewById(R.id.tv_event_name_detail);
        tvEventDescription = view.findViewById(R.id.tv_event_description_detail);
        tvEventDateTime = view.findViewById(R.id.tv_event_date_time_detail);
        tvEventLocation = view.findViewById(R.id.tv_event_location_detail);

        rvEventPhotos = view.findViewById(R.id.rv_event_photos);
        tvNoPhotos = view.findViewById(R.id.tv_no_photos_detail);
        rvEventParticipants = view.findViewById(R.id.rv_event_participants);
        tvNoParticipants = view.findViewById(R.id.tv_no_participants_detail);

        btnGetDirections = view.findViewById(R.id.btn_get_directions_detail);
        btnInvite = view.findViewById(R.id.btn_invite_detail);
        ivUserIcon = view.findViewById(R.id.iv_user_icon_event_detail);

        pbEventDetailsLoading = view.findViewById(R.id.pb_event_details_loading);
        pbPhotosLoading = view.findViewById(R.id.pb_photos_loading);
        pbParticipantsLoading = view.findViewById(R.id.pb_participants_loading);
        btnJoinLeaveEvent = view.findViewById(R.id.btn_join_leave_event_detail);
    }

    private void setupRecyclerViews() {
        rvEventPhotos.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvEventPhotos.setAdapter(photoAdapter);

        rvEventParticipants.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEventParticipants.setAdapter(participantAdapter);
        rvEventParticipants.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        if (getView() == null) return;
        // NavController navController = Navigation.findNavController(getView()); // Initialize once if used multiple times

        btnGetDirections.setOnClickListener(v -> {
            if (currentEvent != null && currentEvent.getLocation() != null) {
                Toast.makeText(getContext(), getString(R.string.navigate_to_directions_toast, currentEvent.getLocation()), Toast.LENGTH_SHORT).show();
                // TODO: Implement navigation to MapFragment with location data
            } else {
                Toast.makeText(getContext(), R.string.location_not_available_toast, Toast.LENGTH_SHORT).show();
            }
        });

        btnInvite.setOnClickListener(v -> {
            Toast.makeText(getContext(), R.string.invite_users_clicked_toast, Toast.LENGTH_SHORT).show();
            // TODO: Implement navigation to an InviteUsersFragment
        });

        ivUserIcon.setOnClickListener(v -> {
            if (authViewModel.isLoggedIn()) {
                Toast.makeText(getContext(), R.string.navigate_to_profile_toast, Toast.LENGTH_SHORT).show();
                // TODO: Implement navigation to ProfileFragment (pass loggedInUserId)
            } else {
                Toast.makeText(getContext(), R.string.navigate_to_login_toast, Toast.LENGTH_SHORT).show();
                // TODO: Implement navigation to LoginActivity/LoginFragment
            }
        });

        if (btnJoinLeaveEvent != null) {
            btnJoinLeaveEvent.setOnClickListener(v -> {
                if (loggedInUserId == null) {
                    Toast.makeText(getContext(), R.string.login_required_to_join_leave_toast, Toast.LENGTH_SHORT).show();
                    // TODO: Navigate to login
                    return;
                }
                if (currentEventId != null) {
                    if (isCurrentUserParticipant) {
                        eventDetailViewModel.leaveCurrentEvent(loggedInUserId);
                    } else {
                        eventDetailViewModel.joinCurrentEvent(loggedInUserId);
                    }
                }
            });
        }
    }

    private void observeViewModels() {
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), userId -> {
            loggedInUserId = userId;
            if (ivUserIcon != null) {
                if (userId != null) {
                    ivUserIcon.setImageResource(R.drawable.ic_profile);
                    ivUserIcon.setContentDescription(getString(R.string.content_desc_profile));
                } else {
                    ivUserIcon.setImageResource(R.drawable.ic_login);
                    ivUserIcon.setContentDescription(getString(R.string.content_desc_login));
                    isCurrentUserParticipant = false; // Reset if user logs out
                    updateJoinLeaveButton();
                }
            }
            if (currentEventId != null && eventDetailViewModel.participantViewModel != null) {
                eventDetailViewModel.participantViewModel.loadParticipants(currentEventId);
            }
        });

        eventDetailViewModel.eventDetailState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbEventDetailsLoading, result instanceof ResultWrapper.Loading);
            if (result instanceof ResultWrapper.Success) {
                // Explicit cast, ensure ResultWrapper.Success holds EventModel
                ResultWrapper.Success<EventModel> successResult = (ResultWrapper.Success<EventModel>) result;
                currentEvent = successResult.getData();
                if (currentEvent != null) {
                    updateEventUI(currentEvent);
                    if (currentEventId != null && eventDetailViewModel.participantViewModel != null) {
                        eventDetailViewModel.participantViewModel.loadParticipants(currentEventId);
                    }
                }
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                String error = errorResult.getMessage();
                Toast.makeText(getContext(), getString(R.string.error_loading_event_details_toast_prefix) + error, Toast.LENGTH_LONG).show();
                if (tvEventName != null) tvEventName.setText(getString(R.string.error_loading_event));
            }
        });

        eventDetailViewModel.participantsState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbParticipantsLoading, result instanceof ResultWrapper.Loading);
            if (rvEventParticipants != null) rvEventParticipants.setVisibility(result instanceof ResultWrapper.Success ? View.VISIBLE : View.GONE);
            if (tvNoParticipants != null) tvNoParticipants.setVisibility(View.GONE);

            if (result instanceof ResultWrapper.Success) {
                ResultWrapper.Success<List<UserModel>> successResult = (ResultWrapper.Success<List<UserModel>>) result;
                List<UserModel> participants = successResult.getData();
                boolean canRemove = currentEvent != null && loggedInUserId != null && loggedInUserId.equals(currentEvent.getCreatedBy());

                isCurrentUserParticipant = false;
                if (participants != null && loggedInUserId != null) {
                    for (UserModel p : participants) {
                        if (loggedInUserId.equals(p.getUserId())) {
                            isCurrentUserParticipant = true;
                            break;
                        }
                    }
                }
                updateJoinLeaveButton();

                if (participants != null && !participants.isEmpty()) {
                    participantAdapter.updateParticipants(participants, canRemove);
                } else {
                    participantAdapter.updateParticipants(new ArrayList<>(), false);
                    if (tvNoParticipants != null) {
                        tvNoParticipants.setVisibility(View.VISIBLE);
                        tvNoParticipants.setText(R.string.no_participants_event_detail);
                    }
                }
            } else if (result instanceof ResultWrapper.Error) {
                if (tvNoParticipants != null) {
                    tvNoParticipants.setVisibility(View.VISIBLE);
                    tvNoParticipants.setText(R.string.error_loading_participants);
                }
            }
        });

        eventDetailViewModel.mediaListState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbPhotosLoading, result instanceof ResultWrapper.Loading);
            if (rvEventPhotos != null) rvEventPhotos.setVisibility(result instanceof ResultWrapper.Success ? View.VISIBLE : View.GONE);
            if (tvNoPhotos != null) tvNoPhotos.setVisibility(View.GONE);

            if (result instanceof ResultWrapper.Success) {
                ResultWrapper.Success<List<MediaModel>> successResult = (ResultWrapper.Success<List<MediaModel>>) result;
                List<MediaModel> mediaItems = successResult.getData();
                if (mediaItems != null && !mediaItems.isEmpty()) {
                    photoAdapter.updateMedia(mediaItems);
                } else {
                    photoAdapter.updateMedia(new ArrayList<>());
                    if (tvNoPhotos != null) {
                        tvNoPhotos.setVisibility(View.VISIBLE);
                        tvNoPhotos.setText(R.string.no_photos_event_detail);
                    }
                }
            } else if (result instanceof ResultWrapper.Error) {
                if (tvNoPhotos != null) {
                    tvNoPhotos.setVisibility(View.VISIBLE);
                    tvNoPhotos.setText(R.string.error_loading_photos);
                }
            }
        });

        eventDetailViewModel.eventActionState.observe(getViewLifecycleOwner(), result -> {
            if (btnJoinLeaveEvent != null) btnJoinLeaveEvent.setEnabled(!(result instanceof ResultWrapper.Loading));
            if (result instanceof ResultWrapper.Loading && btnJoinLeaveEvent != null) {
                btnJoinLeaveEvent.setText(R.string.processing_button_text);
            } else if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), R.string.action_successful_toast, Toast.LENGTH_SHORT).show();
                // Participant list and button state will refresh via participantsState observer
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                String error = errorResult.getMessage();
                Toast.makeText(getContext(), getString(R.string.action_failed_toast_prefix) + error, Toast.LENGTH_SHORT).show();
                updateJoinLeaveButton();
            }
        });

        eventDetailViewModel.participantActionStatus.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ResultWrapper.Success) {
                Toast.makeText(getContext(), R.string.participant_action_successful_toast, Toast.LENGTH_SHORT).show();
            } else if (result instanceof ResultWrapper.Error) {
                ResultWrapper.Error<?> errorResult = (ResultWrapper.Error<?>) result;
                String error = errorResult.getMessage();
                Toast.makeText(getContext(), getString(R.string.participant_action_failed_toast_prefix) + error, Toast.LENGTH_SHORT).show();
            }
        });

        eventDetailViewModel.getNavigateToParticipantProfileId().observe(getViewLifecycleOwner(), participantId -> {
            if (participantId != null && getView() != null) {
                Toast.makeText(getContext(), "Navigate to profile of: " + participantId, Toast.LENGTH_SHORT).show();
                // TODO: Implement actual navigation
            }
        });
    }

    private void updateEventUI(EventModel event) {
        if (tvEventScreenTitle != null) tvEventScreenTitle.setText(event.getTitle());
        if (tvEventName != null) tvEventName.setText(event.getTitle());
        if (tvEventDescription != null) tvEventDescription.setText(event.getDescription());

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()); // Corrected date format
        String startDateStr = event.getStartDate() != null ? dateTimeFormat.format(event.getStartDate()) : getString(R.string.not_available_placeholder);
        String endDateStr = event.getEndDate() != null ? dateTimeFormat.format(event.getEndDate()) : getString(R.string.not_available_placeholder);
        if (tvEventDateTime != null) tvEventDateTime.setText(getString(R.string.event_date_range_format, startDateStr, endDateStr));
        if (tvEventLocation != null) tvEventLocation.setText(event.getLocation() != null ? event.getLocation() : getString(R.string.not_available_placeholder));
    }

    private void updateJoinLeaveButton() {
        if (btnJoinLeaveEvent != null) {
            if (isCurrentUserParticipant) {
                btnJoinLeaveEvent.setText(R.string.leave_event_button);
            } else {
                btnJoinLeaveEvent.setText(R.string.join_event_button);
            }
        }
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) {
            view.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
