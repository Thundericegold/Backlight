package com.example.backlight.activitys;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class BaseActivity extends AppCompatActivity {

    public abstract void initView();
    public abstract void initListener();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // SharedPreferences 已不再使用，数据用 Room 数据库管理
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applySystemBarsInsets(); // 首屏加载时就适配
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applySystemBarsInsets(); // 首屏加载时就适配
    }

    /** 应用系统栏 Insets 防遮挡 */
    private void applySystemBarsInsets() {
        final View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets sysBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    v.setPadding(sysBarsInsets.left, 0, sysBarsInsets.right, 0);
                } else {
                    v.setPadding(0, sysBarsInsets.top, 0, sysBarsInsets.bottom);
                }
                return insets;
            });
            ViewCompat.requestApplyInsets(rootView);
        }
    }

    // 防抖点击
    private long LAST_CLICK_TIME = 0;
    private static final int CLICK_INTERVAL = 500;

    private boolean isFastClick() {
        long currentClickTime = System.currentTimeMillis();
        boolean tooFast = (currentClickTime - LAST_CLICK_TIME) < CLICK_INTERVAL;
        LAST_CLICK_TIME = currentClickTime;
        return tooFast;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (isFastClick()) {
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
