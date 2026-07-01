package com.beeflirt.beetalk.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.db.AppDatabase;
import com.beeflirt.beetalk.db.dao.MatchDao;
import com.beeflirt.beetalk.db.entity.MatchEntity;
import com.beeflirt.beetalk.db.mapper.EntityMapper;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Match;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchRepository {

    private final ApiService apiService;
    private final MatchDao matchDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public MatchRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        AppDatabase db = AppDatabase.getInstance(context);
        this.matchDao = db.matchDao();
    }

    public void getMatches(RepositoryCallback<List<Match>> callback) {
        apiService.getMatches().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("matches"));
                    Type listType = new TypeToken<List<Match>>() {}.getType();
                    List<Match> matches = gson.fromJson(json, listType);

                    if (matches != null) {
                        executor.execute(() -> {
                            matchDao.deleteAll();
                            matchDao.insertAll(EntityMapper.matchesToEntities(matches));
                        });
                        callback.onSuccess(matches);
                    } else {
                        callback.onSuccess(java.util.Collections.emptyList());
                    }
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to load matches";
                    fallbackMatches(callback, msg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                fallbackMatches(callback, t.getMessage());
            }
        });
    }

    private void fallbackMatches(RepositoryCallback<List<Match>> callback, String errorMsg) {
        executor.execute(() -> {
            List<MatchEntity> cached = matchDao.getAll();
            List<Match> result = EntityMapper.entitiesToMatches(cached);
            mainHandler.post(() -> {
                if (!result.isEmpty()) {
                    callback.onSuccess(result);
                } else {
                    callback.onError(errorMsg);
                }
            });
        });
    }
}
