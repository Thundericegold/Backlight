package com.example.backlight.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "marquee_table")
public class MarqueeEntity {

    @PrimaryKey(autoGenerate = true)
    public int id; // 自动生成唯一id

    public String name;       // 跑马灯名称
    public String mode;       // 模式，例如 "marquee"
    public String framesJson; // 帧数据（JSON字符串）
    public int speed;         // 播放速度（毫秒）

    public MarqueeEntity(String name, String mode, String framesJson, int speed) {
        this.name = name;
        this.mode = mode;
        this.framesJson = framesJson;
        this.speed = speed;
    }
}
