package com.example.bit240017_foodmanager;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bit240017_foodmanager.utils.LocaleHelper;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}
