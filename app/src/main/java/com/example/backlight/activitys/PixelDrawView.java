package com.example.backlight.activitys;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    private Paint whiteBgPaint;

    private int mode = MODE_DRAW;
    private boolean editable = true;
    private boolean isPreview = false;
    private PixelDrawView linkedDrawView = null;

    private int previewOffsetX = 0;
    private float previewRotateDegree = 0f;

    private Handler playHandler;
    private Runnable playRunnable;

    private ValueAnimator fadeAnimator;   // 控制淡入淡出的动画器
    private float fadeFactor = 1f;        // 淡入淡出插值因子 (1=白色,0=黑色)
    private boolean isFading = false;     // 是否正在淡入淡出


    public PixelDrawView(Context context) { super(context); init(); }
    public PixelDrawView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        dotStates = new int[rows][cols];

        whiteBgPaint = new Paint();
        whiteBgPaint.setColor(Color.WHITE);

        bgPaint = new Paint();
        bgPaint.setColor(Color.GRAY);

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
        // 绘制白色背景
        canvas.drawRect(0, 0, getWidth(), getHeight(), whiteBgPaint);

        // 绘制网格背景
        float gridWidth = cellSize * cols;
        float gridHeight = cellSize * rows;
        canvas.drawRect(0, 0, gridWidth, gridHeight, bgPaint);

        // 获取数据源：普通模式 / 预览模式
        int[][] srcData;
        int srcCols;
        if (isPreview && fullTextStates != null) {
            srcData = fullTextStates;
            srcCols = totalCols;
        } else {
            srcData = dotStates;
            srcCols = cols;
        }

        // 如果有旋转角度 → 使用旋转后的数据
        if (previewRotateDegree != 0f) {
            srcData = getRotatedStates(srcData, srcCols, previewRotateDegree);
        }

        // 按行列绘制点阵
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
                    // 黑点保持原样
                    canvas.drawCircle(cx, cy, dotRadius, blackPaint);
                } else {
                    // 白点根据是否淡入淡出来决定用什么颜色
                    if (isFading) {
                        // 插值计算黑白渐变颜色
                        int fadeColor = interpolateColor(Color.BLACK, Color.WHITE, fadeFactor);
                        Paint fadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        fadePaint.setColor(fadeColor);
                        canvas.drawCircle(cx, cy, dotRadius, fadePaint);
                    } else {
                        // 普通白点
                        canvas.drawCircle(cx, cy, dotRadius, whitePaint);
                    }
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

    // 恢复普通点阵数据
    public void setDotStates(int[][] states) {
        if (states != null && states.length == rows && states[0].length == cols) {
            for (int r = 0; r < rows; r++) {
                System.arraycopy(states[r], 0, dotStates[r], 0, cols);
            }
            invalidate();
        }
    }

    // 恢复完整文字帧数据
    public void setFullTextStates(int[][] states, int totalCols) {
        if (states != null) {
            this.fullTextStates = states;
            this.totalCols = totalCols;
            displayStartCol = totalCols > cols ? (totalCols - cols) / 2 : 0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    dotStates[r][c] = fullTextStates[r][Math.min(displayStartCol + c, totalCols - 1)];
                }
            }
            invalidate();
        }
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

    /** 获取当前点阵状态（直接引用） **/
    public int[][] getDotStates() {
        return dotStates;
    }

    /** 获取当前点阵状态的副本 **/
    public int[][] getDotStatesCopy() {
        int[][] copy = new int[dotStates.length][];
        for (int i = 0; i < dotStates.length; i++) {
            copy[i] = dotStates[i].clone();
        }
        return copy;
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

    /** 新的保存图片方法，适配 Android 6 ~ 13+ **/
    @SuppressLint("WrongThread")
    public String saveToAlbum(Context ctx, String fileName) throws IOException {
        Bitmap bmp = getBitmapCopy();
        OutputStream outputStream;
        String savedPath;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            Uri uri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            outputStream = ctx.getContentResolver().openOutputStream(uri);
            savedPath = uri.toString();
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            outputStream = new FileOutputStream(file);
            savedPath = file.getAbsolutePath();
        }

        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        outputStream.close();
        return savedPath;
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

    public Bitmap renderFrameToBitmap(int[][] frameData) {
        int bmpWidth = (int) (cellSize * cols);
        int bmpHeight = (int) (cellSize * rows);
        Bitmap bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawRect(0, 0, bmpWidth, bmpHeight, bgPaint);
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

    // 播放帧动画
    public void playFrames(final List<int[][]> frames, final int intervalMs) {
        if (frames == null || frames.isEmpty()) return;
        stopPlayingFrames(); // 先停掉之前的
        playHandler = new android.os.Handler();
        final int[] index = {0};
        playRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] >= frames.size()) {
                    index[0] = 0;
                }
                int[][] frameData = frames.get(index[0]);
                // 更新显示
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        dotStates[r][c] = frameData[r][c];
                    }
                }
                invalidate();
                index[0]++;
                playHandler.postDelayed(this, intervalMs);
            }
        };
        playHandler.post(playRunnable);
    }

    // 停止播放帧动画
    public void stopPlayingFrames() {
        if (playHandler != null && playRunnable != null) {
            playHandler.removeCallbacks(playRunnable);
            playRunnable = null;
            playHandler = null;
        }
    }


    public Bitmap getBitmapCopy() {
        int bmpWidth = (int) (cellSize * cols);
        int bmpHeight = (int) (cellSize * rows);
        Bitmap bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
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
        return bmp;
    }

    public boolean isFading() {
        return isFading;
    }


    public void startFadeEffect() {
        if (isFading) return; // 已经在淡入淡出中，直接返回
        isFading = true;
        fadeAnimator = ValueAnimator.ofFloat(1f, 0f, 1f); // 白→黑→白
        fadeAnimator.setDuration(2000); // 一个循环 2 秒
        fadeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        fadeAnimator.addUpdateListener(animation -> {
            fadeFactor = (float) animation.getAnimatedValue();
            invalidate(); // 触发重绘，颜色会变化
        });
        fadeAnimator.start();
    }

    public void stopFadeEffect() {
        if (fadeAnimator != null) {
            fadeAnimator.cancel();
            fadeAnimator = null;
        }
        fadeFactor = 1f;  // 恢复为白色
        isFading = false;
        invalidate(); // 重绘
    }

    private int interpolateColor(int colorFrom, int colorTo, float factor) {
        int r1 = Color.red(colorFrom);
        int g1 = Color.green(colorFrom);
        int b1 = Color.blue(colorFrom);

        int r2 = Color.red(colorTo);
        int g2 = Color.green(colorTo);
        int b2 = Color.blue(colorTo);

        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return Color.rgb(r, g, b);
    }
}
