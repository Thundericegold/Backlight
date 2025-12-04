package com.example.backlight.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MarqueeDao {

    @Query("SELECT * FROM marquee_table")
    List<MarqueeEntity> getAll();

    @Query("SELECT * FROM marquee_table WHERE name = :name LIMIT 1")
    MarqueeEntity findByName(String name);

    @Insert
    void insert(MarqueeEntity entity);

    @Update
    void update(MarqueeEntity entity);

    @Delete
    void delete(MarqueeEntity entity);

    @Query("DELETE FROM marquee_table WHERE id = :id")
    void deleteById(int id);
}
