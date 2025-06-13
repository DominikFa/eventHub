// src/main/java/com/example/event_hub/View/adapter/UserInviteAdapter.java
package com.example.event_hub.View.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.event_hub.Model.UserModel;
import com.example.event_hub.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UserInviteAdapter extends ListAdapter<UserModel, UserInviteAdapter.UserInviteViewHolder> {

    private final OnUserSelectionChangedListener selectionListener;
    private final Set<Long> selectedUserIds = new HashSet<>();

    /**
     * Listener for tracking user selection changes.
     */
    public interface OnUserSelectionChangedListener {
        void onUserSelectionChanged();
    }

    public UserInviteAdapter(@NonNull OnUserSelectionChangedListener listener) {
        super(new UserDiffCallback());
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public UserInviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_invite, parent, false);
        return new UserInviteViewHolder(view, this::onItemClick);
    }

    @Override
    public void onBindViewHolder(@NonNull UserInviteViewHolder holder, int position) {
        UserModel user = getItem(position);
        holder.bind(user, selectedUserIds.contains(user.getId()));
    }

    /**
     * Handles clicks on an item view or checkbox, toggling its selection state.
     * @param position The adapter position of the clicked item.
     */
    private void onItemClick(int position) {
        UserModel user = getItem(position);
        if (user == null || user.getId() == null) return;

        if (selectedUserIds.contains(user.getId())) {
            selectedUserIds.remove(user.getId());
        } else {
            selectedUserIds.add(user.getId());
        }
        notifyItemChanged(position);
        if (selectionListener != null) {
            selectionListener.onUserSelectionChanged();
        }
    }

    /**
     * Clears all selections and updates the UI efficiently.
     */
    public void clearSelections() {
        if (selectedUserIds.isEmpty()) return;

        // Create a copy to avoid ConcurrentModificationException while iterating
        Set<Long> previouslySelected = new HashSet<>(selectedUserIds);
        selectedUserIds.clear();

        for (int i = 0; i < getCurrentList().size(); i++) {
            if (previouslySelected.contains(getCurrentList().get(i).getId())) {
                notifyItemChanged(i);
            }
        }
        if (selectionListener != null) {
            selectionListener.onUserSelectionChanged();
        }
    }

    /**
     * Overrides the default submitList to also handle retaining selection state.
     */
    @Override
    public void submitList(@Nullable List<UserModel> list) {
        if (list != null) {
            Set<Long> newUserIds = list.stream().map(UserModel::getId).collect(Collectors.toSet());
            selectedUserIds.retainAll(newUserIds); // Keep selections only for users present in the new list
        } else {
            selectedUserIds.clear();
        }
        super.submitList(list);
        if (selectionListener != null) {
            selectionListener.onUserSelectionChanged();
        }
    }

    /**
     * @return A list of all currently selected UserModel objects.
     */
    public List<UserModel> getSelectedUsers() {
        List<UserModel> selected = new ArrayList<>();
        for (UserModel user : getCurrentList()) {
            if (selectedUserIds.contains(user.getId())) {
                selected.add(user);
            }
        }
        return selected;
    }


    /**
     * ViewHolder for displaying a single user item for invitation.
     */
    static class UserInviteViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivUserAvatar;
        final TextView tvUserName;
        final TextView tvUserLogin;
        final CheckBox checkboxInviteUser;
        final Context context;

        public UserInviteViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            this.context = itemView.getContext();
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar_invite_item);
            tvUserName = itemView.findViewById(R.id.tv_user_name_invite_item);
            tvUserLogin = itemView.findViewById(R.id.tv_user_login_invite_item);
            checkboxInviteUser = itemView.findViewById(R.id.checkbox_invite_user);

            // Set listeners to call the adapter's click handler
            View.OnClickListener clickListener = v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(position);
                }
            };
            itemView.setOnClickListener(clickListener);
            checkboxInviteUser.setOnClickListener(clickListener);
        }

        public void bind(final UserModel user, boolean isSelected) {
            String displayName = user.getName() != null && !user.getName().isEmpty() ? user.getName() : user.getLogin();
            tvUserName.setText(displayName);
            tvUserLogin.setText(user.getLogin() != null ? "@" + user.getLogin() : "");
            ivUserAvatar.setContentDescription(displayName);

            // Use Glide to load the user's profile image
            String profileImageUrl = user.getProfileImageUrl();
            if (profileImageUrl == null || profileImageUrl.isEmpty()) {
                Glide.with(context)
                        .load(R.drawable.ic_profile_placeholder) // Directly load placeholder if URL is null/empty
                        .apply(new RequestOptions().circleCrop()) // Apply circleCrop as before
                        .into(ivUserAvatar);
            } else {
                Glide.with(context)
                        .load(profileImageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .circleCrop())
                        .into(ivUserAvatar);
            }

            // Update checkbox state without triggering the listener
            checkboxInviteUser.setChecked(isSelected);
        }

        interface OnItemClickListener {
            void onItemClick(int position);
        }
    }

    /**
     * DiffUtil.Callback for efficiently calculating differences between two lists.
     */
    private static class UserDiffCallback extends DiffUtil.ItemCallback<UserModel> {
        @Override
        public boolean areItemsTheSame(@NonNull UserModel oldItem, @NonNull UserModel newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserModel oldItem, @NonNull UserModel newItem) {
            // Compare only relevant fields for UI updates to improve DiffUtil performance
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                    Objects.equals(oldItem.getLogin(), newItem.getLogin()) &&
                    Objects.equals(oldItem.getProfileImageUrl(), newItem.getProfileImageUrl()) &&
                    Objects.equals(oldItem.getRole(), newItem.getRole()) && // Role might affect visibility/actions
                    Objects.equals(oldItem.getStatus(), newItem.getStatus()); // Status might affect visibility/actions
        }
    }
}