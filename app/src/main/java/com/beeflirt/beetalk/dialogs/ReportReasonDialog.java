package com.beeflirt.beetalk.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportReasonDialog extends Dialog {

    private int reportedUserId;
    private RadioGroup rgReportReasons;
    private EditText etDescription;
    private Button btnCancel, btnSubmit;
    private OnReportSubmittedListener listener;

    public interface OnReportSubmittedListener {
        void onReportSubmitted();
    }

    public ReportReasonDialog(@NonNull Context context, int reportedUserId, OnReportSubmittedListener listener) {
        super(context);
        this.reportedUserId = reportedUserId;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_report_reason);

        rgReportReasons = findViewById(R.id.rgReportReasons);
        etDescription = findViewById(R.id.etDescription);
        btnCancel = findViewById(R.id.btnCancel);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Show description field when "Other" is selected
        rgReportReasons.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbOther) {
                etDescription.setVisibility(View.VISIBLE);
            } else {
                etDescription.setVisibility(View.GONE);
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        int selectedId = rgReportReasons.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(getContext(), "Please select a reason", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedReason = findViewById(selectedId);
        String reason = getReasonKey(selectedId);
        String description = etDescription.getText().toString().trim();

        // Show loading
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        ApiService apiService = ApiClient.getApiService(getContext());
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("reported_id", reportedUserId);
        requestBody.addProperty("reason", reason);
        if (!description.isEmpty()) {
            requestBody.addProperty("description", description);
        }

        apiService.reportUser(requestBody).enqueue(new Callback<com.beeflirt.beetalk.models.ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<com.beeflirt.beetalk.models.ApiResponse<Object>> call,
                    Response<com.beeflirt.beetalk.models.ApiResponse<Object>> response) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit Report");

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Report submitted successfully", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onReportSubmitted();
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed to submit report", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.beeflirt.beetalk.models.ApiResponse<Object>> call, Throwable t) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit Report");
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getReasonKey(int radioButtonId) {
        if (radioButtonId == R.id.rbInappropriatePhotos) {
            return "inappropriate_photos";
        } else if (radioButtonId == R.id.rbHarassment) {
            return "harassment";
        } else if (radioButtonId == R.id.rbSpam) {
            return "spam";
        } else if (radioButtonId == R.id.rbFakeProfile) {
            return "fake_profile";
        } else {
            return "other";
        }
    }
}
