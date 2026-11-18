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

  @Query("SELECT * FROM schemes_of_work")
  suspend fun getAll(): List<SchemeEntity>

  @Query("UPDATE schemes_of_work SET synced = 1 WHERE id = :id")
  suspend fun markSynced(id: String)

  @Query("DELETE FROM schemes_of_work WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM schemes_of_work")
  suspend fun deleteAll()
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

  @Query("SELECT * FROM lesson_plans")
  suspend fun getAll(): List<LessonPlanEntity>

  @Query("UPDATE lesson_plans SET synced = 1, remoteId = :remoteId WHERE id = :id")
  suspend fun markSynced(id: String, remoteId: String)

  @Query("DELETE FROM lesson_plans WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM lesson_plans")
  suspend fun deleteAll()
}

@Dao
interface LessonPlanTemplateDao {
  @Query("SELECT * FROM lesson_plan_templates WHERE id = 1")
  fun observe(): Flow<LessonPlanTemplateEntity?>

  @Query("SELECT * FROM lesson_plan_templates WHERE id = 1")
  suspend fun get(): LessonPlanTemplateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: LessonPlanTemplateEntity)

  @Query("DELETE FROM lesson_plan_templates")
  suspend fun deleteAll()
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

  @Query("SELECT * FROM records_of_work")
  suspend fun getAll(): List<RecordOfWorkEntity>

  @Query("UPDATE records_of_work SET synced = 1, remoteId = :remoteId WHERE id = :id")
  suspend fun markSynced(id: String, remoteId: String)

  @Query("DELETE FROM records_of_work WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM records_of_work")
  suspend fun deleteAll()
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

  @Query("DELETE FROM learners WHERE stream = :stream")
  suspend fun deleteForStream(stream: String)

  @Query("DELETE FROM learners")
  suspend fun deleteAll()

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

  @Query("DELETE FROM attendance_records WHERE stream = :stream AND date = :date")
  suspend fun deleteForStreamDay(stream: String, date: String)

  @Query("DELETE FROM attendance_records")
  suspend fun deleteAll()
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

  @Query("DELETE FROM mark_entries WHERE stream = :stream")
  suspend fun deleteForStream(stream: String)

  @Query("DELETE FROM mark_entries")
  suspend fun deleteAll()
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

  @Query("DELETE FROM observation_notes")
  suspend fun deleteAll()
}

@Dao
interface LearnerProgressDao {
  @Query("SELECT * FROM learner_progress ORDER BY date DESC")
  fun observeAll(): Flow<List<LearnerProgressEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: LearnerProgressEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<LearnerProgressEntity>)

  @Query("DELETE FROM learner_progress WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM learner_progress")
  suspend fun deleteAll()
}

@Dao
interface GuardianMessageDao {
  @Query("SELECT * FROM guardian_messages ORDER BY date DESC")
  fun observeAll(): Flow<List<GuardianMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: GuardianMessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<GuardianMessageEntity>)

  @Query("DELETE FROM guardian_messages")
  suspend fun deleteAll()
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

  @Query("DELETE FROM timetable_slots")
  suspend fun deleteAll()

  @Query("SELECT COUNT(*) FROM timetable_slots")
  suspend fun count(): Int
}
