package com.beeflirt.beetalk.utils;

import android.content.Context;

import com.beeflirt.beetalk.dialogs.BlockReportDialog;
import com.beeflirt.beetalk.dialogs.ReportReasonDialog;

public class BlockReportHelper {

    public static void showBlockReportDialog(Context context, int userId, String userName,
            OnActionCompleteListener listener) {
        BlockReportDialog dialog = new BlockReportDialog(context, userId, userName,
                new BlockReportDialog.OnActionListener() {
                    @Override
                    public void onBlock() {
                        if (listener != null) {
                            listener.onBlockComplete();
                        }
                    }

                    @Override
                    public void onReport() {
                        showReportDialog(context, userId, listener);
                    }
                });
        dialog.show();
    }

    private static void showReportDialog(Context context, int userId, OnActionCompleteListener listener) {
        ReportReasonDialog reportDialog = new ReportReasonDialog(context, userId,
                () -> {
                    if (listener != null) {
                        listener.onReportComplete();
                    }
                });
        reportDialog.show();
    }

    public interface OnActionCompleteListener {
        void onBlockComplete();

        void onReportComplete();
    }
}
