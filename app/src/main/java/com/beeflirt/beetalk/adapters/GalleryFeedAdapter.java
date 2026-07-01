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
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.models.GalleryPost;
import com.beeflirt.beetalk.utils.Constants;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GalleryFeedAdapter extends RecyclerView.Adapter<GalleryFeedAdapter.ViewHolder> {

    private List<GalleryPost> posts;
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(GalleryPost post);
    }

    public GalleryFeedAdapter(List<GalleryPost> posts) {
        this.posts = posts;
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_feed_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GalleryPost post = posts.get(position);

        // Caption
        if (post.getCaption() != null && !post.getCaption().isEmpty()) {
            holder.tvCaption.setText(post.getCaption());
            holder.tvCaption.setVisibility(View.VISIBLE);
        } else {
            holder.tvCaption.setVisibility(View.GONE);
        }

        // User name
        holder.tvName.setText(post.getName() != null ? post.getName() : "User");

        // Gallery photo
        String photoUrl = post.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.IMAGE_BASE_URL + photoUrl;
            Glide.with(holder.itemView.getContext())
                    .load(fullUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder_profile)
                    .into(holder.ivPhoto);
        }

        // User avatar
        String userPhoto = post.getUserPhoto();
        if (userPhoto != null && !userPhoto.isEmpty()) {
            String fullUrl = userPhoto.startsWith("http") ? userPhoto : Constants.IMAGE_BASE_URL + userPhoto;
            Glide.with(holder.itemView.getContext())
                    .load(fullUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_profile)
                    .into(holder.ivAvatar);
        }

        // Click to open detail
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPostClick(post);
        });

        // Like count
        int likeCount = post.getLikeCount();
        holder.tvLikeCount.setText(String.valueOf(likeCount));
        if (post.getIsLiked() == 1) {
            holder.ivLikeIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.error));
        } else {
            holder.ivLikeIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.text_hint));
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        ImageView ivPhoto, ivLikeIcon;
        TextView tvName, tvCaption, tvLikeCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar);
            ivPhoto = itemView.findViewById(R.id.iv_post_photo);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvCaption = itemView.findViewById(R.id.tv_caption);
            ivLikeIcon = itemView.findViewById(R.id.iv_like_icon);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
        }
    }
}
