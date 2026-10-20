package com.example.bit240017_foodmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "BIT240017_FoodManager.db";
    public static final int DATABASE_VERSION = 1;

    // Table 1: DishCategory
    public static final String TABLE_CATEGORY = "BIT240017_DishCategory";
    public static final String COL_CAT_ID = "BIT240017_id";
    public static final String COL_CAT_NAME = "BIT240017_name";
    public static final String COL_CAT_DESC = "BIT240017_description";
    public static final String COL_CAT_IS_SYNCED = "BIT240017_isSynced";
    public static final String COL_CAT_IS_DELETED = "BIT240017_isDeleted";

    // Table 2: Dish
    public static final String TABLE_DISH = "BIT240017_Dish";
    public static final String COL_DISH_ID = "BIT240017_id";
    public static final String COL_DISH_NAME = "BIT240017_name";
    public static final String COL_DISH_PRICE = "BIT240017_price";
    public static final String COL_DISH_PREP_TIME = "BIT240017_preparationTime";
    public static final String COL_DISH_IS_AVAILABLE = "BIT240017_isAvailable";
    public static final String COL_DISH_DESC = "BIT240017_description";
    public static final String COL_DISH_CAT_ID = "BIT240017_dishCategoryId";
    public static final String COL_DISH_IS_SYNCED = "BIT240017_isSynced";
    public static final String COL_DISH_IS_DELETED = "BIT240017_isDeleted";
    public static final String COL_DISH_UPDATED_AT = "BIT240017_updatedAt";

    // Table 3: DishImage
    public static final String TABLE_IMAGE = "BIT240017_DishImage";
    public static final String COL_IMG_ID = "BIT240017_id";
    public static final String COL_IMG_DISH_ID = "BIT240017_dishId";
    public static final String COL_IMG_URL = "BIT240017_imageUrl";
    public static final String COL_IMG_IS_THUMBNAIL = "BIT240017_isThumbnail";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Table 1
        String createCategoryTable = "CREATE TABLE " + TABLE_CATEGORY + " (" +
                COL_CAT_ID + " TEXT PRIMARY KEY, " +
                COL_CAT_NAME + " TEXT NOT NULL, " +
                COL_CAT_DESC + " TEXT, " +
                COL_CAT_IS_SYNCED + " INTEGER DEFAULT 1, " +
                COL_CAT_IS_DELETED + " INTEGER DEFAULT 0)";
        db.execSQL(createCategoryTable);

        // Create Table 2
        String createDishTable = "CREATE TABLE " + TABLE_DISH + " (" +
                COL_DISH_ID + " TEXT PRIMARY KEY, " +
                COL_DISH_NAME + " TEXT NOT NULL, " +
                COL_DISH_PRICE + " REAL NOT NULL, " +
                COL_DISH_PREP_TIME + " INTEGER NOT NULL, " +
                COL_DISH_IS_AVAILABLE + " INTEGER DEFAULT 1, " +
                COL_DISH_DESC + " TEXT, " +
                COL_DISH_CAT_ID + " TEXT NOT NULL, " +
                COL_DISH_IS_SYNCED + " INTEGER DEFAULT 1, " +
                COL_DISH_IS_DELETED + " INTEGER DEFAULT 0, " +
                COL_DISH_UPDATED_AT + " INTEGER NOT NULL)";
        db.execSQL(createDishTable);

        // Create Table 3
        String createImageTable = "CREATE TABLE " + TABLE_IMAGE + " (" +
                COL_IMG_ID + " TEXT PRIMARY KEY, " +
                COL_IMG_DISH_ID + " TEXT NOT NULL, " +
                COL_IMG_URL + " TEXT NOT NULL, " +
                COL_IMG_IS_THUMBNAIL + " INTEGER DEFAULT 0)";
        db.execSQL(createImageTable);

        // Seed Initial Data (At least 3 Categories, 5 Dishes with images)
        seedInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DISH);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        onCreate(db);
    }

    private void seedInitialData(SQLiteDatabase db) {
        // Seed 3 Categories
        insertCategorySeed(db, "cat_1", "Appetizers", "Light starter dishes served before main courses");
        insertCategorySeed(db, "cat_2", "Main Courses", "Flavorful and filling primary meals");
        insertCategorySeed(db, "cat_3", "Desserts", "Sweet desserts and fresh refreshing drinks");

        // Seed 5 Dishes
        // Dish 1: Spring Rolls (Quick - 12 mins)
        insertDishSeed(db, "dish_1", "Crispy Spring Rolls", 45000.0, 12, 1, "Traditional crispy rolls with meat, shrimp and fresh herbs", "cat_1");
        insertImageSeed(db, "img_1_1", "dish_1", "https://images.unsplash.com/photo-1544025162-d76694265947?w=500", 1);
        insertImageSeed(db, "img_1_2", "dish_1", "https://images.unsplash.com/photo-1541544741938-0af808871cc0?w=500", 0);

        // Dish 2: Pho Bo (Regular - 20 mins)
        insertDishSeed(db, "dish_2", "Traditional Beef Pho", 65000.0, 20, 1, "Rich beef broth served with rice noodles, beef slices and green onions", "cat_2");
        insertImageSeed(db, "img_2_1", "dish_2", "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=500", 1);
        insertImageSeed(db, "img_2_2", "dish_2", "https://images.unsplash.com/photo-1503764654157-72d979d9af2f?w=500", 0);

        // Dish 3: Grilled Pork Rice (Quick - 15 mins)
        insertDishSeed(db, "dish_3", "Broken Rice with Grilled Pork", 55000.0, 15, 1, "Fragrant broken rice with marinated lemongrass grilled pork chop", "cat_2");
        insertImageSeed(db, "img_3_1", "dish_3", "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=500", 1);

        // Dish 4: Mango Smoothie (Quick - 8 mins)
        insertDishSeed(db, "dish_4", "Fresh Mango Smoothie", 35000.0, 8, 1, "Blended fresh ripe tropical mango with sweet milk", "cat_3");
        insertImageSeed(db, "img_4_1", "dish_4", "https://images.unsplash.com/photo-1553530666-ba11a7da3888?w=500", 1);

        // Dish 5: Beef Steak (Regular - 25 mins) - Stopped selling (IsAvailable = 0)
        insertDishSeed(db, "dish_5", "Black Pepper Beef Steak", 120000.0, 25, 0, "Tender beefsteak served with rich black pepper sauce and potato fries", "cat_2");
        insertImageSeed(db, "img_5_1", "dish_5", "https://images.unsplash.com/photo-1544025162-d76694265947?w=500", 1);
    }

    private void insertCategorySeed(SQLiteDatabase db, String id, String name, String desc) {
        ContentValues values = new ContentValues();
        values.put(COL_CAT_ID, id);
        values.put(COL_CAT_NAME, name);
        values.put(COL_CAT_DESC, desc);
        values.put(COL_CAT_IS_SYNCED, 0);
        values.put(COL_CAT_IS_DELETED, 0);
        db.insert(TABLE_CATEGORY, null, values);
    }

    private void insertDishSeed(SQLiteDatabase db, String id, String name, double price, int prepTime, int isAvailable, String desc, String catId) {
        ContentValues values = new ContentValues();
        values.put(COL_DISH_ID, id);
        values.put(COL_DISH_NAME, name);
        values.put(COL_DISH_PRICE, price);
        values.put(COL_DISH_PREP_TIME, prepTime);
        values.put(COL_DISH_IS_AVAILABLE, isAvailable);
        values.put(COL_DISH_DESC, desc);
        values.put(COL_DISH_CAT_ID, catId);
        values.put(COL_DISH_IS_SYNCED, 0);
        values.put(COL_DISH_IS_DELETED, 0);
        values.put(COL_DISH_UPDATED_AT, System.currentTimeMillis());
        db.insert(TABLE_DISH, null, values);
    }

    private void insertImageSeed(SQLiteDatabase db, String id, String dishId, String url, int isThumbnail) {
        ContentValues values = new ContentValues();
        values.put(COL_IMG_ID, id);
        values.put(COL_IMG_DISH_ID, dishId);
        values.put(COL_IMG_URL, url);
        values.put(COL_IMG_IS_THUMBNAIL, isThumbnail);
        db.insert(TABLE_IMAGE, null, values);
    }
}
