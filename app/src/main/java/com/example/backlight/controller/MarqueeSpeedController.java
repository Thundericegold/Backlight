package com.example.backlight.controller;

import android.widget.SeekBar;
import android.widget.TextView;

public class MarqueeSpeedController {

    // 固定 20 档速度值（毫秒）
    private final int[] marqueeSpeedLevels = {
            500, 475, 450, 425, 400, 375, 350, 325, 300, 275,
            250, 225, 200,175, 150, 125, 100, 75, 50, 25
    };

    private final int[] rotateSpeedLevels = {
            38, 36, 34, 32, 30, 28, 26, 24, 22, 20,
            20, 18, 16,14, 12, 10, 8, 6, 4, 2
    };

    private int[] speedLevels;

    private int currentSpeedIndex; // 当前档位（0~19）
    private SpeedChangeListener listener;

    // 回调接口：把速度值传回给使用方
    public interface SpeedChangeListener {
        void onSpeedChanged(int speedMs);
    }

    /**
     * 构造方法
     * @param seekBar  速度拖动条
     * @param tvSpeed  速度显示文本
     */
    public MarqueeSpeedController(SeekBar seekBar, TextView tvSpeed,int mode) {

        speedLevels= mode==1?marqueeSpeedLevels:rotateSpeedLevels;
        // 设置最大值=19，表示20档
        seekBar.setMax(speedLevels.length - 1);
        seekBar.setProgress(10); // 默认第10档（250ms）
        tvSpeed.setText("速度：" + speedLevels[seekBar.getProgress()] + "ms");

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
}
