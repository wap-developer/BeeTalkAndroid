package com.beeflirt.beetalk.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.beeflirt.beetalk.R;

/**
 * Modern, reusable loading dialog to replace all inline ProgressBar usages.
 * Shows a circular progress indicator with a customizable message.
 *
 * Usage:
 *   LoadingDialog loadingDialog = new LoadingDialog(this);
 *   loadingDialog.show("Signing in...");
 *   loadingDialog.dismiss();
 */
public class LoadingDialog {

    private final Context context;
    private Dialog dialog;
    private TextView tvTitle;
    private TextView tvSubtitle;

    public LoadingDialog(Context context) {
        this.context = context;
        buildDialog();
    }

    private void buildDialog() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        dialog.setContentView(view);

        tvTitle    = view.findViewById(R.id.tv_loading_title);
        tvSubtitle = view.findViewById(R.id.tv_loading_subtitle);

        // Transparent + rounded window
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    /**
     * Show the dialog with default message.
     */
    public void show() {
        show("Please wait...", "Processing your request");
    }

    /**
     * Show the dialog with a custom title.
     */
    public void show(String title) {
        show(title, "Processing your request");
    }

    /**
     * Show the dialog with a custom title and subtitle.
     */
    public void show(String title, String subtitle) {
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    /**
     * Dismiss the dialog safely.
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * Check whether the dialog is currently visible.
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
