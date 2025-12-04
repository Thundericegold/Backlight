package com.example.backlight.activitys;

import android.os.Handler;
import java.util.Arrays;

/**
 * 列渐变动画控制器
 * 控制文字逐列显现和消失的循环特效
 */
public class SplitViewTow {
    private PixelDrawView view;
    private boolean isColumnFadeRunning = false; // 当前是否正在列渐变
    private boolean isShowingPhase = true;       // true=显现，false=消失
    private int currentColumn = 0;               // 当前操作的列索引
    private Handler fadeHandler = new Handler(); // 控制动画运行的 Handler
    private Runnable fadeRunnable;               // 动画任务
    private int fadeInterval = 100;              // 每列间隔毫秒
    private int[][] previewOriginalStates;       // 保存原始文字点阵
    private int[][] previewWorkingStates;        // 工作数组（渐变过程显示的状态）

    public SplitViewTow(PixelDrawView view) {
        this.view = view;
    }

    /**
     * 启动列渐变动画
     * @param fullTextStates 原始完整文字点阵
     * @param totalCols 总列数
     * @param rows 总行数
     */
    public void startColumnFade(int[][] fullTextStates, int totalCols, int rows) {
        if (fullTextStates == null) return;
        isColumnFadeRunning = true;
        isShowingPhase = true;
        currentColumn = 0;

        // 保存原始文字
        previewOriginalStates = new int[rows][totalCols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(fullTextStates[r], 0, previewOriginalStates[r], 0, totalCols);
        }

        // 创建工作数组初始全黑
        previewWorkingStates = new int[rows][totalCols];
        for (int r = 0; r < rows; r++) {
            Arrays.fill(previewWorkingStates[r], 0);
        }

        view.invalidate();

        fadeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isColumnFadeRunning) return;

                if (isShowingPhase) {
                    // 显现阶段：每次增加一列
                    if (currentColumn < totalCols) {
                        for (int r = 0; r < rows; r++) {
                            previewWorkingStates[r][currentColumn] = previewOriginalStates[r][currentColumn];
                        }
                        currentColumn++;
                    } else {
                        // 切换到消失阶段
                        isShowingPhase = false;
                        currentColumn = 0;
                    }
                } else {
                    // 消失阶段：每次变黑一列
                    if (currentColumn < totalCols) {
                        for (int r = 0; r < rows; r++) {
                            previewWorkingStates[r][currentColumn] = 0;
                        }
                        currentColumn++;
                    } else {
                        // 切换回显现阶段
                        isShowingPhase = true;
                        currentColumn = 0;
                    }
                }

                view.invalidate();
                fadeHandler.postDelayed(this, fadeInterval);
            }
        };

        fadeHandler.postDelayed(fadeRunnable, fadeInterval);
    }

    /**
     * 停止列渐变动画
     */
    public void stopColumnFade() {
        isColumnFadeRunning = false;
        fadeHandler.removeCallbacks(fadeRunnable);
        previewWorkingStates = null;
        view.invalidate();
    }

    /**
     * 当前是否正在列渐变
     */
    public boolean isColumnFadeRunning() {
        return isColumnFadeRunning;
    }

    /**
     * 获取当前动画显示的工作数组
     */
    public int[][] getPreviewWorkingStates() {
        return previewWorkingStates;
    }
}
