package com.example.bit240017_foodmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bit240017_foodmanager.models.Dish;
import com.example.bit240017_foodmanager.models.DishCategory;
import com.example.bit240017_foodmanager.models.DishImage;
import com.example.bit240017_foodmanager.models.DishWithDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DishDao {
    public enum SortOrder {
        PRICE_ASC,
        PRICE_DESC,
        PREP_TIME_ASC
    }

    private final DatabaseHelper dbHelper;
    private final CategoryDao categoryDao;
    private final ImageDao imageDao;

    public DishDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.categoryDao = new CategoryDao(context);
        this.imageDao = new ImageDao(context);
    }

    public List<DishWithDetails> getDishesFiltered(String searchQuery,
                                                  String categoryId,
                                                  Boolean isAvailableOnly,
                                                  Double minPrice,
                                                  Double maxPrice,
                                                  SortOrder sortOrder) {
        List<DishWithDetails> resultList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        StringBuilder selectionBuilder = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();

        // Always exclude soft-deleted dishes
        selectionBuilder.append(DatabaseHelper.COL_DISH_IS_DELETED).append(" = 0");

        // 1. Partial Search by Name or Description
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            selectionBuilder.append(" AND (").append(DatabaseHelper.COL_DISH_NAME)
                    .append(" LIKE ? OR ").append(DatabaseHelper.COL_DISH_DESC).append(" LIKE ?)");
            String queryArg = "%" + searchQuery.trim() + "%";
            selectionArgs.add(queryArg);
            selectionArgs.add(queryArg);
        }

        // 2. Category Filter
        if (categoryId != null && !categoryId.trim().isEmpty() && !categoryId.equalsIgnoreCase("ALL")) {
            selectionBuilder.append(" AND ").append(DatabaseHelper.COL_DISH_CAT_ID).append(" = ?");
            selectionArgs.add(categoryId.trim());
        }

        // 3. Available Status Filter
        if (Boolean.TRUE.equals(isAvailableOnly)) {
            selectionBuilder.append(" AND ").append(DatabaseHelper.COL_DISH_IS_AVAILABLE).append(" = 1");
        }

        // 4. Price Range Filter
        if (minPrice != null && minPrice >= 0) {
            selectionBuilder.append(" AND ").append(DatabaseHelper.COL_DISH_PRICE).append(" >= ?");
            selectionArgs.add(String.valueOf(minPrice));
        }
        if (maxPrice != null && maxPrice > 0) {
            selectionBuilder.append(" AND ").append(DatabaseHelper.COL_DISH_PRICE).append(" <= ?");
            selectionArgs.add(String.valueOf(maxPrice));
        }

        // 5. Sorting
        String orderBy;
        if (sortOrder == SortOrder.PRICE_DESC) {
            orderBy = DatabaseHelper.COL_DISH_PRICE + " DESC";
        } else if (sortOrder == SortOrder.PREP_TIME_ASC) {
            orderBy = DatabaseHelper.COL_DISH_PREP_TIME + " ASC";
        } else {
            orderBy = DatabaseHelper.COL_DISH_PRICE + " ASC";
        }

        String[] argsArray = selectionArgs.toArray(new String[0]);
        Cursor cursor = db.query(DatabaseHelper.TABLE_DISH, null,
                selectionBuilder.toString(), argsArray, null, null, orderBy);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Dish dish = cursorToDish(cursor);
                DishCategory category = categoryDao.getCategoryById(dish.getDishCategoryId());
                List<DishImage> images = imageDao.getImagesForDish(dish.getId());

                resultList.add(new DishWithDetails(dish, category, images));
            }
            cursor.close();
        }

        return resultList;
    }

    public DishWithDetails getDishDetailsById(String dishId) {
        if (dishId == null) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COL_DISH_ID + " = ? AND " + DatabaseHelper.COL_DISH_IS_DELETED + " = 0";

        Cursor cursor = db.query(DatabaseHelper.TABLE_DISH, null, selection, new String[]{dishId}, null, null, null);
        DishWithDetails details = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Dish dish = cursorToDish(cursor);
                DishCategory category = categoryDao.getCategoryById(dish.getDishCategoryId());
                List<DishImage> images = imageDao.getImagesForDish(dish.getId());
                details = new DishWithDetails(dish, category, images);
            }
            cursor.close();
        }
        return details;
    }

    public boolean isDishNameDuplicateInCategory(String name, String categoryId, String excludeDishId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DISH +
                " WHERE LOWER(" + DatabaseHelper.COL_DISH_NAME + ") = LOWER(?)" +
                " AND " + DatabaseHelper.COL_DISH_CAT_ID + " = ?" +
                " AND " + DatabaseHelper.COL_DISH_IS_DELETED + " = 0");

        List<String> args = new ArrayList<>();
        args.add(name.trim());
        args.add(categoryId);

        if (excludeDishId != null && !excludeDishId.isEmpty()) {
            query.append(" AND ").append(DatabaseHelper.COL_DISH_ID).append(" != ?");
            args.add(excludeDishId);
        }

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count > 0;
    }

    public String insertDish(Dish dish, List<DishImage> images) throws IllegalArgumentException {
        // Validation Constraints
        validateDish(dish);

        if (isDishNameDuplicateInCategory(dish.getName(), dish.getDishCategoryId(), null)) {
            throw new IllegalArgumentException("DUPLICATE_NAME");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (dish.getId() == null || dish.getId().isEmpty()) {
            dish.setId("dish_" + UUID.randomUUID().toString());
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_DISH_ID, dish.getId());
        values.put(DatabaseHelper.COL_DISH_NAME, dish.getName());
        values.put(DatabaseHelper.COL_DISH_PRICE, dish.getPrice());
        values.put(DatabaseHelper.COL_DISH_PREP_TIME, dish.getPreparationTime());
        values.put(DatabaseHelper.COL_DISH_IS_AVAILABLE, dish.isAvailable() ? 1 : 0);
        values.put(DatabaseHelper.COL_DISH_DESC, dish.getDescription());
        values.put(DatabaseHelper.COL_DISH_CAT_ID, dish.getDishCategoryId());
        values.put(DatabaseHelper.COL_DISH_IS_SYNCED, dish.isSynced() ? 1 : 0);
        values.put(DatabaseHelper.COL_DISH_IS_DELETED, dish.isDeleted() ? 1 : 0);
        values.put(DatabaseHelper.COL_DISH_UPDATED_AT, System.currentTimeMillis());

        long result = db.insert(DatabaseHelper.TABLE_DISH, null, values);
        if (result != -1) {
            if (images != null && !images.isEmpty()) {
                imageDao.saveImagesForDish(dish.getId(), images);
            }
            return dish.getId();
        }
        return null;
    }

    public boolean updateDish(Dish dish, List<DishImage> images) throws IllegalArgumentException {
        validateDish(dish);

        if (isDishNameDuplicateInCategory(dish.getName(), dish.getDishCategoryId(), dish.getId())) {
            throw new IllegalArgumentException("DUPLICATE_NAME");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_DISH_NAME, dish.getName());
        values.put(DatabaseHelper.COL_DISH_PRICE, dish.getPrice());
        values.put(DatabaseHelper.COL_DISH_PREP_TIME, dish.getPreparationTime());
        values.put(DatabaseHelper.COL_DISH_IS_AVAILABLE, dish.isAvailable() ? 1 : 0);
        values.put(DatabaseHelper.COL_DISH_DESC, dish.getDescription());
        values.put(DatabaseHelper.COL_DISH_CAT_ID, dish.getDishCategoryId());
        values.put(DatabaseHelper.COL_DISH_IS_SYNCED, dish.isSynced() ? 1 : 0);
        values.put(DatabaseHelper.COL_DISH_IS_DELETED, dish.isDeleted() ? 1 : 0);
        values.put(DatabaseHelper.COL_DISH_UPDATED_AT, System.currentTimeMillis());

        int rows = db.update(DatabaseHelper.TABLE_DISH, values,
                DatabaseHelper.COL_DISH_ID + " = ?", new String[]{dish.getId()});

        if (rows > 0) {
            imageDao.saveImagesForDish(dish.getId(), images);
            return true;
        }
        return false;
    }

    public boolean deleteDish(String dishId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Soft delete for offline sync
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_DISH_IS_DELETED, 1);
        values.put(DatabaseHelper.COL_DISH_IS_SYNCED, 0);

        int rows = db.update(DatabaseHelper.TABLE_DISH, values,
                DatabaseHelper.COL_DISH_ID + " = ?", new String[]{dishId});
        return rows > 0;
    }

    public List<DishWithDetails> getUnsyncedDishes() {
        List<DishWithDetails> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COL_DISH_IS_SYNCED + " = 0";
        Cursor cursor = db.query(DatabaseHelper.TABLE_DISH, null, selection, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Dish dish = cursorToDish(cursor);
                DishCategory cat = categoryDao.getCategoryById(dish.getDishCategoryId());
                List<DishImage> images = imageDao.getImagesForDish(dish.getId());
                list.add(new DishWithDetails(dish, cat, images));
            }
            cursor.close();
        }
        return list;
    }

    public void markDishAsSynced(String dishId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_DISH_IS_SYNCED, 1);
        db.update(DatabaseHelper.TABLE_DISH, values, DatabaseHelper.COL_DISH_ID + " = ?", new String[]{dishId});
    }

    public void updateDishId(String oldId, String newId) {
        if (oldId == null || newId == null || oldId.equals(newId)) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues dishValues = new ContentValues();
            dishValues.put(DatabaseHelper.COL_DISH_ID, newId);
            dishValues.put(DatabaseHelper.COL_DISH_IS_SYNCED, 1);
            db.update(DatabaseHelper.TABLE_DISH, dishValues, DatabaseHelper.COL_DISH_ID + " = ?", new String[]{oldId});

            ContentValues imgValues = new ContentValues();
            imgValues.put(DatabaseHelper.COL_IMG_DISH_ID, newId);
            db.update(DatabaseHelper.TABLE_IMAGE, imgValues, DatabaseHelper.COL_IMG_DISH_ID + " = ?", new String[]{oldId});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void hardDeleteDish(String dishId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_DISH, DatabaseHelper.COL_DISH_ID + " = ?", new String[]{dishId});
        db.delete(DatabaseHelper.TABLE_IMAGE, DatabaseHelper.COL_IMG_DISH_ID + " = ?", new String[]{dishId});
    }

    private void validateDish(Dish dish) {
        if (dish == null) {
            throw new IllegalArgumentException("DISH_NULL");
        }
        if (dish.getName() == null || dish.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("NAME_EMPTY");
        }
        if (dish.getPrice() <= 0) {
            throw new IllegalArgumentException("PRICE_INVALID");
        }
        if (dish.getPreparationTime() <= 0) {
            throw new IllegalArgumentException("PREP_TIME_INVALID");
        }
        if (dish.getDishCategoryId() == null || dish.getDishCategoryId().trim().isEmpty()) {
            throw new IllegalArgumentException("CATEGORY_NULL");
        }
    }

    private Dish cursorToDish(Cursor cursor) {
        Dish dish = new Dish();
        dish.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_ID)));
        dish.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_NAME)));
        dish.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_PRICE)));
        dish.setPreparationTime(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_PREP_TIME)));
        dish.setAvailable(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_IS_AVAILABLE)) == 1);
        dish.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_DESC)));
        dish.setDishCategoryId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_CAT_ID)));
        dish.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_IS_SYNCED)) == 1);
        dish.setDeleted(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_IS_DELETED)) == 1);
        dish.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DISH_UPDATED_AT)));
        return dish;
    }
}
