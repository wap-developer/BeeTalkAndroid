package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.beeflirt.beetalk.R;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private final int[] avatarResIds;
    private int selectedPosition = -1;
    private OnAvatarSelectedListener listener;

    public interface OnAvatarSelectedListener {
        void onAvatarSelected(int resId, int position);
    }

    public AvatarAdapter(int[] avatarResIds, OnAvatarSelectedListener listener) {
        this.avatarResIds = avatarResIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        int resId = avatarResIds[position];
        holder.ivAvatar.setImageResource(resId);

        // Show selection state
        if (position == selectedPosition) {
            holder.borderFrame.setBackgroundResource(R.drawable.bg_avatar_selected);
            holder.ivAvatar.setScaleX(1.05f);
            holder.ivAvatar.setScaleY(1.05f);
        } else {
            holder.borderFrame.setBackgroundResource(R.drawable.bg_avatar_unselected);
            holder.ivAvatar.setScaleX(1f);
            holder.ivAvatar.setScaleY(1f);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            if (oldPos >= 0) notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);

            // Animate selection
            holder.ivAvatar.animate()
                    .scaleX(1.1f).scaleY(1.1f)
                    .setDuration(100)
                    .withEndAction(() -> holder.ivAvatar.animate()
                            .scaleX(1.05f).scaleY(1.05f)
                            .setDuration(100)
                            .start())
                    .start();

            if (listener != null) {
                listener.onAvatarSelected(resId, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return avatarResIds.length;
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        selectedPosition = position;
        if (oldPos >= 0) notifyItemChanged(oldPos);
        if (selectedPosition >= 0) notifyItemChanged(selectedPosition);
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        FrameLayout borderFrame;

        AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            borderFrame = itemView.findViewById(R.id.border_frame);
        }
    }
}
