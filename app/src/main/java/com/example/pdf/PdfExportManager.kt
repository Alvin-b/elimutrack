package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.domain.model.AttendanceRecord
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CbcPerformanceLevel
import com.example.domain.model.Learner
import com.example.domain.model.LessonPlan
import com.example.domain.model.MarkEntry
import com.example.domain.model.ObservationNote
import com.example.domain.model.RecordOfWork
import com.example.domain.model.SchemeOfWork
import com.example.domain.model.TimetableSlot
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportedPdf(
  val file: File,
  val uri: Uri,
  val title: String,
)

class PdfExportManager(private val context: Context) {

  private val pageWidth = 595 // A4 portrait points
  private val pageHeight = 842
  private val margin = 36f
  private val contentWidth = pageWidth - margin * 2

  private val brandPaint = Paint().apply {
    color = Color.parseColor("#4F46E5")
    style = Paint.Style.FILL
  }
  private val headerBoxPaint = Paint().apply {
    color = Color.parseColor("#EEF2FF")
    style = Paint.Style.FILL
  }
  private val peachBannerPaint = Paint().apply {
    color = Color.parseColor("#FAD2A7")
    style = Paint.Style.FILL
  }
  private val blueHeaderPaint = Paint().apply {
    color = Color.parseColor("#C8DBFA")
    style = Paint.Style.FILL
  }
  private val creamCellPaint = Paint().apply {
    color = Color.parseColor("#FEF7E0")
    style = Paint.Style.FILL
  }
  private val borderPaint = Paint().apply {
    color = Color.parseColor("#94A3B8")
    style = Paint.Style.STROKE
    strokeWidth = 0.8f
  }
  private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    textSize = 15f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.parseColor("#E0E7FF")
    textSize = 9.5f
  }
  private val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.parseColor("#1E1B4B")
    textSize = 11.5f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.parseColor("#0F172A")
    textSize = 9.5f
  }
  private val boldBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.parseColor("#0F172A")
    textSize = 9.5f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val smallMutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.parseColor("#475569")
    textSize = 8f
  }

  private fun exportDir(): File = File(context.filesDir, "exports").apply { mkdirs() }

  private fun finalizePdf(doc: PdfDocument, fileName: String, title: String): ExportedPdf {
    val outFile = File(exportDir(), fileName)
    FileOutputStream(outFile).use { stream ->
      doc.writeTo(stream)
    }
    doc.close()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    return ExportedPdf(file = outFile, uri = uri, title = title)
  }

  private fun drawDocumentHeader(
    canvas: Canvas,
    schoolName: String,
    docTitle: String,
    metaLine1: String,
    metaLine2: String,
  ): Float {
    canvas.drawRect(margin, margin, pageWidth - margin, margin + 52f, brandPaint)
    canvas.drawText(schoolName.uppercase(Locale.getDefault()), margin + 12f, margin + 20f, titlePaint)
    canvas.drawText("ELIMUTRACK CBC ERP 2026 • KICD CURRICULUM ALIGNED", margin + 12f, margin + 36f, subtitlePaint)

    var y = margin + 62f
    canvas.drawRect(margin, y, pageWidth - margin, y + 48f, headerBoxPaint)
    canvas.drawRect(margin, y, pageWidth - margin, y + 48f, borderPaint)
    canvas.drawText(docTitle, margin + 10f, y + 16f, sectionPaint)
    canvas.drawText(metaLine1, margin + 10f, y + 30f, bodyPaint)
    canvas.drawText(metaLine2, margin + 10f, y + 42f, smallMutedPaint)
    y += 58f
    return y
  }

  private fun drawFooter(canvas: Canvas, pageNum: Int) {
    val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
    canvas.drawText("Generated via ElimuTrack Teacher App • $stamp", margin, pageHeight - 20f, smallMutedPaint)
    canvas.drawText("Page $pageNum", pageWidth - margin - 40f, pageHeight - 20f, smallMutedPaint)
  }

  private fun measureWrappedHeight(text: String, paint: TextPaint, width: Int): Int {
    val safeText = text.ifBlank { "-" }
    val layout = StaticLayout.Builder.obtain(safeText, 0, safeText.length, paint, width.coerceAtLeast(20))
      .setAlignment(Layout.Alignment.ALIGN_NORMAL)
      .setLineSpacing(1.5f, 1.0f)
      .build()
    return layout.height
  }

  private fun drawWrappedText(canvas: Canvas, text: String, paint: TextPaint, x: Float, y: Float, width: Int): Float {
    val safeText = text.ifBlank { "-" }
    val layout = StaticLayout.Builder.obtain(safeText, 0, safeText.length, paint, width.coerceAtLeast(20))
      .setAlignment(Layout.Alignment.ALIGN_NORMAL)
      .setLineSpacing(1.5f, 1.0f)
      .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
    return y + layout.height
  }

  fun exportSchemeOfWork(scheme: SchemeOfWork): ExportedPdf {
    val doc = PdfDocument()
    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
    var canvas = page.canvas
    var y = drawDocumentHeader(
      canvas = canvas,
      schoolName = scheme.schoolName,
      docTitle = "KICD SCHEME OF WORK — ${scheme.learningArea.uppercase(Locale.getDefault())}",
      metaLine1 = "Grade: ${scheme.grade}   |   Term: ${scheme.term}   |   Year: ${scheme.year}",
      metaLine2 = "Teacher: ${scheme.teacherName}   |   Title: ${scheme.title}",
    )

    val cols = floatArrayOf(34f, 92f, 115f, 90f, 112f, 80f) // total 523 = contentWidth
    val headers = listOf("WK/LSN", "STRAND & SUB-STRAND", "SPECIFIC LEARNING OUTCOMES", "KEY INQUIRY Q.", "LEARNING EXPERIENCES & RESOURCES", "ASSESSMENT")

    fun drawTableHeader(c: Canvas, topY: Float): Float {
      var x = margin
      val h = 24f
      c.drawRect(margin, topY, pageWidth - margin, topY + h, blueHeaderPaint)
      headers.forEachIndexed { idx, label ->
        c.drawRect(x, topY, x + cols[idx], topY + h, borderPaint)
        drawWrappedText(c, label, boldBodyPaint, x + 3f, topY + 4f, cols[idx].toInt() - 6)
        x += cols[idx]
      }
      return topY + h
    }

    y = drawTableHeader(canvas, y)

    scheme.rows.forEach { row ->
      val cells = listOf(
        "Wk ${row.week}\nLsn ${row.lesson}",
        "${row.strand}\n${row.subStrand}",
        row.specificOutcomes,
        row.keyInquiryQuestion,
        "${row.learningExperiences}\nResources: ${row.learningResources}",
        "${row.assessmentMethods}\n${row.reflection}",
      )
      val maxH = cells.mapIndexed { idx, text ->
        measureWrappedHeight(text, bodyPaint, cols[idx].toInt() - 6) + 10
      }.maxOrNull()?.toFloat() ?: 36f

      if (y + maxH > pageHeight - 48f) {
        drawFooter(canvas, pageNum)
        doc.finishPage(page)
        pageNum++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        canvas = page.canvas
        y = margin
        y = drawTableHeader(canvas, y)
      }

      var x = margin
      cells.forEachIndexed { idx, text ->
        canvas.drawRect(x, y, x + cols[idx], y + maxH, borderPaint)
        drawWrappedText(canvas, text, bodyPaint, x + 3f, y + 4f, cols[idx].toInt() - 6)
        x += cols[idx]
      }
      y += maxH
    }

    drawFooter(canvas, pageNum)
    doc.finishPage(page)
    val safeName = "Scheme_${scheme.grade}_${scheme.learningArea}_${scheme.id.takeLast(6)}.pdf".replace(" ", "_")
    return finalizePdf(doc, safeName, scheme.title)
  }

  fun exportLessonPlan(plan: LessonPlan): ExportedPdf {
    val doc = PdfDocument()
    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
    var canvas = page.canvas

    var y = drawDocumentHeader(
      canvas = canvas,
      schoolName = plan.schoolName,
      docTitle = "KICD CBC LESSON PLAN — ${plan.learningArea.uppercase(Locale.getDefault())}",
      metaLine1 = "Grade: ${plan.grade} ${plan.stream}   |   Term: ${plan.term}   |   Week ${plan.week}, Lesson ${plan.lessonNumber}",
      metaLine2 = "Date: ${plan.date}   |   Time: ${plan.timeSlot}   |   Roll: ${plan.rollCount}   |   Teacher: ${plan.teacherName}",
    )

    val sections = listOf(
      "STRAND & SUB-STRAND" to "${plan.strand} — ${plan.subStrand}",
      "SPECIFIC LEARNING OUTCOMES" to plan.specificOutcomes,
      "KEY INQUIRY QUESTIONS" to plan.keyInquiryQuestions,
      "CORE COMPETENCIES" to plan.coreCompetencies,
      "VALUES & PCIs" to "Values: ${plan.values}\nPCIs: ${plan.pcis}",
      "LEARNING RESOURCES" to plan.learningResources,
      "1. INTRODUCTION (5 mins)" to plan.introduction,
      "2. LESSON DEVELOPMENT (25 mins)" to plan.lessonDevelopment,
      "3. CONCLUSION & ASSESSMENT (5 mins)" to plan.conclusion,
      "EXTENDED ACTIVITY" to plan.extendedActivity,
      "TEACHER SELF-EVALUATION / REFLECTION" to plan.reflection,
    )

    sections.forEach { (heading, body) ->
      val bodyH = measureWrappedHeight(body, bodyPaint, contentWidth.toInt() - 16)
      val boxH = bodyH + 26f
      if (y + boxH > pageHeight - 48f) {
        drawFooter(canvas, pageNum)
        doc.finishPage(page)
        pageNum++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        canvas = page.canvas
        y = margin
      }
      canvas.drawRect(margin, y, pageWidth - margin, y + 18f, blueHeaderPaint)
      canvas.drawRect(margin, y, pageWidth - margin, y + boxH, borderPaint)
      canvas.drawText(heading, margin + 8f, y + 12f, boldBodyPaint)
      drawWrappedText(canvas, body, bodyPaint, margin + 8f, y + 22f, contentWidth.toInt() - 16)
      y += boxH + 6f
    }

    drawFooter(canvas, pageNum)
    doc.finishPage(page)
    val safeName = "LessonPlan_${plan.grade}_Wk${plan.week}_L${plan.lessonNumber}.pdf".replace(" ", "_")
    return finalizePdf(doc, safeName, "${plan.learningArea} Lesson Plan")
  }

  fun exportRecordOfWork(records: List<RecordOfWork>, schoolName: String, teacherName: String, grade: String, learningArea: String, term: String): ExportedPdf {
    val doc = PdfDocument()
    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
    var canvas = page.canvas

    var y = drawDocumentHeader(
      canvas = canvas,
      schoolName = schoolName,
      docTitle = "RECORD OF WORK COVERED — ${learningArea.uppercase(Locale.getDefault())}",
      metaLine1 = "Grade: $grade   |   Term: $term   |   Teacher: $teacherName",
      metaLine2 = "Total Entries: ${records.size}",
    )

    val cols = floatArrayOf(58f, 115f, 185f, 120f, 45f)
    val headers = listOf("WK / DATE", "STRAND / SUB-STRAND", "WORK ACTUALLY COVERED", "REMARKS / CHALLENGES", "SIGN")

    fun drawHeader(c: Canvas, topY: Float): Float {
      var x = margin
      val h = 22f
      c.drawRect(margin, topY, pageWidth - margin, topY + h, blueHeaderPaint)
      headers.forEachIndexed { idx, label ->
        c.drawRect(x, topY, x + cols[idx], topY + h, borderPaint)
        drawWrappedText(c, label, boldBodyPaint, x + 3f, topY + 4f, cols[idx].toInt() - 6)
        x += cols[idx]
      }
      return topY + h
    }

    y = drawHeader(canvas, y)
    records.forEach { rec ->
      val cells = listOf(
        "Wk ${rec.week}\n${rec.date}",
        "${rec.strand}\n${rec.subStrand}",
        rec.workCovered,
        rec.challengesRemarks,
        rec.teacherInitials,
      )
      val maxH = cells.mapIndexed { idx, txt ->
        measureWrappedHeight(txt, bodyPaint, cols[idx].toInt() - 6) + 10
      }.maxOrNull()?.toFloat() ?: 30f

      if (y + maxH > pageHeight - 48f) {
        drawFooter(canvas, pageNum)
        doc.finishPage(page)
        pageNum++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        canvas = page.canvas
        y = drawHeader(canvas, margin)
      }

      var x = margin
      cells.forEachIndexed { idx, txt ->
        canvas.drawRect(x, y, x + cols[idx], y + maxH, borderPaint)
        drawWrappedText(canvas, txt, bodyPaint, x + 3f, y + 4f, cols[idx].toInt() - 6)
        x += cols[idx]
      }
      y += maxH
    }

    drawFooter(canvas, pageNum)
    doc.finishPage(page)
    val safeName = "RecordOfWork_${grade}_${learningArea}.pdf".replace(" ", "_")
    return finalizePdf(doc, safeName, "Record of Work — $learningArea")
  }

  fun exportStudentProgressReport(
    schoolName: String,
    teacherName: String,
    term: String,
    year: Int,
    learner: Learner,
    marks: List<MarkEntry>,
    attendance: List<AttendanceRecord>,
    observations: List<ObservationNote>,
    overallComment: String,
  ): ExportedPdf {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
    val canvas = page.canvas

    val presentCount = attendance.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
    val attendancePct = if (attendance.isEmpty()) "N/A" else "${(presentCount * 100) / attendance.size}% ($presentCount/${attendance.size})"

    var y = drawDocumentHeader(
      canvas = canvas,
      schoolName = schoolName,
      docTitle = "CBC LEARNER PROGRESS & REPORT CARD",
      metaLine1 = "Learner: ${learner.fullName}   |   Adm No: ${learner.admissionNo}   |   Class: ${learner.grade} ${learner.stream}",
      metaLine2 = "Term: $term $year   |   Attendance: $attendancePct   |   Class Teacher: $teacherName",
    )

    // Marks Table
    canvas.drawText("LEARNING AREA PERFORMANCE (KICD RUBRICS)", margin, y + 10f, sectionPaint)
    y += 16f
    val cols = floatArrayOf(165f, 80f, 65f, 65f, 148f)
    val headers = listOf("LEARNING AREA", "ASSESSMENT", "SCORE", "CBC LEVEL", "TEACHER REMARKS")
    var x = margin
    canvas.drawRect(margin, y, pageWidth - margin, y + 22f, blueHeaderPaint)
    headers.forEachIndexed { idx, h ->
      canvas.drawRect(x, y, x + cols[idx], y + 22f, borderPaint)
      drawWrappedText(canvas, h, boldBodyPaint, x + 4f, y + 4f, cols[idx].toInt() - 8)
      x += cols[idx]
    }
    y += 22f

    if (marks.isEmpty()) {
      canvas.drawRect(margin, y, pageWidth - margin, y + 24f, borderPaint)
      canvas.drawText("No marks recorded for this term yet.", margin + 8f, y + 15f, bodyPaint)
      y += 28f
    } else {
      marks.forEach { m ->
        val rowCells = listOf(
          m.learningArea,
          m.assessmentType,
          "${m.score.toInt()}/${m.maxScore.toInt()}",
          "${m.level.code} (${m.level.title})",
          m.teacherComment.ifBlank { m.level.title },
        )
        val rowH = rowCells.mapIndexed { idx, txt ->
          measureWrappedHeight(txt, bodyPaint, cols[idx].toInt() - 8) + 8
        }.maxOrNull()?.toFloat() ?: 24f

        var cx = margin
        rowCells.forEachIndexed { idx, txt ->
          canvas.drawRect(cx, y, cx + cols[idx], y + rowH, borderPaint)
          drawWrappedText(canvas, txt, bodyPaint, cx + 4f, y + 4f, cols[idx].toInt() - 8)
          cx += cols[idx]
        }
        y += rowH
      }
      y += 10f
    }

    // Core Competency Observations
    canvas.drawText("CORE COMPETENCIES & OBSERVATIONS", margin, y + 10f, sectionPaint)
    y += 16f
    if (observations.isEmpty()) {
      canvas.drawRect(margin, y, pageWidth - margin, y + 24f, borderPaint)
      canvas.drawText("No competency observations logged yet.", margin + 8f, y + 15f, bodyPaint)
      y += 30f
    } else {
      observations.take(5).forEach { obs ->
        val txt = "• ${obs.competency} [${obs.rating.code}]: ${obs.note} (${obs.date})"
        val h = measureWrappedHeight(txt, bodyPaint, contentWidth.toInt() - 12) + 8
        canvas.drawRect(margin, y, pageWidth - margin, y + h, borderPaint)
        drawWrappedText(canvas, txt, bodyPaint, margin + 6f, y + 4f, contentWidth.toInt() - 12)
        y += h
      }
      y += 10f
    }

    // Rubric Legend
    canvas.drawText("CBC PERFORMANCE KEY:  EE (80-100%) Exceeding   |   ME (60-79%) Meeting   |   AE (40-59%) Approaching   |   BE (0-39%) Below", margin, y + 8f, smallMutedPaint)
    y += 18f

    // Overall Comment & Signatures
    val commentH = measureWrappedHeight(overallComment, bodyPaint, contentWidth.toInt() - 16) + 26f
    canvas.drawRect(margin, y, pageWidth - margin, y + 18f, peachBannerPaint)
    canvas.drawRect(margin, y, pageWidth - margin, y + commentH, borderPaint)
    canvas.drawText("CLASS TEACHER'S GENERAL COMMENT", margin + 8f, y + 12f, boldBodyPaint)
    drawWrappedText(canvas, overallComment, bodyPaint, margin + 8f, y + 22f, contentWidth.toInt() - 16)
    y += commentH + 24f

    canvas.drawText("Class Teacher Signature: _______________________", margin, y, bodyPaint)
    canvas.drawText("Headteacher Signature: _______________________", pageWidth - margin - 220f, y, bodyPaint)

    drawFooter(canvas, 1)
    doc.finishPage(page)
    val safeName = "ProgressReport_${learner.admissionNo}_${learner.fullName}.pdf".replace(" ", "_")
    return finalizePdf(doc, safeName, "${learner.fullName} Progress Report")
  }

  fun exportTimetable(
    schoolName: String,
    grade: String,
    term: String,
    year: Int,
    slots: List<TimetableSlot>,
  ): ExportedPdf {
    val lWidth = 842 // A4 landscape
    val lHeight = 595
    val lContentWidth = lWidth - margin * 2
    val timeSlots = com.example.data.curriculum.KicdCurriculumRepository.timeSlotsForGrade(grade)
    val formatTitle = com.example.data.curriculum.KicdCurriculumRepository.timetableFormatTitle(grade)

    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(lWidth, lHeight, 1).create())
    val canvas = page.canvas

    var y = margin
    canvas.drawRect(margin, y, lWidth - margin, y + 28f, peachBannerPaint)
    canvas.drawRect(margin, y, lWidth - margin, y + 28f, borderPaint)
    canvas.drawText(formatTitle, margin + 12f, y + 18f, sectionPaint)
    y += 34f
    canvas.drawText("SCHOOL: $schoolName     GRADE: $grade     TERM: $term     YEAR: $year", margin, y + 10f, boldBodyPaint)
    y += 22f

    val dayColW = 48f
    val slotColW = (lContentWidth - dayColW) / timeSlots.size.toFloat()

    var hx = margin
    canvas.drawRect(margin, y, lWidth - margin, y + 26f, blueHeaderPaint)
    canvas.drawRect(hx, y, hx + dayColW, y + 26f, borderPaint)
    drawWrappedText(canvas = canvas, text = "DAY", paint = smallMutedPaint, x = hx + 4f, y = y + 7f, width = dayColW.toInt() - 8)
    hx += dayColW

    timeSlots.forEach { (idx, range) ->
      val customTime = slots.firstOrNull { it.slotIndex == idx }?.timeRange ?: range
      canvas.drawRect(hx, y, hx + slotColW, y + 26f, borderPaint)
      drawWrappedText(canvas = canvas, text = customTime, paint = smallMutedPaint, x = hx + 3f, y = y + 6f, width = slotColW.toInt() - 6)
      hx += slotColW
    }
    y += 26f

    val breakCellPaint = Paint().apply {
      color = Color.parseColor("#D4DAE4")
      style = Paint.Style.FILL
    }
    val days = listOf("MON", "TUE", "WED", "THU", "FRI")
    val rowH = 68f
    days.forEach { day ->
      var rx = margin
      canvas.drawRect(rx, y, rx + dayColW, y + rowH, blueHeaderPaint)
      canvas.drawRect(rx, y, rx + dayColW, y + rowH, borderPaint)
      canvas.drawText(day, rx + 10f, y + 36f, boldBodyPaint)
      rx += dayColW

      val daySlots = slots.filter { it.dayOfWeek == day }.sortedBy { it.slotIndex }
      timeSlots.forEach { (i, _) ->
        val slot = daySlots.firstOrNull { it.slotIndex == i }
        val isBreak = slot?.isBreak ?: false
        canvas.drawRect(rx, y, rx + slotColW, y + rowH, if (isBreak) breakCellPaint else creamCellPaint)
        canvas.drawRect(rx, y, rx + slotColW, y + rowH, borderPaint)
        val label = slot?.learningArea ?: "-"
        drawWrappedText(canvas, label, smallMutedPaint, rx + 3f, y + 14f, slotColW.toInt() - 6)
        rx += slotColW
      }
      y += rowH
    }

    val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
    canvas.drawText("Generated via ElimuTrack Teacher App • $stamp", margin, lHeight - 20f, smallMutedPaint)
    doc.finishPage(page)
    val safeName = "Timetable_${grade}_${year}.pdf".replace(" ", "_")
    return finalizePdf(doc, safeName, "$grade Timetable")
  }

  fun openOrSharePdf(exported: ExportedPdf, shareOnly: Boolean = false) {
    val intent = if (shareOnly) {
      Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, exported.uri)
        putExtra(Intent.EXTRA_SUBJECT, exported.title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    } else {
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(exported.uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    }
    val chooser = Intent.createChooser(intent, exported.title).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }

  fun copyPdfToUri(sourceFile: File, targetUri: Uri) {
    context.contentResolver.openOutputStream(targetUri)?.use { out ->
      sourceFile.inputStream().use { input ->
        input.copyTo(out)
      }
    }
  }

  fun writeTextToUri(content: String, targetUri: Uri) {
    context.contentResolver.openOutputStream(targetUri)?.bufferedWriter()?.use { writer ->
      writer.write(content)
    }
  }
}
