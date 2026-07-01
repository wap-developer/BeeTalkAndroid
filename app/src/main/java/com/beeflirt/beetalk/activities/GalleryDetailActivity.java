package com.beeflirt.beetalk.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adapters.CommentAdapter;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Comment;
import com.beeflirt.beetalk.utils.Constants;
import com.beeflirt.beetalk.utils.SharedPrefManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GalleryDetailActivity extends AppCompatActivity implements CommentAdapter.OnCommentActionListener {

    private static final String TAG = "GalleryDetailActivity";
    private static final int PAGE_SIZE = 20;
    private static final long POLL_INTERVAL = 5000;

    private int postId;
    private int currentUserId;
    private ApiService apiService;

    // Views
    private ImageView ivPostPhoto;
    private CircleImageView ivUserAvatar;
    private TextView tvUserName, tvPostTime, tvCaption, tvCommentCount;
    private RecyclerView rvComments;
    private ProgressBar progressComments;
    private LinearLayout layoutEmptyComments;
    private EditText etComment;
    private ImageView btnSend;
    private LinearLayout layoutReplyIndicator;
    private TextView tvReplyIndicator;
    private ImageView btnCancelReply;
    private NestedScrollView scrollView;
    private LinearLayout btnLikePost;
    private ImageView ivLikeIcon;
    private TextView tvLikeCount;
    private boolean isPostLiked = false;

    // Data
    private CommentAdapter commentAdapter;
    private List<Comment> comments = new ArrayList<>();
    private Set<Integer> loadedCommentIds = new HashSet<>();
    private int commentsOffset = 0;
    private boolean isLoadingComments = false;
    private boolean hasMoreComments = true;

    // Reply state
    private Integer replyingToCommentId = null;
    private String replyingToUserName = null;

    // Polling
    private Handler pollHandler;
    private Runnable pollRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_detail);

        postId = getIntent().getIntExtra("post_id", 0);
        if (postId == 0) {
            finish();
            return;
        }

        apiService = ApiClient.getApiService(this);
        currentUserId = SharedPrefManager.getInstance(this).getUserId();

        initViews();
        setupListeners();
        loadPostDetail();
        loadComments(false);
        startPolling();
    }

    private void initViews() {
        ivPostPhoto = findViewById(R.id.iv_post_photo);
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        tvUserName = findViewById(R.id.tv_user_name);
        tvPostTime = findViewById(R.id.tv_post_time);
        tvCaption = findViewById(R.id.tv_caption);
        tvCommentCount = findViewById(R.id.tv_comment_count);
        rvComments = findViewById(R.id.rv_comments);
        progressComments = findViewById(R.id.progress_comments);
        layoutEmptyComments = findViewById(R.id.layout_empty_comments);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send);
        layoutReplyIndicator = findViewById(R.id.layout_reply_indicator);
        tvReplyIndicator = findViewById(R.id.tv_reply_indicator);
        btnCancelReply = findViewById(R.id.btn_cancel_reply);
        scrollView = findViewById(R.id.scroll_view);
        btnLikePost = findViewById(R.id.btn_like_post);
        ivLikeIcon = findViewById(R.id.iv_like_icon);
        tvLikeCount = findViewById(R.id.tv_like_count);

        commentAdapter = new CommentAdapter(comments, currentUserId, this);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
        rvComments.setNestedScrollingEnabled(false);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> sendComment());

        btnCancelReply.setOnClickListener(v -> cancelReply());

        btnLikePost.setOnClickListener(v -> togglePostLike());

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int childHeight = v.getChildAt(0).getMeasuredHeight();
                    int scrollViewHeight = v.getMeasuredHeight();
                    if (scrollY >= childHeight - scrollViewHeight - 200) {
                        if (!isLoadingComments && hasMoreComments) {
                            loadComments(true);
                        }
                    }
                });
    }

    private void loadPostDetail() {
        apiService.getPostDetail(postId).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (isFinishing()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    Map<String, Object> post = (Map<String, Object>) data.get("post");

                    if (post != null) {
                        // Photo
                        String photoUrl = (String) post.get("photo_url");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.IMAGE_BASE_URL + photoUrl;
                            Glide.with(GalleryDetailActivity.this)
                                    .load(fullUrl)
                                    .placeholder(R.drawable.placeholder_profile)
                                    .into(ivPostPhoto);
                        }

                        // User info
                        String name = (String) post.get("name");
                        tvUserName.setText(name != null ? name : "User");

                        String userPhoto = (String) post.get("user_photo");
                        if (userPhoto != null && !userPhoto.isEmpty()) {
                            String fullUrl = userPhoto.startsWith("http") ? userPhoto : Constants.IMAGE_BASE_URL + userPhoto;
                            Glide.with(GalleryDetailActivity.this)
                                    .load(fullUrl)
                                    .placeholder(R.drawable.placeholder_profile)
                                    .into(ivUserAvatar);
                        }

                        // Caption
                        String caption = (String) post.get("caption");
                        if (caption != null && !caption.isEmpty()) {
                            tvCaption.setText(caption);
                            tvCaption.setVisibility(View.VISIBLE);
                        }

                        // Time
                        String createdAt = (String) post.get("created_at");
                        if (createdAt != null) {
                            tvPostTime.setText(getTimeAgo(createdAt));
                        }

                        // Comment count
                        Object countObj = post.get("comment_count");
                        int commentCount = 0;
                        if (countObj instanceof Double) commentCount = ((Double) countObj).intValue();
                        else if (countObj instanceof Integer) commentCount = (Integer) countObj;
                        tvCommentCount.setText(commentCount + " comments");

                        // Like data
                        Object likeCountObj = post.get("like_count");
                        int likeCount = 0;
                        if (likeCountObj instanceof Double) likeCount = ((Double) likeCountObj).intValue();
                        else if (likeCountObj instanceof Integer) likeCount = (Integer) likeCountObj;
                        tvLikeCount.setText(String.valueOf(likeCount));

                        Object isLikedObj = post.get("is_liked");
                        int isLikedVal = 0;
                        if (isLikedObj instanceof Double) isLikedVal = ((Double) isLikedObj).intValue();
                        else if (isLikedObj instanceof Integer) isLikedVal = (Integer) isLikedObj;
                        isPostLiked = isLikedVal == 1;
                        updateLikeIcon();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "Failed to load post detail: " + t.getMessage());
            }
        });
    }

    private void loadComments(boolean append) {
        if (isLoadingComments) return;
        isLoadingComments = true;

        if (!append) {
            progressComments.setVisibility(View.VISIBLE);
        }

        apiService.getPostComments(postId, PAGE_SIZE, commentsOffset)
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (isFinishing()) return;
                isLoadingComments = false;
                progressComments.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("comments"));
                    Type listType = new TypeToken<List<Comment>>() {}.getType();
                    List<Comment> newComments = gson.fromJson(json, listType);

                    if (newComments != null && !newComments.isEmpty()) {
                        int added = 0;
                        for (Comment c : newComments) {
                            if (!loadedCommentIds.contains(c.getId())) {
                                loadedCommentIds.add(c.getId());
                                comments.add(c);
                                added++;
                            }
                        }
                        if (added > 0) {
                            commentAdapter.notifyDataSetChanged();
                        }
                        commentsOffset += newComments.size();
                        hasMoreComments = newComments.size() >= PAGE_SIZE;

                        rvComments.setVisibility(View.VISIBLE);
                        layoutEmptyComments.setVisibility(View.GONE);
                    } else {
                        hasMoreComments = false;
                        if (comments.isEmpty()) {
                            rvComments.setVisibility(View.GONE);
                            layoutEmptyComments.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (isFinishing()) return;
                isLoadingComments = false;
                progressComments.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load comments: " + t.getMessage());
            }
        });
    }

    private void sendComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) return;

        btnSend.setEnabled(false);

        JsonObject body = new JsonObject();
        body.addProperty("comment_text", text);
        if (replyingToCommentId != null) {
            body.addProperty("parent_comment_id", replyingToCommentId);
        }

        apiService.addComment(postId, body).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (isFinishing()) return;
                btnSend.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("comment"));
                    Comment newComment = gson.fromJson(json, Comment.class);

                    if (newComment != null && !loadedCommentIds.contains(newComment.getId())) {
                        loadedCommentIds.add(newComment.getId());
                        comments.add(0, newComment);
                        commentAdapter.notifyItemInserted(0);
                        rvComments.setVisibility(View.VISIBLE);
                        layoutEmptyComments.setVisibility(View.GONE);

                        // Update count
                        tvCommentCount.setText(comments.size() + " comments");
                    }

                    etComment.setText("");
                    cancelReply();
                    hideKeyboard();
                    scrollView.smoothScrollTo(0, ivPostPhoto.getHeight());
                } else {
                    Toast.makeText(GalleryDetailActivity.this, "Failed to add comment", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (isFinishing()) return;
                btnSend.setEnabled(true);
                Toast.makeText(GalleryDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelReply() {
        replyingToCommentId = null;
        replyingToUserName = null;
        layoutReplyIndicator.setVisibility(View.GONE);
    }

    private void updateLikeIcon() {
        if (isPostLiked) {
            ivLikeIcon.setColorFilter(getResources().getColor(R.color.error));
        } else {
            ivLikeIcon.setColorFilter(getResources().getColor(R.color.text_hint));
        }
    }

    private void togglePostLike() {
        apiService.likePost(postId).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (isFinishing()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();

                    Object isLikedObj = data.get("is_liked");
                    if (isLikedObj instanceof Boolean) isPostLiked = (Boolean) isLikedObj;

                    Object likeCountObj = data.get("like_count");
                    int likeCount = 0;
                    if (likeCountObj instanceof Double) likeCount = ((Double) likeCountObj).intValue();
                    else if (likeCountObj instanceof Integer) likeCount = (Integer) likeCountObj;
                    tvLikeCount.setText(String.valueOf(likeCount));

                    updateLikeIcon();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "Post like failed: " + t.getMessage());
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    // CommentAdapter.OnCommentActionListener
    @Override
    public void onLikeClick(Comment comment, int position) {
        apiService.likeComment(postId, comment.getId())
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (isFinishing()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();

                    Object isLikedObj = data.get("is_liked");
                    boolean isLiked = false;
                    if (isLikedObj instanceof Boolean) isLiked = (Boolean) isLikedObj;

                    Object likeCountObj = data.get("like_count");
                    int likeCount = 0;
                    if (likeCountObj instanceof Double) likeCount = ((Double) likeCountObj).intValue();

                    comment.setIsLiked(isLiked ? 1 : 0);
                    comment.setLikeCount(likeCount);
                    commentAdapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "Like failed: " + t.getMessage());
            }
        });
    }

    @Override
    public void onReplyClick(Comment comment) {
        replyingToCommentId = comment.getId();
        replyingToUserName = comment.getUserName();
        layoutReplyIndicator.setVisibility(View.VISIBLE);
        tvReplyIndicator.setText("Replying to " + (replyingToUserName != null ? replyingToUserName : "user"));
        etComment.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onDeleteClick(Comment comment, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    apiService.deleteComment(postId, comment.getId())
                            .enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call,
                                Response<ApiResponse<Object>> response) {
                            if (isFinishing()) return;

                            if (response.isSuccessful()) {
                                loadedCommentIds.remove(comment.getId());
                                comments.remove(position);
                                commentAdapter.notifyItemRemoved(position);
                                tvCommentCount.setText(comments.size() + " comments");

                                if (comments.isEmpty()) {
                                    rvComments.setVisibility(View.GONE);
                                    layoutEmptyComments.setVisibility(View.VISIBLE);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            Toast.makeText(GalleryDetailActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewRepliesClick(Comment comment, int position) {
        apiService.getCommentReplies(postId, comment.getId())
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (isFinishing()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("replies"));
                    Type listType = new TypeToken<List<Comment>>() {}.getType();
                    List<Comment> replies = gson.fromJson(json, listType);

                    if (replies != null && !replies.isEmpty()) {
                        int insertPos = position + 1;
                        for (Comment reply : replies) {
                            if (!loadedCommentIds.contains(reply.getId())) {
                                loadedCommentIds.add(reply.getId());
                                comments.add(insertPos, reply);
                                insertPos++;
                            }
                        }
                        commentAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "Failed to load replies: " + t.getMessage());
            }
        });
    }

    // Polling
    private void startPolling() {
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = () -> {
            if (isFinishing()) return;
            pollNewComments();
            pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
    }

    private void pollNewComments() {
        apiService.getPostComments(postId, 10, 0)
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (isFinishing()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("comments"));
                    Type listType = new TypeToken<List<Comment>>() {}.getType();
                    List<Comment> latestComments = gson.fromJson(json, listType);

                    if (latestComments != null) {
                        int added = 0;
                        for (Comment c : latestComments) {
                            if (!loadedCommentIds.contains(c.getId())) {
                                loadedCommentIds.add(c.getId());
                                comments.add(0, c);
                                added++;
                            }
                        }
                        if (added > 0) {
                            commentAdapter.notifyDataSetChanged();
                            rvComments.setVisibility(View.VISIBLE);
                            layoutEmptyComments.setVisibility(View.GONE);
                            tvCommentCount.setText(comments.size() + " comments");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private String getTimeAgo(String timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(timestamp);
            if (date == null) return "";

            long diffMs = System.currentTimeMillis() - date.getTime();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);

            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + "m ago";
            if (hours < 24) return hours + "h ago";
            if (days < 7) return days + "d ago";
            SimpleDateFormat shortFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
            return shortFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        super.onDestroy();
    }
}
