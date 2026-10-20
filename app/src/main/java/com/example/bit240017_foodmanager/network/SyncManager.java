package com.example.bit240017_foodmanager.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bit240017_foodmanager.database.CategoryDao;
import com.example.bit240017_foodmanager.database.DishDao;
import com.example.bit240017_foodmanager.models.Dish;
import com.example.bit240017_foodmanager.models.DishCategory;
import com.example.bit240017_foodmanager.models.DishWithDetails;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class SyncManager {

    private static final String TAG = "SyncManager";
    private static SyncManager instance;

    private final Context context;
    private final DishDao dishDao;
    private final CategoryDao categoryDao;
    private final MockApiService apiService;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private final MutableLiveData<Boolean> isOnlineLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSyncingLiveData = new MutableLiveData<>(false);

    public interface SyncCallback {
        void onSyncCompleted();
    }

    private SyncCallback syncCallback;

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context.getApplicationContext());
        }
        return instance;
    }

    private SyncManager(Context context) {
        this.context = context;
        this.dishDao = new DishDao(context);
        this.categoryDao = new CategoryDao(context);
        this.apiService = RetrofitClient.getApiService();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        registerNetworkCallback();
    }

    public void setSyncCallback(SyncCallback callback) {
        this.syncCallback = callback;
    }

    public LiveData<Boolean> getIsOnlineLiveData() {
        return isOnlineLiveData;
    }

    public LiveData<Boolean> getIsSyncingLiveData() {
        return isSyncingLiveData;
    }

    public boolean isOnline() {
        Boolean online = isOnlineLiveData.getValue();
        return online != null && online;
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        // Check initial state
        Network activeNet = cm.getActiveNetwork();
        if (activeNet != null) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNet);
            boolean connected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            isOnlineLiveData.postValue(connected);
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Network Available -> Triggering Auto Sync");
                isOnlineLiveData.postValue(true);
                triggerAutoSync();
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "Network Lost -> Offline Mode");
                isOnlineLiveData.postValue(false);
            }
        });
    }

    public void triggerAutoSync() {
        executorService.execute(() -> {
            List<DishCategory> unsyncedCategories = categoryDao.getUnsyncedCategories();
            List<DishWithDetails> unsyncedDishes = dishDao.getUnsyncedDishes();

            if (unsyncedCategories.isEmpty() && unsyncedDishes.isEmpty()) {
                Log.d(TAG, "No unsynced items found.");
                return;
            }

            isSyncingLiveData.postValue(true);
            Log.d(TAG, "Starting Auto Sync...");

            // 1. Sync Categories First
            for (DishCategory cat : unsyncedCategories) {
                try {
                    if (cat.isDeleted()) {
                        String targetId = cat.getMockApiId() != null && !cat.getMockApiId().isEmpty() ? cat.getMockApiId() : cat.getId();
                        Response<Void> resp = apiService.deleteCategory(targetId).execute();
                        if (resp.isSuccessful() || resp.code() == 404) {
                            categoryDao.hardDeleteCategory(cat.getId());
                            if (!targetId.equals(cat.getId())) {
                                categoryDao.hardDeleteCategory(targetId);
                            }
                        }
                    } else {
                        boolean isNewLocal = cat.getId() != null && cat.getId().startsWith("cat_");
                        if (!isNewLocal) {
                            String targetId = cat.getMockApiId() != null && !cat.getMockApiId().isEmpty() ? cat.getMockApiId() : cat.getId();
                            Response<DishCategory> updateResp = apiService.updateCategory(targetId, cat).execute();
                            if (updateResp.isSuccessful()) {
                                categoryDao.markCategoryAsSynced(cat.getId());
                            } else {
                                Response<DishCategory> createResp = apiService.createCategory(cat).execute();
                                if (createResp.isSuccessful() && createResp.body() != null) {
                                    DishCategory created = createResp.body();
                                    String remoteId = created.getMockApiId() != null && !created.getMockApiId().isEmpty() ? created.getMockApiId() : created.getId();
                                    if (remoteId != null && !remoteId.isEmpty()) {
                                        categoryDao.updateCategoryId(cat.getId(), remoteId);
                                    } else {
                                        categoryDao.markCategoryAsSynced(cat.getId());
                                    }
                                } else {
                                    categoryDao.markCategoryAsSynced(cat.getId());
                                }
                            }
                        } else {
                            Response<DishCategory> createResp = apiService.createCategory(cat).execute();
                            if (createResp.isSuccessful() && createResp.body() != null) {
                                DishCategory created = createResp.body();
                                String remoteId = created.getMockApiId() != null && !created.getMockApiId().isEmpty() ? created.getMockApiId() : created.getId();
                                if (remoteId != null && !remoteId.isEmpty()) {
                                    categoryDao.updateCategoryId(cat.getId(), remoteId);
                                } else {
                                    categoryDao.markCategoryAsSynced(cat.getId());
                                }
                            } else {
                                categoryDao.markCategoryAsSynced(cat.getId());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Sync error for category " + cat.getId() + ": " + e.getMessage());
                    categoryDao.markCategoryAsSynced(cat.getId());
                }
            }

            // 2. Sync Dishes
            for (DishWithDetails details : unsyncedDishes) {
                Dish dish = details.getDish();
                if (dish == null) continue;

                if (details.getImages() != null) {
                    dish.setDishImages(details.getImages());
                }

                try {
                    if (dish.isDeleted()) {
                        String deleteTargetId = dish.getMockApiId() != null && !dish.getMockApiId().isEmpty() ? dish.getMockApiId() : dish.getId();
                        Response<Void> resp = apiService.deleteDish(deleteTargetId).execute();
                        if (resp.isSuccessful() || resp.code() == 404) {
                            dishDao.hardDeleteDish(dish.getId());
                            if (!deleteTargetId.equals(dish.getId())) {
                                dishDao.hardDeleteDish(deleteTargetId);
                            }
                        }
                    } else {
                        boolean isNewLocal = dish.getId() != null && dish.getId().startsWith("dish_");
                        if (!isNewLocal) {
                            String updateTargetId = dish.getMockApiId() != null && !dish.getMockApiId().isEmpty() ? dish.getMockApiId() : dish.getId();
                            Response<Dish> updateResp = apiService.updateDish(updateTargetId, dish).execute();
                            if (updateResp.isSuccessful()) {
                                dishDao.markDishAsSynced(dish.getId());
                            } else if (updateResp.code() == 404) {
                                Response<Dish> createResp = apiService.createDish(dish).execute();
                                if (createResp.isSuccessful() && createResp.body() != null) {
                                    Dish created = createResp.body();
                                    String remoteId = created.getMockApiId() != null && !created.getMockApiId().isEmpty() ? created.getMockApiId() : created.getId();
                                    if (remoteId != null && !remoteId.isEmpty()) {
                                        dishDao.updateDishId(dish.getId(), remoteId);
                                    } else {
                                        dishDao.markDishAsSynced(dish.getId());
                                    }
                                } else {
                                    dishDao.markDishAsSynced(dish.getId());
                                }
                            } else {
                                dishDao.markDishAsSynced(dish.getId());
                            }
                        } else {
                            Response<Dish> createResp = apiService.createDish(dish).execute();
                            if (createResp.isSuccessful() && createResp.body() != null) {
                                Dish created = createResp.body();
                                String remoteId = created.getMockApiId() != null && !created.getMockApiId().isEmpty() ? created.getMockApiId() : created.getId();
                                if (remoteId != null && !remoteId.isEmpty()) {
                                    dishDao.updateDishId(dish.getId(), remoteId);
                                } else {
                                    dishDao.markDishAsSynced(dish.getId());
                                }
                            } else {
                                dishDao.markDishAsSynced(dish.getId());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Sync error for dish " + dish.getId() + ": " + e.getMessage());
                    dishDao.markDishAsSynced(dish.getId());
                }
            }

            isSyncingLiveData.postValue(false);
            mainHandler.post(() -> {
                if (syncCallback != null) {
                    syncCallback.onSyncCompleted();
                }
            });
        });
    }
}
