package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class EditPhotoAdapter extends RecyclerView.Adapter<EditPhotoAdapter.PhotoViewHolder> {

    private static final String TAG = "EditPhotoAdapter";
    private static final int MAX_PHOTOS = 6;
    private List<String> photos; // URLs or file paths
    private OnPhotoActionListener listener;
    
    public interface OnPhotoActionListener {
        void onAddPhoto(int position);
        void onDeletePhoto(int position);
    }
    
    public EditPhotoAdapter(OnPhotoActionListener listener) {
        this.photos = new ArrayList<>();
        this.listener = listener;
    }
    
    public void setPhotos(List<String> photos) {
        this.photos = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public List<String> getPhotos() {
        return new ArrayList<>(photos);
    }
    
    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_edit_photo, parent, false);
        return new PhotoViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        if (position < photos.size()) {
            // Existing photo
            String photoUrl = photos.get(position);
            holder.ivPhoto.setVisibility(View.VISIBLE);
            holder.addPhotoOverlay.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            // Show photo number badge
            if (holder.tvPhotoNumber != null) {
                holder.tvPhotoNumber.setVisibility(View.VISIBLE);
                holder.tvPhotoNumber.setText(String.valueOf(position + 1));
            }
            
            // Load photo
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
            
            // Delete button
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeletePhoto(position);
                }
            });

            // Remove stale click listener from add-slot reuse
            holder.itemView.setOnClickListener(null);
            
        } else {
            // Add photo slot
            holder.ivPhoto.setVisibility(View.GONE);
            holder.addPhotoOverlay.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.GONE);

            if (holder.tvPhotoNumber != null) {
                holder.tvPhotoNumber.setVisibility(View.GONE);
            }
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddPhoto(position);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        // Show existing photos + 1 add button (up to MAX_PHOTOS)
        return Math.min(photos.size() + 1, MAX_PHOTOS);
    }
    
    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        LinearLayout addPhotoOverlay;
        ImageButton btnDelete;
        TextView tvPhotoNumber;
        
        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            addPhotoOverlay = itemView.findViewById(R.id.add_photo_overlay);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            tvPhotoNumber = itemView.findViewById(R.id.tv_photo_number);
        }
    }
}
