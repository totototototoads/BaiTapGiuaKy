package com.example.bit240017_foodmanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bit240017_foodmanager.databinding.ItemImageGalleryBinding;
import com.example.bit240017_foodmanager.models.DishImage;

import java.util.ArrayList;
import java.util.List;

public class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder> {

    public interface OnThumbnailClickListener {
        void onSetThumbnail(DishImage image);
    }

    private List<DishImage> imageList = new ArrayList<>();
    private final OnThumbnailClickListener listener;

    public ImageGalleryAdapter(OnThumbnailClickListener listener) {
        this.listener = listener;
    }

    public void setImages(List<DishImage> images) {
        this.imageList = images != null ? images : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemImageGalleryBinding binding = ItemImageGalleryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ImageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        DishImage image = imageList.get(position);
        Context context = holder.itemView.getContext();

        Glide.with(context)
                .load(image.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.binding.ivGalleryImage);

        if (image.isThumbnail()) {
            holder.binding.tvThumbnailTag.setVisibility(View.VISIBLE);
            holder.binding.btnSetThumbnail.setVisibility(View.GONE);
        } else {
            holder.binding.tvThumbnailTag.setVisibility(View.GONE);
            holder.binding.btnSetThumbnail.setVisibility(View.VISIBLE);
        }

        holder.binding.btnSetThumbnail.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetThumbnail(image);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ItemImageGalleryBinding binding;

        ImageViewHolder(ItemImageGalleryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
