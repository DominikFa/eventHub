package com.example.event_hub.View.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Import Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.event_hub.Model.MediaModel;
import com.example.event_hub.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventPhotoAdapter extends RecyclerView.Adapter<EventPhotoAdapter.EventPhotoViewHolder> {

    private List<MediaModel> mediaItems;
    private final OnPhotoClickListener onPhotoClickListener;

    public interface OnPhotoClickListener {
        void onPhotoClick(MediaModel mediaItem, View sharedImageView); // Added sharedImageView for transitions
    }

    public EventPhotoAdapter(List<MediaModel> initialMediaItems, OnPhotoClickListener onPhotoClickListener) {
        this.mediaItems = new ArrayList<>(initialMediaItems);
        this.onPhotoClickListener = onPhotoClickListener;
    }

    @NonNull
    @Override
    public EventPhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_photo, parent, false);
        return new EventPhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventPhotoViewHolder holder, int position) {
        MediaModel mediaItem = mediaItems.get(position);
        holder.bind(mediaItem, onPhotoClickListener);
    }

    @Override
    public int getItemCount() {
        return mediaItems != null ? mediaItems.size() : 0;
    }

    public void updateMedia(List<MediaModel> newMediaItems) {
        final MediaDiffCallback diffCallback = new MediaDiffCallback(this.mediaItems, newMediaItems);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.mediaItems.clear();
        this.mediaItems.addAll(newMediaItems);
        diffResult.dispatchUpdatesTo(this);
    }

    static class EventPhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventPhoto;

        public EventPhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventPhoto = itemView.findViewById(R.id.iv_event_photo_item);
        }

        public void bind(final MediaModel mediaItem, final OnPhotoClickListener clickListener) {
            // Set a unique transition name for shared element transition
            String transitionName = "photo_" + mediaItem.getMediaId();
            ivEventPhoto.setTransitionName(transitionName);

            if (mediaItem.getMediaFileReference() != null && !mediaItem.getMediaFileReference().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(mediaItem.getMediaFileReference()) // This should be a URL or valid file path
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_placeholder_image) // Placeholder while loading
                                .error(R.drawable.ic_placeholder_image_error) // Error placeholder if load fails
                                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache strategy
                                .centerCrop()) // Or .fitCenter() depending on desired scaling
                        .into(ivEventPhoto);
            } else {
                // Fallback if no image reference
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_placeholder_image)
                        .centerCrop()
                        .into(ivEventPhoto);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPhotoClick(mediaItem, ivEventPhoto); // Pass the ImageView for transition
                }
            });
        }
    }

    private static class MediaDiffCallback extends DiffUtil.Callback {
        private final List<MediaModel> oldList;
        private final List<MediaModel> newList;

        public MediaDiffCallback(List<MediaModel> oldList, List<MediaModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getMediaId().equals(newList.get(newItemPosition).getMediaId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            MediaModel oldMedia = oldList.get(oldItemPosition);
            MediaModel newMedia = newList.get(newItemPosition);
            return Objects.equals(oldMedia.getMediaFileReference(), newMedia.getMediaFileReference()) &&
                    Objects.equals(oldMedia.getDescription(), newMedia.getDescription()) &&
                    Objects.equals(oldMedia.getMediaType(), newMedia.getMediaType()); // Compare more fields if needed
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}
