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
import com.beeflirt.beetalk.models.Message;
import com.beeflirt.beetalk.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;

    private static final SimpleDateFormat SERVER_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final SimpleDateFormat LOCAL_TIME = new SimpleDateFormat("HH:mm", Locale.getDefault());

    static {
        SERVER_PARSER.setTimeZone(TimeZone.getTimeZone(Constants.APP_TIMEZONE));
        LOCAL_TIME.setTimeZone(TimeZone.getTimeZone(Constants.APP_TIMEZONE));
    }

    private List<Message> messages;
    private int currentUserId;
    private OnImageClickListener imageClickListener;

    public interface OnImageClickListener {
        void onImageClick(String imageUrl);
    }

    public ChatAdapter(List<Message> messages, int currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.imageClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        boolean isSent = message.getSenderId() == currentUserId;

        if (message.isImageMessage()) {
            return isSent ? VIEW_TYPE_SENT_IMAGE : VIEW_TYPE_RECEIVED_IMAGE;
        }
        return isSent ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_SENT_IMAGE:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent_image, parent, false);
                return new SentImageViewHolder(view);
            case VIEW_TYPE_RECEIVED_IMAGE:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received_image, parent, false);
                return new ReceivedImageViewHolder(view);
            case VIEW_TYPE_SENT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageViewHolder(view);
            default:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received, parent, false);
                return new ReceivedMessageViewHolder(view);
        }
    }

    private int lastAnimatedPosition = -1;

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        } else if (holder instanceof SentImageViewHolder) {
            ((SentImageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedImageViewHolder) {
            ((ReceivedImageViewHolder) holder).bind(message);
        }

        if (position > lastAnimatedPosition) {
            android.view.View itemView = holder.itemView;
            itemView.setAlpha(0f);
            itemView.setTranslationY(40f);
            itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .setStartDelay(30)
                    .start();
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private static String formatTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";
        try {
            Date date = SERVER_PARSER.parse(timestamp);
            if (date == null) return "";
            return LOCAL_TIME.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private String getFullImageUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        return url.startsWith("http") ? url : Constants.IMAGE_BASE_URL + url;
    }

    // Text sent
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessage());
            tvTime.setText(formatTime(message.getCreatedAt()));
        }
    }

    // Text received
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessage());
            tvTime.setText(formatTime(message.getCreatedAt()));
        }
    }

    // Image sent
    class SentImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMedia;
        TextView tvTime;

        SentImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMedia = itemView.findViewById(R.id.iv_media);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(Message message) {
            tvTime.setText(formatTime(message.getCreatedAt()));
            String fullUrl = getFullImageUrl(message.getMediaUrl());
            Glide.with(itemView.getContext())
                    .load(fullUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder_profile)
                    .into(ivMedia);

            ivMedia.setOnClickListener(v -> {
                if (imageClickListener != null) imageClickListener.onImageClick(fullUrl);
            });
        }
    }

    // Image received
    class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMedia;
        TextView tvTime;

        ReceivedImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMedia = itemView.findViewById(R.id.iv_media);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(Message message) {
            tvTime.setText(formatTime(message.getCreatedAt()));
            String fullUrl = getFullImageUrl(message.getMediaUrl());
            Glide.with(itemView.getContext())
                    .load(fullUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder_profile)
                    .into(ivMedia);

            ivMedia.setOnClickListener(v -> {
                if (imageClickListener != null) imageClickListener.onImageClick(fullUrl);
            });
        }
    }
}
