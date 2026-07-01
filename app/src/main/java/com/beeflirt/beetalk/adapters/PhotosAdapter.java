package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder> {
    
    private List<String> photos;
    
    public PhotosAdapter() {
        this.photos = new ArrayList<>();
    }
    
    public void setPhotos(List<String> photos) {
        this.photos = photos != null ? photos : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String photoUrl = photos.get(position);

        if (photoUrl.startsWith("http")) {
            // URL from server
            Glide.with(holder.itemView.getContext())
                    .load(photoUrl)
                    .centerCrop()
                    .into(holder.ivPhoto);
        } else {
            String AddPhotoUrl = Constants.IMAGE_BASE_URL + photoUrl;
            // Local file path
            Glide.with(holder.itemView.getContext())
                    .load(AddPhotoUrl)
                    .centerCrop()
                    .into(holder.ivPhoto);
        }
    }
    
    @Override
    public int getItemCount() {
        return photos.size();
    }
    
    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        
        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
        }
    }
}
