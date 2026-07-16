package com.pdnp.dailydigest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: CapturedMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<CapturedMessage>)

    @Query("SELECT * FROM messages WHERE dayKey = :dayKey ORDER BY timestamp DESC")
    fun byDay(dayKey: String): Flow<List<CapturedMessage>>

    @Query("SELECT * FROM messages WHERE dayKey = :dayKey ORDER BY timestamp ASC")
    suspend fun byDayOnce(dayKey: String): List<CapturedMessage>
}

@Dao
interface CallDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<CallEntry>)

    @Query("SELECT * FROM calls WHERE dayKey = :dayKey ORDER BY timestamp DESC")
    fun byDay(dayKey: String): Flow<List<CallEntry>>

    @Query("SELECT * FROM calls WHERE dayKey = :dayKey ORDER BY timestamp ASC")
    suspend fun byDayOnce(dayKey: String): List<CallEntry>
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, priority DESC, createdAt DESC")
    fun all(): Flow<List<TaskItem>>

    @Insert
    suspend fun insert(task: TaskItem): Long

    @Update
    suspend fun update(task: TaskItem)

    @Delete
    suspend fun delete(task: TaskItem)
}

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meetings ORDER BY startedAt DESC")
    fun all(): Flow<List<Meeting>>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun byId(id: Long): Meeting?

    @Insert
    suspend fun insert(meeting: Meeting): Long

    @Update
    suspend fun update(meeting: Meeting)

    @Delete
    suspend fun delete(meeting: Meeting)
}

@Dao
interface SummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DaySummary)

    @Query("SELECT * FROM day_summaries WHERE dayKey = :dayKey")
    fun byDay(dayKey: String): Flow<DaySummary?>
}
