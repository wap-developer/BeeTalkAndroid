package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.models.Match;
import com.beeflirt.beetalk.utils.AnimationHelper;
import com.beeflirt.beetalk.utils.Constants;
import com.google.android.material.button.MaterialButton;

import java.util.List;

// Removed CircleImageView import

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.ViewHolder> {

    private List<Match> matches;
    private OnMatchClickListener listener;

    public interface OnMatchClickListener {
        void onMatchClick(Match match);

        void onUnmatchClick(Match match);
    }

    public MatchesAdapter(List<Match> matches, OnMatchClickListener listener) {
        this.matches = matches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Match match = matches.get(position);

        // Apply entrance animation with stagger
        AnimationHelper.slideUpFadeIn(holder.itemView, position * 50L);

        // Set name
        holder.tvName.setText(match.getName());

        // Set bio
        if (match.getBio() != null && !match.getBio().isEmpty()) {
            holder.tvBio.setText(match.getBio());
            holder.tvBio.setVisibility(View.VISIBLE);
        } else {
            holder.tvBio.setText("No bio yet");
            holder.tvBio.setVisibility(View.VISIBLE);
        }

        // Set location - display city, or "Unknown" if null
        String city = match.getCity();
        if (city != null && !city.isEmpty()) {
            holder.tvLocation.setText(city);
        } else {
            holder.tvLocation.setText("Unknown");
        }

        if (match.getPrimaryPhoto() != null && !match.getPrimaryPhoto().isEmpty()) {
            if (match.getPrimaryPhoto().startsWith("http")) {
                Glide.with(holder.itemView.getContext())
                        .load(match.getPrimaryPhoto())
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(holder.ivPhoto);
            } else {
                String AddPhotoUrl = Constants.IMAGE_BASE_URL + match.getPrimaryPhoto();
                // Local file path
                Glide.with(holder.itemView.getContext())
                        .load(AddPhotoUrl)
                        .centerCrop()
                        .into(holder.ivPhoto);
            }
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // Send Message button click
        holder.btnSendMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMatchClick(match);
            }
        });

        // Long press to unmatch
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onUnmatchClick(match);
            }
            return true;
        });

        // Online status
        if (match.getIsOnline() == 1) {
            holder.viewOnlineDot.setVisibility(View.VISIBLE);
        } else {
            holder.viewOnlineDot.setVisibility(View.GONE);
        }
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
        ImageView ivPhoto;
        TextView tvName;
        TextView tvBio;
        TextView tvLocation;
        MaterialButton btnSendMessage;
        View viewOnlineDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            tvName = itemView.findViewById(R.id.tv_name);
            tvBio = itemView.findViewById(R.id.tv_bio);
            tvLocation = itemView.findViewById(R.id.tv_location);
            btnSendMessage = itemView.findViewById(R.id.btn_send_message);
            viewOnlineDot = itemView.findViewById(R.id.view_online_dot);
        }
    }
}
