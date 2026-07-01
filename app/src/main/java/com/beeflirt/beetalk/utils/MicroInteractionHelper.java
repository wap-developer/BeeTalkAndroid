package com.beeflirt.beetalk.utils;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class MicroInteractionHelper {

    /**
     * FAB hide animation on scroll down
     */
    public static void hideFAB(View fab) {
        fab.animate()
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /**
     * FAB show animation on scroll up
     */
    public static void showFAB(View fab) {
        fab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /**
     * Pulse animation for notifications
     */
    public static void pulseAnimation(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);

        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.setRepeatCount(2);
        scaleY.setRepeatCount(2);

        scaleX.start();
        scaleY.start();
    }

    /**
     * Success checkmark animation
     */
    public static void successAnimation(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setVisibility(View.VISIBLE);

        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();
    }

    /**
     * Ripple effect on touch
     */
    public static void rippleEffect(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    /**
     * Bounce animation
     */
    public static void bounceAnimation(View view) {
        view.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    /**
     * Rotate animation for refresh
     */
    public static void rotateRefresh(View view) {
        view.animate()
                .rotation(360f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> view.setRotation(0f))
                .start();
    }
}
