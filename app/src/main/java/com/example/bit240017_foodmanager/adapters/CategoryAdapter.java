package com.example.bit240017_foodmanager.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bit240017_foodmanager.databinding.ItemCategoryBinding;
import com.example.bit240017_foodmanager.models.DishCategory;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryActionListener {
        void onDeleteCategory(DishCategory category);
    }

    private List<DishCategory> categoryList = new ArrayList<>();
    private final OnCategoryActionListener listener;

    public CategoryAdapter(OnCategoryActionListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<DishCategory> categories) {
        this.categoryList = categories != null ? categories : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        DishCategory cat = categoryList.get(position);
        holder.binding.tvCatName.setText(cat.getName());
        holder.binding.tvCatDesc.setText(cat.getDescription());

        holder.binding.btnDeleteCat.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteCategory(cat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        final ItemCategoryBinding binding;

        CategoryViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
