package com.example.fundoonotes.data.repository.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fundoonotes.data.repository.room.entity.LabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels")
    fun getAllLabels(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE id = :labelId")
    suspend fun getLabelById(labelId: String): LabelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelEntity): Long

    @Update
    suspend fun updateLabel(label: LabelEntity)

    @Query("UPDATE labels SET name = :labelName WHERE id = :labelId")
    suspend fun updateLabelName(labelId: String, labelName: String)

    @Query("UPDATE labels SET noteIds = :noteIds WHERE id = :labelId")
    suspend fun updateLabelNoteIds(labelId: String, noteIds: List<String>)

    @Query("DELETE FROM labels WHERE id = :labelId")
    suspend fun deleteLabel(labelId: String)

    @Query("SELECT id FROM labels ORDER BY id DESC LIMIT 1")
    suspend fun getLastInsertedId(): String?

    @Query("DELETE FROM labels")
    suspend fun clearAllLabels()
}