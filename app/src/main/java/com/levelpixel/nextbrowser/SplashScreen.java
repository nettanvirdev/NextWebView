package com.levelpixel.nextbrowser;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {
    private View logoContainer;
    private TextView appNameText, taglineText;
    private View splashContent;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ensure we're not in fullscreen mode
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        setContentView(R.layout.activity_splash_screen);

        logoContainer = findViewById(R.id.logo_container);
        appNameText = findViewById(R.id.splash_text);
        taglineText = findViewById(R.id.splash_tagline);
        splashContent = findViewById(R.id.splash_content);

        startEntranceAnimation();
        new Handler(Looper.getMainLooper()).postDelayed(this::startExitAnimation, 1500);
    }

    private void startEntranceAnimation() {
        AnimatorSet entranceSet = new AnimatorSet();

        // Logo animation with slight bounce
        ObjectAnimator logoFade = ObjectAnimator.ofFloat(logoContainer, View.ALPHA, 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoContainer, View.SCALE_X, 0.8f, 1.05f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoContainer, View.SCALE_Y, 0.8f, 1.05f, 1f);

        // Text animations
        ObjectAnimator titleFade = ObjectAnimator.ofFloat(appNameText, View.ALPHA, 0f, 1f);
        ObjectAnimator titleTranslate = ObjectAnimator.ofFloat(appNameText, View.TRANSLATION_Y, 20f, 0f);

        ObjectAnimator taglineFade = ObjectAnimator.ofFloat(taglineText, View.ALPHA, 0f, 1f);
        ObjectAnimator taglineTranslate = ObjectAnimator.ofFloat(taglineText, View.TRANSLATION_Y, 10f, 0f);

        // Logo animation
        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(logoFade, logoScaleX, logoScaleY);
        logoAnim.setInterpolator(new OvershootInterpolator(0.5f));
        logoAnim.setDuration(600);

        // Text animations
        AnimatorSet textAnim = new AnimatorSet();
        textAnim.playTogether(titleFade, titleTranslate, taglineFade, taglineTranslate);
        textAnim.setInterpolator(new DecelerateInterpolator());
        textAnim.setDuration(400);

        // Play logo first, then text
        entranceSet.play(logoAnim).before(textAnim);
        entranceSet.start();
    }

    private void startExitAnimation() {
        AnimatorSet exitSet = new AnimatorSet();

        ObjectAnimator contentFade = ObjectAnimator.ofFloat(splashContent, View.ALPHA, 1f, 0f);
        ObjectAnimator contentScale = ObjectAnimator.ofFloat(splashContent, View.SCALE_X, 1f, 1.05f);
        ObjectAnimator contentScaleY = ObjectAnimator.ofFloat(splashContent, View.SCALE_Y, 1f, 1.05f);

        exitSet.playTogether(contentFade, contentScale, contentScaleY);
        exitSet.setDuration(400);
        exitSet.setInterpolator(new AccelerateInterpolator());

        exitSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                startActivity(new Intent(SplashScreen.this, MainActivity.class));
                finish();
                overridePendingTransition(0, 0);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        exitSet.start();
    }
}