package com.example.backlight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;

import com.example.backlight.activitys.BaseActivity;
import com.example.backlight.activitys.PixelDrawView;
import com.example.backlight.activitys.MarqueeListActivity;
import com.example.backlight.utils.AnimatedGifEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {
    private PixelDrawView drawView;
    private PixelDrawView previewView;

    private Button btnDraw, btnErase, btnClear, btnInput, btnSave, btnSaveMarquee, btnViewSavedMarquee;
    private Button btnMarquee, btnRotateCW, btnRotateCCW;

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
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initListener() {
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

// 初始化权限请求器
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "缺少存储权限，无法保存", Toast.LENGTH_SHORT).show();
                    }
                });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sysBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                v.setPadding(sysBarsInsets.left, 0, sysBarsInsets.right, 0);
            } else {
                v.setPadding(0, sysBarsInsets.top, 0, sysBarsInsets.bottom);
            }
            return insets;
        });

        initView();
        initListener();
        previewView.setAsPreviewOf(drawView);
        previewView.setMode(PixelDrawView.MODE_DRAW);
        previewView.clearDots();
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

    /** 保存 GIF (已适配 Android 6 ~ 13+) **/
    private void saveMarqueeAsGif(String saveName) {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("name", saveName);
            jsonObj.put("mode", "marquee");
            JSONArray framesArray = new JSONArray();

            int[][] fullStates = previewView.getFullTextStatesCopy();
            int totalCols = previewView.getTotalCols();
            int displayCols = previewView.getCols();
            int rows = fullStates.length;
            int frameCount = totalCols + displayCols;
            List<Bitmap> gifFrames = new ArrayList<>();

            for (int offset = 0; offset < frameCount; offset++) {
                int[][] frame = new int[rows][displayCols];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < displayCols; c++) {
                        int srcCol = (c + offset) % totalCols;
                        frame[r][c] = fullStates[r][srcCol];
                    }
                }
                Bitmap bmp = previewView.renderFrameToBitmap(frame);
                gifFrames.add(bmp);

                JSONArray rowSet = new JSONArray();
                for (int rr = 0; rr < rows; rr++) {
                    JSONArray rowJson = new JSONArray();
                    for (int cc = 0; cc < displayCols; cc++) {
                        rowJson.put(frame[rr][cc]);
                    }
                    rowSet.put(rowJson);
                }
                framesArray.put(rowSet);
            }

            jsonObj.put("frames", framesArray);
            String existing = sharedPreferences.getString("saved_marquee_list", "[]");
            JSONArray listArray = new JSONArray(existing);
            listArray.put(jsonObj);
            editor.putString("saved_marquee_list", listArray.toString());
            editor.apply();

            OutputStream gifOut;
            String saveLocation;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, saveName + ".gif");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/gif");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                gifOut = getContentResolver().openOutputStream(uri);
                saveLocation = uri.toString();
            } else {
                File gifDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                if (!gifDir.exists()) gifDir.mkdirs();
                File gifFile = new File(gifDir, saveName + ".gif");
                gifOut = new FileOutputStream(gifFile);
                saveLocation = gifFile.getAbsolutePath();
            }

            AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
            gifEncoder.start(gifOut);
            gifEncoder.setRepeat(0);
            gifEncoder.setDelay(250);
            for (Bitmap frameBmp : gifFrames) {
                gifEncoder.addFrame(frameBmp);
                frameBmp.recycle();
            }
            gifEncoder.finish();
            gifOut.close();

            Toast.makeText(this, "保存成功：" + saveLocation, Toast.LENGTH_LONG).show();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        } finally {
            btnSaveMarquee.setEnabled(true);
        }
    }

    /** 新权限申请逻辑 **/
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

    /** 启动定时动画执行 **/
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

    /** 停止动画 **/
    private void stopAnimation(Button btn, String defaultText) {
        animHandler.removeCallbacks(animTask);
        isAnimRunning = false;
        btn.setText(defaultText);
        previewView.resetOffset();
    }

}
