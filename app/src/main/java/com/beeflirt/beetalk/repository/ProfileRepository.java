package com.beeflirt.beetalk.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.db.AppDatabase;
import com.beeflirt.beetalk.db.dao.PhotoDao;
import com.beeflirt.beetalk.db.dao.ProfileDao;
import com.beeflirt.beetalk.db.entity.PhotoEntity;
import com.beeflirt.beetalk.db.entity.ProfileEntity;
import com.beeflirt.beetalk.db.mapper.EntityMapper;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Profile;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {

    private final ApiService apiService;
    private final ProfileDao profileDao;
    private final PhotoDao photoDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ProfileRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        AppDatabase db = AppDatabase.getInstance(context);
        this.profileDao = db.profileDao();
        this.photoDao = db.photoDao();
    }

    public void getProfile(int userId, RepositoryCallback<Profile> callback) {
        apiService.getProfile().enqueue(new Callback<ApiResponse<Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<Profile>> call,
                    Response<ApiResponse<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Profile profile = response.body().getData();

                    if (profile != null) {
                        executor.execute(() -> {
                            profileDao.insert(EntityMapper.toEntity(profile));
                            if (profile.getPhotos() != null) {
                                photoDao.deleteByUserId(profile.getUserId());
                                photoDao.insertAll(EntityMapper.photosToEntities(profile.getPhotos()));
                            }
                        });
                        callback.onSuccess(profile);
                    } else {
                        fallbackProfile(userId, callback, "Profile data is empty");
                    }
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to load profile";
                    fallbackProfile(userId, callback, msg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Profile>> call, Throwable t) {
                fallbackProfile(userId, callback, t.getMessage());
            }
        });
    }

    private void fallbackProfile(int userId, RepositoryCallback<Profile> callback, String errorMsg) {
        executor.execute(() -> {
            ProfileEntity cached = profileDao.getByUserId(userId);
            if (cached != null) {
                List<PhotoEntity> photos = photoDao.getByUserId(userId);
                Profile result = EntityMapper.toProfile(cached, photos);
                mainHandler.post(() -> callback.onSuccess(result));
            } else {
                mainHandler.post(() -> callback.onError(errorMsg));
            }
        });
    }
}
