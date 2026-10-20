package com.example.bit240017_foodmanager;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bit240017_foodmanager.adapters.ImageUrlAdapter;
import com.example.bit240017_foodmanager.database.CategoryDao;
import com.example.bit240017_foodmanager.database.DishDao;
import com.example.bit240017_foodmanager.databinding.ActivityDishAddEditBinding;
import com.example.bit240017_foodmanager.models.Dish;
import com.example.bit240017_foodmanager.models.DishCategory;
import com.example.bit240017_foodmanager.models.DishImage;
import com.example.bit240017_foodmanager.models.DishWithDetails;
import com.example.bit240017_foodmanager.network.SyncManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DishAddEditActivity extends BaseActivity {

    private ActivityDishAddEditBinding binding;
    private DishDao dishDao;
    private CategoryDao categoryDao;
    private ImageUrlAdapter imageUrlAdapter;
    private SyncManager syncManager;

    private String editingDishId = null;
    private Dish existingDish = null;
    private List<DishCategory> categoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDishAddEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dishDao = new DishDao(this);
        categoryDao = new CategoryDao(this);
        syncManager = SyncManager.getInstance(this);

        if (getIntent().hasExtra("DISH_ID")) {
            editingDishId = getIntent().getStringExtra("DISH_ID");
        }

        setupToolbar();
        setupCategorySpinner();
        setupImageRecyclerView();

        binding.btnAddImage.setOnClickListener(v -> addImageUrl());
        binding.btnSaveDish.setOnClickListener(v -> saveDish());

        if (editingDishId != null) {
            binding.toolbar.setTitle(R.string.title_edit_dish);
            loadExistingDishData();
        } else {
            binding.toolbar.setTitle(R.string.title_add_dish);
            // Default sample image for quick testing
            imageUrlAdapter.addImage(new DishImage(null, null, "https://images.unsplash.com/photo-1544025162-d76694265947?w=500", true));
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategorySpinner() {
        categoryList = categoryDao.getAllCategories();
        List<String> categoryNames = new ArrayList<>();
        for (DishCategory cat : categoryList) {
            categoryNames.add(cat.getName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spCategory.setAdapter(spinnerAdapter);
    }

    private void setupImageRecyclerView() {
        imageUrlAdapter = new ImageUrlAdapter(new ImageUrlAdapter.OnUrlActionListener() {
            @Override
            public void onSelectThumbnail(int position) {
                imageUrlAdapter.setThumbnail(position);
            }

            @Override
            public void onRemoveUrl(int position) {
                imageUrlAdapter.removeImage(position);
            }
        });

        binding.rvImageUrls.setLayoutManager(new LinearLayoutManager(this));
        binding.rvImageUrls.setAdapter(imageUrlAdapter);
    }

    private void loadExistingDishData() {
        DishWithDetails details = dishDao.getDishDetailsById(editingDishId);
        if (details == null || details.getDish() == null) {
            finish();
            return;
        }

        existingDish = details.getDish();

        binding.etDishName.setText(existingDish.getName());
        binding.etPrice.setText(String.valueOf((long) existingDish.getPrice()));
        binding.etPrepTime.setText(String.valueOf(existingDish.getPreparationTime()));
        binding.switchIsAvailable.setChecked(existingDish.isAvailable());
        binding.etDescription.setText(existingDish.getDescription() != null ? existingDish.getDescription() : "");

        // Select Category in Spinner
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).getId().equalsIgnoreCase(existingDish.getDishCategoryId())) {
                binding.spCategory.setSelection(i);
                break;
            }
        }

        // Load Images
        if (details.getImages() != null && !details.getImages().isEmpty()) {
            imageUrlAdapter.setImages(details.getImages());
        }
    }

    private void addImageUrl() {
        String url = binding.etImageUrl.getText() != null ? binding.etImageUrl.getText().toString().trim() : "";
        if (TextUtils.isEmpty(url)) {
            binding.tilImageUrl.setError(getString(R.string.error_invalid_image_url));
            return;
        }

        binding.tilImageUrl.setError(null);
        boolean isFirst = imageUrlAdapter.getImages().isEmpty();
        imageUrlAdapter.addImage(new DishImage(null, editingDishId, url, isFirst));
        binding.etImageUrl.setText("");
    }

    private void saveDish() {
        // Clear previous errors
        binding.tilDishName.setError(null);
        binding.tilPrice.setError(null);
        binding.tilPrepTime.setError(null);

        String name = binding.etDishName.getText() != null ? binding.etDishName.getText().toString().trim() : "";
        String priceStr = binding.etPrice.getText() != null ? binding.etPrice.getText().toString().trim() : "";
        String prepTimeStr = binding.etPrepTime.getText() != null ? binding.etPrepTime.getText().toString().trim() : "";
        String description = binding.etDescription.getText() != null ? binding.etDescription.getText().toString().trim() : "";
        boolean isAvailable = binding.switchIsAvailable.isChecked();

        // 1. Name Validation
        if (TextUtils.isEmpty(name)) {
            binding.tilDishName.setError(getString(R.string.error_empty_name));
            return;
        }

        // 2. Price Validation
        double price = 0;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                binding.tilPrice.setError(getString(R.string.error_invalid_price));
                return;
            }
        } catch (NumberFormatException e) {
            binding.tilPrice.setError(getString(R.string.error_invalid_price));
            return;
        }

        // 3. Prep Time Validation
        int prepTime = 0;
        try {
            prepTime = Integer.parseInt(prepTimeStr);
            if (prepTime <= 0) {
                binding.tilPrepTime.setError(getString(R.string.error_invalid_prep_time));
                return;
            }
        } catch (NumberFormatException e) {
            binding.tilPrepTime.setError(getString(R.string.error_invalid_prep_time));
            return;
        }

        // 4. Category Validation
        int catPosition = binding.spCategory.getSelectedItemPosition();
        if (catPosition < 0 || catPosition >= categoryList.size()) {
            Toast.makeText(this, R.string.error_empty_category, Toast.LENGTH_SHORT).show();
            return;
        }
        String categoryId = categoryList.get(catPosition).getId();

        // 5. Duplicate Name Validation
        if (dishDao.isDishNameDuplicateInCategory(name, categoryId, editingDishId)) {
            binding.tilDishName.setError(getString(R.string.error_duplicate_dish_name));
            return;
        }

        // 6. Image List Validation
        List<DishImage> images = imageUrlAdapter.getImages();
        if (images.isEmpty()) {
            Toast.makeText(this, R.string.error_no_images, Toast.LENGTH_SHORT).show();
            return;
        }

        // Save / Update Dish
        Dish dish = existingDish != null ? existingDish : new Dish();
        if (editingDishId == null) {
            dish.setId("dish_" + UUID.randomUUID().toString());
        }
        dish.setName(name);
        dish.setPrice(price);
        dish.setPreparationTime(prepTime);
        dish.setAvailable(isAvailable);
        dish.setDescription(description);
        dish.setDishCategoryId(categoryId);
        dish.setSynced(false); // Pending sync for offline/online sync manager
        dish.setDeleted(false);

        try {
            if (editingDishId == null) {
                dishDao.insertDish(dish, images);
            } else {
                dishDao.updateDish(dish, images);
            }

            Toast.makeText(this, R.string.msg_dish_saved, Toast.LENGTH_SHORT).show();
            syncManager.triggerAutoSync();
            finish();

        } catch (IllegalArgumentException e) {
            if ("DUPLICATE_NAME".equals(e.getMessage())) {
                binding.tilDishName.setError(getString(R.string.error_duplicate_dish_name));
            } else {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
