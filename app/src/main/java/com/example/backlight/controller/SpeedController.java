package com.example.backlight.controller;

import android.widget.SeekBar;
import android.widget.TextView;

public class SpeedController {

    // 固定 20 档速度值（毫秒）
    private final int[] marqueeSpeedLevels = {
            500, 475, 450, 425, 400, 375, 350, 325, 300, 275,
            250, 225, 200, 175, 150, 125, 100, 75, 50, 25
    };

    private final int[] rotateSpeedLevels = {
            38, 36, 34, 32, 30, 28, 26, 24, 22, 20,
            20, 18, 16, 14, 12, 10, 8, 6, 4, 2
    };

    private int[] speedLevels;               // 当前速度数组
    private int currentSpeedIndex = 0;       // 当前档位（0~19）
    private SpeedChangeListener listener;    // 外部速度变化回调
    private final SeekBar seekBar;           // 绑定的拖动条
    private final TextView tvSpeed;          // 显示速度文本

    // 回调接口：把速度值传回给使用方
    public interface SpeedChangeListener {
        void onSpeedChanged(int speedMs);
    }

    /**
     * 构造方法
     * @param seekBar 速度拖动条
     * @param tvSpeed 速度显示文本
     * @param mode    初始模式（1=跑马灯，其他=旋转）
     */
    public SpeedController(SeekBar seekBar, TextView tvSpeed, int mode) {
        this.seekBar = seekBar;
        this.tvSpeed = tvSpeed;

        // 初始化模式
        speedLevels = (mode == 1) ? marqueeSpeedLevels : rotateSpeedLevels;
        seekBar.setMax(speedLevels.length - 1);

        // 默认第10档
        currentSpeedIndex = 10;
        seekBar.setProgress(currentSpeedIndex);
        tvSpeed.setText("速度：" + speedLevels[currentSpeedIndex] + "ms");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSpeedIndex = progress;
                int speed = speedLevels[progress];
                tvSpeed.setText("速度：" + speed + "ms");

                if (listener != null) {
                    listener.onSpeedChanged(speed);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /** 设置速度变化的监听器 */
    public void setOnSpeedChangeListener(SpeedChangeListener listener) {
        this.listener = listener;
    }

    /** 获取当前速度毫秒值 */
    public int getCurrentSpeedMs() {
        return speedLevels[currentSpeedIndex];
    }

    /** 获取当前档位(0~19) */
    public int getCurrentSpeedIndex() {
        return currentSpeedIndex;
    }

    /**
     * 切换速度模式（不会触发回调，避免无限递归）
     * @param mode 1=跑马灯，其它=旋转
     */
    public void setMode(int mode) {
        // 切换速度数组
        speedLevels = (mode == 1) ? marqueeSpeedLevels : rotateSpeedLevels;

        // 刷新 SeekBar 最大值
        if (seekBar != null) {
            seekBar.setMax(speedLevels.length - 1);
        }

        // 保留原档位，如果超过新数组长度则回到 0 档
        if (currentSpeedIndex >= speedLevels.length) {
            currentSpeedIndex = 0;
        }

        // 更新UI显示
        if (tvSpeed != null) {
            tvSpeed.setText("速度：" + speedLevels[currentSpeedIndex] + "ms");
        }
    }
}
