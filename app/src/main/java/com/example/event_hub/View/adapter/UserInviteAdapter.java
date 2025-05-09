package com.example.event_hub.View.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_hub.Model.UserModel;
import com.example.event_hub.R;
// import com.bumptech.glide.Glide; // If using Glide for avatars

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class UserInviteAdapter extends RecyclerView.Adapter<UserInviteAdapter.UserInviteViewHolder> {

    private List<UserModel> userList;
    private final OnUserSelectionChangedListener selectionListener;
    private final Set<String> selectedUserIds = new HashSet<>(); // To keep track of selected users

    public interface OnUserSelectionChangedListener {
        void onUserSelectionChanged(UserModel user, boolean isSelected);
    }

    public UserInviteAdapter(List<UserModel> initialUserList, OnUserSelectionChangedListener listener) {
        this.userList = new ArrayList<>(initialUserList);
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public UserInviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_invite, parent, false);
        return new UserInviteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserInviteViewHolder holder, int position) {
        UserModel user = userList.get(position);
        holder.bind(user, selectedUserIds.contains(user.getUserId()), selectionListener, selectedUserIds);
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public void updateUsers(List<UserModel> newUserList) {
        final UserDiffCallback diffCallback = new UserDiffCallback(this.userList, newUserList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.userList.clear();
        this.userList.addAll(newUserList);
        diffResult.dispatchUpdatesTo(this);
    }

    public void clearSelections() {
        selectedUserIds.clear();
        notifyDataSetChanged(); // Re-bind all to uncheck
    }

    public List<UserModel> getSelectedUsers() {
        List<UserModel> selected = new ArrayList<>();
        for (UserModel user : userList) {
            if (selectedUserIds.contains(user.getUserId())) {
                selected.add(user);
            }
        }
        return selected;
    }


    static class UserInviteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName, tvUserLogin;
        CheckBox checkboxInviteUser;
        Random random = new Random();

        public UserInviteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar_invite_item);
            tvUserName = itemView.findViewById(R.id.tv_user_name_invite_item);
            tvUserLogin = itemView.findViewById(R.id.tv_user_login_invite_item);
            checkboxInviteUser = itemView.findViewById(R.id.checkbox_invite_user);
        }

        public void bind(final UserModel user,
                         boolean isSelected,
                         final OnUserSelectionChangedListener listener,
                         final Set<String> selectedUserIds) {

            if (user.getUserDetails() != null && user.getUserDetails().getFullName() != null && !user.getUserDetails().getFullName().isEmpty()) {
                tvUserName.setText(user.getUserDetails().getFullName());
                ivUserAvatar.setContentDescription(user.getUserDetails().getFullName());
                if (!user.getUserDetails().getFullName().isEmpty()) {
                    // tvParticipantAvatar.setText(String.valueOf(participant.getUserDetails().getFullName().charAt(0)).toUpperCase());
                } else {
                    // tvParticipantAvatar.setText("?");
                }
            } else {
                tvUserName.setText(user.getLogin()); // Fallback to login
                ivUserAvatar.setContentDescription(user.getLogin());
            }
            tvUserLogin.setText(user.getLogin() != null ? "@" + user.getLogin() : "");

            // TODO: Load actual avatar using Glide from user.getUserDetails().getProfileImageUrl()
            // For now, placeholder
            ivUserAvatar.setImageResource(R.drawable.ic_profile_placeholder);
            int Rnd = random.nextInt(156) + 100;
            int Gnd = random.nextInt(156) + 100;
            int Bnd = random.nextInt(156) + 100;
            if (ivUserAvatar.getBackground() != null) {
                ivUserAvatar.getBackground().mutate().setTint(Color.rgb(Rnd,Gnd,Bnd));
            }


            checkboxInviteUser.setOnCheckedChangeListener(null); // Avoid listener conflicts during rebind
            checkboxInviteUser.setChecked(isSelected);
            checkboxInviteUser.setOnCheckedChangeListener((buttonView, isNowChecked) -> {
                if (isNowChecked) {
                    selectedUserIds.add(user.getUserId());
                } else {
                    selectedUserIds.remove(user.getUserId());
                }
                if (listener != null) {
                    listener.onUserSelectionChanged(user, isNowChecked);
                }
            });

            itemView.setOnClickListener(v -> {
                checkboxInviteUser.setChecked(!checkboxInviteUser.isChecked());
                // The listener on checkbox will handle logic
            });
        }
    }

    private static class UserDiffCallback extends DiffUtil.Callback {
        private final List<UserModel> oldList;
        private final List<UserModel> newList;

        public UserDiffCallback(List<UserModel> oldList, List<UserModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getUserId().equals(newList.get(newItemPosition).getUserId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            UserModel oldUser = oldList.get(oldItemPosition);
            UserModel newUser = newList.get(newItemPosition);
            return oldUser.equals(newUser) && // Checks userId
                    Objects.equals(oldUser.getLogin(), newUser.getLogin()) &&
                    Objects.equals(oldUser.getUserDetails(), newUser.getUserDetails()); // UserDetails needs equals()
        }
    }
}
