package com.example.backlight.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "marquee_table")
public class MarqueeEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String mode;
    public String framesJson;
    public int speed;
    public String gifPath;  // 新字段：GIF 文件在 APP 内部的路径

    public MarqueeEntity(String name, String mode, String framesJson, int speed, String gifPath) {
        this.name = name;
        this.mode = mode;
        this.framesJson = framesJson;
        this.speed = speed;
        this.gifPath = gifPath;
    }
}

