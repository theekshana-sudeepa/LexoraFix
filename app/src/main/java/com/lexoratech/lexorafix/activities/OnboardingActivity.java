package com.lexoratech.lexorafix.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.adapters.OnboardingAdapter;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.OnboardingItem;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutDots;
    private MaterialButton btnNextStep;
    private TextView btnSkip;
    private TextView tvPageCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply user's selected Dark Mode preference
        com.lexoratech.lexorafix.utils.ThemeUtils.applyTheme(this);

        setContentView(R.layout.activity_onboarding);

        layoutDots = findViewById(R.id.layoutDots);
        btnNextStep = findViewById(R.id.btnNextStep);
        btnSkip = findViewById(R.id.btnSkip);
        tvPageCounter = findViewById(R.id.tvPageCounter);

        setupOnboardingItems();

        ViewPager2 viewPagerOnboarding = findViewById(R.id.viewPagerOnboarding);
        viewPagerOnboarding.setAdapter(onboardingAdapter);

        // Smooth parallax page transformer
        viewPagerOnboarding.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1f - absPos * 0.4f);

            // Parallax on image card
            View card = page.findViewById(R.id.cardImage);
            if (card != null) {
                card.setTranslationX(-position * page.getWidth() * 0.15f);
            }
        });

        setupOnboardingIndicators();
        setCurrentOnboardingIndicator(0);
        tvPageCounter.setText("1 / " + onboardingAdapter.getItemCount());

        viewPagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentOnboardingIndicator(position);
                tvPageCounter.setText((position + 1) + " / " + onboardingAdapter.getItemCount());

                if (position == onboardingAdapter.getItemCount() - 1) {
                    btnNextStep.setText("Get Started  \u2192");
                    btnSkip.setVisibility(View.INVISIBLE);

                    // Pop animation on button
                    AnimatorSet popAnim = new AnimatorSet();
                    popAnim.playTogether(
                            ObjectAnimator.ofFloat(btnNextStep, "scaleX", 0.9f, 1.05f, 1f),
                            ObjectAnimator.ofFloat(btnNextStep, "scaleY", 0.9f, 1.05f, 1f)
                    );
                    popAnim.setDuration(400);
                    popAnim.setInterpolator(new OvershootInterpolator(2f));
                    popAnim.start();
                } else {
                    btnNextStep.setText("Next  \u203A");
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnNextStep.setOnClickListener(v -> {
            if (viewPagerOnboarding.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                viewPagerOnboarding.setCurrentItem(viewPagerOnboarding.getCurrentItem() + 1);
            } else {
                completeOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> completeOnboarding());

        // Entrance animation for button
        btnNextStep.setAlpha(0f);
        btnNextStep.setTranslationY(40f);
        btnNextStep.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(300).start();
    }

    private void completeOnboarding() {
        SharedPreferencesManager.getInstance(getApplicationContext()).setFirstLaunchDone();
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        onboardingItems.add(new OnboardingItem(
                R.drawable.img_promo_tools,
                "\uD83D\uDD27",
                "Expert Craftsmen",
                "At Your Service",
                "Connect with verified, top-rated professionals for plumbing, electrical, carpentry, and more."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.img_promo_garden,
                "\uD83D\uDEE0\uFE0F",
                "Book & Shop",
                "Anytime, Anywhere",
                "Schedule service bookings, browse premium tools, and manage everything from one place."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.img_promo_drills,
                "\u2714\uFE0F",
                "Secure, Rated",
                "& Guaranteed",
                "Every job is backed by quality checks, verified reviews, and hassle-free customer support."
        ));

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void setupOnboardingIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(6, 0, 6, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(), R.drawable.onboarding_dot_inactive));
            indicators[i].setLayoutParams(layoutParams);
            layoutDots.addView(indicators[i]);
        }
    }

    private void setCurrentOnboardingIndicator(int index) {
        int childCount = layoutDots.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView dot = (ImageView) layoutDots.getChildAt(i);
            boolean isActive = (i == index);

            dot.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                    isActive ? R.drawable.onboarding_dot_active : R.drawable.onboarding_dot_inactive));

            // Reset any previous scale
            dot.setScaleX(1f);
            dot.setScaleY(1f);
        }
    }
}
