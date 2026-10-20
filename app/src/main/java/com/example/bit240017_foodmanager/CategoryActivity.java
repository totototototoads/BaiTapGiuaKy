package com.example.bit240017_foodmanager;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bit240017_foodmanager.adapters.CategoryAdapter;
import com.example.bit240017_foodmanager.database.CategoryDao;
import com.example.bit240017_foodmanager.databinding.ActivityCategoryBinding;
import com.example.bit240017_foodmanager.databinding.DialogAddCategoryBinding;
import com.example.bit240017_foodmanager.models.DishCategory;

import java.util.List;

public class CategoryActivity extends BaseActivity {

    private ActivityCategoryBinding binding;
    private CategoryDao categoryDao;
    private CategoryAdapter categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        categoryDao = new CategoryDao(this);

        setupToolbar();
        setupRecyclerView();

        binding.fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        loadCategories();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(this::confirmDeleteCategory);
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void loadCategories() {
        List<DishCategory> categories = categoryDao.getAllCategories();
        categoryAdapter.setCategories(categories);
    }

    private void showAddCategoryDialog() {
        DialogAddCategoryBinding dialogBinding = DialogAddCategoryBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_add_category)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = dialogBinding.etCatName.getText() != null ? dialogBinding.etCatName.getText().toString().trim() : "";
                String desc = dialogBinding.etCatDesc.getText() != null ? dialogBinding.etCatDesc.getText().toString().trim() : "";

                if (TextUtils.isEmpty(name)) {
                    dialogBinding.tilCatName.setError(getString(R.string.error_empty_cat_name));
                    return;
                }

                DishCategory newCat = new DishCategory(null, name, desc);
                newCat.setSynced(false);
                String id = categoryDao.insertCategory(newCat);

                if (id != null) {
                    Toast.makeText(CategoryActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                    com.example.bit240017_foodmanager.network.SyncManager.getInstance(CategoryActivity.this).triggerAutoSync();
                    loadCategories();
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void confirmDeleteCategory(DishCategory category) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_cat_msg)
                .setPositiveButton(R.string.btn_confirm_delete, (dialog, which) -> {
                    try {
                        categoryDao.deleteCategory(category.getId());
                        Toast.makeText(this, R.string.msg_category_deleted, Toast.LENGTH_SHORT).show();
                        com.example.bit240017_foodmanager.network.SyncManager.getInstance(this).triggerAutoSync();
                        loadCategories();
                    } catch (IllegalStateException e) {
                        // Business Constraint 4.4: Cannot delete category in use
                        Toast.makeText(this, R.string.error_cat_in_use, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
