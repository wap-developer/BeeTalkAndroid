package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.models.Match;
import com.beeflirt.beetalk.utils.Constants;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewMatchesAdapter extends RecyclerView.Adapter<NewMatchesAdapter.ViewHolder> {

    private List<Match> matches;
    private OnNewMatchClickListener listener;

    public interface OnNewMatchClickListener {
        void onNewMatchClick(Match match);
    }

    public NewMatchesAdapter(List<Match> matches, OnNewMatchClickListener listener) {
        this.matches = matches;
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
        Match match = matches.get(position);

        holder.tvName.setText(match.getName());

        if (match.getCity() != null && !match.getCity().isEmpty()) {
            holder.tvDistance.setText(match.getCity());
            holder.tvDistance.setVisibility(View.VISIBLE);
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        String photoUrl = match.getPrimaryPhoto();
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

        // Online status
        if (match.getIsOnline() == 1) {
            holder.viewOnlineDot.setVisibility(View.VISIBLE);
        } else {
            holder.viewOnlineDot.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNewMatchClick(match);
        });

        // Animate entrance
        holder.itemView.setAlpha(0f);
        holder.itemView.animate().alpha(1f).setDuration(300).setStartDelay(position * 50L).start();
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    public void updateMatches(List<Match> newMatches) {
        this.matches = newMatches;
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
