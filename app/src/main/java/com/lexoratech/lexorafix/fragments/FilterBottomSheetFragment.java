package com.lexoratech.lexorafix.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.slider.RangeSlider;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bottom sheet for filtering products by price range, sort order, category,
 * minimum rating, and stock availability.
 */
public class FilterBottomSheetFragment extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFiltersApplied(float minPrice, float maxPrice, String sortOrder,
                String categoryDocId, int minRating, boolean inStockOnly);
    }

    private FilterListener listener;
    private float currentMinPrice = 0, currentMaxPrice = 50000;
    private String currentSort = "Newest";
    private String currentCategoryDocId = ""; // empty => All
    private int currentMinRating = 0; // 0 => All
    private boolean currentInStockOnly = false;

    private List<Category> categories = new ArrayList<>();

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public void setCurrentFilters(float min, float max, String sort,
            String categoryDocId, int minRating, boolean inStockOnly) {
        this.currentMinPrice = min;
        this.currentMaxPrice = max;
        this.currentSort = sort;
        this.currentCategoryDocId = categoryDocId != null ? categoryDocId : "";
        this.currentMinRating = minRating;
        this.currentInStockOnly = inStockOnly;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RangeSlider priceSlider = view.findViewById(R.id.price_slider);
        TextView tvPriceRange = view.findViewById(R.id.tv_price_range);
        ChipGroup sortGroup = view.findViewById(R.id.sort_chip_group);
        ChipGroup categoryGroup = view.findViewById(R.id.category_chip_group);
        ChipGroup ratingGroup = view.findViewById(R.id.rating_chip_group);
        SwitchCompat switchInStock = view.findViewById(R.id.switch_in_stock);
        MaterialButton btnApply = view.findViewById(R.id.btn_apply);
        MaterialButton btnReset = view.findViewById(R.id.btn_reset);

        // ── Price slider ──
        priceSlider.setValueFrom(0);
        priceSlider.setValueTo(50000);
        priceSlider.setStepSize(500);
        priceSlider.setValues(currentMinPrice, currentMaxPrice);
        updatePriceLabel(tvPriceRange, currentMinPrice, currentMaxPrice);

        priceSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            updatePriceLabel(tvPriceRange, values.get(0), values.get(1));
        });

        // ── Sort chips ──
        selectChipByText(sortGroup, currentSort);

        // ── Dynamic category chips ──
        buildCategoryChips(categoryGroup);

        // ── Rating chips ──
        String ratingLabel = ratingToLabel(currentMinRating);
        selectChipByText(ratingGroup, ratingLabel);

        // ── Stock switch ──
        switchInStock.setChecked(currentInStockOnly);

        // ── Apply ──
        btnApply.setOnClickListener(v -> {
            List<Float> prices = priceSlider.getValues();
            String sort = getSelectedChipText(sortGroup, "Newest");
            String catDocId = getSelectedCategoryDocId(categoryGroup);
            int minRating = labelToRating(getSelectedChipText(ratingGroup, "All"));
            boolean inStockOnly = switchInStock.isChecked();

            if (listener != null) {
                listener.onFiltersApplied(prices.get(0), prices.get(1), sort,
                        catDocId, minRating, inStockOnly);
            }
            dismiss();
        });

        // ── Reset ──
        btnReset.setOnClickListener(v -> {
            priceSlider.setValues(0f, 50000f);
            updatePriceLabel(tvPriceRange, 0, 50000);
            selectChipByText(sortGroup, "Newest");
            selectChipByText(categoryGroup, "All");
            selectChipByText(ratingGroup, "All");
            switchInStock.setChecked(false);
        });
    }

    // ── Helpers ──

    private void buildCategoryChips(ChipGroup group) {
        group.removeAllViews();

        // "All" chip
        Chip allChip = new Chip(requireContext());
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setTag(""); // empty doc id means "All"
        applyChipStyle(allChip);
        group.addView(allChip);

        // One chip per Firestore category
        for (Category cat : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(cat.getName());
            chip.setCheckable(true);
            chip.setTag(cat.getDocumentId());
            applyChipStyle(chip);
            group.addView(chip);
        }

        // Pre-select
        if (currentCategoryDocId == null || currentCategoryDocId.isEmpty()) {
            allChip.setChecked(true);
        } else {
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip c = (Chip) group.getChildAt(i);
                if (currentCategoryDocId.equals(c.getTag())) {
                    c.setChecked(true);
                    break;
                }
            }
        }
    }

    private void applyChipStyle(Chip chip) {
        // Programmatically mirror the XML style attributes
        chip.setChipBackgroundColorResource(R.color.colorSurface);
        chip.setCheckedIconVisible(false);
        chip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    private String getSelectedCategoryDocId(ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id != -1) {
            Chip chip = group.findViewById(id);
            if (chip != null && chip.getTag() instanceof String) {
                return (String) chip.getTag();
            }
        }
        return ""; // "All"
    }

    private void updatePriceLabel(TextView tv, float min, float max) {
        tv.setText(String.format(Locale.getDefault(), "LKR %.0f — LKR %.0f", min, max));
    }

    private void selectChipByText(ChipGroup group, String text) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(text)) {
                chip.setChecked(true);
                return;
            }
        }
    }

    private String getSelectedChipText(ChipGroup group, String defaultVal) {
        int id = group.getCheckedChipId();
        if (id != -1) {
            Chip chip = group.findViewById(id);
            return chip != null ? chip.getText().toString() : defaultVal;
        }
        return defaultVal;
    }

    private String ratingToLabel(int rating) {
        switch (rating) {
            case 2:
                return "2★ & Up";
            case 3:
                return "3★ & Up";
            case 4:
                return "4★ & Up";
            default:
                return "All";
        }
    }

    private int labelToRating(String label) {
        if (label.startsWith("4"))
            return 4;
        if (label.startsWith("3"))
            return 3;
        if (label.startsWith("2"))
            return 2;
        return 0;
    }
}
