package com.example.backlight.activitys;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PixelDrawView extends View {
    public static final int MODE_DRAW = 1;
    public static final int MODE_ERASE = 2;

    public static final int SIZE_SMALL = 1;
    public static final int SIZE_MEDIUM = 2;
    public static final int SIZE_LARGE = 3;

    private int cols = 20;
    private int rows = 15;
    private float cellSize;
    private float dotRadius;
    private float[][][] dotCenters;

    private int[][] dotStates;
    private int[][] fullTextStates;
    private int totalCols = cols;
    private int displayStartCol = 0;

    private Paint blackPaint;
    private Paint whitePaint;
    private Paint bgPaint;
    private Paint whiteBgPaint; // 新增白底画笔

    private int mode = MODE_DRAW;
    private boolean editable = true;
    private boolean isPreview = false;
    private PixelDrawView linkedDrawView = null;

    private int previewOffsetX = 0;
    private float previewRotateDegree = 0f;

    // 动画播放控制
    private Handler playHandler;
    private Runnable playRunnable;

    public PixelDrawView(Context context) { super(context); init(); }
    public PixelDrawView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        dotStates = new int[rows][cols];

        whiteBgPaint = new Paint();
        whiteBgPaint.setColor(Color.WHITE); // 白色背景

        bgPaint = new Paint();
        bgPaint.setColor(Color.GRAY); // 灰色点阵背景

        blackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackPaint.setColor(Color.BLACK);

        whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setColor(Color.WHITE);

        dotCenters = new float[rows][cols][2];
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cellSize = Math.min(w / (cols * 1f), h / (rows * 1f));
        dotRadius = cellSize * 0.15f;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                dotCenters[r][c][0] = cellSize * (c + 0.5f);
                dotCenters[r][c][1] = cellSize * (r + 0.5f);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. 整个View背景为白色
        canvas.drawRect(0, 0, getWidth(), getHeight(), whiteBgPaint);

        // 2. 仅点阵区域绘制为灰色背景
        float gridWidth = cellSize * cols;
        float gridHeight = cellSize * rows;
        canvas.drawRect(0, 0, gridWidth, gridHeight, bgPaint);

        // 3. 决定绘制数据源
        int[][] srcData;
        int srcCols;
        if (isPreview && fullTextStates != null) {
            srcData = fullTextStates;
            srcCols = totalCols;
        } else {
            srcData = dotStates;
            srcCols = cols;
        }

        // 4. 处理旋转
        if (previewRotateDegree != 0f) {
            srcData = getRotatedStates(srcData, srcCols, previewRotateDegree);
        }

        // 5. 绘制点
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int srcCol;
                if (isPreview && previewOffsetX != 0) {
                    srcCol = (c + previewOffsetX) % srcCols;
                } else if (isPreview && srcCols > cols) {
                    srcCol = Math.min(displayStartCol + c, srcCols - 1);
                } else {
                    srcCol = c;
                }
                float cx = dotCenters[r][c][0];
                float cy = dotCenters[r][c][1];
                if (srcData[r][srcCol] == 0) {
                    canvas.drawCircle(cx, cy, dotRadius, blackPaint);
                } else {
                    canvas.drawCircle(cx, cy, dotRadius, whitePaint);
                }
            }
        }
    }

    private int[][] getRotatedStates(int[][] src, int srcCols, float degree) {
        int[][] rotated = new int[rows][srcCols];
        double radians = Math.toRadians(degree);
        float centerX = (srcCols - 1) / 2f;
        float centerY = (rows - 1) / 2f;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < srcCols; c++) {
                if (src[r][c] != 0) {
                    float dx = c - centerX;
                    float dy = r - centerY;
                    float rotatedX = (float) (dx * Math.cos(radians) - dy * Math.sin(radians));
                    float rotatedY = (float) (dx * Math.sin(radians) + dy * Math.cos(radians));
                    int newC = Math.round(rotatedX + centerX);
                    int newR = Math.round(rotatedY + centerY);
                    if (newR >= 0 && newR < rows && newC >= 0 && newC < srcCols) {
                        rotated[newR][newC] = src[r][c];
                    }
                }
            }
        }
        return rotated;
    }

    public void drawTextOnGrid(String text, int sizeMode) {
        clearDots();
        fullTextStates = null;
        if (text == null || text.isEmpty()) { invalidate(); return; }

        float baseSize;
        if (sizeMode == SIZE_LARGE) baseSize = rows * 1.2f;
        else if (sizeMode == SIZE_SMALL) baseSize = rows * 0.8f;
        else baseSize = rows;

        Paint measure = new Paint(Paint.ANTI_ALIAS_FLAG);
        measure.setTextSize(baseSize);
        float textWidthPx = measure.measureText(text);
        totalCols = Math.max(cols, Math.round(textWidthPx));

        Bitmap tmp = Bitmap.createBitmap(totalCols, rows, Bitmap.Config.ARGB_8888);
        Canvas tmpCanvas = new Canvas(tmp);
        tmpCanvas.drawColor(Color.BLACK);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(baseSize);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        tmpCanvas.drawText(text, 0, (rows - textHeight) / 2f - fm.ascent, textPaint);

        fullTextStates = new int[rows][totalCols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < totalCols; c++) {
                int color = tmp.getPixel(c, r);
                fullTextStates[r][c] = (Color.red(color) > 128) ? 1 : 0;
            }
        }

        displayStartCol = totalCols > cols ? (totalCols - cols) / 2 : 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                dotStates[r][c] = fullTextStates[r][Math.min(displayStartCol + c, totalCols - 1)];
            }
        }
        invalidate();
    }

    public void setMode(int m) { mode = m; }
    public boolean hasFullTextStates() { return fullTextStates != null; }

    public int[][] getFullTextStatesCopy() {
        if (fullTextStates == null) return null;
        int[][] copy = new int[fullTextStates.length][fullTextStates[0].length];
        for (int r = 0; r < fullTextStates.length; r++) {
            System.arraycopy(fullTextStates[r], 0, copy[r], 0, fullTextStates[r].length);
        }
        return copy;
    }

    public int getTotalCols() { return totalCols; }
    public int getCols() { return cols; }
    public void clearDots() {
        for (int[] row : dotStates) java.util.Arrays.fill(row, 0);
        invalidate();
    }

    public int[][] getDotStates() { return dotStates; }
    public void setEditable(boolean e) { editable = e; }

    @SuppressLint("ClickableViewAccessibility")
    public void setAsPreviewOf(PixelDrawView target) {
        isPreview = true;
        linkedDrawView = target;
        this.editable = false;
        target.setOnTouchListener((v, e) -> {
            v.onTouchEvent(e);
            this.dotStates = target.getDotStates();
            this.fullTextStates = target.fullTextStates;
            this.totalCols = target.totalCols;
            this.displayStartCol = target.displayStartCol;
            this.invalidate();
            return true;
        });
    }

    public void scrollLeft() {
        if (!isPreview) return;
        previewOffsetX += 1;
        invalidate();
    }

    public void resetAll() {
        clearDots();
        fullTextStates = null;
        totalCols = cols;
        displayStartCol = 0;
        previewOffsetX = 0;
        previewRotateDegree = 0f;
        invalidate();
    }

    public void resetOffset() {
        previewOffsetX = 0;
        previewRotateDegree = 0f;
        invalidate();
    }

    public void addPreviewRotateDegree(float d) {
        previewRotateDegree += d;
        invalidate();
    }

    /** 保存仅点阵区域的图像 */
    @SuppressLint("WrongThread")
    public String saveToAlbum(Context ctx, String fileName) throws IOException {
        int bmpWidth = (int)(cellSize * cols);
        int bmpHeight = (int)(cellSize * rows);
        Bitmap bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // 灰底
        canvas.drawRect(0, 0, bmpWidth, bmpHeight, bgPaint);

        int[][] srcData;
        int srcCols;
        if (isPreview && fullTextStates != null) {
            srcData = fullTextStates;
            srcCols = totalCols;
        } else {
            srcData = dotStates;
            srcCols = cols;
        }

        if (previewRotateDegree != 0f) {
            srcData = getRotatedStates(srcData, srcCols, previewRotateDegree);
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int srcCol;
                if (isPreview && previewOffsetX != 0) {
                    srcCol = (c + previewOffsetX) % srcCols;
                } else if (isPreview && srcCols > cols) {
                    srcCol = Math.min(displayStartCol + c, srcCols - 1);
                } else {
                    srcCol = c;
                }
                float cx = cellSize * (c + 0.5f);
                float cy = cellSize * (r + 0.5f);
                if (srcData[r][srcCol] == 0) {
                    canvas.drawCircle(cx, cy, dotRadius, blackPaint);
                } else {
                    canvas.drawCircle(cx, cy, dotRadius, whitePaint);
                }
            }
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        android.media.MediaScannerConnection.scanFile(ctx,
                new String[]{file.getAbsolutePath()},
                new String[]{"image/png"}, null);
        return file.getAbsolutePath();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!editable || isPreview) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {
            int[] nearest = findNearestDot(event.getX(), event.getY());
            if (nearest != null) {
                dotStates[nearest[0]][nearest[1]] = (mode == MODE_DRAW ? 1 : 0);
                invalidate();
                fullTextStates = dotStates;
                totalCols = cols;
            }
        }
        return true;
    }

    private int[] findNearestDot(float x, float y) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float dx = x - dotCenters[r][c][0];
                float dy = y - dotCenters[r][c][1];
                if (Math.sqrt(dx * dx + dy * dy) <= dotRadius * 2.5) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    // 用于展示保存的帧
    public void setFrameData(int[][] frame) {
        this.dotStates = frame;
        invalidate();
    }

    public void stopPlayingFrames() {
        if (playHandler != null && playRunnable != null) {
            playHandler.removeCallbacks(playRunnable);
            playRunnable = null;
        }
    }

    public void playFrames(List<int[][]> frames, int intervalMs) {
        stopPlayingFrames();
        playHandler = new Handler();
        playRunnable = new Runnable() {
            int index = 0;
            @Override
            public void run() {
                setFrameData(frames.get(index));
                index = (index + 1) % frames.size();
                playHandler.postDelayed(this, intervalMs);
            }
        };
        playHandler.post(playRunnable);
    }

    public int[][] getDotStatesCopy() {
        int[][] copy = new int[dotStates.length][];
        for (int i = 0; i < dotStates.length; i++) {
            copy[i] = dotStates[i].clone();
        }
        return copy;
    }

    public void syncFromLinkedDrawView() {
        if (linkedDrawView != null) {
            this.dotStates = linkedDrawView.getDotStatesCopy();
            this.fullTextStates = linkedDrawView.getFullTextStatesCopy();
            this.totalCols = linkedDrawView.getTotalCols();
            this.displayStartCol = linkedDrawView.displayStartCol;
            invalidate();
        }
    }
    public Bitmap renderFrameToBitmap(int[][] frameData) {
        // 灰底尺寸
        int bmpWidth = (int) (cellSize * cols);
        int bmpHeight = (int) (cellSize * rows);

        // 创建只包含灰底区域的 Bitmap
        Bitmap bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // 灰色背景
        canvas.drawRect(0, 0, bmpWidth, bmpHeight, bgPaint);

        // 绘制点阵
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float cx = cellSize * (c + 0.5f);
                float cy = cellSize * (r + 0.5f);
                if (frameData[r][c] == 0) {
                    canvas.drawCircle(cx, cy, dotRadius, blackPaint);
                } else {
                    canvas.drawCircle(cx, cy, dotRadius, whitePaint);
                }
            }
        }

        return bmp;
    }

    /**
     * 获取当前显示点阵的 Bitmap（只包含灰色点阵区域）
     */
    public Bitmap getBitmapCopy() {
        int bmpWidth = (int) (cellSize * cols);
        int bmpHeight = (int) (cellSize * rows);
        Bitmap bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // 灰色背景
        canvas.drawRect(0, 0, bmpWidth, bmpHeight, bgPaint);

        // 决定绘制的数据源
        int[][] srcData;
        int srcCols;
        if (isPreview && fullTextStates != null) {
            srcData = fullTextStates;
            srcCols = totalCols;
        } else {
            srcData = dotStates;
            srcCols = cols;
        }

        // 是否旋转
        if (previewRotateDegree != 0f) {
            srcData = getRotatedStates(srcData, srcCols, previewRotateDegree);
        }

        // 绘制当前点阵
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int srcCol;
                if (isPreview && previewOffsetX != 0) {
                    srcCol = (c + previewOffsetX) % srcCols;
                } else if (isPreview && srcCols > cols) {
                    srcCol = Math.min(displayStartCol + c, srcCols - 1);
                } else {
                    srcCol = c;
                }
                float cx = cellSize * (c + 0.5f);
                float cy = cellSize * (r + 0.5f);
                if (srcData[r][srcCol] == 0) {
                    canvas.drawCircle(cx, cy, dotRadius, blackPaint);
                } else {
                    canvas.drawCircle(cx, cy, dotRadius, whitePaint);
                }
            }
        }
        return bmp;
    }




}
