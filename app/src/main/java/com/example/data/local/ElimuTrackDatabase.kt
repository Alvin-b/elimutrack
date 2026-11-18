package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    SchemeEntity::class,
    LessonPlanEntity::class,
    LessonPlanTemplateEntity::class,
    RecordOfWorkEntity::class,
    LearnerEntity::class,
    AttendanceEntity::class,
    MarkEntryEntity::class,
    ObservationEntity::class,
    LearnerProgressEntity::class,
    GuardianMessageEntity::class,
    TimetableSlotEntity::class,
  ],
  version = 5,
  exportSchema = false,
)
abstract class ElimuTrackDatabase : RoomDatabase() {
  abstract fun schemeDao(): SchemeDao
  abstract fun lessonPlanDao(): LessonPlanDao
  abstract fun lessonPlanTemplateDao(): LessonPlanTemplateDao
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
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
          .fallbackToDestructiveMigration(true)
          .build()
          .also { instance = it }
      }

    /** Adds the links that let offline schemes flow into lesson plans and records. */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lesson_plans ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE lesson_plans ADD COLUMN schemeId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE lesson_plans ADD COLUMN schemeLocalId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records_of_work ADD COLUMN topic TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records_of_work ADD COLUMN lessonNumber INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE records_of_work ADD COLUMN classSize INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE records_of_work ADD COLUMN schemeId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records_of_work ADD COLUMN lessonPlanId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records_of_work ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records_of_work ADD COLUMN schemeLocalId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE records_of_work ADD COLUMN lessonPlanLocalId TEXT NOT NULL DEFAULT ''")
      }
    }

    /** Keeps each scheme tied to its class and caches the school's plan template. */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "schemes_of_work", "stream", "TEXT NOT NULL DEFAULT ''")
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS lesson_plan_templates (" +
            "id INTEGER NOT NULL PRIMARY KEY, " +
            "name TEXT NOT NULL, " +
            "sectionsJson TEXT NOT NULL, " +
            "updatedAt INTEGER NOT NULL)"
        )
        // The upstream version 3 schema already had some plan/record links,
        // so only add the columns that are genuinely missing.
        addColumnIfMissing(db, "lesson_plans", "remoteId", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "lesson_plans", "schemeId", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "lesson_plans", "schemeLocalId", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "records_of_work", "remoteId", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "records_of_work", "schemeLocalId", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "records_of_work", "lessonPlanLocalId", "TEXT NOT NULL DEFAULT ''")
      }
    }

    /** Preserves answers for school-defined lesson-plan sections. */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "lesson_plans", "extraContentJson", "TEXT NOT NULL DEFAULT ''")
      }
    }

    private fun addColumnIfMissing(
      db: SupportSQLiteDatabase,
      table: String,
      column: String,
      definition: String,
    ) {
      val present = db.query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
          .any { it.equals(column, ignoreCase = true) }
      }
      if (!present) db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
    }
  }
}
