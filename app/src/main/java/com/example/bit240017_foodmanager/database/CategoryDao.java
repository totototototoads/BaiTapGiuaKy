package com.example.bit240017_foodmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bit240017_foodmanager.models.DishCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CategoryDao {
    private final DatabaseHelper dbHelper;

    public CategoryDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public List<DishCategory> getAllCategories() {
        List<DishCategory> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COL_CAT_IS_DELETED + " = 0";
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORY, null, selection, null, null, null, DatabaseHelper.COL_CAT_NAME + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                DishCategory category = cursorToCategory(cursor);
                list.add(category);
            }
            cursor.close();
        }
        return list;
    }

    public DishCategory getCategoryById(String id) {
        if (id == null) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COL_CAT_ID + " = ? AND " + DatabaseHelper.COL_CAT_IS_DELETED + " = 0";
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORY, null, selection, new String[]{id}, null, null, null);

        DishCategory category = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                category = cursorToCategory(cursor);
            }
            cursor.close();
        }
        return category;
    }

    public boolean isCategoryInUse(String categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DISH +
                " WHERE " + DatabaseHelper.COL_DISH_CAT_ID + " = ? AND " +
                DatabaseHelper.COL_DISH_IS_DELETED + " = 0";
        Cursor cursor = db.rawQuery(query, new String[]{categoryId});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count > 0;
    }

    public String insertCategory(DishCategory category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (category.getId() == null || category.getId().isEmpty()) {
            category.setId("cat_" + UUID.randomUUID().toString());
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CAT_ID, category.getId());
        values.put(DatabaseHelper.COL_CAT_NAME, category.getName());
        values.put(DatabaseHelper.COL_CAT_DESC, category.getDescription());
        values.put(DatabaseHelper.COL_CAT_IS_SYNCED, category.isSynced() ? 1 : 0);
        values.put(DatabaseHelper.COL_CAT_IS_DELETED, category.isDeleted() ? 1 : 0);

        long result = db.insert(DatabaseHelper.TABLE_CATEGORY, null, values);
        return result != -1 ? category.getId() : null;
    }

    public boolean updateCategory(DishCategory category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CAT_NAME, category.getName());
        values.put(DatabaseHelper.COL_CAT_DESC, category.getDescription());
        values.put(DatabaseHelper.COL_CAT_IS_SYNCED, category.isSynced() ? 1 : 0);
        values.put(DatabaseHelper.COL_CAT_IS_DELETED, category.isDeleted() ? 1 : 0);

        int rows = db.update(DatabaseHelper.TABLE_CATEGORY, values,
                DatabaseHelper.COL_CAT_ID + " = ?", new String[]{category.getId()});
        return rows > 0;
    }

    public boolean deleteCategory(String id) throws IllegalStateException {
        // Business Constraint 4.4: Do not allow deleting category in use
        if (isCategoryInUse(id)) {
            throw new IllegalStateException("CATEGORY_IN_USE");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Soft delete for offline sync
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CAT_IS_DELETED, 1);
        values.put(DatabaseHelper.COL_CAT_IS_SYNCED, 0);

        int rows = db.update(DatabaseHelper.TABLE_CATEGORY, values,
                DatabaseHelper.COL_CAT_ID + " = ?", new String[]{id});
        return rows > 0;
    }

    public List<DishCategory> getUnsyncedCategories() {
        List<DishCategory> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COL_CAT_IS_SYNCED + " = 0";
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORY, null, selection, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToCategory(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public void markCategoryAsSynced(String catId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CAT_IS_SYNCED, 1);
        db.update(DatabaseHelper.TABLE_CATEGORY, values, DatabaseHelper.COL_CAT_ID + " = ?", new String[]{catId});
    }

    public void updateCategoryId(String oldId, String newId) {
        if (oldId == null || newId == null || oldId.equals(newId)) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues catValues = new ContentValues();
            catValues.put(DatabaseHelper.COL_CAT_ID, newId);
            catValues.put(DatabaseHelper.COL_CAT_IS_SYNCED, 1);
            db.update(DatabaseHelper.TABLE_CATEGORY, catValues, DatabaseHelper.COL_CAT_ID + " = ?", new String[]{oldId});

            ContentValues dishValues = new ContentValues();
            dishValues.put(DatabaseHelper.COL_DISH_CAT_ID, newId);
            db.update(DatabaseHelper.TABLE_DISH, dishValues, DatabaseHelper.COL_DISH_CAT_ID + " = ?", new String[]{oldId});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void hardDeleteCategory(String catId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_CATEGORY, DatabaseHelper.COL_CAT_ID + " = ?", new String[]{catId});
    }

    private DishCategory cursorToCategory(Cursor cursor) {
        DishCategory cat = new DishCategory();
        cat.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ID)));
        cat.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME)));
        cat.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_DESC)));
        cat.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_IS_SYNCED)) == 1);
        cat.setDeleted(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_IS_DELETED)) == 1);
        return cat;
    }
}
