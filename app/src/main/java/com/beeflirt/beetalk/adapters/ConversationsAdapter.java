package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.models.Conversation;
import com.beeflirt.beetalk.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {

    private List<Conversation> conversations;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationsAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);

        // Name
        holder.tvName.setText(conversation.getName());

        // Last message
        if (conversation.getLastMessage() != null && !conversation.getLastMessage().isEmpty()) {
            holder.tvLastMessage.setText(conversation.getLastMessage());
        } else {
            holder.tvLastMessage.setText("Say hello! \uD83D\uDC4B");
        }

        // Time ago
        if (conversation.getLastMessageTime() != null) {
            holder.tvTime.setText(getTimeAgo(conversation.getLastMessageTime()));
        } else {
            holder.tvTime.setText("");
        }

        // Profile photo
        String photoUrl = conversation.getPrimaryPhoto();
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

        // Unread badge
        if (conversation.getUnreadCount() > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(conversation.getUnreadCount() > 99
                    ? "99+" : String.valueOf(conversation.getUnreadCount()));

            // Bold name + message when unread
            holder.tvName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
            holder.tvLastMessage.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
            holder.tvTime.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.primary));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
            holder.tvName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
            holder.tvLastMessage.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
            holder.tvTime.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_hint));
        }

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conversation);
            }
        });

        // Online status
        if (conversation.getIsOnline() == 1) {
            holder.viewOnlineDot.setVisibility(View.VISIBLE);
        } else {
            holder.viewOnlineDot.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void updateConversations(List<Conversation> newConversations) {
        this.conversations = newConversations;
        notifyDataSetChanged();
    }

    /**
     * Format timestamp ke relative time yang readable
     * "Just now", "2 min ago", "1 hour ago", "Yesterday", "3 days ago", "Mar 15"
     */
    private String getTimeAgo(String timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone(Constants.APP_TIMEZONE));
            Date date = sdf.parse(timestamp);
            if (date == null) return "";

            long diffMs = System.currentTimeMillis() - date.getTime();
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);

            if (seconds < 60) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " min ago";
            } else if (hours < 24) {
                return hours == 1 ? "1 hour ago" : hours + " hours ago";
            } else if (days == 1) {
                return "Yesterday";
            } else if (days < 7) {
                return days + " days ago";
            } else {
                // Show date for older messages
                SimpleDateFormat shortFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
                return shortFormat.format(date);
            }
        } catch (Exception e) {
            return "";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivPhoto;
        TextView tvName;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnreadBadge;
        View viewOnlineDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvUnreadBadge = itemView.findViewById(R.id.tv_unread_badge);
            viewOnlineDot = itemView.findViewById(R.id.view_online_dot);
        }
    }
}
