package com.example.bit240017_foodmanager.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class DishImage implements Serializable {
    @SerializedName("BIT240017_id")
    private String id;

    @SerializedName("BIT240017_dishId")
    private String dishId;

    @SerializedName("BIT240017_imageUrl")
    private String imageUrl;

    @SerializedName("BIT240017_isThumbnail")
    private boolean isThumbnail;

    public DishImage() {
    }

    public DishImage(String id, String dishId, String imageUrl, boolean isThumbnail) {
        this.id = id;
        this.dishId = dishId;
        this.imageUrl = imageUrl;
        this.isThumbnail = isThumbnail;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDishId() {
        return dishId;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isThumbnail() {
        return isThumbnail;
    }

    public void setThumbnail(boolean thumbnail) {
        isThumbnail = thumbnail;
    }
}
