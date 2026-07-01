package com.beeflirt.beetalk.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adapters.ChatAdapter;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Message;
import com.beeflirt.beetalk.repository.ChatRepository;
import com.beeflirt.beetalk.repository.RepositoryCallback;
import com.beeflirt.beetalk.utils.Constants;
import com.beeflirt.beetalk.utils.SharedPrefManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {

    private static final String TAG = "ChatActivity";
    private static final long POLL_INTERVAL_MS = 5000; // Poll setiap 5 detik

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Message> messages;
    private EditText etMessage;
    private FrameLayout btnSend;
    private ImageView btnAttach;
    private ProgressBar progressBar;
    private View tvEmpty;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private int matchId;
    private int otherUserId;
    private int currentUserId;
    private String userName;
    private String userPhoto;
    private int isOnline;
    private String lastSeen;

    private ApiService apiService;
    private SharedPrefManager prefManager;
    private ChatRepository chatRepository;

    // Polling handler
    private Handler pollHandler;
    private boolean isPolling = false;
    private boolean isFirstLoad = true;
    private boolean isLoadingMore = false;
    private boolean hasMoreMessages = true;
    private static final int MESSAGES_PER_PAGE = 50;

    // FCM broadcast receiver - instant message refresh
    private android.content.BroadcastReceiver messageReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            String fcmMatchId = intent.getStringExtra("match_id");
            if (fcmMatchId != null && String.valueOf(matchId).equals(fcmMatchId)) {
                pollNewMessages(); // Instant refresh
            }
        }
    };

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPolling) {
                pollNewMessages();
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyImeInset = true; // Keyboard-aware padding
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data from intent
        matchId = getIntent().getIntExtra("match_id", 0);
        otherUserId = getIntent().getIntExtra("user_id", 0);
        userName = getIntent().getStringExtra("user_name");
        userPhoto = getIntent().getStringExtra("user_photo");
        isOnline = getIntent().getIntExtra("is_online", 0);
        lastSeen = getIntent().getStringExtra("last_seen");

        // Initialize API and preferences
        apiService = ApiClient.getApiService(this);
        prefManager = SharedPrefManager.getInstance(this);
        chatRepository = new ChatRepository(this);
        currentUserId = prefManager.getUserId();

        // Initialize polling handler
        pollHandler = new Handler(Looper.getMainLooper());

        // Setup toolbar
        setupToolbar();

        // Initialize views
        recyclerView = findViewById(R.id.recycler_view);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);

        // Initialize messages list
        messages = new ArrayList<>();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup send button
        btnSend.setOnClickListener(v -> sendMessage());

        // Setup attach button
        btnAttach = findViewById(R.id.btn_attach);
        if (btnAttach != null) {
            btnAttach.setOnClickListener(v -> openImagePicker());
        }

        // Setup image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            sendMediaMessage(imageUri);
                        }
                    }
                });

        // Load messages (first load with progress bar)
        loadMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPolling();
        // Register FCM broadcast receiver
        androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this).registerReceiver(messageReceiver,
                        new android.content.IntentFilter("com.beeflirt.beetalk.NEW_MESSAGE"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
        // Unregister FCM broadcast receiver
        androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        pollHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Start polling untuk pesan baru
     */
    private void startPolling() {
        if (!isPolling) {
            isPolling = true;
            // Mulai polling setelah delay pertama (pesan awal sudah di-load di onCreate)
            pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
            Log.d(TAG, "Polling started");
        }
    }

    /**
     * Stop polling
     */
    private void stopPolling() {
        isPolling = false;
        pollHandler.removeCallbacks(pollRunnable);
        Log.d(TAG, "Polling stopped");
    }

    /**
     * Poll pesan baru tanpa menampilkan loading indicator.
     * Hanya menambahkan pesan baru yang belum ada di list.
     */
    private void pollNewMessages() {
        apiService.getMessages(matchId).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {

                if (!isPolling) return; // Activity sudah di-pause/destroy

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Map<String, Object> data = apiResponse.getData();

                        Gson gson = new Gson();
                        String messagesJson = gson.toJson(data.get("messages"));
                        Type listType = new TypeToken<List<Message>>() {}.getType();
                        List<Message> serverMessages = gson.fromJson(messagesJson, listType);

                        if (serverMessages != null && !serverMessages.isEmpty()) {
                            // Cari pesan baru berdasarkan ID (bukan count)
                            // Kumpulkan ID yang sudah ada di local list
                            java.util.Set<Integer> existingIds = new java.util.HashSet<>();
                            for (Message m : messages) {
                                existingIds.add(m.getId());
                            }

                            // Tambahkan hanya pesan yang belum ada
                            List<Message> newOnes = new ArrayList<>();
                            for (Message serverMsg : serverMessages) {
                                if (serverMsg.getId() > 0 && !existingIds.contains(serverMsg.getId())) {
                                    newOnes.add(serverMsg);
                                }
                            }

                            if (newOnes.isEmpty()) return;

                            int oldSize = messages.size();
                            messages.addAll(newOnes);
                            adapter.notifyItemRangeInserted(oldSize, newOnes.size());

                            // Scroll ke bawah jika user sudah di dekat bawah
                            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                            if (layoutManager != null) {
                                int lastVisible = layoutManager.findLastVisibleItemPosition();
                                // Auto-scroll jika user berada di dekat pesan terakhir (dalam 3 item)
                                if (lastVisible >= oldSize - 3) {
                                    recyclerView.scrollToPosition(messages.size() - 1);
                                }
                            }

                            // Hide empty view
                            tvEmpty.setVisibility(android.view.View.GONE);

                            // Mark as read
                            markAsRead();

                            Log.d(TAG, "Poll: " + (serverMessages.size() - oldSize) + " new messages");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                // Silent fail - jangan ganggu user, polling akan coba lagi
                Log.w(TAG, "Poll failed: " + t.getMessage());
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(userName);
        }

        // Set user info in toolbar
        CircleImageView ivToolbarPhoto = findViewById(R.id.iv_toolbar_photo);
        TextView tvToolbarName = findViewById(R.id.tv_toolbar_name);
        TextView tvToolbarStatus = findViewById(R.id.tv_toolbar_status);
        View viewOnlineDot = findViewById(R.id.view_online_dot);

        tvToolbarName.setText(userName);

        // Set online status
        String statusText = com.beeflirt.beetalk.utils.PresenceUtils.getStatusText(isOnline, lastSeen);
        tvToolbarStatus.setText(statusText);
        if (com.beeflirt.beetalk.utils.PresenceUtils.isOnline(isOnline)) {
            tvToolbarStatus.setTextColor(getResources().getColor(R.color.success));
            viewOnlineDot.setVisibility(View.VISIBLE);
        } else {
            tvToolbarStatus.setTextColor(getResources().getColor(R.color.text_secondary));
            viewOnlineDot.setVisibility(View.GONE);
        }

        if (userPhoto != null && !userPhoto.isEmpty()) {
            if (userPhoto.startsWith("http")) {
                Glide.with(this)
                        .load(userPhoto)
                        .centerCrop()
                        .into(ivToolbarPhoto);
            } else {
                String addPhotoUrl = Constants.IMAGE_BASE_URL + userPhoto;
                Glide.with(this)
                        .load(addPhotoUrl)
                        .centerCrop()
                        .into(ivToolbarPhoto);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(messages, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Load more saat scroll ke atas
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy < 0 && !isLoadingMore && hasMoreMessages) {
                    LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                    if (lm != null && lm.findFirstVisibleItemPosition() <= 2) {
                        loadOlderMessages();
                    }
                }
            }
        });
    }

    private void loadOlderMessages() {
        if (isLoadingMore || !hasMoreMessages) return;
        isLoadingMore = true;

        int offset = messages.size();
        apiService.getMessages(matchId, MESSAGES_PER_PAGE, offset).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                isLoadingMore = false;

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("messages"));
                    Type listType = new TypeToken<List<Message>>() {}.getType();
                    List<Message> olderMessages = gson.fromJson(json, listType);

                    if (olderMessages == null || olderMessages.isEmpty()) {
                        hasMoreMessages = false;
                        return;
                    }

                    // Filter duplikat
                    java.util.Set<Integer> existingIds = new java.util.HashSet<>();
                    for (Message m : messages) existingIds.add(m.getId());

                    List<Message> newOnes = new ArrayList<>();
                    for (Message m : olderMessages) {
                        if (m.getId() > 0 && !existingIds.contains(m.getId())) {
                            newOnes.add(m);
                        }
                    }

                    if (!newOnes.isEmpty()) {
                        messages.addAll(0, newOnes);
                        adapter.notifyItemRangeInserted(0, newOnes.size());
                        // Keep scroll position
                        recyclerView.scrollToPosition(newOnes.size());
                    }

                    if (olderMessages.size() < MESSAGES_PER_PAGE) {
                        hasMoreMessages = false;
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                isLoadingMore = false;
            }
        });
    }

    private void loadMessages() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        tvEmpty.setVisibility(android.view.View.GONE);

        chatRepository.getMessages(matchId, new RepositoryCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> newMessages) {
                progressBar.setVisibility(android.view.View.GONE);

                if (newMessages != null && !newMessages.isEmpty()) {
                    messages.clear();
                    messages.addAll(newMessages);
                    adapter.notifyDataSetChanged();

                    // Scroll to bottom
                    recyclerView.scrollToPosition(messages.size() - 1);

                    // Mark messages as read
                    markAsRead();
                } else {
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                }

                isFirstLoad = false;
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
                tvEmpty.setVisibility(android.view.View.VISIBLE);
                isFirstLoad = false;
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (messageText.isEmpty()) {
            return;
        }

        // Disable send button
        btnSend.setEnabled(false);

        JsonObject messageData = new JsonObject();
        messageData.addProperty("message", messageText);

        apiService.sendMessage(matchId, messageData).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                btnSend.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        // Clear input
                        etMessage.setText("");

                        // Gunakan data message langsung dari server (sudah punya ID + timestamp yang benar)
                        Map<String, Object> msgData = apiResponse.getData();
                        if (msgData != null) {
                            Gson gson = new Gson();
                            String msgJson = gson.toJson(msgData);
                            Message serverMessage = gson.fromJson(msgJson, Message.class);

                            if (serverMessage != null && serverMessage.getId() > 0) {
                                messages.add(serverMessage);
                                adapter.notifyItemInserted(messages.size() - 1);
                                chatRepository.cacheMessage(serverMessage);
                            }
                        }

                        // Scroll to bottom
                        recyclerView.scrollToPosition(messages.size() - 1);

                        // Hide empty view
                        tvEmpty.setVisibility(android.view.View.GONE);

                        Log.d(TAG, "Message sent successfully");
                    } else {
                        Toast.makeText(ChatActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                btnSend.setEnabled(true);
                Toast.makeText(ChatActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void sendMediaMessage(Uri imageUri) {
        try {
            File tempFile = com.beeflirt.beetalk.utils.ImageUtils.compressImage(this, imageUri);

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), tempFile);
            MultipartBody.Part mediaPart = MultipartBody.Part.createFormData("media", tempFile.getName(), requestFile);
            RequestBody messagePart = RequestBody.create(MediaType.parse("text/plain"), "");

            btnSend.setEnabled(false);

            apiService.sendMediaMessage(matchId, mediaPart, messagePart)
                    .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                        Response<ApiResponse<Map<String, Object>>> response) {
                    btnSend.setEnabled(true);
                    tempFile.delete();

                    if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                        Map<String, Object> msgData = response.body().getData();
                        if (msgData != null) {
                            Gson gson = new Gson();
                            String msgJson = gson.toJson(msgData);
                            Message serverMessage = gson.fromJson(msgJson, Message.class);

                            if (serverMessage != null && serverMessage.getId() > 0) {
                                messages.add(serverMessage);
                                adapter.notifyItemInserted(messages.size() - 1);
                                chatRepository.cacheMessage(serverMessage);
                            }
                        }
                        recyclerView.scrollToPosition(messages.size() - 1);
                        tvEmpty.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(ChatActivity.this, "Failed to send image", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                    btnSend.setEnabled(true);
                    tempFile.delete();
                    Toast.makeText(ChatActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "sendMediaMessage error: " + e.getMessage());
        }
    }

    private void markAsRead() {
        apiService.markMessagesAsRead(matchId).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                Log.d(TAG, "Messages marked as read");
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "Failed to mark as read: " + t.getMessage());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_unmatch) {
            confirmUnmatch();
            return true;
        } else if (item.getItemId() == R.id.action_block_report) {
            showBlockReportDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showBlockReportDialog() {
        com.beeflirt.beetalk.utils.BlockReportHelper.showBlockReportDialog(
                this,
                otherUserId,
                userName,
                new com.beeflirt.beetalk.utils.BlockReportHelper.OnActionCompleteListener() {
                    @Override
                    public void onBlockComplete() {
                        finish();
                    }

                    @Override
                    public void onReportComplete() {
                        android.widget.Toast.makeText(ChatActivity.this,
                                "Thank you for your report",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmUnmatch() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Unmatch")
                .setMessage("Are you sure you want to unmatch with " + userName
                        + "? This will delete all your conversation history.")
                .setPositiveButton("Unmatch", (dialog, which) -> unmatch())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void unmatch() {
        progressBar.setVisibility(android.view.View.VISIBLE);

        apiService.unmatch(matchId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                progressBar.setVisibility(android.view.View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Toast.makeText(ChatActivity.this, "Unmatched successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ChatActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to unmatch", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(ChatActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
