package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    SchemeEntity::class,
    LessonPlanEntity::class,
    RecordOfWorkEntity::class,
    LearnerEntity::class,
    AttendanceEntity::class,
    MarkEntryEntity::class,
    ObservationEntity::class,
    LearnerProgressEntity::class,
    GuardianMessageEntity::class,
    TimetableSlotEntity::class,
  ],
  version = 3,
  exportSchema = false,
)
abstract class ElimuTrackDatabase : RoomDatabase() {
  abstract fun schemeDao(): SchemeDao
  abstract fun lessonPlanDao(): LessonPlanDao
  abstract fun recordOfWorkDao(): RecordOfWorkDao
  abstract fun learnerDao(): LearnerDao
  abstract fun attendanceDao(): AttendanceDao
  abstract fun markEntryDao(): MarkEntryDao
  abstract fun observationDao(): ObservationDao
  abstract fun learnerProgressDao(): LearnerProgressDao
  abstract fun guardianMessageDao(): GuardianMessageDao
  abstract fun timetableDao(): TimetableDao

  companion object {
    @Volatile
    private var instance: ElimuTrackDatabase? = null

    fun getInstance(context: Context): ElimuTrackDatabase =
      instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          ElimuTrackDatabase::class.java,
          "elimutrack_teacher.db",
        ).fallbackToDestructiveMigration(true)
          .build()
          .also { instance = it }
      }
  }
}
