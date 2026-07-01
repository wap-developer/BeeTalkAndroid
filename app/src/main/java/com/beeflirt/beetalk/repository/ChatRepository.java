package com.beeflirt.beetalk.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.db.AppDatabase;
import com.beeflirt.beetalk.db.dao.ConversationDao;
import com.beeflirt.beetalk.db.dao.MessageDao;
import com.beeflirt.beetalk.db.entity.ConversationEntity;
import com.beeflirt.beetalk.db.entity.MessageEntity;
import com.beeflirt.beetalk.db.mapper.EntityMapper;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Conversation;
import com.beeflirt.beetalk.models.Message;
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

public class ChatRepository {

    private final ApiService apiService;
    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ChatRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        AppDatabase db = AppDatabase.getInstance(context);
        this.conversationDao = db.conversationDao();
        this.messageDao = db.messageDao();
    }

    public void getConversations(RepositoryCallback<List<Conversation>> callback) {
        apiService.getConversations().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("conversations"));
                    Type listType = new TypeToken<List<Conversation>>() {}.getType();
                    List<Conversation> conversations = gson.fromJson(json, listType);

                    if (conversations != null) {
                        executor.execute(() -> {
                            conversationDao.deleteAll();
                            conversationDao.insertAll(EntityMapper.conversationsToEntities(conversations));
                        });
                        callback.onSuccess(conversations);
                    } else {
                        callback.onSuccess(java.util.Collections.emptyList());
                    }
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to load conversations";
                    fallbackConversations(callback, msg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                fallbackConversations(callback, t.getMessage());
            }
        });
    }

    private void fallbackConversations(RepositoryCallback<List<Conversation>> callback, String errorMsg) {
        executor.execute(() -> {
            List<ConversationEntity> cached = conversationDao.getAll();
            List<Conversation> result = EntityMapper.entitiesToConversations(cached);
            mainHandler.post(() -> {
                if (!result.isEmpty()) {
                    callback.onSuccess(result);
                } else {
                    callback.onError(errorMsg);
                }
            });
        });
    }

    public void getMessages(int matchId, RepositoryCallback<List<Message>> callback) {
        apiService.getMessages(matchId, 100, 0).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("messages"));
                    Type listType = new TypeToken<List<Message>>() {}.getType();
                    List<Message> messages = gson.fromJson(json, listType);

                    if (messages != null) {
                        executor.execute(() -> {
                            messageDao.deleteByMatchId(matchId);
                            messageDao.insertAll(EntityMapper.messagesToEntities(messages));
                        });
                        callback.onSuccess(messages);
                    } else {
                        callback.onSuccess(java.util.Collections.emptyList());
                    }
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to load messages";
                    fallbackMessages(matchId, callback, msg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                fallbackMessages(matchId, callback, t.getMessage());
            }
        });
    }

    private void fallbackMessages(int matchId, RepositoryCallback<List<Message>> callback, String errorMsg) {
        executor.execute(() -> {
            List<MessageEntity> cached = messageDao.getByMatchId(matchId);
            List<Message> result = EntityMapper.entitiesToMessages(cached);
            mainHandler.post(() -> {
                if (!result.isEmpty()) {
                    callback.onSuccess(result);
                } else {
                    callback.onError(errorMsg);
                }
            });
        });
    }

    public void cacheMessage(Message message) {
        executor.execute(() -> messageDao.insert(EntityMapper.toEntity(message)));
    }
}
