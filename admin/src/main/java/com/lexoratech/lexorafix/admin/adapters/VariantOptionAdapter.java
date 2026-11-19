package com.lexoratech.lexorafix.admin.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.lexoratech.lexorafix.admin.R;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the variant option-type rows in the admin form.
 * Each row holds an option name (e.g. "Voltage") and a comma-separated
 * list of values (e.g. "12V, 24V").
 */
public class VariantOptionAdapter extends RecyclerView.Adapter<VariantOptionAdapter.OptionViewHolder> {

    public static class OptionData {
        public String name = "";
        public String valuesRaw = ""; // comma-separated string for editing

        public OptionData() {
        }

        public OptionData(String name, List<String> values) {
            this.name = name != null ? name : "";
            this.valuesRaw = values != null ? android.text.TextUtils.join(", ", values) : "";
        }

        /**
         * Returns trimmed, non-empty values parsed from the raw comma-separated string.
         */
        public List<String> getParsedValues() {
            List<String> result = new ArrayList<>();
            if (valuesRaw == null || valuesRaw.trim().isEmpty())
                return result;
            for (String part : valuesRaw.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty())
                    result.add(trimmed);
            }
            return result;
        }
    }

    private final List<OptionData> options;

    public VariantOptionAdapter(List<OptionData> options) {
        this.options = options;
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_variant_option, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        holder.bind(options.get(position));
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    public List<OptionData> getOptions() {
        return options;
    }

    class OptionViewHolder extends RecyclerView.ViewHolder {
        TextInputEditText etName, etValues;
        ImageButton btnDelete;
        TextWatcher nameWatcher, valuesWatcher;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            etName = itemView.findViewById(R.id.et_option_name);
            etValues = itemView.findViewById(R.id.et_option_values);
            btnDelete = itemView.findViewById(R.id.btn_delete_option);
        }

        void bind(OptionData data) {
            // Remove old watchers before setting text
            if (nameWatcher != null)
                etName.removeTextChangedListener(nameWatcher);
            if (valuesWatcher != null)
                etValues.removeTextChangedListener(valuesWatcher);

            etName.setText(data.name);
            etValues.setText(data.valuesRaw);

            nameWatcher = simpleWatcher(s -> data.name = s);
            valuesWatcher = simpleWatcher(s -> data.valuesRaw = s);

            etName.addTextChangedListener(nameWatcher);
            etValues.addTextChangedListener(valuesWatcher);

            btnDelete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < options.size()) {
                    options.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, options.size());
                }
            });
        }

        private TextWatcher simpleWatcher(java.util.function.Consumer<String> setter) {
            return new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {
                }

                @Override
                public void onTextChanged(CharSequence s, int st, int b, int c) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    setter.accept(s.toString());
                }
            };
        }
    }
}
