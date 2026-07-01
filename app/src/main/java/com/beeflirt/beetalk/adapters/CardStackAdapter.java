package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.models.Profile;
import com.beeflirt.beetalk.utils.Constants;

import java.util.List;

public class CardStackAdapter extends RecyclerView.Adapter<CardStackAdapter.ViewHolder> {

    private List<Profile> profiles;

    public CardStackAdapter(List<Profile> profiles) {
        this.profiles = profiles;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Profile profile = profiles.get(position);

        // Set profile data
        holder.tvName.setText(profile.getName() + ", " + profile.getAge());

        if (profile.getBio() != null && !profile.getBio().isEmpty()) {
            holder.tvBio.setText(profile.getBio());
            holder.tvBio.setVisibility(View.VISIBLE);
        } else {
            holder.tvBio.setVisibility(View.GONE);
        }

        // Set distance
        if (profile.getDistance() > 0) {
            holder.tvDistance.setText(String.format("%.1f km away", profile.getDistance()));
            holder.tvDistance.setVisibility(View.VISIBLE);
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        // Load primary photo
        String photoUrl = profile.getPrimaryPhoto();
        if (photoUrl == null || photoUrl.isEmpty()) {
            // Fallback: cek dari photos list
            if (profile.getPhotos() != null && !profile.getPhotos().isEmpty()) {
                photoUrl = profile.getPhotos().get(0).getPhotoUrl();
            }
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.IMAGE_BASE_URL + photoUrl;
            Glide.with(holder.itemView.getContext())
                    .load(fullUrl)
                    .apply(new RequestOptions()
                            .override(720, 1280)
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.placeholder_profile)
                            .error(R.drawable.placeholder_error))
                    .into(holder.ivPhoto);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.placeholder_profile);
        }
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    public void updateProfiles(List<Profile> newProfiles) {
        this.profiles = newProfiles;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName;
        TextView tvBio;
        TextView tvDistance;
        TextView tvLike;
        TextView tvNope;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            tvName = itemView.findViewById(R.id.tv_name);
            tvBio = itemView.findViewById(R.id.tv_bio);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvLike = itemView.findViewById(R.id.tv_like);
            tvNope = itemView.findViewById(R.id.tv_nope);
        }
    }
}
