package com.example.event_hub.View.adapter;

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
    private boolean canRemoveParticipants;

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
        this.participants = new ArrayList<>(initialParticipants);
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
        this.canRemoveParticipants = canRemove;
        diffResult.dispatchUpdatesTo(this);
    }

    // Added public getter for participants list
    public List<UserModel> getParticipants() {
        return new ArrayList<>(participants); // Return a copy to prevent external modification
    }


    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        TextView tvParticipantName;
        TextView tvParticipantAvatar;
        ImageButton btnRemoveParticipant;
        Random random = new Random();

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
            String displayName;
            String avatarChar;

            if (participant.getName() != null && !participant.getName().isEmpty()) {
                displayName = participant.getName();
                avatarChar = String.valueOf(displayName.charAt(0)).toUpperCase();
            } else if (participant.getLogin() != null && !participant.getLogin().isEmpty()) {
                displayName = participant.getLogin();
                avatarChar = String.valueOf(displayName.charAt(0)).toUpperCase();
            } else {
                displayName = "Unknown User";
                avatarChar = "U";
            }

            tvParticipantName.setText(displayName);
            tvParticipantAvatar.setText(avatarChar);

            int R = random.nextInt(156) + 100;
            int G = random.nextInt(156) + 100;
            int B = random.nextInt(156) + 100;
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
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            UserModel oldParticipant = oldList.get(oldItemPosition);
            UserModel newParticipant = newList.get(newItemPosition);
            return Objects.equals(oldParticipant.getLogin(), newParticipant.getLogin()) &&
                    Objects.equals(oldParticipant.getName(), newParticipant.getName()) &&
                    Objects.equals(oldParticipant.getRole(), newParticipant.getRole()) &&
                    Objects.equals(oldParticipant.getStatus(), newParticipant.getStatus()) &&
                    Objects.equals(oldParticipant.getProfileImageUrl(), newParticipant.getProfileImageUrl()) &&
                    Objects.equals(oldParticipant.getDescription(), newParticipant.getDescription());
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}