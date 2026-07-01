package com.beeflirt.beetalk.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.dialogs.LoadingDialog;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.GalleryPost;
import com.beeflirt.beetalk.utils.Constants;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GalleryActivity extends BaseActivity {

    private RecyclerView rvGallery;
    private LinearLayout layoutEmpty;
    private View progressBar;
    private ApiService apiService;
    private LoadingDialog loadingDialog;
    private List<GalleryPost> posts = new ArrayList<>();
    private RecyclerView.Adapter<?> adapter;

    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getApiService(this);
        loadingDialog = new LoadingDialog(this);

        rvGallery = findViewById(R.id.rv_gallery);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);

        // Grid 2 columns
        setupAdapter();
        rvGallery.setLayoutManager(new GridLayoutManager(this, 2));
        rvGallery.setAdapter(adapter);

        // FAB upload
        findViewById(R.id.fab_upload).setOnClickListener(v -> checkPermissionAndPick());

        // Launchers
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { if (granted) openPicker(); });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) showUploadDialog();
                    }
                });

        loadGallery();
    }

    private void setupAdapter() {
        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_my_gallery, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                GalleryPost post = posts.get(position);

                ImageView ivPhoto = holder.itemView.findViewById(R.id.iv_photo);
                android.widget.TextView tvCaption = holder.itemView.findViewById(R.id.tv_caption);
                View btnDelete = holder.itemView.findViewById(R.id.btn_delete);

                String url = post.getPhotoUrl();
                if (url != null && !url.isEmpty()) {
                    String fullUrl = url.startsWith("http") ? url : Constants.IMAGE_BASE_URL + url;
                    Glide.with(GalleryActivity.this).load(fullUrl).centerCrop().into(ivPhoto);
                }

                if (post.getCaption() != null && !post.getCaption().isEmpty()) {
                    tvCaption.setText(post.getCaption());
                    tvCaption.setVisibility(View.VISIBLE);
                } else {
                    tvCaption.setVisibility(View.GONE);
                }

                btnDelete.setOnClickListener(v -> confirmDelete(post, position));

                // Animate
                holder.itemView.setAlpha(0f);
                holder.itemView.animate().alpha(1f).setDuration(300).setStartDelay(position * 50L).start();
            }

            @Override
            public int getItemCount() { return posts.size(); }
        };
    }

    private void loadGallery() {
        progressBar.setVisibility(View.VISIBLE);

        apiService.getMyGallery().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("posts"));
                    Type listType = new TypeToken<List<GalleryPost>>() {}.getType();
                    List<GalleryPost> result = gson.fromJson(json, listType);

                    posts.clear();
                    if (result != null) posts.addAll(result);
                    adapter.notifyDataSetChanged();
                    updateEmpty();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GalleryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmpty() {
        if (posts.isEmpty()) {
            rvGallery.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvGallery.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void checkPermissionAndPick() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openPicker();
        } else {
            permissionLauncher.launch(perm);
        }
    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void showUploadDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_upload_gallery);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        ImageView ivPreview = dialog.findViewById(R.id.iv_preview);
        EditText etCaption = dialog.findViewById(R.id.et_caption);
        View btnCancel = dialog.findViewById(R.id.btn_cancel);
        View btnPost = dialog.findViewById(R.id.btn_post);

        Glide.with(this).load(selectedImageUri).centerCrop().into(ivPreview);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnPost.setOnClickListener(v -> {
            dialog.dismiss();
            uploadPost(selectedImageUri, etCaption.getText().toString().trim());
        });

        dialog.show();
    }

    private void uploadPost(Uri imageUri, String caption) {
        loadingDialog.show("Posting...", "Uploading your photo");

        try {
            File file = com.beeflirt.beetalk.utils.ImageUtils.compressImage(this, imageUri);

            RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
            MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo", file.getName(), reqFile);
            RequestBody captionPart = RequestBody.create(MediaType.parse("text/plain"), caption);

            apiService.uploadGalleryPost(photoPart, captionPart).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                        Response<ApiResponse<Map<String, Object>>> response) {
                    if (file.exists()) file.delete();
                    loadingDialog.dismiss();

                    if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                        Toast.makeText(GalleryActivity.this, "Posted!", Toast.LENGTH_SHORT).show();
                        loadGallery();
                    } else {
                        Toast.makeText(GalleryActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                    if (file.exists()) file.delete();
                    loadingDialog.dismiss();
                    Toast.makeText(GalleryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            loadingDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(GalleryPost post, int position) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (d, w) -> {
                    apiService.deleteGalleryPost(post.getId()).enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                            if (response.isSuccessful()) {
                                posts.remove(position);
                                adapter.notifyItemRemoved(position);
                                updateEmpty();
                                Toast.makeText(GalleryActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            Toast.makeText(GalleryActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private File createTempFile(Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            File f = File.createTempFile("gallery_", ".jpg", getCacheDir());
            java.io.FileOutputStream os = new java.io.FileOutputStream(f);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) os.write(buf, 0, len);
            os.close();
            is.close();
            return f;
        } catch (Exception e) {
            return null;
        }
    }
}
