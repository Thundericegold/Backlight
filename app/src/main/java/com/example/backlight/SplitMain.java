package com.example.backlight;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.example.backlight.activitys.PixelDrawView;
import com.example.backlight.data.AppDatabase;
import com.example.backlight.data.MarqueeDao;
import com.example.backlight.data.MarqueeEntity;
import com.example.backlight.utils.AnimatedGifEncoder;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 跑马灯保存处理类
 * 负责生成所有帧 -> 保存GIF文件 -> 保存记录到数据库
 */
public class SplitMain {

    /** 保存生成结果对象 */
    public static class GifResult {
        public final String gifPath;      // GIF 保存路径
        public final String framesJson;   // 所有帧的 JSON 记录

        public GifResult(String gifPath, String framesJson) {
            this.gifPath = gifPath;
            this.framesJson = framesJson;
        }
    }

    /**
     * 生成GIF文件并返回结果
     */
    public static GifResult generateGifFromPreview(Context context,
                                                   PixelDrawView previewView,
                                                   String saveName,
                                                   int delayMs)
            throws Exception {

        JSONArray framesArray = new JSONArray();
        int[][] fullStates = previewView.getFullTextStatesCopy();
        int totalCols = previewView.getTotalCols();
        int displayCols = previewView.getCols();
        int rows = fullStates.length;
        int frameCount = totalCols + displayCols;
        List<Bitmap> gifFrames = new ArrayList<>();

        // 生成所有帧
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

        // 保存到APP内部目录
        File gifDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "marquees");
        if (!gifDir.exists()) gifDir.mkdirs();
        File gifFile = new File(gifDir, saveName + ".gif");

        try (OutputStream gifOut = new FileOutputStream(gifFile)) {
            AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
            gifEncoder.start(gifOut);
            gifEncoder.setRepeat(0);
            gifEncoder.setDelay(delayMs);
            for (Bitmap frameBmp : gifFrames) {
                gifEncoder.addFrame(frameBmp);
                frameBmp.recycle();
            }
            gifEncoder.finish();
        }

        return new GifResult(gifFile.getAbsolutePath(), framesArray.toString());
    }

    /**
     * 保存跑马灯记录到数据库
     */
    public static void saveMarqueeRecord(Context context,
                                         String saveName,
                                         GifResult result,
                                         int delayMs) {
        AppDatabase db = AppDatabase.getInstance(context);
        MarqueeDao dao = db.marqueeDao();
        dao.insert(new MarqueeEntity(
                saveName,
                "marquee",
                result.framesJson,
                delayMs,
                result.gifPath
        ));
    }
}
