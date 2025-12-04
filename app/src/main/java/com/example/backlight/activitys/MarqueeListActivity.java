package com.example.backlight.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.backlight.R;
import com.example.backlight.data.AppDatabase;
import com.example.backlight.data.MarqueeDao;
import com.example.backlight.data.MarqueeEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MarqueeListActivity extends BaseActivity {

    private ListView listView;
    private PixelDrawView marqueePreview;
    private Button btnRefresh;
    private List<JSONObject> savedEffects = new ArrayList<>();
    private MarqueeAdapter adapter;
    private int playIntervalMs = 250; // 默认播放速度

    private MarqueeDao dao;

    @Override
    public void initView() {
        listView = findViewById(R.id.marqueeListView);
        marqueePreview = findViewById(R.id.marqueePreview);
        btnRefresh = findViewById(R.id.btnRefresh);

        // 初始化数据库 DAO
        dao = AppDatabase.getInstance(this).marqueeDao();
    }

    @Override
    public void initListener() {
        btnRefresh.setOnClickListener(v -> {
            loadSavedMarquee();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show();
        });

        listView.setOnItemClickListener((parent, view, position, id) -> playSelectedMarquee(position));

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
                                    openGifFile(savedEffects.get(position).optString("name"));
                                } else {
                                    Toast.makeText(this, "无读取图片权限", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 4:
                                if (ensureReadPermission()) {
                                    shareGifFile(savedEffects.get(position).optString("name"));
                                } else {
                                    Toast.makeText(this, "无读取图片权限", Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }
                    }).show();
            return true;
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marquee_list);
        initView();
        initListener();
        loadSavedMarquee();
        adapter = new MarqueeAdapter(this, savedEffects);
        listView.setAdapter(adapter);

        String autoPlayName = getIntent().getStringExtra("play_on_open");
        if (autoPlayName != null) {
            for (int i = 0; i < savedEffects.size(); i++) {
                if (autoPlayName.equals(savedEffects.get(i).optString("name"))) {
                    int finalI = i;
                    marqueePreview.post(() -> playSelectedMarquee(finalI));
                    break;
                }
            }
        }
    }

    private class MarqueeAdapter extends ArrayAdapter<JSONObject> {
        public MarqueeAdapter(MarqueeListActivity context, List<JSONObject> data) {
            super(context, 0, data);
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_marquee, parent, false);
            }
            TextView tvName = convertView.findViewById(R.id.tvName);
            ImageView imgGif = convertView.findViewById(R.id.imgGif);

            JSONObject obj = getItem(position);
            if (obj != null) {
                String name = obj.optString("name", "未命名");
                tvName.setText(name);

                File gifFile = new File(obj.optString("gifPath"));
                if (gifFile.exists()) {
                    Glide.with(MarqueeListActivity.this)
                            .asGif()
                            .load(gifFile)
                            .override(80, 80)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(imgGif);
                } else {
                    imgGif.setImageResource(android.R.color.transparent);
                }
            }
            return convertView;
        }
    }

    private void loadSavedMarquee() {
        savedEffects.clear();
        List<MarqueeEntity> entities = dao.getAll();
        for (MarqueeEntity e : entities) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", e.name);
                obj.put("mode", e.mode);
                obj.put("frames", new JSONArray(e.framesJson));
                obj.put("speed", e.speed);
                obj.put("gifPath", e.gifPath); // 存 GIF 路径
                savedEffects.add(obj);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    private void playSelectedMarquee(int position) {
        try {
            marqueePreview.stopPlayingFrames();
            JSONObject selected = savedEffects.get(position);
            if (selected.has("speed")) {
                playIntervalMs = selected.getInt("speed");
            }
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

    private void renameMarquee(int position) {
        final EditText editText = new EditText(this);
        editText.setHint("请输入新名称");
        new AlertDialog.Builder(this)
                .setTitle("重命名跑马灯")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        MarqueeEntity entity = dao.findByName(savedEffects.get(position).optString("name"));
                        if (entity != null) {
                            entity.name = newName;
                            dao.update(entity);
                            loadSavedMarquee();
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteMarquee(int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除跑马灯")
                .setMessage("确定删除该跑马灯吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    MarqueeEntity entity = dao.findByName(savedEffects.get(position).optString("name"));
                    if (entity != null) {
                        dao.delete(entity);
                    }
                    loadSavedMarquee();
                    adapter.notifyDataSetChanged();
                    marqueePreview.stopPlayingFrames();
                })
                .setNegativeButton("取消", null)
                .show();
    }

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
                            MarqueeEntity entity = dao.findByName(savedEffects.get(position).optString("name"));
                            if (entity != null) {
                                entity.speed = ms;
                                dao.update(entity);
                            }
                            playSelectedMarquee(position);
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "输入无效", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openGifFile(String gifPath) {
        File gifFile = new File(gifPath);
        if (gifFile.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", gifFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, "image/gif");
            startActivity(Intent.createChooser(intent, "查看GIF"));
        } else {
            Toast.makeText(this, "GIF不存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareGifFile(String gifPath) {
        File gifFile = new File(gifPath);
        if (gifFile.exists()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/gif");
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", gifFile);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(shareIntent, "分享GIF"));
        } else {
            Toast.makeText(this, "GIF不存在", Toast.LENGTH_SHORT).show();
        }
    }


    private File findGifFile(String name) {
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(picturesDir, name + ".gif");
    }

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
