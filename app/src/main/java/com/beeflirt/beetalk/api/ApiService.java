package com.beeflirt.beetalk.api;

import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Profile;
import com.beeflirt.beetalk.models.User;
import com.google.gson.JsonObject;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ==================== AUTHENTICATION ====================

    @POST("auth/register")
    Call<ApiResponse<Map<String, Object>>> register(@Body JsonObject body);

    @POST("auth/login")
    Call<ApiResponse<Map<String, Object>>> login(@Body JsonObject body);

    @GET("auth/me")
    Call<ApiResponse<User>> getCurrentUser();

    @POST("auth/logout")
    Call<ApiResponse<Object>> logout();

    @POST("auth/refresh")
    Call<ApiResponse<Map<String, String>>> refreshToken();

    @PUT("auth/change-password")
    Call<ApiResponse<Object>> changePassword(@Body JsonObject body);

    @POST("auth/update-email")
    Call<ApiResponse<Map<String, Object>>> updateEmail(@Body JsonObject body);

    @POST("auth/verify-otp")
    Call<ApiResponse<Object>> verifyOtp(@Body JsonObject body);

    @POST("auth/resend-verification")
    Call<ApiResponse<Object>> resendVerification(@Body JsonObject body);

    @POST("auth/forgot-password")
    Call<ApiResponse<Object>> forgotPassword(@Body JsonObject body);

    @POST("auth/verify-reset-otp")
    Call<ApiResponse<Object>> verifyResetOtp(@Body JsonObject body);

    @POST("auth/reset-password")
    Call<ApiResponse<Object>> resetPassword(@Body JsonObject body);

    @DELETE("auth/account")
    Call<ApiResponse<Object>> deleteAccount();

    // ==================== PROFILE ====================

    @GET("profile")
    Call<ApiResponse<Profile>> getProfile();

    @GET("profile/completion-status")
    Call<ApiResponse<Map<String, Object>>> getProfileCompletionStatus();

    @PUT("profile")
    Call<ApiResponse<Profile>> updateProfile(@Body JsonObject body);

    @POST("profile/populate-location") //
    Call<ApiResponse<Map<String, String>>> populateLocation();

    @Multipart
    @POST("profile/photos")
    Call<ApiResponse<Map<String, Object>>> uploadPhoto(@Part MultipartBody.Part photo);

    @DELETE("profile/photos/{id}")
    Call<ApiResponse<Object>> deletePhoto(@Path("id") int photoId);

    @PUT("profile/photos/{id}/primary")
    Call<ApiResponse<Object>> setPrimaryPhoto(@Path("id") int photoId);

    @GET("profile/preferences")
    Call<ApiResponse<Map<String, Object>>> getPreferences();

    @PUT("profile/preferences")
    Call<ApiResponse<Map<String, Object>>> updatePreferences(@Body JsonObject body);

    // ==================== SWIPE & MATCH ====================

    @GET("swipe/potential")
    Call<ApiResponse<Map<String, Object>>> getPotentialMatches();

    @POST("swipe")
    Call<ApiResponse<Map<String, Object>>> swipe(@Body JsonObject body);

    @POST("swipe/super-like")
    Call<ApiResponse<Map<String, Object>>> superLike(@Body JsonObject body);

    @GET("swipe/super-like-quota")
    Call<ApiResponse<Map<String, Object>>> getSuperLikeQuota();

    @POST("swipe/super-like-reward")
    Call<ApiResponse<Map<String, Object>>> addRewardedSuperLike();

    @GET("swipe/received-super-likes")
    Call<ApiResponse<Map<String, Object>>> getReceivedSuperLikes();

    @GET("swipe/undo-quota")
    Call<ApiResponse<Map<String, Object>>> getUndoQuota();

    @POST("swipe/undo")
    Call<ApiResponse<Map<String, Object>>> undoSwipe();

    // ==================== PREMIUM ====================

    @GET("premium/entitlements")
    Call<ApiResponse<Map<String, Object>>> getEntitlements();

    @GET("premium/who-liked-me")
    Call<ApiResponse<Map<String, Object>>> getWhoLikedMe();

    @POST("premium/verify-purchase")
    Call<ApiResponse<Map<String, Object>>> verifyPurchase(@Body JsonObject body);

    @POST("premium/boost")
    Call<ApiResponse<Map<String, Object>>> activateBoost();

    // ==================== NOTIFICATIONS ====================

    @POST("notifications/register-token")
    Call<Object> registerDeviceToken(@Body JsonObject body);

    @GET("matches")
    Call<ApiResponse<Map<String, Object>>> getMatches();

    @DELETE("matches/{id}")
    Call<ApiResponse<Object>> unmatch(@Path("id") int matchId);

    // ==================== CHAT ====================

    @GET("chat/conversations")
    Call<ApiResponse<Map<String, Object>>> getConversations();

    @GET("chat/{matchId}/messages")
    Call<ApiResponse<Map<String, Object>>> getMessages(@Path("matchId") int matchId);

    @GET("chat/{matchId}/messages")
    Call<ApiResponse<Map<String, Object>>> getMessages(@Path("matchId") int matchId, @retrofit2.http.Query("limit") int limit, @retrofit2.http.Query("offset") int offset);

    @POST("chat/{matchId}/messages")
    Call<ApiResponse<Map<String, Object>>> sendMessage(
            @Path("matchId") int matchId,
            @Body JsonObject body);

    @Multipart
    @POST("chat/{matchId}/messages")
    Call<ApiResponse<Map<String, Object>>> sendMediaMessage(
            @Path("matchId") int matchId,
            @Part MultipartBody.Part media,
            @Part("message") RequestBody message);

    @PUT("chat/messages/{matchId}/read")
    Call<ApiResponse<Map<String, Object>>> markMessagesAsRead(@Path("matchId") int matchId);

    // ==================== BLOCK & REPORT ====================

    @POST("block")
    Call<ApiResponse<Object>> blockUser(@Body JsonObject body);

    @DELETE("block/{userId}")
    Call<ApiResponse<Object>> unblockUser(@Path("userId") int userId);

    @GET("blocked")
    Call<ApiResponse<Map<String, Object>>> getBlockedUsers();

    @POST("report")
    Call<ApiResponse<Object>> reportUser(@Body JsonObject body);

    @GET("reports")
    Call<ApiResponse<Map<String, Object>>> getUserReports();

    // ── Explore ──
    @GET("explore/nearby")
    Call<ApiResponse<Map<String, Object>>> getNearbyUsers();

    @GET("explore/nearby")
    Call<ApiResponse<Map<String, Object>>> getNearbyUsers(@Query("limit") int limit, @Query("offset") int offset);

    // ── Gallery ──
    @GET("gallery/feed")
    Call<ApiResponse<Map<String, Object>>> getGalleryFeed();

    @GET("gallery/feed")
    Call<ApiResponse<Map<String, Object>>> getGalleryFeed(@Query("limit") int limit, @Query("offset") int offset);

    @GET("gallery")
    Call<ApiResponse<Map<String, Object>>> getMyGallery();

    @Multipart
    @POST("gallery")
    Call<ApiResponse<Map<String, Object>>> uploadGalleryPost(@Part MultipartBody.Part photo, @Part("caption") RequestBody caption);

    @DELETE("gallery/{id}")
    Call<ApiResponse<Object>> deleteGalleryPost(@Path("id") int id);

    // ── Gallery Comments ──
    @GET("gallery/posts/{id}")
    Call<ApiResponse<Map<String, Object>>> getPostDetail(@Path("id") int postId);

    @GET("gallery/posts/{id}/comments")
    Call<ApiResponse<Map<String, Object>>> getPostComments(@Path("id") int postId, @Query("limit") int limit, @Query("offset") int offset);

    @POST("gallery/posts/{id}/comments")
    Call<ApiResponse<Map<String, Object>>> addComment(@Path("id") int postId, @Body JsonObject body);

    @DELETE("gallery/posts/{id}/comments/{commentId}")
    Call<ApiResponse<Object>> deleteComment(@Path("id") int postId, @Path("commentId") int commentId);

    @POST("gallery/posts/{id}/comments/{commentId}/like")
    Call<ApiResponse<Map<String, Object>>> likeComment(@Path("id") int postId, @Path("commentId") int commentId);

    @POST("gallery/posts/{id}/like")
    Call<ApiResponse<Map<String, Object>>> likePost(@Path("id") int postId);

    @GET("gallery/posts/{id}/comments/{commentId}/replies")
    Call<ApiResponse<Map<String, Object>>> getCommentReplies(@Path("id") int postId, @Path("commentId") int commentId);
}
