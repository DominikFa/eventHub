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
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar; // For isSameDay
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class EventDetailFragment extends Fragment {

    private EventDetailViewModel eventDetailViewModel;
    private AuthViewModel authViewModel;

    private TextView tvEventName, tvEventDescription, tvEventScreenTitle;
    private TextView tvEventDateTime, tvEventLocation;
    private RecyclerView rvEventPhotos, rvEventParticipants;
    private TextView tvNoPhotos, tvNoParticipants;
    private MaterialButton btnGetDirections, btnInvite, btnJoinLeaveEvent, btnUploadPhoto, btnDeleteEvent;
    private ImageView ivUserIcon;
    private ProgressBar pbEventDetailsLoading, pbPhotosLoading, pbParticipantsLoading;

    private EventPhotoAdapter photoAdapter;
    private ParticipantAdapter participantAdapter;

    private String currentEventId;
    private EventModel currentEvent;
    private boolean isCurrentUserParticipant = false;
    private String loggedInUserId;
    private String currentAuthToken;

    public EventDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventDetailViewModel = new ViewModelProvider(this).get(EventDetailViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (getArguments() != null) {
            currentEventId = getArguments().getString("eventId");
        }
        setupAdapters();
    }

    private void setupAdapters() {
        photoAdapter = new EventPhotoAdapter(new ArrayList<>(), (mediaItem, sharedImageView) -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Photo: " + (mediaItem.getFileName() != null ? mediaItem.getFileName() : "Image"))
                    .setMessage("ID: " + mediaItem.getMediaId())
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (currentAuthToken == null) {
                            Toast.makeText(getContext(), "Login to delete photo.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete this photo?")
                                .setPositiveButton("Yes", (d, w) -> eventDetailViewModel.deleteMediaFromCurrentEvent(mediaItem.getMediaId(), currentAuthToken))
                                .setNegativeButton("No", null)
                                .show();
                    })
                    .setNegativeButton("View (TBD)", (dialog, which) -> Toast.makeText(getContext(), "View full screen TBD", Toast.LENGTH_SHORT).show())
                    .setNeutralButton("Cancel", null)
                    .show();
        });

        participantAdapter = new ParticipantAdapter(
                new ArrayList<>(),
                participant -> {
                    if (getView() != null && participant.getUserId() != null) {
                        NavController navController = Navigation.findNavController(getView());
                        Bundle args = new Bundle();
                        args.putString("userId", participant.getUserId());
                        navController.navigate(R.id.action_eventDetailFragment_to_profileFragment, args);
                    }
                },
                participant -> {
                    if (currentAuthToken == null) {
                        Toast.makeText(getContext(), "Login required for this action.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.dialog_title_remove_participant)
                            .setMessage(getString(R.string.dialog_message_remove_participant_confirm,
                                    (participant.getUserDetails() != null && participant.getUserDetails().getFullName() != null && !participant.getUserDetails().getFullName().isEmpty()
                                            ? participant.getUserDetails().getFullName() : participant.getLogin())))
                            .setPositiveButton(R.string.dialog_button_remove, (dialog, which) -> {
                                eventDetailViewModel.deleteParticipant(participant.getUserId(), currentAuthToken);
                            })
                            .setNegativeButton(R.string.dialog_button_cancel, null)
                            .show();
                },
                false
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventDetailViewModel != null) {
            eventDetailViewModel.clearStates();
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
        btnUploadPhoto = view.findViewById(R.id.btn_upload_photo_detail); // ID from XML
        btnDeleteEvent = view.findViewById(R.id.btn_delete_event_detail); // ID from XML
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
        NavController navController = Navigation.findNavController(getView());

        btnGetDirections.setOnClickListener(v -> {
            if (currentEvent != null && currentEvent.getLocation() != null && !currentEvent.getLocation().isEmpty()) {
                Bundle args = new Bundle();
                args.putString("eventLocation", currentEvent.getLocation());
                if (currentEventId != null) args.putString("eventId", currentEventId);
                navController.navigate(R.id.action_eventDetailFragment_to_mapFragment, args);
            } else {
                Toast.makeText(getContext(), R.string.location_not_available_toast, Toast.LENGTH_SHORT).show();
            }
        });

        ivUserIcon.setOnClickListener(v -> {
            if (currentAuthToken != null && loggedInUserId != null) {
                Bundle args = new Bundle();
                args.putString("userId", loggedInUserId);
                navController.navigate(R.id.action_eventDetailFragment_to_profileFragment, args);
            } else {
                navController.navigate(R.id.action_eventDetailFragment_to_loginActivity);
            }
        });

        btnInvite.setOnClickListener(v -> {
            if (currentEventId == null) { Toast.makeText(getContext(), R.string.error_event_id_not_found, Toast.LENGTH_SHORT).show(); return; }
            if (currentAuthToken == null) {
                Toast.makeText(getContext(), "Login to invite users.", Toast.LENGTH_SHORT).show();
                navController.navigate(R.id.action_eventDetailFragment_to_loginActivity); return;
            }
            // Attempt navigation; repository will perform permission check via token
            Bundle args = new Bundle();
            args.putString("eventId", currentEventId);
            navController.navigate(R.id.action_eventDetailFragment_to_inviteUsersFragment, args);
        });

        btnUploadPhoto.setOnClickListener(v -> {
            if (currentEventId == null || currentAuthToken == null) {
                Toast.makeText(getContext(), "Login and select event to upload photos.", Toast.LENGTH_SHORT).show();
                if(currentAuthToken == null) navController.navigate(R.id.action_eventDetailFragment_to_loginActivity);
                return;
            }
            // TODO: Implement actual image picker logic (e.g., using ActivityResultLauncher)
            MediaModel newMedia = new MediaModel();
            newMedia.setFileName("upload_" + System.currentTimeMillis() + ".jpg");
            newMedia.setMediaType("image/jpeg");
            newMedia.setUsage("gallery");
            // mediaFileReference will be a placeholder URL set by the repository in the simulated upload
            newMedia.setMediaFileReference(null);
            eventDetailViewModel.uploadMediaToCurrentEvent(newMedia, currentAuthToken);
        });

        btnDeleteEvent.setOnClickListener(v -> {
            if (currentEventId == null || currentAuthToken == null) {
                Toast.makeText(getContext(), "Login and event context required to delete.", Toast.LENGTH_SHORT).show();
                if(currentAuthToken == null) navController.navigate(R.id.action_eventDetailFragment_to_loginActivity);
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
                    .setPositiveButton("Yes, Delete", (dialog, which) -> {
                        eventDetailViewModel.deleteCurrentEvent(currentAuthToken);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnJoinLeaveEvent.setOnClickListener(v -> {
            if (currentAuthToken == null) {
                Toast.makeText(getContext(), R.string.login_required_to_join_leave_toast, Toast.LENGTH_SHORT).show();
                navController.navigate(R.id.action_eventDetailFragment_to_loginActivity); return;
            }
            if (currentEventId != null) {
                if (isCurrentUserParticipant) {
                    eventDetailViewModel.leaveCurrentEvent(currentAuthToken);
                } else {
                    eventDetailViewModel.joinCurrentEvent(currentAuthToken);
                }
            }
        });
    }

    private void observeViewModels() {
        authViewModel.currentUserId.observe(getViewLifecycleOwner(), id -> {
            loggedInUserId = id;
            updateButtonStates();
            if (currentEventId != null) { // Re-check participation status on user change
                eventDetailViewModel.participantViewModel.loadParticipants(currentEventId);
            }
        });
        authViewModel.currentJwtToken.observe(getViewLifecycleOwner(), token -> {
            currentAuthToken = token;
            updateButtonStates();
        });

        eventDetailViewModel.eventDetailState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbEventDetailsLoading, result instanceof ResultWrapper.Loading);
            if (result instanceof ResultWrapper.Success) {
                currentEvent = ((ResultWrapper.Success<EventModel>) result).getData();
                if (currentEvent != null) {
                    updateEventUI(currentEvent);
                    updateButtonStates();
                } else {
                    Toast.makeText(getContext(), "Event data not found or was deleted.", Toast.LENGTH_SHORT).show();
                    if(getView() != null && Navigation.findNavController(getView()).getCurrentDestination().getId() == R.id.eventDetailFragment) {
                        Navigation.findNavController(getView()).popBackStack();
                    }
                }
            } else if (result instanceof ResultWrapper.Error) {
                String error = ((ResultWrapper.Error<?>) result).getMessage();
                Toast.makeText(getContext(), getString(R.string.error_loading_event_details_toast_prefix) + (error!=null?error:""), Toast.LENGTH_LONG).show();
                if(getView() != null && Navigation.findNavController(getView()).getCurrentDestination().getId() == R.id.eventDetailFragment) {
                    Navigation.findNavController(getView()).popBackStack();
                }
            }
        });

        eventDetailViewModel.participantsState.observe(getViewLifecycleOwner(), result -> {
            handleVisibility(pbParticipantsLoading, result instanceof ResultWrapper.Loading);
            rvEventParticipants.setVisibility(View.GONE); tvNoParticipants.setVisibility(View.GONE);

            if (result instanceof ResultWrapper.Success) {
                List<UserModel> participants = ((ResultWrapper.Success<List<UserModel>>) result).getData();
                boolean canUiHintManageParticipants = currentEvent != null && loggedInUserId != null && loggedInUserId.equals(currentEvent.getCreatedBy());

                isCurrentUserParticipant = false;
                if (participants != null && loggedInUserId != null) {
                    for (UserModel p : participants) if (loggedInUserId.equals(p.getUserId())) { isCurrentUserParticipant = true; break; }
                }
                updateButtonStates(); // This calls updateJoinLeaveButtonVisibilityAndText

                if (participants != null && !participants.isEmpty()) {
                    participantAdapter.updateParticipants(participants, canUiHintManageParticipants);
                    rvEventParticipants.setVisibility(View.VISIBLE);
                } else {
                    participantAdapter.updateParticipants(new ArrayList<>(), false);
                    tvNoParticipants.setVisibility(View.VISIBLE);
                    tvNoParticipants.setText(R.string.no_participants_event_detail);
                }
            } else if (result instanceof ResultWrapper.Error) { /* ... handle error ... */ }
            else if (result instanceof ResultWrapper.Idle) { /* ... handle idle ... */ }
        });

        eventDetailViewModel.mediaListState.observe(getViewLifecycleOwner(), result -> { /* ... as before ... */});

        eventDetailViewModel.eventActionState.observe(getViewLifecycleOwner(), result -> {
            btnJoinLeaveEvent.setEnabled(!(result instanceof ResultWrapper.Loading));
            btnDeleteEvent.setEnabled(!(result instanceof ResultWrapper.Loading));

            if (!(result instanceof ResultWrapper.Loading || result instanceof ResultWrapper.Idle)) {
                if (currentEventId != null) { // Refresh participants after join/leave/delete event
                    eventDetailViewModel.participantViewModel.loadParticipants(currentEventId);
                }
                if (result instanceof ResultWrapper.Success) {
                    Toast.makeText(getContext(), "Event action successful!", Toast.LENGTH_SHORT).show();
                    if (eventDetailViewModel.wasLastActionDeleteSuccess(result)) { // Check if event was deleted
                        if(getView() != null && Navigation.findNavController(getView()).getCurrentDestination().getId() == R.id.eventDetailFragment) {
                            Navigation.findNavController(getView()).popBackStack();
                        }
                    } else {
                        // If join/leave, reload event details to get any potential updates
                        if (currentEventId != null) eventDetailViewModel.loadEventAllDetails(currentEventId);
                    }
                } else if (result instanceof ResultWrapper.Error) {
                    String error = ((ResultWrapper.Error<?>) result).getMessage();
                    Toast.makeText(getContext(), "Event action failed: " + (error != null ? error : "Unknown error"), Toast.LENGTH_SHORT).show();
                }
                updateButtonStates();
            }
        });

        eventDetailViewModel.participantActionStatus.observe(getViewLifecycleOwner(), result -> {
            if (!(result instanceof ResultWrapper.Loading || result instanceof ResultWrapper.Idle)) {
                if (result instanceof ResultWrapper.Success) {
                    Toast.makeText(getContext(), R.string.participant_action_successful_toast, Toast.LENGTH_SHORT).show();
                    // Participant list is refreshed by repo calling fetchEventParticipants
                } else if (result instanceof ResultWrapper.Error) { /* ... handle error ... */ }
            }
        });
        eventDetailViewModel.mediaUploadOperationState.observe(getViewLifecycleOwner(), result -> { /* ... handle media upload status ... */});
        eventDetailViewModel.mediaDeleteOperationState.observe(getViewLifecycleOwner(), result -> { /* ... handle media delete status ... */});
        eventDetailViewModel.getNavigateToParticipantProfileId().observe(getViewLifecycleOwner(), participantId -> { /* ... as before ... */});
    }

    private void updateEventUI(EventModel event) {
        if (getContext() == null || event == null) return; // Guard against null context or event
        tvEventScreenTitle.setText(event.getTitle());
        tvEventName.setText(event.getTitle());
        tvEventDescription.setText(event.getDescription());
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        String startDateStr = event.getStartDate() != null ? dateTimeFormat.format(event.getStartDate()) : getString(R.string.not_available_placeholder);
        String endDateStr = event.getEndDate() != null ? dateTimeFormat.format(event.getEndDate()) : getString(R.string.not_available_placeholder);
        if (event.getStartDate() != null && event.getEndDate() != null) {
            if (isSameDay(event.getStartDate(), event.getEndDate())) {
                SimpleDateFormat justTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat justDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                tvEventDateTime.setText(justDateFormat.format(event.getStartDate()) + ", " + justTimeFormat.format(event.getStartDate()) + " - " + justTimeFormat.format(event.getEndDate()));
            } else {
                tvEventDateTime.setText(getString(R.string.event_date_range_format, startDateStr, endDateStr));
            }
        } else if (event.getStartDate() != null) {
            tvEventDateTime.setText(startDateStr);
        } else {
            tvEventDateTime.setText(getString(R.string.not_available_placeholder));
        }
        tvEventLocation.setText(event.getLocation() != null ? event.getLocation() : getString(R.string.not_available_placeholder));
    }

    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void updateButtonStates() {
        updateJoinLeaveButtonVisibilityAndText();
        updateAdminActionButtonsVisibility();
    }

    private void updateJoinLeaveButtonVisibilityAndText() {
        if (currentAuthToken == null) {
            btnJoinLeaveEvent.setText(R.string.join_event_button);
            btnJoinLeaveEvent.setEnabled(true);
        } else {
            btnJoinLeaveEvent.setEnabled(currentEvent != null); // Enable if event data is loaded
            if (isCurrentUserParticipant) {
                btnJoinLeaveEvent.setText(R.string.leave_event_button);
            } else {
                btnJoinLeaveEvent.setText(R.string.join_event_button);
            }
        }
    }

    private void updateAdminActionButtonsVisibility() {
        boolean isLoggedIn = currentAuthToken != null;
        // Visibility for UX. Repository makes the final permission decision.
        // Show if logged in AND event data is available.
        btnInvite.setVisibility(isLoggedIn && currentEvent != null ? View.VISIBLE : View.GONE);
        btnUploadPhoto.setVisibility(isLoggedIn && currentEvent != null ? View.VISIBLE : View.GONE);
        btnDeleteEvent.setVisibility(isLoggedIn && currentEvent != null ? View.VISIBLE : View.GONE);
    }

    private void handleVisibility(View view, boolean isLoading) {
        if (view != null) { view.setVisibility(isLoading ? View.VISIBLE : View.GONE); }
    }
}