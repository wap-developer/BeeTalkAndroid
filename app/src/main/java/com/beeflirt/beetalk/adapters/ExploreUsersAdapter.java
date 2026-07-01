package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.models.ExploreUser;
import com.beeflirt.beetalk.utils.Constants;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ExploreUsersAdapter extends RecyclerView.Adapter<ExploreUsersAdapter.ViewHolder> {

    private List<ExploreUser> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(ExploreUser user);
    }

    public ExploreUsersAdapter(List<ExploreUser> users) {
        this.users = users;
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_explore_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExploreUser user = users.get(position);

        holder.tvName.setText(user.getName());

        if (user.getDistance() > 0) {
            holder.tvDistance.setText(String.format("%.1f km", user.getDistance()));
            holder.tvDistance.setVisibility(View.VISIBLE);
        } else if (user.getCity() != null) {
            holder.tvDistance.setText(user.getCity());
            holder.tvDistance.setVisibility(View.VISIBLE);
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        String photoUrl = user.getPrimaryPhoto();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.IMAGE_BASE_URL + photoUrl;
            Glide.with(holder.itemView.getContext())
                    .load(fullUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_profile)
                    .error(R.drawable.placeholder_error)
                    .into(holder.ivPhoto);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.placeholder_profile);
        }

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(user);
        });

        // Online status
        if (user.getIsOnline() == 1) {
            holder.viewOnlineDot.setVisibility(View.VISIBLE);
        } else {
            holder.viewOnlineDot.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<ExploreUser> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivPhoto;
        TextView tvName, tvDistance;
        View viewOnlineDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            viewOnlineDot = itemView.findViewById(R.id.view_online_dot);
        }
    }
}
