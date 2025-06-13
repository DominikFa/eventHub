package com.example.event_hub.View.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.event_hub.Model.InvitationModel;
import com.example.event_hub.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder> {

    private List<InvitationModel> invitationList;
    private final OnInvitationActionListener listener;
    private List<com.example.event_hub.Model.EventModel> eventListCache;

    public interface OnInvitationActionListener {
        void onAcceptInvitation(InvitationModel invitation);
        void onDeclineInvitation(InvitationModel invitation);
        void onInvitationClick(InvitationModel invitation);
    }

    public InvitationAdapter(List<InvitationModel> initialInvitationList,
                             List<com.example.event_hub.Model.EventModel> eventListCache,
                             OnInvitationActionListener listener) {
        this.invitationList = new ArrayList<>(initialInvitationList);
        this.eventListCache = new ArrayList<>(eventListCache != null ? eventListCache : new ArrayList<>());
        this.listener = listener;
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invitation_card, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        InvitationModel invitation = invitationList.get(position);
        String eventTitle = "Unknown Event";
        if (invitation.getEvent() != null) {
            eventTitle = "Event ID: " + invitation.getEvent().getId();
            if (eventListCache != null) {
                for (com.example.event_hub.Model.EventModel event : eventListCache) {
                    if (event.getId().equals(invitation.getEvent().getId())) {
                        eventTitle = event.getName();
                        break;
                    }
                }
            }
        }
        holder.bind(invitation, eventTitle, listener);
    }

    @Override
    public int getItemCount() {
        return invitationList != null ? invitationList.size() : 0;
    }

    public void updateInvitations(List<InvitationModel> newInvitationList, List<com.example.event_hub.Model.EventModel> newEventListCache) {
        final InvitationDiffCallback diffCallback = new InvitationDiffCallback(this.invitationList, newInvitationList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.invitationList.clear();
        this.invitationList.addAll(newInvitationList);
        if (newEventListCache != null) {
            this.eventListCache.clear();
            this.eventListCache.addAll(newEventListCache);
        }
        diffResult.dispatchUpdatesTo(this);
    }

    static class InvitationViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle, tvInvitationStatus, tvSentDate;
        Button btnAccept, btnDecline;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tv_invitation_event_title);
            tvInvitationStatus = itemView.findViewById(R.id.tv_invitation_status);
            tvSentDate = itemView.findViewById(R.id.tv_invitation_sent_date);
            btnAccept = itemView.findViewById(R.id.btn_accept_invitation);
            btnDecline = itemView.findViewById(R.id.btn_decline_invitation);
        }

        public void bind(final InvitationModel invitation, final String eventTitle, final OnInvitationActionListener listener) {
            tvEventTitle.setText(eventTitle != null ? eventTitle : "Unknown Event");
            tvInvitationStatus.setText("Status: " + invitation.getInvitationStatus());
            if (invitation.getSentAt() != null) {
                tvSentDate.setText("Sent: " + dateFormat.format(invitation.getSentAt()));
            } else {
                tvSentDate.setText("Sent: N/A");
            }

            if ("sent".equalsIgnoreCase(invitation.getInvitationStatus())) {
                btnAccept.setVisibility(View.VISIBLE);
                btnDecline.setVisibility(View.VISIBLE);
            } else {
                btnAccept.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
            }

            btnAccept.setOnClickListener(v -> listener.onAcceptInvitation(invitation));
            btnDecline.setOnClickListener(v -> listener.onDeclineInvitation(invitation));
            itemView.setOnClickListener(v -> listener.onInvitationClick(invitation));
        }
    }

    private static class InvitationDiffCallback extends DiffUtil.Callback {
        private final List<InvitationModel> oldList;
        private final List<InvitationModel> newList;

        public InvitationDiffCallback(List<InvitationModel> oldList, List<InvitationModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getInvitationId().equals(newList.get(newItemPosition).getInvitationId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}