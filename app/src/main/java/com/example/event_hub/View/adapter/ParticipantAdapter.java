package com.example.event_hub.View.adapter; // Or your preferred adapter package

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_hub.Model.UserModel;
import com.example.event_hub.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    private List<UserModel> participants;
    private final OnParticipantClickListener onParticipantClickListener;
    private final OnRemoveParticipantClickListener onRemoveParticipantClickListener;
    private boolean canRemoveParticipants; // To control visibility of remove button

    public interface OnParticipantClickListener {
        void onParticipantClick(UserModel participant);
    }

    public interface OnRemoveParticipantClickListener {
        void onRemoveClick(UserModel participant);
    }

    public ParticipantAdapter(List<UserModel> initialParticipants,
                              OnParticipantClickListener onParticipantClickListener,
                              OnRemoveParticipantClickListener onRemoveParticipantClickListener,
                              boolean canRemoveParticipants) {
        this.participants = new ArrayList<>(initialParticipants); // Work with a copy
        this.onParticipantClickListener = onParticipantClickListener;
        this.onRemoveParticipantClickListener = onRemoveParticipantClickListener;
        this.canRemoveParticipants = canRemoveParticipants;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        UserModel participant = participants.get(position);
        holder.bind(participant, onParticipantClickListener, onRemoveParticipantClickListener, canRemoveParticipants);
    }

    @Override
    public int getItemCount() {
        return participants != null ? participants.size() : 0;
    }

    public void updateParticipants(List<UserModel> newParticipants, boolean canRemove) {
        final ParticipantDiffCallback diffCallback = new ParticipantDiffCallback(this.participants, newParticipants);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.participants.clear();
        this.participants.addAll(newParticipants);
        this.canRemoveParticipants = canRemove; // Update permission flag as well
        diffResult.dispatchUpdatesTo(this);
    }

    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        TextView tvParticipantName;
        TextView tvParticipantAvatar;
        ImageButton btnRemoveParticipant;
        Random random = new Random(); // For random avatar colors

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvParticipantName = itemView.findViewById(R.id.tv_participant_name);
            tvParticipantAvatar = itemView.findViewById(R.id.tv_participant_avatar);
            btnRemoveParticipant = itemView.findViewById(R.id.btn_remove_participant);
        }

        public void bind(final UserModel participant,
                         final OnParticipantClickListener clickListener,
                         final OnRemoveParticipantClickListener removeClickListener,
                         boolean canRemove) {
            if (participant.getUserDetails() != null && participant.getUserDetails().getFullName() != null && !participant.getUserDetails().getFullName().isEmpty()) {
                tvParticipantName.setText(participant.getUserDetails().getFullName());
                if (!participant.getUserDetails().getFullName().isEmpty()) {
                    tvParticipantAvatar.setText(String.valueOf(participant.getUserDetails().getFullName().charAt(0)).toUpperCase());
                } else {
                    tvParticipantAvatar.setText("?");
                }
            } else if (participant.getLogin() != null && !participant.getLogin().isEmpty()) {
                tvParticipantName.setText(participant.getLogin()); // Fallback to login/username
                tvParticipantAvatar.setText(String.valueOf(participant.getLogin().charAt(0)).toUpperCase());
            } else {
                tvParticipantName.setText("Unknown User");
                tvParticipantAvatar.setText("U");
            }

            // Set a random background color for the avatar for visual distinction
            // In a real app, you might have profile images or a more consistent color scheme
            int R = random.nextInt(156) + 100; // Between 100 and 255
            int G = random.nextInt(156) + 100;
            int B = random.nextInt(156) + 100;
            // Ensure avatar background drawable is suitable for tinting
            if (tvParticipantAvatar.getBackground() != null) {
                tvParticipantAvatar.getBackground().mutate().setTint(Color.rgb(R,G,B));
            }


            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onParticipantClick(participant);
                }
            });

            if (canRemove) {
                btnRemoveParticipant.setVisibility(View.VISIBLE);
                btnRemoveParticipant.setOnClickListener(v -> {
                    if (removeClickListener != null) {
                        removeClickListener.onRemoveClick(participant);
                    }
                });
            } else {
                btnRemoveParticipant.setVisibility(View.GONE);
            }
        }
    }

    // DiffUtil.Callback implementation
    private static class ParticipantDiffCallback extends DiffUtil.Callback {
        private final List<UserModel> oldList;
        private final List<UserModel> newList;

        public ParticipantDiffCallback(List<UserModel> oldList, List<UserModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Check if items represent the same object, typically by unique ID
            return oldList.get(oldItemPosition).getUserId().equals(newList.get(newItemPosition).getUserId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Check if the content of the items is the same
            // This depends on what changes should trigger a rebind vs. full item change
            UserModel oldParticipant = oldList.get(oldItemPosition);
            UserModel newParticipant = newList.get(newItemPosition);
            // Implement a proper equals in UserModel or compare relevant fields
            return oldParticipant.equals(newParticipant) &&
                    Objects.equals(oldParticipant.getUserDetails(), newParticipant.getUserDetails());
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            // Optional: Implement for more granular updates if areContentsTheSame is false
            // but you want to animate specific parts of the view.
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}
