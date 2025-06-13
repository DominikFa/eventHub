package com.example.event_hub.View.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
        void onPhotoClick(MediaModel mediaItem, View sharedImageView);
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
            String transitionName = "photo_" + mediaItem.getMediaId();
            ivEventPhoto.setTransitionName(transitionName);

            // Corrected: Use getDownloadUrl() instead of getMediaFileReference()
            if (mediaItem.getDownloadUrl() != null && !mediaItem.getDownloadUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(mediaItem.getDownloadUrl()) // Use getDownloadUrl()
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_placeholder_image)
                                .error(R.drawable.ic_placeholder_image_error)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerCrop())
                        .into(ivEventPhoto);
            } else {
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_placeholder_image)
                        .centerCrop()
                        .into(ivEventPhoto);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPhotoClick(mediaItem, ivEventPhoto);
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
            // Corrected: Use getDownloadUrl() instead of getMediaFileReference()
            // Removed comparison for getDescription() as MediaModel does not have this field.
            return Objects.equals(oldMedia.getDownloadUrl(), newMedia.getDownloadUrl()) &&
                    Objects.equals(oldMedia.getMediaType(), newMedia.getMediaType());
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}