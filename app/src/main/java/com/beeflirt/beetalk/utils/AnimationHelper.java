package com.beeflirt.beetalk.utils;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class AnimationHelper {

    /**
     * Apply entrance animation with delay
     */
    public static void animateEntrance(View view, int animationRes, long delay) {
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), animationRes);
        animation.setStartOffset(delay);
        view.startAnimation(animation);
    }

    /**
     * Apply button press animation (scale down)
     */
    public static void animateButtonPress(View button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    button.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    /**
     * Apply shake animation for errors
     */
    public static void animateShake(View view) {
        Animation shake = AnimationUtils.loadAnimation(view.getContext(),
                com.beeflirt.beetalk.R.anim.shake);
        view.startAnimation(shake);
    }

    /**
     * Fade in animation
     */
    public static void fadeIn(View view, long duration) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .start();
    }

    /**
     * Fade out animation
     */
    public static void fadeOut(View view, long duration) {
        view.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction(() -> view.setVisibility(View.GONE))
                .start();
    }

    /**
     * Slide up with fade animation
     */
    public static void slideUpFadeIn(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(100f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(delay)
                .start();
    }

    /**
     * Scale animation
     */
    public static void scaleIn(View view, long delay) {
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(delay)
                .start();
    }
}
