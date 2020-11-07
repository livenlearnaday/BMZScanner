package io.github.livenlearnaday.bmzscanner.data.source.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import java.util.List;

import io.github.livenlearnaday.bmzscanner.data.entity.CodeDetail;

@Dao
public interface CodeDetailDao {

    @Insert
    void insert(CodeDetail codeDetail);

    @Update
    void update(CodeDetail codeDetail);

    @Delete
    void delete(CodeDetail codeDetail);

    @Query("DELETE FROM codes")
    void deleteAllCodeDetail();

    @Query("SELECT * FROM codes")
    LiveData<List<CodeDetail>> getAllCodeDetail();


}
