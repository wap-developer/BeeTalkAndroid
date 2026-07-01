package com.beeflirt.beetalk.utils;

import android.app.Activity;
import android.content.Intent;

import com.beeflirt.beetalk.R;

public class ActivityTransitionHelper {

    /**
     * Start activity with fade transition
     */
    public static void startActivityWithFade(Activity activity, Intent intent) {
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Start activity with slide transition
     */
    public static void startActivityWithSlide(Activity activity, Intent intent) {
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Finish activity with fade transition
     */
    public static void finishWithFade(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Finish activity with slide back transition
     */
    public static void finishWithSlideBack(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
