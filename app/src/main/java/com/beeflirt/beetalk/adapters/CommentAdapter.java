package com.beeflirt.beetalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.models.Comment;
import com.beeflirt.beetalk.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_COMMENT = 0;
    private static final int TYPE_REPLY = 1;

    private List<Comment> comments;
    private int currentUserId;
    private OnCommentActionListener listener;

    public interface OnCommentActionListener {
        void onLikeClick(Comment comment, int position);
        void onReplyClick(Comment comment);
        void onDeleteClick(Comment comment, int position);
        void onViewRepliesClick(Comment comment, int position);
    }

    public CommentAdapter(List<Comment> comments, int currentUserId, OnCommentActionListener listener) {
        this.comments = comments;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Comment comment = comments.get(position);
        return comment.getParentCommentId() != null ? TYPE_REPLY : TYPE_COMMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_REPLY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment_reply, parent, false);
            return new ReplyViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Comment comment = comments.get(position);

        if (holder instanceof CommentViewHolder) {
            bindComment((CommentViewHolder) holder, comment, position);
        } else if (holder instanceof ReplyViewHolder) {
            bindReply((ReplyViewHolder) holder, comment, position);
        }
    }

    private void bindComment(CommentViewHolder holder, Comment comment, int position) {
        holder.tvName.setText(comment.getUserName() != null ? comment.getUserName() : "User");
        holder.tvCommentText.setText(comment.getCommentText());
        holder.tvTime.setText(getTimeAgo(comment.getCreatedAt()));
        holder.tvLikeCount.setText(String.valueOf(comment.getLikeCount()));

        // Like state
        if (comment.getIsLiked() == 1) {
            holder.ivLikeIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.error));
        } else {
            holder.ivLikeIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.text_hint));
        }

        // Avatar
        String userPhoto = comment.getUserPhoto();
        if (userPhoto != null && !userPhoto.isEmpty()) {
            String fullUrl = userPhoto.startsWith("http") ? userPhoto : Constants.IMAGE_BASE_URL + userPhoto;
            Glide.with(holder.itemView.getContext())
                    .load(fullUrl)
                    .placeholder(R.drawable.placeholder_profile)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.placeholder_profile);
        }

        // Delete button visibility
        if (comment.getUserId() == currentUserId) {
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // View replies
        if (comment.getReplyCount() > 0) {
            holder.btnViewReplies.setVisibility(View.VISIBLE);
            holder.btnViewReplies.setText("View " + comment.getReplyCount() + " replies");
        } else {
            holder.btnViewReplies.setVisibility(View.GONE);
        }

        // Click listeners
        holder.btnLike.setOnClickListener(v -> {
            if (listener != null) listener.onLikeClick(comment, position);
        });
        holder.btnReply.setOnClickListener(v -> {
            if (listener != null) listener.onReplyClick(comment);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(comment, position);
        });
        holder.btnViewReplies.setOnClickListener(v -> {
            if (listener != null) listener.onViewRepliesClick(comment, position);
        });
    }

    private void bindReply(ReplyViewHolder holder, Comment comment, int position) {
        holder.tvName.setText(comment.getUserName() != null ? comment.getUserName() : "User");
        holder.tvCommentText.setText(comment.getCommentText());
        holder.tvTime.setText(getTimeAgo(comment.getCreatedAt()));
        holder.tvLikeCount.setText(String.valueOf(comment.getLikeCount()));

        // Like state
        if (comment.getIsLiked() == 1) {
            holder.ivLikeIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.error));
        } else {
            holder.ivLikeIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.text_hint));
        }

        // Avatar
        String userPhoto = comment.getUserPhoto();
        if (userPhoto != null && !userPhoto.isEmpty()) {
            String fullUrl = userPhoto.startsWith("http") ? userPhoto : Constants.IMAGE_BASE_URL + userPhoto;
            Glide.with(holder.itemView.getContext())
                    .load(fullUrl)
                    .placeholder(R.drawable.placeholder_profile)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.placeholder_profile);
        }

        // Delete button
        if (comment.getUserId() == currentUserId) {
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Click listeners
        holder.btnLike.setOnClickListener(v -> {
            if (listener != null) listener.onLikeClick(comment, position);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(comment, position);
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private String getTimeAgo(String timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone(Constants.APP_TIMEZONE));
            Date date = sdf.parse(timestamp);
            if (date == null) return "";

            long diffMs = System.currentTimeMillis() - date.getTime();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);

            if (minutes < 1) return "now";
            if (minutes < 60) return minutes + "m";
            if (hours < 24) return hours + "h";
            if (days < 7) return days + "d";
            SimpleDateFormat shortFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
            return shortFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvTime, tvCommentText, tvLikeCount;
        TextView btnReply, btnViewReplies, btnDelete;
        ImageView ivLikeIcon;
        LinearLayout btnLike;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvCommentText = itemView.findViewById(R.id.tv_comment_text);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            ivLikeIcon = itemView.findViewById(R.id.iv_like_icon);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnReply = itemView.findViewById(R.id.btn_reply);
            btnViewReplies = itemView.findViewById(R.id.btn_view_replies);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvTime, tvCommentText, tvLikeCount;
        TextView btnDelete;
        ImageView ivLikeIcon;
        LinearLayout btnLike;

        ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvCommentText = itemView.findViewById(R.id.tv_comment_text);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            ivLikeIcon = itemView.findViewById(R.id.iv_like_icon);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
