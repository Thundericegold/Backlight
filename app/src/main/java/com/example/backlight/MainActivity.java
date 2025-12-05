package com.example.backlight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.backlight.activitys.BaseActivity;
import com.example.backlight.activitys.MarqueeListActivity;
import com.example.backlight.activitys.PixelDrawView;

import java.io.IOException;

public class MainActivity extends BaseActivity {
    private PixelDrawView drawView;
    private PixelDrawView previewView;

    private Button btnDraw, btnErase, btnClear, btnInput, btnSave, btnSaveMarquee, btnViewSavedMarquee;
    private Button btnMarquee, btnRotateCW, btnRotateCCW, btnFade, btnGradient;

    private Handler animHandler = new Handler();
    private Runnable animTask;
    private boolean isAnimRunning = false;

    // 权限请求器
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    public void initView() {
        drawView = findViewById(R.id.drawView);
        previewView = findViewById(R.id.previewView);
        btnDraw = findViewById(R.id.btnDraw);
        btnErase = findViewById(R.id.btnErase);
        btnClear = findViewById(R.id.btnClear);
        btnInput = findViewById(R.id.btnInput);
        btnSave = findViewById(R.id.btnSave);
        btnSaveMarquee = findViewById(R.id.btnSaveMarquee);
        btnViewSavedMarquee = findViewById(R.id.btnViewSavedMarquee);
        btnMarquee = findViewById(R.id.btnMarquee);
        btnRotateCW = findViewById(R.id.btnRotateCW);
        btnRotateCCW = findViewById(R.id.btnRotateCCW);
        btnFade = findViewById(R.id.btnFade);
        btnGradient = findViewById(R.id.btnGradient);
        disableButton();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initListener() {
        drawView.setOnContentChangeListener(isEmpty -> {
            btnClear.setEnabled(!isEmpty);
            btnErase.setEnabled(!isEmpty);
            if (isEmpty) {
                canvasMonitoring();
            }
        });
        btnDraw.setOnClickListener(v -> {
            stopAnimation(btnMarquee, "跑马灯");
            drawView.setMode(PixelDrawView.MODE_DRAW);
        });
        btnErase.setOnClickListener(v -> {
            stopAnimation(btnMarquee, "跑马灯");
            drawView.setMode(PixelDrawView.MODE_ERASE);
        });
        btnClear.setOnClickListener(v -> {
            stopAnimation(btnMarquee, "跑马灯");
            previewView.stopColumnFade();
            drawView.resetAll();
            previewView.resetAll();
            drawView.setMode(PixelDrawView.MODE_DRAW);
        });
        btnInput.setOnClickListener(v -> {
            stopAnimation(btnMarquee, "跑马灯");
            showTextInputDialog();
        });
        btnSave.setOnClickListener(v -> {
            stopAnimation(btnMarquee, "跑马灯");
            checkStoragePermission(() -> {
                try {
                    String path = drawView.saveToAlbum(MainActivity.this,
                            "pixel_" + System.currentTimeMillis() + ".png");
                    Toast.makeText(this, "已保存到相册: " + path, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
        btnSaveMarquee.setOnClickListener(v -> {
            stopAnimation(btnMarquee, "跑马灯");
            if (previewView != null && previewView.hasFullTextStates()) {
                checkStoragePermission(this::showSaveMarqueeDialog);
            } else {
                Toast.makeText(this, "请先输入文字并在预览区显示", Toast.LENGTH_SHORT).show();
            }
        });
        btnViewSavedMarquee.setOnClickListener(v -> {
            stopAnimation(btnMarquee, "跑马灯");
            startActivity(new Intent(MainActivity.this, MarqueeListActivity.class));
        });
        btnMarquee.setOnClickListener(v -> {
            if (!isAnimRunning) {
                startAnimation(() -> previewView.scrollLeft(), 250);
                isAnimRunning = true;
                btnMarquee.setText("停止跑马灯");
            } else {
                stopAnimation(btnMarquee, "跑马灯");
            }
        });
        btnRotateCW.setOnClickListener(v -> {
            if (!isAnimRunning) {
                startAnimation(() -> previewView.addPreviewRotateDegree(5f), 20);
                isAnimRunning = true;
                btnRotateCW.setText("停止旋转");
            } else {
                stopAnimation(btnRotateCW, "顺时针旋转");
            }
        });
        btnRotateCCW.setOnClickListener(v -> {
            if (!isAnimRunning) {
                startAnimation(() -> previewView.addPreviewRotateDegree(-5f), 20);
                isAnimRunning = true;
                btnRotateCCW.setText("停止旋转");
            } else {
                stopAnimation(btnRotateCCW, "逆时针旋转");
            }
        });
        drawView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                stopAnimation(btnMarquee, "跑马灯");
            }
            return v.onTouchEvent(event);
        });
        btnFade.setOnClickListener(v -> {
            if (previewView.isFading()) {
                previewView.stopFadeEffect();
                btnFade.setText("淡入淡出");
                return;
            }
            if (!previewView.hasFullTextStates()) {
                Toast.makeText(this, "请输入文字才能使用该特效", Toast.LENGTH_SHORT).show();
                return;
            }
            previewView.startFadeEffect();
            btnFade.setText("停止淡入淡出");
        });
        btnGradient.setOnClickListener(v -> {
            if (!previewView.hasFullTextStates()) {
                Toast.makeText(this, "预览中没有文字，无法使用该特效", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!previewView.isColumnFadeRunning()) {
                previewView.startColumnFade();
                btnGradient.setText("停止渐变");
            } else {
                previewView.stopColumnFade();
                btnGradient.setText("开始渐变");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "缺少存储权限，无法保存", Toast.LENGTH_SHORT).show();
                    }
                });

        previewView.setAsPreviewOf(drawView);
        previewView.setMode(PixelDrawView.MODE_DRAW);
        previewView.clearDots();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("dotStates", arrayToJson(drawView.getDotStatesCopy()));
        if (drawView.hasFullTextStates()) {
            outState.putString("fullTextStates", arrayToJson(drawView.getFullTextStatesCopy()));
            outState.putInt("totalCols", drawView.getTotalCols());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String dotJson = savedInstanceState.getString("dotStates");
        String fullJson = savedInstanceState.getString("fullTextStates");
        int totalCols = savedInstanceState.getInt("totalCols", drawView.getCols());

        if (dotJson != null) {
            drawView.setDotStates(jsonToArray(dotJson));
            previewView.setDotStates(jsonToArray(dotJson));
        }
        if (fullJson != null) {
            drawView.setFullTextStates(jsonToArray(fullJson), totalCols);
            previewView.setFullTextStates(jsonToArray(fullJson), totalCols);
        }
        disableButton();
    }

    private String arrayToJson(int[][] arr) {
        try {
            org.json.JSONArray outer = new org.json.JSONArray();
            for (int[] row : arr) {
                org.json.JSONArray inner = new org.json.JSONArray();
                for (int v : row) {
                    inner.put(v);
                }
                outer.put(inner);
            }
            return outer.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private int[][] jsonToArray(String json) {
        try {
            org.json.JSONArray outer = new org.json.JSONArray(json);
            int[][] arr = new int[outer.length()][outer.getJSONArray(0).length()];
            for (int r = 0; r < outer.length(); r++) {
                org.json.JSONArray inner = outer.getJSONArray(r);
                for (int c = 0; c < inner.length(); c++) {
                    arr[r][c] = inner.getInt(c);
                }
            }
            return arr;
        } catch (Exception e) {
            return null;
        }
    }

    private void showTextInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("输入文字及大小（最多20字符）");

        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        final EditText inputField = new EditText(MainActivity.this);
        inputField.setHint("请输入文字");
        inputField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        inputField.setBackgroundResource(R.drawable.rounded_edit_text);
        inputField.setPadding(20, 20, 20, 20);
        layout.addView(inputField);

        final RadioGroup sizeGroup = new RadioGroup(MainActivity.this);
        sizeGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbLarge = new RadioButton(MainActivity.this);
        rbLarge.setText("大");
        RadioButton rbMedium = new RadioButton(MainActivity.this);
        rbMedium.setText("中");
        RadioButton rbSmall = new RadioButton(MainActivity.this);
        rbSmall.setText("小");
        sizeGroup.addView(rbLarge);
        sizeGroup.addView(rbMedium);
        sizeGroup.addView(rbSmall);
        rbMedium.setChecked(true);
        layout.addView(sizeGroup);

        builder.setView(layout);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String text = inputField.getText().toString().trim();
            if (!text.isEmpty()) {
                int sizeMode = PixelDrawView.SIZE_MEDIUM;
                if (rbLarge.isChecked()) sizeMode = PixelDrawView.SIZE_LARGE;
                if (rbSmall.isChecked()) sizeMode = PixelDrawView.SIZE_SMALL;
                drawView.drawTextOnGrid(text, sizeMode);
                previewView.drawTextOnGrid(text, sizeMode);
                autoMarquee();
            }
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showSaveMarqueeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("输入保存名称");

        final EditText nameInput = new EditText(MainActivity.this);
        nameInput.setHint("请输入跑马灯名称");
        nameInput.setBackgroundResource(R.drawable.rounded_edit_text);
        nameInput.setPadding(20, 20, 20, 20);
        builder.setView(nameInput);

        builder.setPositiveButton("保存", (dialog, w) -> {
            String saveName = nameInput.getText().toString().trim();
            if (saveName.isEmpty()) {
                Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSaveMarquee.setEnabled(false);
            saveMarqueeAsGif(saveName);
        });
        builder.setNegativeButton("取消", (dialog, w) -> dialog.dismiss());
        builder.show();
    }

    private void saveMarqueeAsGif(String saveName) {
        Toast.makeText(this, "正在保存跑马灯，请稍候...", Toast.LENGTH_SHORT).show();
        btnSaveMarquee.setEnabled(false);

        new Thread(() -> {
            try {
                SplitMain.GifResult result =
                        SplitMain.generateGifFromPreview(MainActivity.this, previewView, saveName, 250);
                SplitMain.saveMarqueeRecord(MainActivity.this, saveName, result, 250);

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "保存完成: " + result.gifPath, Toast.LENGTH_LONG).show();
                    btnSaveMarquee.setEnabled(true);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                    btnSaveMarquee.setEnabled(true);
                });
            }
        }).start();
    }

    private void checkStoragePermission(Runnable onGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
        }
        onGranted.run();
    }

    private void startAnimation(Runnable action, int intervalMs) {
        animTask = new Runnable() {
            @Override
            public void run() {
                action.run();
                animHandler.postDelayed(this, intervalMs);
            }
        };
        animHandler.post(animTask);
    }

    private void stopAnimation(Button btn, String defaultText) {
        animHandler.removeCallbacks(animTask);
        isAnimRunning = false;
        btn.setText(defaultText);
        previewView.resetOffset();
    }

    private void disableButton() {
        btnClear.setEnabled(!drawView.isEmpty());
        btnErase.setEnabled(!drawView.isEmpty());
    }

    public void canvasMonitoring() {
        stopAnimation(btnMarquee, "跑马灯");
        stopAnimation(btnRotateCW, "顺时针旋转");
        stopAnimation(btnRotateCCW, "逆时针旋转");
        previewView.stopFadeEffect();
        btnFade.setText("淡入淡出");
        previewView.stopColumnFade();
        btnGradient.setText("开始渐变");
    }

    private void autoMarquee() {
        if (previewView.isOutCanvas()) {
            startAnimation(() -> previewView.scrollLeft(), 250);
            isAnimRunning = true;
            btnMarquee.setText("停止跑马灯");
        }
    }
}
