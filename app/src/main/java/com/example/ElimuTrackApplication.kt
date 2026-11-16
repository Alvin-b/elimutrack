package com.example

import android.app.Application
import com.example.data.local.ElimuTrackDatabase
import com.example.data.local.SessionPreferences
import com.example.data.repository.TeacherRepository
import com.example.pdf.PdfExportManager

class ElimuTrackApplication : Application() {
  lateinit var database: ElimuTrackDatabase
    private set

  lateinit var sessionPreferences: SessionPreferences
    private set

  lateinit var repository: TeacherRepository
    private set

  lateinit var pdfExportManager: PdfExportManager
    private set

  override fun onCreate() {
    super.onCreate()
    database = ElimuTrackDatabase.getInstance(this)
    sessionPreferences = SessionPreferences(this)
    repository = TeacherRepository(database, sessionPreferences)
    pdfExportManager = PdfExportManager(this)
  }
}
