package com.beeflirt.beetalk.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.models.BlockedUser;
import com.beeflirt.beetalk.utils.Constants;

import java.util.List;

public class BlockedUsersAdapter extends RecyclerView.Adapter<BlockedUsersAdapter.ViewHolder> {

    private Context context;
    private List<BlockedUser> blockedUsers;
    private OnUnblockClickListener listener;

    public interface OnUnblockClickListener {
        void onUnblockClick(BlockedUser user, int position);
    }

    public BlockedUsersAdapter(Context context, List<BlockedUser> blockedUsers, OnUnblockClickListener listener) {
        this.context = context;
        this.blockedUsers = blockedUsers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockedUser user = blockedUsers.get(position);

        holder.tvName.setText(user.getName());
        holder.tvAge.setText(user.getAge() + " years old");

        // Format blocked date
        String blockedDate = "Blocked recently";
        if (user.getCreatedAt() != null) {
            blockedDate = "Blocked on " + user.getCreatedAt().substring(0, 10);
        }
        holder.tvBlockedDate.setText(blockedDate);

        // Load profile photo
        if (user.getPrimaryPhoto() != null && !user.getPrimaryPhoto().isEmpty()) {
            String photoUrl = user.getPrimaryPhoto().startsWith("http")
                    ? user.getPrimaryPhoto()
                    : Constants.IMAGE_BASE_URL + user.getPrimaryPhoto();
            Glide.with(context)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .centerCrop()
                    .into(holder.ivProfilePhoto);
        } else {
            holder.ivProfilePhoto.setImageResource(R.drawable.ic_person);
        }

        holder.btnUnblock.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUnblockClick(user, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return blockedUsers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePhoto;
        TextView tvName, tvAge, tvBlockedDate;
        Button btnUnblock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePhoto = itemView.findViewById(R.id.ivProfilePhoto);
            tvName = itemView.findViewById(R.id.tvName);
            tvAge = itemView.findViewById(R.id.tvAge);
            tvBlockedDate = itemView.findViewById(R.id.tvBlockedDate);
            btnUnblock = itemView.findViewById(R.id.btnUnblock);
        }
    }
}
