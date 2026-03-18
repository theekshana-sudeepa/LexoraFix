package com.lexoratech.lexorafix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.OnboardingItem;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnboardingViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_onboarding, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.setOnboardingData(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle, textTitleAccent, textDescription, textEmoji;
        private ImageView imageOnboarding;
        private MaterialCardView cardImage;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textTitleAccent = itemView.findViewById(R.id.textTitleAccent);
            textDescription = itemView.findViewById(R.id.textDescription);
            textEmoji = itemView.findViewById(R.id.textEmoji);
            imageOnboarding = itemView.findViewById(R.id.imageOnboarding);
            cardImage = itemView.findViewById(R.id.cardImage);
        }

        void setOnboardingData(OnboardingItem item) {
            textTitle.setText(item.getTitleLine1());
            textTitleAccent.setText(item.getTitleLine2());
            textDescription.setText(item.getDescription());
            textEmoji.setText(item.getEmoji());
            imageOnboarding.setImageResource(item.getImage());

            // --- Smooth entrance animations ---

            // Card: scale up + fade in
            AnimationSet cardAnim = new AnimationSet(true);
            ScaleAnimation scaleUp = new ScaleAnimation(0.8f, 1f, 0.8f, 1f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleUp.setDuration(600);
            AlphaAnimation cardFade = new AlphaAnimation(0f, 1f);
            cardFade.setDuration(600);
            cardAnim.addAnimation(scaleUp);
            cardAnim.addAnimation(cardFade);
            cardImage.startAnimation(cardAnim);

            // Emoji: fade in with slight bounce
            AnimationSet emojiAnim = new AnimationSet(true);
            ScaleAnimation emojiBounce = new ScaleAnimation(0.4f, 1f, 0.4f, 1f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            emojiBounce.setDuration(400);
            AlphaAnimation emojiFade = new AlphaAnimation(0f, 1f);
            emojiFade.setDuration(400);
            emojiAnim.addAnimation(emojiBounce);
            emojiAnim.addAnimation(emojiFade);
            emojiAnim.setStartOffset(350);
            textEmoji.startAnimation(emojiAnim);

            // Title line 1: slide up
            TranslateAnimation titleSlide = new TranslateAnimation(0, 0, 40, 0);
            titleSlide.setDuration(450);
            AlphaAnimation titleFade = new AlphaAnimation(0f, 1f);
            titleFade.setDuration(450);
            AnimationSet titleAnim = new AnimationSet(true);
            titleAnim.addAnimation(titleSlide);
            titleAnim.addAnimation(titleFade);
            titleAnim.setStartOffset(500);
            textTitle.startAnimation(titleAnim);

            // Title line 2 (accent): slide up slightly later
            TranslateAnimation accentSlide = new TranslateAnimation(0, 0, 30, 0);
            accentSlide.setDuration(450);
            AlphaAnimation accentFade = new AlphaAnimation(0f, 1f);
            accentFade.setDuration(450);
            AnimationSet accentAnim = new AnimationSet(true);
            accentAnim.addAnimation(accentSlide);
            accentAnim.addAnimation(accentFade);
            accentAnim.setStartOffset(600);
            textTitleAccent.startAnimation(accentAnim);

            // Description: fade in last
            AlphaAnimation descFade = new AlphaAnimation(0f, 1f);
            descFade.setDuration(500);
            descFade.setStartOffset(750);
            textDescription.startAnimation(descFade);
        }
    }
}
