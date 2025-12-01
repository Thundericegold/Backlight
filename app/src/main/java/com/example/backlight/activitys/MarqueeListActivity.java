package com.example.backlight.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.backlight.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MarqueeListActivity extends AppCompatActivity {
    private ListView listView;
    private PixelDrawView marqueePreview;
    private List<JSONObject> savedEffects = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<String> names = new ArrayList<>();
    private int playIntervalMs = 250; // 默认播放速度

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marquee_list);

        listView = findViewById(R.id.marqueeListView);
        marqueePreview = findViewById(R.id.marqueePreview);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        btnRefresh.setOnClickListener(v -> {
            loadSavedMarquee();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show();
        });

        loadSavedMarquee();

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);

        // 单击播放
        listView.setOnItemClickListener((parent, view, position, id) -> playSelectedMarquee(position));

        // 长按菜单
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String[] options = {"重命名", "删除", "调整播放速度", "打开GIF", "分享GIF"};
            new AlertDialog.Builder(this)
                    .setTitle("管理跑马灯")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                renameMarquee(position);
                                break;
                            case 1:
                                deleteMarquee(position);
                                break;
                            case 2:
                                adjustSpeed(position);
                                break;
                            case 3:
                                if (ensureReadPermission()) {
                                    openGifFile(names.get(position));
                                } else {
                                    Toast.makeText(this, "无读取图片权限", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 4:
                                if (ensureReadPermission()) {
                                    shareGifFile(names.get(position));
                                } else {
                                    Toast.makeText(this, "无读取图片权限", Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }
                    }).show();
            return true;
        });
    }

    /** 加载保存的跑马灯列表 */
    private void loadSavedMarquee() {
        try {
            String savedJson = getSharedPreferences(BaseActivity.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                    .getString("saved_marquee_list", "[]");
            JSONArray arr = new JSONArray(savedJson);
            names.clear();
            savedEffects.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                savedEffects.add(obj);
                names.add(obj.optString("name", "未命名"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 播放选中跑马灯 */
    private void playSelectedMarquee(int position) {
        try {
            marqueePreview.stopPlayingFrames();
            JSONObject selected = savedEffects.get(position);
            JSONArray framesJson = selected.getJSONArray("frames");
            List<int[][]> frames = new ArrayList<>();
            for (int f = 0; f < framesJson.length(); f++) {
                JSONArray frameRows = framesJson.getJSONArray(f);
                int rows = frameRows.length();
                int cols = frameRows.getJSONArray(0).length();
                int[][] frame = new int[rows][cols];
                for (int r = 0; r < rows; r++) {
                    JSONArray rowJson = frameRows.getJSONArray(r);
                    for (int c = 0; c < cols; c++) {
                        frame[r][c] = rowJson.getInt(c);
                    }
                }
                frames.add(frame);
            }
            marqueePreview.playFrames(frames, playIntervalMs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 重命名跑马灯 */
    private void renameMarquee(int position) {
        final EditText editText = new EditText(this);
        editText.setHint("请输入新名称");
        new AlertDialog.Builder(this)
                .setTitle("重命名跑马灯")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        try {
                            savedEffects.get(position).put("name", newName);
                            saveChanges();
                            loadSavedMarquee();
                            adapter.notifyDataSetChanged();
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 删除跑马灯 */
    private void deleteMarquee(int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除跑马灯")
                .setMessage("确定删除该跑马灯吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    savedEffects.remove(position);
                    saveChanges();
                    loadSavedMarquee();
                    adapter.notifyDataSetChanged();
                    marqueePreview.stopPlayingFrames();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 调整播放速度并立即应用 */
    private void adjustSpeed(int position) {
        final EditText editText = new EditText(this);
        editText.setHint("请输入速度（毫秒）");
        new AlertDialog.Builder(this)
                .setTitle("调整播放速度")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String val = editText.getText().toString().trim();
                    try {
                        int ms = Integer.parseInt(val);
                        if (ms > 0) {
                            playIntervalMs = ms;
                            playSelectedMarquee(position);
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "输入无效", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 打开对应GIF（已兼容 Android 10+ Scoped Storage） */
    private void openGifFile(String name) {
        File gifFile = findGifFile(name);
        if (gifFile != null && gifFile.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", gifFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(gifFile);
            }
            intent.setDataAndType(uri, "image/gif");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开GIF", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "GIF文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    /** 分享对应GIF（已兼容 Android 10+ Scoped Storage） */
    private void shareGifFile(String name) {
        File gifFile = findGifFile(name);
        if (gifFile != null && gifFile.exists()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/gif");
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", gifFile);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(gifFile);
            }
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            try {
                startActivity(Intent.createChooser(shareIntent, "分享GIF"));
            } catch (Exception e) {
                Toast.makeText(this, "无法分享GIF", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "GIF文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    /** 查找 GIF 文件（兼容 Android 6~13+） **/
    private File findGifFile(String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 用 MediaStore 查询
            try (Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME},
                    MediaStore.Images.Media.DISPLAY_NAME + "=?",
                    new String[]{name + ".gif"},
                    null
            )) {
                if (cursor != null && cursor.moveToFirst()) {
                    // Android 10+ 读取绝对路径
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                    Uri contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    return new File(getRealPathFromUri(contentUri)); // getRealPathFromUri 方法要适配
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        } else {
            // Android 6~9 用旧目录
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            return new File(picturesDir, name + ".gif");
        }
    }

    /** 从 contentUri 获取真实文件路径（兼容 Android 6~13+） **/
    private String getRealPathFromUri(Uri uri) {
        String filePath = null;
        // 查询 MediaStore 数据库
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{MediaStore.Images.Media.DATA}, // 旧字段，在 Android Q 已弃用但有的机型还返回
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                if (idx != -1) {
                    filePath = cursor.getString(idx);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 如果 filePath 是 null，用更通用的方法尝试打开
        if (filePath == null) {
            try {
                // 把 Uri 里的内容复制到临时文件，这样也能用 FileProvider 共享
                File tempFile = new File(getCacheDir(), System.currentTimeMillis() + ".gif");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                filePath = tempFile.getAbsolutePath();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        return filePath;
    }



    /** 保存修改到SP */
    private void saveChanges() {
        try {
            JSONArray arr = new JSONArray();
            for (JSONObject obj : savedEffects) {
                arr.put(obj);
            }
            getSharedPreferences(BaseActivity.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                    .edit()
                    .putString("saved_marquee_list", arr.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 权限检查方法 */
    private boolean ensureReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (marqueePreview != null) {
            marqueePreview.stopPlayingFrames();
        }
    }
}
