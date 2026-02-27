package com.lexoratech.lexorafix.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.adapters.CategoryAdapter;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.models.Category;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class CategoryFragment extends Fragment {

    private CategoryAdapter adapter;
    private final List<Category> allCategories = new ArrayList<>();
    private View layoutEmptySearch;
    private RecyclerView rvCategories;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        rvCategories = view.findViewById(R.id.rv_categories);
        layoutEmptySearch = view.findViewById(R.id.layout_empty_search);
        EditText etSearch = view.findViewById(R.id.et_search);

        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        adapter = new CategoryAdapter(requireContext(), new ArrayList<>());
        adapter.setOnCategoryClickListener(category -> {
            ProductListFragment fragment = ProductListFragment.newInstance(
                    category.getDocumentId(), category.getName());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right,
                            android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right)
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvCategories.setAdapter(adapter);

        // Load categories from Firestore
        FirestoreHelper.getInstance().getAllCategories(
                new FirestoreHelper.FirestoreCallback<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.QuerySnapshot result) {
                        allCategories.clear();
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            Category cat = Category.fromMap(doc.getData());
                            if (cat != null) {
                                cat.setDocumentId(doc.getId());
                                allCategories.add(cat);
                            }
                        }
                        adapter.filterList(new ArrayList<>(allCategories));
                        updateEmptyState(allCategories.size());
                    }

                    @Override
                    public void onError(String message) {
                        // Silent fail
                    }
                });

        // Advanced Real-time Search Logic
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return view;
    }

    private void filter(String query) {
        if (adapter == null)
            return;

        List<Category> filteredList = new ArrayList<>();
        if (query.trim().isEmpty()) {
            filteredList.addAll(allCategories);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Category item : allCategories) {
                if (item.getName() != null && item.getName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.filterList(filteredList);
        updateEmptyState(filteredList.size());
    }

    private void updateEmptyState(int size) {
        if (layoutEmptySearch != null && rvCategories != null) {
            if (size == 0) {
                layoutEmptySearch.setVisibility(View.VISIBLE);
                rvCategories.setVisibility(View.GONE);
            } else {
                layoutEmptySearch.setVisibility(View.GONE);
                rvCategories.setVisibility(View.VISIBLE);
            }
        }
    }
}
