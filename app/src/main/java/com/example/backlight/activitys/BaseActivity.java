package com.example.backlight.activitys;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor editor;
    public final static String SHARED_PREFERENCES_NAME = "BacklightSharedPreferences";

    public abstract void initView();
    public abstract void initListener();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    //-----------------------防抖start---------------------------
    private long LAST_CLICK_TIME; // 上一次点击时间
    private boolean isCanClick = true;

    private void isFastClick() {
        long currentClickTime = System.currentTimeMillis();
        // 两次点击间隔不能少于500ms
        isCanClick = (currentClickTime - LAST_CLICK_TIME) >= 500;
        LAST_CLICK_TIME = currentClickTime;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isCanClick) {
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
//-----------------------防抖end---------------------------
}
