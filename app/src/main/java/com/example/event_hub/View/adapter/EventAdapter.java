package com.example.event_hub.View.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_hub.Model.EventModel;
import com.example.event_hub.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<EventModel> eventList;
    private final OnEventClickListener onEventClickListener;

    public interface OnEventClickListener {
        void onEventClick(EventModel event);
    }

    public EventAdapter(List<EventModel> initialEventList, OnEventClickListener onEventClickListener) {
        this.eventList = new ArrayList<>(initialEventList);
        this.onEventClickListener = onEventClickListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventModel event = eventList.get(position);
        holder.bind(event, onEventClickListener);
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    public void updateEvents(List<EventModel> newEventList) {
        final EventDiffCallback diffCallback = new EventDiffCallback(this.eventList, newEventList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.eventList.clear();
        this.eventList.addAll(newEventList);
        diffResult.dispatchUpdatesTo(this);
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle, tvEventDescription, tvEventDate, tvEventLocation;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());


        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tv_event_title_card);
            tvEventDescription = itemView.findViewById(R.id.tv_event_description_card);
            tvEventDate = itemView.findViewById(R.id.tv_event_date_card);
            tvEventLocation = itemView.findViewById(R.id.tv_event_location_card);
        }

        public void bind(final EventModel event, final OnEventClickListener clickListener) {
            tvEventTitle.setText(event.getName());
            tvEventDescription.setText(event.getDescription());

            String dateString = itemView.getContext().getString(R.string.not_available_placeholder);
            if (event.getStartDate() != null) {
                dateString = dateFormat.format(event.getStartDate());
                if (event.getEndDate() != null && !isSameDay(event.getStartDate(), event.getEndDate())) {
                    dateString += " - " + dateFormat.format(event.getEndDate());
                } else if (event.getStartDate() != null){
                    dateString = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(event.getStartDate());
                    dateString += ", " + timeFormat.format(event.getStartDate());
                    if (event.getEndDate() != null) {
                        dateString += " - " + timeFormat.format(event.getEndDate());
                    }
                }
            }
            tvEventDate.setText(dateString);

            if (event.getLocation() != null && event.getLocation().getFullAddress() != null && !event.getLocation().getFullAddress().isEmpty()) {
                tvEventLocation.setText(event.getLocation().getFullAddress());
                tvEventLocation.setVisibility(View.VISIBLE);
            } else {
                tvEventLocation.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onEventClick(event);
                }
            });
        }

        private boolean isSameDay(java.util.Date date1, java.util.Date date2) {
            if (date1 == null || date2 == null) {
                return false;
            }
            java.util.Calendar cal1 = java.util.Calendar.getInstance();
            java.util.Calendar cal2 = java.util.Calendar.getInstance();
            cal1.setTime(date1);
            cal2.setTime(date2);
            return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                    cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
        }
    }

    private static class EventDiffCallback extends DiffUtil.Callback {
        private final List<EventModel> oldList;
        private final List<EventModel> newList;

        public EventDiffCallback(List<EventModel> oldList, List<EventModel> newList) {
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
            EventModel oldEvent = oldList.get(oldItemPosition);
            EventModel newEvent = newList.get(newItemPosition);
            return Objects.equals(oldEvent.getName(), newEvent.getName()) &&
                    Objects.equals(oldEvent.getDescription(), newEvent.getDescription()) &&
                    Objects.equals(oldEvent.getLocation(), newEvent.getLocation()) &&
                    Objects.equals(oldEvent.getStartDate(), newEvent.getStartDate()) &&
                    Objects.equals(oldEvent.getEndDate(), newEvent.getEndDate());
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}