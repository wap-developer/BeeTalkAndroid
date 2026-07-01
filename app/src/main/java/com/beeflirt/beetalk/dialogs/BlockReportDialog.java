package com.beeflirt.beetalk.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BlockReportDialog extends Dialog {

    private int targetUserId;
    private String targetUserName;
    private Button btnBlock, btnReport, btnCancel;
    private OnActionListener listener;

    public interface OnActionListener {
        void onBlock();

        void onReport();
    }

    public BlockReportDialog(@NonNull Context context, int targetUserId, String targetUserName,
            OnActionListener listener) {
        super(context);
        this.targetUserId = targetUserId;
        this.targetUserName = targetUserName;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_block_report);

        TextView tvTitle = findViewById(R.id.tvDialogTitle);
        btnBlock = findViewById(R.id.btnBlock);
        btnReport = findViewById(R.id.btnReport);
        btnCancel = findViewById(R.id.btnCancel);

        tvTitle.setText("Actions for " + targetUserName);

        btnBlock.setOnClickListener(v -> {
            dismiss();
            showBlockConfirmation();
        });

        btnReport.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onReport();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void showBlockConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Block " + targetUserName + "?")
                .setMessage(
                        "They won't be able to see your profile or message you. You won't see them in your swipe stack.")
                .setPositiveButton("Block", (dialog, which) -> blockUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void blockUser() {
        ApiService apiService = ApiClient.getApiService(getContext());
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("blocked_id", targetUserId);

        apiService.blockUser(requestBody).enqueue(new Callback<com.beeflirt.beetalk.models.ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<com.beeflirt.beetalk.models.ApiResponse<Object>> call,
                    Response<com.beeflirt.beetalk.models.ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), targetUserName + " has been blocked", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onBlock();
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to block user", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.beeflirt.beetalk.models.ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
