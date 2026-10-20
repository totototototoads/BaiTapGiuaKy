package com.example.bit240017_foodmanager.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DishWithDetails implements Serializable {
    private Dish dish;
    private DishCategory category;
    private List<DishImage> images = new ArrayList<>();

    public DishWithDetails() {
    }

    public DishWithDetails(Dish dish, DishCategory category, List<DishImage> images) {
        this.dish = dish;
        this.category = category;
        if (images != null) {
            this.images = images;
        }
    }

    public Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    public DishCategory getCategory() {
        return category;
    }

    public void setCategory(DishCategory category) {
        this.category = category;
    }

    public List<DishImage> getImages() {
        return images;
    }

    public void setImages(List<DishImage> images) {
        this.images = images;
    }

    public String getThumbnailUrl() {
        if (images != null) {
            for (DishImage img : images) {
                if (img.isThumbnail()) {
                    return img.getImageUrl();
                }
            }
            if (!images.isEmpty()) {
                return images.get(0).getImageUrl();
            }
        }
        return null;
    }
}
