package com.example.backlight.controller;

import android.animation.ValueAnimator;
import android.graphics.Color;

import com.example.backlight.activitys.PixelDrawView;

/**
 * 淡入淡出动画控制器
 * 用于在 PixelDrawView 中切换白点到黑点的渐变效果
 */
public class FadeController {
    private PixelDrawView view;   // 主绘图视图
    private ValueAnimator fadeAnimator;  // 控制淡入淡出的动画器
    private float fadeFactor = 1f;       // 淡入淡出插值因子 (1=白色,0=黑色)
    private boolean isFading = false;    // 当前是否在淡入淡出

    public FadeController(PixelDrawView view) {
        this.view = view;
    }

    /**
     * 是否正在淡入淡出
     */
    public boolean isFading() {
        return isFading;
    }

    /**
     * 开始淡入淡出循环动画（白 -> 黑 -> 白）
     */
    public void startFadeEffect() {
        if (isFading) return; // 已在动画中，防止重复启动
        isFading = true;
        fadeAnimator = ValueAnimator.ofFloat(1f, 0f, 1f); // 白→黑→白
        fadeAnimator.setDuration(2000); // 一个循环 2 秒
        fadeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        fadeAnimator.addUpdateListener(animation -> {
            fadeFactor = (float) animation.getAnimatedValue();
            view.invalidate(); // 每一帧更新视图
        });
        fadeAnimator.start();
    }

    /**
     * 停止淡入淡出动画
     */
    public void stopFadeEffect() {
        if (fadeAnimator != null) {
            fadeAnimator.cancel();
            fadeAnimator = null;
        }
        fadeFactor = 1f;  // 恢复为白色
        isFading = false;
        view.invalidate();
    }

    /**
     * 获取当前插值后的颜色（从 colorFrom 到 colorTo 渐变）
     */
    public int getInterpolatedColor(int colorFrom, int colorTo) {
        int r1 = Color.red(colorFrom);
        int g1 = Color.green(colorFrom);
        int b1 = Color.blue(colorFrom);

        int r2 = Color.red(colorTo);
        int g2 = Color.green(colorTo);
        int b2 = Color.blue(colorTo);

        int r = (int) (r1 + (r2 - r1) * fadeFactor);
        int g = (int) (g1 + (g2 - g1) * fadeFactor);
        int b = (int) (b1 + (b2 - b1) * fadeFactor);

        return Color.rgb(r, g, b);
    }
}
