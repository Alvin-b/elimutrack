package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SchemeDao {
  @Query("SELECT * FROM schemes_of_work ORDER BY updatedAt DESC")
  fun observeAll(): Flow<List<SchemeEntity>>

  @Query("SELECT * FROM schemes_of_work WHERE synced = 0")
  suspend fun getUnsynced(): List<SchemeEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: SchemeEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<SchemeEntity>)

  @Query("DELETE FROM schemes_of_work WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface LessonPlanDao {
  @Query("SELECT * FROM lesson_plans ORDER BY updatedAt DESC")
  fun observeAll(): Flow<List<LessonPlanEntity>>

  @Query("SELECT * FROM lesson_plans WHERE synced = 0")
  suspend fun getUnsynced(): List<LessonPlanEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: LessonPlanEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<LessonPlanEntity>)

  @Query("DELETE FROM lesson_plans WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface RecordOfWorkDao {
  @Query("SELECT * FROM records_of_work ORDER BY updatedAt DESC")
  fun observeAll(): Flow<List<RecordOfWorkEntity>>

  @Query("SELECT * FROM records_of_work WHERE synced = 0")
  suspend fun getUnsynced(): List<RecordOfWorkEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: RecordOfWorkEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<RecordOfWorkEntity>)

  @Query("DELETE FROM records_of_work WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface LearnerDao {
  @Query("SELECT * FROM learners ORDER BY grade ASC, fullName ASC")
  fun observeAll(): Flow<List<LearnerEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: LearnerEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<LearnerEntity>)

  @Query("UPDATE learners SET feeBalanceKes = :newBalance WHERE id = :id")
  suspend fun updateFeeBalance(id: String, newBalance: Double)

  @Query("DELETE FROM learners WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("SELECT COUNT(*) FROM learners")
  suspend fun count(): Int
}

@Dao
interface AttendanceDao {
  @Query("SELECT * FROM attendance_records ORDER BY date DESC, learnerName ASC")
  fun observeAll(): Flow<List<AttendanceEntity>>

  @Query("SELECT * FROM attendance_records WHERE synced = 0")
  suspend fun getUnsynced(): List<AttendanceEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<AttendanceEntity>)
}

@Dao
interface MarkEntryDao {
  @Query("SELECT * FROM mark_entries ORDER BY updatedAt DESC")
  fun observeAll(): Flow<List<MarkEntryEntity>>

  @Query("SELECT * FROM mark_entries WHERE synced = 0")
  suspend fun getUnsynced(): List<MarkEntryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: MarkEntryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<MarkEntryEntity>)
}

@Dao
interface ObservationDao {
  @Query("SELECT * FROM observation_notes ORDER BY date DESC")
  fun observeAll(): Flow<List<ObservationEntity>>

  @Query("SELECT * FROM observation_notes WHERE synced = 0")
  suspend fun getUnsynced(): List<ObservationEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: ObservationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<ObservationEntity>)

  @Query("DELETE FROM observation_notes WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface LearnerProgressDao {
  @Query("SELECT * FROM learner_progress ORDER BY date DESC")
  fun observeAll(): Flow<List<LearnerProgressEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: LearnerProgressEntity)
}

@Dao
interface GuardianMessageDao {
  @Query("SELECT * FROM guardian_messages ORDER BY date DESC")
  fun observeAll(): Flow<List<GuardianMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: GuardianMessageEntity)
}

@Dao
interface TimetableDao {
  @Query("SELECT * FROM timetable_slots ORDER BY grade ASC, dayOfWeek ASC, slotIndex ASC")
  fun observeAll(): Flow<List<TimetableSlotEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: TimetableSlotEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<TimetableSlotEntity>)

  @Query("SELECT COUNT(*) FROM timetable_slots WHERE grade = :grade")
  suspend fun countForGrade(grade: String): Int

  @Query("DELETE FROM timetable_slots WHERE grade = :grade")
  suspend fun deleteForGrade(grade: String)

  @Query("SELECT COUNT(*) FROM timetable_slots")
  suspend fun count(): Int
}
