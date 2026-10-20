package com.example.bit240017_foodmanager.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class DishCategory implements Serializable {
    @SerializedName("id")
    private String mockApiId;

    @SerializedName("BIT240017_id")
    private String id;

    @SerializedName("BIT240017_name")
    private String name;

    @SerializedName("BIT240017_description")
    private String description;

    @SerializedName("BIT240017_isSynced")
    private boolean isSynced = true;

    @SerializedName("BIT240017_isDeleted")
    private boolean isDeleted = false;

    public DishCategory() {
    }

    public DishCategory(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
