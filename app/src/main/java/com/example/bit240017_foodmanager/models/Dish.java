package com.example.bit240017_foodmanager.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Dish implements Serializable {
    @SerializedName("id")
    private String mockApiId;

    @SerializedName("BIT240017_id")
    private String id;

    @SerializedName("BIT240017_name")
    private String name;

    @SerializedName("BIT240017_price")
    private double price;

    @SerializedName("BIT240017_preparationTime")
    private int preparationTime;

    @SerializedName("BIT240017_isAvailable")
    private boolean isAvailable = true;

    @SerializedName("BIT240017_description")
    private String description;

    @SerializedName("BIT240017_dishCategoryId")
    private String dishCategoryId;

    @SerializedName("BIT240017_dishImages")
    private List<DishImage> dishImages = new ArrayList<>();

    @SerializedName("BIT240017_isSynced")
    private boolean isSynced = true;

    @SerializedName("BIT240017_isDeleted")
    private boolean isDeleted = false;

    @SerializedName("BIT240017_updatedAt")
    private long updatedAt;

    public Dish() {
        this.updatedAt = System.currentTimeMillis();
    }

    public Dish(String id, String name, double price, int preparationTime, boolean isAvailable, String description, String dishCategoryId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.preparationTime = preparationTime;
        this.isAvailable = isAvailable;
        this.description = description;
        this.dishCategoryId = dishCategoryId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMockApiId() {
        return mockApiId;
    }

    public void setMockApiId(String mockApiId) {
        this.mockApiId = mockApiId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getPreparationTime() {
        return preparationTime;
    }

    public void setPreparationTime(int preparationTime) {
        this.preparationTime = preparationTime;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDishCategoryId() {
        return dishCategoryId;
    }

    public void setDishCategoryId(String dishCategoryId) {
        this.dishCategoryId = dishCategoryId;
    }

    public List<DishImage> getDishImages() {
        return dishImages;
    }

    public void setDishImages(List<DishImage> dishImages) {
        this.dishImages = dishImages;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Quick/Regular prep time calculation requirement:
    // PreparationTime <= 15 -> Quick / Nhanh
    // PreparationTime > 15 -> Regular / Thông thường
    public boolean isQuickPreparation() {
        return preparationTime <= 15;
    }
}
