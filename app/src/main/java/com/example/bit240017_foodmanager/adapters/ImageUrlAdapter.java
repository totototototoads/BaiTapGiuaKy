package com.example.bit240017_foodmanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bit240017_foodmanager.databinding.ItemImageUrlInputBinding;
import com.example.bit240017_foodmanager.models.DishImage;

import java.util.ArrayList;
import java.util.List;

public class ImageUrlAdapter extends RecyclerView.Adapter<ImageUrlAdapter.UrlViewHolder> {

    public interface OnUrlActionListener {
        void onSelectThumbnail(int position);
        void onRemoveUrl(int position);
    }

    private List<DishImage> imageList = new ArrayList<>();
    private final OnUrlActionListener listener;

    public ImageUrlAdapter(OnUrlActionListener listener) {
        this.listener = listener;
    }

    public void setImages(List<DishImage> images) {
        this.imageList = images != null ? images : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<DishImage> getImages() {
        return imageList;
    }

    public void addImage(DishImage image) {
        if (imageList.isEmpty()) {
            image.setThumbnail(true); // First image is thumbnail by default
        }
        imageList.add(image);
        notifyDataSetChanged();
    }

    public void setThumbnail(int position) {
        for (int i = 0; i < imageList.size(); i++) {
            imageList.get(i).setThumbnail(i == position);
        }
        notifyDataSetChanged();
    }

    public void removeImage(int position) {
        if (position >= 0 && position < imageList.size()) {
            boolean wasThumbnail = imageList.get(position).isThumbnail();
            imageList.remove(position);
            if (wasThumbnail && !imageList.isEmpty()) {
                imageList.get(0).setThumbnail(true);
            }
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public UrlViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemImageUrlInputBinding binding = ItemImageUrlInputBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new UrlViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UrlViewHolder holder, int position) {
        DishImage image = imageList.get(position);
        Context context = holder.itemView.getContext();

        holder.binding.tvUrl.setText(image.getImageUrl());
        holder.binding.rbIsThumbnail.setChecked(image.isThumbnail());

        Glide.with(context)
                .load(image.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.binding.ivPreview);

        holder.binding.rbIsThumbnail.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelectThumbnail(holder.getAdapterPosition());
            }
        });

        holder.binding.btnRemoveUrl.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveUrl(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class UrlViewHolder extends RecyclerView.ViewHolder {
        final ItemImageUrlInputBinding binding;

        UrlViewHolder(ItemImageUrlInputBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
