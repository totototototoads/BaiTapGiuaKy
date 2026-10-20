package com.example.bit240017_foodmanager.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bit240017_foodmanager.R;
import com.example.bit240017_foodmanager.databinding.ItemDishBinding;
import com.example.bit240017_foodmanager.models.Dish;
import com.example.bit240017_foodmanager.models.DishWithDetails;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DishAdapter extends RecyclerView.Adapter<DishAdapter.DishViewHolder> {

    public interface OnDishClickListener {
        void onDishClick(DishWithDetails dish);
        void onEditDish(DishWithDetails dish);
        void onDeleteDish(DishWithDetails dish);
    }

    private List<DishWithDetails> dishList = new ArrayList<>();
    private final OnDishClickListener listener;

    public DishAdapter(OnDishClickListener listener) {
        this.listener = listener;
    }

    public void setDishes(List<DishWithDetails> dishes) {
        this.dishList = dishes != null ? dishes : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDishBinding binding = ItemDishBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DishViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DishViewHolder holder, int position) {
        DishWithDetails item = dishList.get(position);
        Dish dish = item.getDish();
        Context context = holder.itemView.getContext();

        if (dish == null) return;

        // Name
        holder.binding.tvDishName.setText(dish.getName());

        // Category
        if (item.getCategory() != null) {
            holder.binding.tvCategoryName.setText(item.getCategory().getName());
        } else {
            holder.binding.tvCategoryName.setText("");
        }

        // Price formatting
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        holder.binding.tvDishPrice.setText(df.format(dish.getPrice()) + " VNĐ");

        // Preparation Time Badge requirement:
        // <= 15 mins -> Quick / Nhanh
        // > 15 mins -> Regular / Thông thường
        if (dish.isQuickPreparation()) {
            holder.binding.tvPrepTimeBadge.setText(context.getString(R.string.badge_quick));
            holder.binding.tvPrepTimeBadge.setBackgroundColor(Color.parseColor("#2E7D32")); // Green
        } else {
            holder.binding.tvPrepTimeBadge.setText(context.getString(R.string.badge_regular));
            holder.binding.tvPrepTimeBadge.setBackgroundColor(Color.parseColor("#1565C0")); // Blue
        }

        // Status Badge
        if (dish.isAvailable()) {
            holder.binding.tvStatusBadge.setText(context.getString(R.string.status_available));
            holder.binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#388E3C"));
        } else {
            holder.binding.tvStatusBadge.setText(context.getString(R.string.status_unavailable));
            holder.binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
        }

        // Load Thumbnail Image
        String thumbnailUrl = item.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            Glide.with(context)
                    .load(thumbnailUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.binding.ivDishThumbnail);
        } else {
            holder.binding.ivDishThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDishClick(item);
        });

        holder.binding.btnEditDish.setOnClickListener(v -> {
            if (listener != null) listener.onEditDish(item);
        });

        holder.binding.btnDeleteDish.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteDish(item);
        });
    }

    @Override
    public int getItemCount() {
        return dishList.size();
    }

    static class DishViewHolder extends RecyclerView.ViewHolder {
        final ItemDishBinding binding;

        DishViewHolder(ItemDishBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
