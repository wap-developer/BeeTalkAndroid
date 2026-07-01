package com.beeflirt.beetalk.fragments;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adapters.AvatarAdapter;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Photo;
import com.beeflirt.beetalk.dialogs.LoadingDialog;
import com.beeflirt.beetalk.utils.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SetupPhotosFragment extends Fragment {

    private CircleImageView ivProfilePhoto;
    private CardView btnUploadPhoto, btnChooseAvatar, cardAvatarGrid, cardRequirement;
    private RecyclerView rvAvatars;
    private View uploadLoadingOverlay;
    private View btnChangePhoto;
    private TextView tvRequirement;

    private ApiService apiService;
    private AvatarAdapter avatarAdapter;
    private LoadingDialog loadingDialog;

    private boolean hasPhoto = false; // Track apakah user sudah pilih foto/avatar
    private int selectedAvatarResId = -1; // Track avatar yang dipilih

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    private OnPhotoCountChangeListener listener;

    // Avatar resource IDs
    private static final int[] AVATAR_RES_IDS = {
            R.drawable.avatar_male_1,
            R.drawable.avatar_male_2,
            R.drawable.avatar_male_3,
            R.drawable.avatar_male_4,
            R.drawable.avatar_female_1,
            R.drawable.avatar_female_2,
            R.drawable.avatar_female_3,
            R.drawable.avatar_female_4,
    };

    public interface OnPhotoCountChangeListener {
        void onPhotoCountChanged(int count);
    }

    public void setOnPhotoCountChangeListener(OnPhotoCountChangeListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_photos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo);
        btnUploadPhoto = view.findViewById(R.id.btn_upload_photo);
        btnChooseAvatar = view.findViewById(R.id.btn_choose_avatar);
        cardAvatarGrid = view.findViewById(R.id.card_avatar_grid);
        cardRequirement = view.findViewById(R.id.card_requirement);
        rvAvatars = view.findViewById(R.id.rv_avatars);
        uploadLoadingOverlay = view.findViewById(R.id.upload_loading_overlay);
        btnChangePhoto = view.findViewById(R.id.btn_change_photo);
        tvRequirement = view.findViewById(R.id.tv_requirement);

        apiService = ApiClient.getApiService(requireContext());
        loadingDialog = new LoadingDialog(requireContext());

        // Setup launchers
        setupImagePickerLauncher();
        setupPermissionLauncher();

        // Setup avatar grid
        setupAvatarGrid();

        // Click listeners
        btnUploadPhoto.setOnClickListener(v -> {
            hideAvatarGrid();
            checkPermissionAndOpenPicker();
        });

        btnChooseAvatar.setOnClickListener(v -> toggleAvatarGrid());

        btnChangePhoto.setOnClickListener(v -> checkPermissionAndOpenPicker());

        // Load existing photo if any
        loadExistingPhoto();
    }

    private void setupAvatarGrid() {
        avatarAdapter = new AvatarAdapter(AVATAR_RES_IDS, (resId, position) -> {
            selectedAvatarResId = resId;
            ivProfilePhoto.setImageResource(resId);

            // Animate the profile photo
            ivProfilePhoto.animate()
                    .scaleX(0.9f).scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction(() -> ivProfilePhoto.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(150)
                            .start())
                    .start();

            setHasPhoto(true);

            // Upload avatar as profile photo
            uploadAvatarAsPhoto(resId);
        });

        rvAvatars.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        rvAvatars.setAdapter(avatarAdapter);
    }

    private void toggleAvatarGrid() {
        if (cardAvatarGrid.getVisibility() == View.VISIBLE) {
            hideAvatarGrid();
        } else {
            showAvatarGrid();
        }
    }

    private void showAvatarGrid() {
        cardAvatarGrid.setVisibility(View.VISIBLE);
        cardAvatarGrid.setAlpha(0f);
        cardAvatarGrid.setTranslationY(-20f);
        cardAvatarGrid.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();
    }

    private void hideAvatarGrid() {
        if (cardAvatarGrid.getVisibility() == View.VISIBLE) {
            cardAvatarGrid.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(200)
                    .withEndAction(() -> cardAvatarGrid.setVisibility(View.GONE))
                    .start();
        }
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // Show preview immediately
                            Glide.with(requireContext())
                                    .load(imageUri)
                                    .centerCrop()
                                    .into(ivProfilePhoto);

                            selectedAvatarResId = -1; // Reset avatar selection
                            if (avatarAdapter != null) {
                                avatarAdapter.setSelectedPosition(-1);
                            }

                            showUploadLoading(true);
                            uploadPhoto(imageUri);
                        }
                    }
                }
        );
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(requireContext(), "Gallery permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkPermissionAndOpenPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void showUploadLoading(boolean show) {
        if (show) {
            loadingDialog.show("Uploading...", "Setting up your profile photo");
        } else {
            loadingDialog.dismiss();
        }
    }

    /**
     * Upload foto dari gallery
     */
    private void uploadPhoto(Uri imageUri) {
        try {
            File photoFile = com.beeflirt.beetalk.utils.ImageUtils.compressImage(requireContext(), imageUri);

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("photo", photoFile.getName(), requestFile);

            apiService.uploadPhoto(body).enqueue(new Callback<ApiResponse<java.util.Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<java.util.Map<String, Object>>> call,
                        Response<ApiResponse<java.util.Map<String, Object>>> response) {
                    if (photoFile.exists()) photoFile.delete();
                    showUploadLoading(false);

                    if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                        setHasPhoto(true);
                        Toast.makeText(requireContext(), "Photo uploaded!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to upload photo", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<java.util.Map<String, Object>>> call, Throwable t) {
                    if (photoFile.exists()) photoFile.delete();
                    showUploadLoading(false);
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            showUploadLoading(false);
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Upload avatar drawable sebagai foto profil.
     * Convert drawable ke bitmap, simpan ke temp file, lalu upload.
     */
    private void uploadAvatarAsPhoto(int avatarResId) {
        showUploadLoading(true);

        try {
            // Convert drawable to bitmap
            android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(requireContext(), avatarResId);
            if (drawable == null) {
                showUploadLoading(false);
                return;
            }

            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                    256, 256, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            // Save to temp file
            File tempFile = File.createTempFile("avatar_", ".png", requireContext().getCacheDir());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // Upload
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/png"), tempFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("photo", tempFile.getName(), requestFile);

            apiService.uploadPhoto(body).enqueue(new Callback<ApiResponse<java.util.Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<java.util.Map<String, Object>>> call,
                        Response<ApiResponse<java.util.Map<String, Object>>> response) {
                    if (tempFile.exists()) tempFile.delete();
                    showUploadLoading(false);

                    if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                        setHasPhoto(true);
                        Toast.makeText(requireContext(), "Avatar set!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to set avatar", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<java.util.Map<String, Object>>> call, Throwable t) {
                    if (tempFile.exists()) tempFile.delete();
                    showUploadLoading(false);
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            showUploadLoading(false);
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load foto profil yang sudah ada (jika user kembali ke step ini)
     */
    private void loadExistingPhoto() {
        apiService.getProfile().enqueue(new Callback<ApiResponse<com.beeflirt.beetalk.models.Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.beeflirt.beetalk.models.Profile>> call,
                    Response<ApiResponse<com.beeflirt.beetalk.models.Profile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    com.beeflirt.beetalk.models.Profile profile = response.body().getData();
                    if (profile != null && profile.getPhotos() != null && !profile.getPhotos().isEmpty()) {
                        Photo primaryPhoto = profile.getPhotos().get(0);
                        String photoUrl = primaryPhoto.getPhotoUrl();

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.IMAGE_BASE_URL + photoUrl;
                            if (isAdded()) {
                                Glide.with(requireContext())
                                        .load(fullUrl)
                                        .centerCrop()
                                        .into(ivProfilePhoto);
                            }
                            setHasPhoto(true);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.beeflirt.beetalk.models.Profile>> call, Throwable t) {
                // Ignore - first time setup
            }
        });
    }

    private void setHasPhoto(boolean has) {
        hasPhoto = has;

        // Update requirement card
        if (cardRequirement != null) {
            if (has) {
                cardRequirement.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> cardRequirement.setVisibility(View.GONE))
                        .start();
            } else {
                cardRequirement.setVisibility(View.VISIBLE);
                cardRequirement.setAlpha(1f);
            }
        }

        // Notify parent
        if (listener != null) {
            listener.onPhotoCountChanged(has ? 1 : 0);
        }
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = File.createTempFile("upload_", ".jpg", requireContext().getCacheDir());
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getPhotoCount() {
        return hasPhoto ? 1 : 0;
    }
}
