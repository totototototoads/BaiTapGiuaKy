package com.example.bit240017_foodmanager;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bit240017_foodmanager.adapters.ImageGalleryAdapter;
import com.example.bit240017_foodmanager.database.DishDao;
import com.example.bit240017_foodmanager.database.ImageDao;
import com.example.bit240017_foodmanager.databinding.ActivityDishDetailBinding;
import com.example.bit240017_foodmanager.models.Dish;
import com.example.bit240017_foodmanager.models.DishWithDetails;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DishDetailActivity extends BaseActivity {

    private ActivityDishDetailBinding binding;
    private DishDao dishDao;
    private ImageDao imageDao;
    private ImageGalleryAdapter galleryAdapter;
    private String dishId;
    private DishWithDetails currentDetails;

    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> loadDishDetails()
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDishDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dishDao = new DishDao(this);
        imageDao = new ImageDao(this);

        dishId = getIntent().getStringExtra("DISH_ID");
        if (dishId == null) {
            Toast.makeText(this, R.string.empty_dish_list, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupGalleryRecyclerView();

        binding.btnEditDish.setOnClickListener(v -> {
            Intent intent = new Intent(DishDetailActivity.this, DishAddEditActivity.class);
            intent.putExtra("DISH_ID", dishId);
            editLauncher.launch(intent);
        });

        binding.btnDeleteDish.setOnClickListener(v -> confirmDelete());

        loadDishDetails();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupGalleryRecyclerView() {
        galleryAdapter = new ImageGalleryAdapter(image -> {
            if (dishId != null && image != null) {
                imageDao.setThumbnailImage(dishId, image.getId());
                Toast.makeText(this, R.string.is_thumbnail_tag, Toast.LENGTH_SHORT).show();
                loadDishDetails();
            }
        });

        binding.rvImageGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvImageGallery.setAdapter(galleryAdapter);
    }

    private void loadDishDetails() {
        currentDetails = dishDao.getDishDetailsById(dishId);
        if (currentDetails == null || currentDetails.getDish() == null) {
            finish();
            return;
        }

        Dish dish = currentDetails.getDish();

        binding.tvDetailName.setText(dish.getName());

        if (currentDetails.getCategory() != null) {
            binding.tvDetailCategory.setText(currentDetails.getCategory().getName());
        } else {
            binding.tvDetailCategory.setText("");
        }

        // Format Price
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        binding.tvDetailPrice.setText(df.format(dish.getPrice()) + " VNĐ");

        binding.tvDetailPrepTime.setText(dish.getPreparationTime() + " " + getString(R.string.input_prep_time));

        // Prep time classification
        if (dish.isQuickPreparation()) {
            binding.tvPrepTimeBadge.setText(R.string.badge_quick);
            binding.tvPrepTimeBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
        } else {
            binding.tvPrepTimeBadge.setText(R.string.badge_regular);
            binding.tvPrepTimeBadge.setBackgroundColor(Color.parseColor("#1565C0"));
        }

        // Availability status
        if (dish.isAvailable()) {
            binding.tvStatusBadge.setText(R.string.status_available);
            binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#388E3C"));
        } else {
            binding.tvStatusBadge.setText(R.string.status_unavailable);
            binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
        }

        binding.tvDetailDescription.setText(dish.getDescription() != null ? dish.getDescription() : "");

        galleryAdapter.setImages(currentDetails.getImages());
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_dish_msg)
                .setPositiveButton(R.string.btn_confirm_delete, (dialog, which) -> {
                    dishDao.deleteDish(dishId);
                    Toast.makeText(this, R.string.msg_dish_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
