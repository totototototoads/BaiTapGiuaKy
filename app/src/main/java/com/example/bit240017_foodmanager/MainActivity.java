package com.example.bit240017_foodmanager;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bit240017_foodmanager.adapters.DishAdapter;
import com.example.bit240017_foodmanager.database.CategoryDao;
import com.example.bit240017_foodmanager.database.DishDao;
import com.example.bit240017_foodmanager.databinding.ActivityMainBinding;
import com.example.bit240017_foodmanager.databinding.DialogFilterDishBinding;
import com.example.bit240017_foodmanager.models.DishCategory;
import com.example.bit240017_foodmanager.models.DishWithDetails;
import com.example.bit240017_foodmanager.network.SyncManager;
import com.example.bit240017_foodmanager.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;
    private DishDao dishDao;
    private CategoryDao categoryDao;
    private DishAdapter dishAdapter;
    private SyncManager syncManager;

    // Filter State Variables (Persisted across screen navigation)
    private String currentSearchQuery = "";
    private String selectedCategoryId = "ALL";
    private boolean isAvailableOnly = false;
    private Double minPrice = null;
    private Double maxPrice = null;
    private DishDao.SortOrder sortOrder = DishDao.SortOrder.PRICE_ASC;

    private final ActivityResultLauncher<Intent> detailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> loadDishes()
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dishDao = new DishDao(this);
        categoryDao = new CategoryDao(this);
        syncManager = SyncManager.getInstance(this);

        setupToolbar();
        setupSearchAndFilter();
        setupRecyclerView();
        setupNetworkObserver();

        binding.fabAddDish.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DishAddEditActivity.class);
            detailLauncher.launch(intent);
        });

        loadDishes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDishes();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 101, 0, R.string.btn_manage_categories)
                .setIcon(android.R.drawable.ic_menu_agenda)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu.add(0, 102, 1, R.string.lang_switch)
                .setIcon(android.R.drawable.ic_menu_sort_by_size)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 101) {
            Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
            startActivity(intent);
            return true;
        } else if (id == 102) {
            showLanguageDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLanguageDialog() {
        String currentLang = LocaleHelper.getLanguage(this);
        int checkedItem = LocaleHelper.LANGUAGE_VIETNAMESE.equals(currentLang) ? 1 : 0;
        String[] languages = new String[]{getString(R.string.lang_english), getString(R.string.lang_vietnamese)};

        new AlertDialog.Builder(this)
                .setTitle(R.string.lang_switch)
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String newLang = which == 1 ? LocaleHelper.LANGUAGE_VIETNAMESE : LocaleHelper.LANGUAGE_ENGLISH;
                    LocaleHelper.setLocale(MainActivity.this, newLang);
                    dialog.dismiss();
                    // Restart activity to apply language everywhere
                    finish();
                    startActivity(getIntent());
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void setupSearchAndFilter() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString() : "";
                loadDishes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        DialogFilterDishBinding filterBinding = DialogFilterDishBinding.inflate(getLayoutInflater());

        // Setup Category Spinner
        List<DishCategory> categories = categoryDao.getAllCategories();
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add(getString(R.string.filter_all_categories));

        int selectedIndex = 0;
        for (int i = 0; i < categories.size(); i++) {
            DishCategory cat = categories.get(i);
            categoryNames.add(cat.getName());
            if (cat.getId().equalsIgnoreCase(selectedCategoryId)) {
                selectedIndex = i + 1;
            }
        }

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterBinding.spCategoryFilter.setAdapter(catAdapter);
        filterBinding.spCategoryFilter.setSelection(selectedIndex);

        // Setup Availability Switch
        filterBinding.switchAvailableOnly.setChecked(isAvailableOnly);

        // Setup Price Range
        if (minPrice != null) filterBinding.etMinPrice.setText(String.valueOf(minPrice));
        if (maxPrice != null) filterBinding.etMaxPrice.setText(String.valueOf(maxPrice));

        // Setup Sort Order Spinner
        String[] sortOptions = new String[]{
                getString(R.string.sort_price_asc),
                getString(R.string.sort_price_desc),
                getString(R.string.sort_prep_time_asc)
        };
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterBinding.spSortOrder.setAdapter(sortAdapter);

        if (sortOrder == DishDao.SortOrder.PRICE_DESC) {
            filterBinding.spSortOrder.setSelection(1);
        } else if (sortOrder == DishDao.SortOrder.PREP_TIME_ASC) {
            filterBinding.spSortOrder.setSelection(2);
        } else {
            filterBinding.spSortOrder.setSelection(0);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.filter_title)
                .setView(filterBinding.getRoot())
                .setPositiveButton(R.string.btn_apply_filter, (dialog, which) -> {
                    int catPos = filterBinding.spCategoryFilter.getSelectedItemPosition();
                    if (catPos == 0) {
                        selectedCategoryId = "ALL";
                    } else {
                        selectedCategoryId = categories.get(catPos - 1).getId();
                    }

                    isAvailableOnly = filterBinding.switchAvailableOnly.isChecked();

                    String minStr = filterBinding.etMinPrice.getText() != null ? filterBinding.etMinPrice.getText().toString().trim() : "";
                    String maxStr = filterBinding.etMaxPrice.getText() != null ? filterBinding.etMaxPrice.getText().toString().trim() : "";

                    minPrice = TextUtils.isEmpty(minStr) ? null : Double.parseDouble(minStr);
                    maxPrice = TextUtils.isEmpty(maxStr) ? null : Double.parseDouble(maxStr);

                    int sortPos = filterBinding.spSortOrder.getSelectedItemPosition();
                    if (sortPos == 1) {
                        sortOrder = DishDao.SortOrder.PRICE_DESC;
                    } else if (sortPos == 2) {
                        sortOrder = DishDao.SortOrder.PREP_TIME_ASC;
                    } else {
                        sortOrder = DishDao.SortOrder.PRICE_ASC;
                    }

                    loadDishes();
                })
                .setNeutralButton(R.string.btn_reset_filter, (dialog, which) -> {
                    selectedCategoryId = "ALL";
                    isAvailableOnly = false;
                    minPrice = null;
                    maxPrice = null;
                    sortOrder = DishDao.SortOrder.PRICE_ASC;
                    loadDishes();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void setupRecyclerView() {
        dishAdapter = new DishAdapter(new DishAdapter.OnDishClickListener() {
            @Override
            public void onDishClick(DishWithDetails dish) {
                Intent intent = new Intent(MainActivity.this, DishDetailActivity.class);
                intent.putExtra("DISH_ID", dish.getDish().getId());
                detailLauncher.launch(intent);
            }

            @Override
            public void onEditDish(DishWithDetails dish) {
                Intent intent = new Intent(MainActivity.this, DishAddEditActivity.class);
                intent.putExtra("DISH_ID", dish.getDish().getId());
                detailLauncher.launch(intent);
            }

            @Override
            public void onDeleteDish(DishWithDetails dish) {
                confirmDeleteDish(dish);
            }
        });

        binding.rvDishes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDishes.setAdapter(dishAdapter);
    }

    private void confirmDeleteDish(DishWithDetails dish) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_dish_msg)
                .setPositiveButton(R.string.btn_confirm_delete, (dialog, which) -> {
                    boolean success = dishDao.deleteDish(dish.getDish().getId());
                    if (success) {
                        Toast.makeText(this, R.string.msg_dish_deleted, Toast.LENGTH_SHORT).show();
                        syncManager.triggerAutoSync();
                        loadDishes();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void setupNetworkObserver() {
        syncManager.getIsOnlineLiveData().observe(this, isOnline -> {
            if (Boolean.TRUE.equals(isOnline)) {
                binding.tvNetworkStatus.setText(R.string.status_online);
                binding.tvNetworkStatus.setBackgroundColor(Color.parseColor("#2E7D32")); // Green
            } else {
                binding.tvNetworkStatus.setText(R.string.status_offline);
                binding.tvNetworkStatus.setBackgroundColor(Color.parseColor("#C62828")); // Red
            }
        });

        syncManager.getIsSyncingLiveData().observe(this, isSyncing -> {
            if (Boolean.TRUE.equals(isSyncing)) {
                binding.tvNetworkStatus.setText(R.string.status_syncing);
                binding.tvNetworkStatus.setBackgroundColor(Color.parseColor("#1565C0")); // Blue
            } else {
                Boolean online = syncManager.getIsOnlineLiveData().getValue();
                if (Boolean.TRUE.equals(online)) {
                    binding.tvNetworkStatus.setText(R.string.status_online);
                    binding.tvNetworkStatus.setBackgroundColor(Color.parseColor("#2E7D32"));
                }
            }
        });

        syncManager.setSyncCallback(this::loadDishes);
    }

    private void loadDishes() {
        List<DishWithDetails> filteredList = dishDao.getDishesFiltered(
                currentSearchQuery,
                selectedCategoryId,
                isAvailableOnly,
                minPrice,
                maxPrice,
                sortOrder
        );

        dishAdapter.setDishes(filteredList);

        if (filteredList.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvDishes.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvDishes.setVisibility(View.VISIBLE);
        }
    }
}
