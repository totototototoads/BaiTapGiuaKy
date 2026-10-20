package com.example.bit240017_foodmanager.network;

import com.example.bit240017_foodmanager.models.Dish;
import com.example.bit240017_foodmanager.models.DishCategory;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface MockApiService {

    @GET("BIT240017_DishCategory")
    Call<List<DishCategory>> getCategories();

    @POST("BIT240017_DishCategory")
    Call<DishCategory> createCategory(@Body DishCategory category);

    @PUT("BIT240017_DishCategory/{id}")
    Call<DishCategory> updateCategory(@Path("id") String id, @Body DishCategory category);

    @DELETE("BIT240017_DishCategory/{id}")
    Call<Void> deleteCategory(@Path("id") String id);

    @GET("BIT240017_Dish")
    Call<List<Dish>> getDishes();

    @POST("BIT240017_Dish")
    Call<Dish> createDish(@Body Dish dish);

    @PUT("BIT240017_Dish/{id}")
    Call<Dish> updateDish(@Path("id") String id, @Body Dish dish);

    @DELETE("BIT240017_Dish/{id}")
    Call<Void> deleteDish(@Path("id") String id);
}
