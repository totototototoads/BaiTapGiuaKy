package com.example.bit240017_foodmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bit240017_foodmanager.models.DishImage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImageDao {
    private final DatabaseHelper dbHelper;

    public ImageDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public List<DishImage> getImagesForDish(String dishId) {
        List<DishImage> list = new ArrayList<>();
        if (dishId == null) return list;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COL_IMG_DISH_ID + " = ?";
        Cursor cursor = db.query(DatabaseHelper.TABLE_IMAGE, null, selection, new String[]{dishId}, null, null, DatabaseHelper.COL_IMG_IS_THUMBNAIL + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToImage(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public String insertImage(DishImage image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (image.getId() == null || image.getId().isEmpty()) {
            image.setId("img_" + UUID.randomUUID().toString());
        }

        // If this image is set as thumbnail, reset all other thumbnails for this dish
        if (image.isThumbnail()) {
            resetThumbnailsForDish(db, image.getDishId());
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_IMG_ID, image.getId());
        values.put(DatabaseHelper.COL_IMG_DISH_ID, image.getDishId());
        values.put(DatabaseHelper.COL_IMG_URL, image.getImageUrl());
        values.put(DatabaseHelper.COL_IMG_IS_THUMBNAIL, image.isThumbnail() ? 1 : 0);

        long result = db.insert(DatabaseHelper.TABLE_IMAGE, null, values);
        return result != -1 ? image.getId() : null;
    }

    public void setThumbnailImage(String dishId, String targetImageId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Reset all thumbnails for this dish
            ContentValues resetValues = new ContentValues();
            resetValues.put(DatabaseHelper.COL_IMG_IS_THUMBNAIL, 0);
            db.update(DatabaseHelper.TABLE_IMAGE, resetValues, DatabaseHelper.COL_IMG_DISH_ID + " = ?", new String[]{dishId});

            // Set new thumbnail
            ContentValues setValues = new ContentValues();
            setValues.put(DatabaseHelper.COL_IMG_IS_THUMBNAIL, 1);
            db.update(DatabaseHelper.TABLE_IMAGE, setValues, DatabaseHelper.COL_IMG_ID + " = ?", new String[]{targetImageId});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void saveImagesForDish(String dishId, List<DishImage> newImages) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Delete old images
            db.delete(DatabaseHelper.TABLE_IMAGE, DatabaseHelper.COL_IMG_DISH_ID + " = ?", new String[]{dishId});

            // Insert new images
            boolean hasThumbnail = false;
            if (newImages != null) {
                for (DishImage img : newImages) {
                    img.setDishId(dishId);
                    if (img.getId() == null || img.getId().isEmpty()) {
                        img.setId("img_" + UUID.randomUUID().toString());
                    }
                    if (img.isThumbnail()) {
                        if (!hasThumbnail) {
                            hasThumbnail = true;
                        } else {
                            img.setThumbnail(false); // Max 1 thumbnail
                        }
                    }

                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COL_IMG_ID, img.getId());
                    values.put(DatabaseHelper.COL_IMG_DISH_ID, dishId);
                    values.put(DatabaseHelper.COL_IMG_URL, img.getImageUrl());
                    values.put(DatabaseHelper.COL_IMG_IS_THUMBNAIL, img.isThumbnail() ? 1 : 0);
                    db.insert(DatabaseHelper.TABLE_IMAGE, null, values);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteImagesForDish(String dishId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_IMAGE, DatabaseHelper.COL_IMG_DISH_ID + " = ?", new String[]{dishId});
    }

    private void resetThumbnailsForDish(SQLiteDatabase db, String dishId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_IMG_IS_THUMBNAIL, 0);
        db.update(DatabaseHelper.TABLE_IMAGE, values, DatabaseHelper.COL_IMG_DISH_ID + " = ?", new String[]{dishId});
    }

    private DishImage cursorToImage(Cursor cursor) {
        DishImage img = new DishImage();
        img.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_IMG_ID)));
        img.setDishId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_IMG_DISH_ID)));
        img.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_IMG_URL)));
        img.setThumbnail(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_IMG_IS_THUMBNAIL)) == 1);
        return img;
    }
}
