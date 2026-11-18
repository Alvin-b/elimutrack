package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.domain.model.LessonPlan
import com.example.domain.model.LessonPlanTemplateSection
import com.example.domain.model.RecordOfWork
import com.example.ui.components.HighlighterButton
import com.example.ui.components.SubScreenHeader
import com.example.ui.theme.LocalElimuColors
import com.example.ui.viewmodel.TeacherUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val defaultCbcSections = listOf(
  LessonPlanTemplateSection("sec1", "1. Specific learning outcomes"),
  LessonPlanTemplateSection("sec2", "2. Core competencies"),
  LessonPlanTemplateSection("sec3", "3. Key inquiry question"),
  LessonPlanTemplateSection("sec4", "4. Learning resources"),
  LessonPlanTemplateSection("sec5", "5. Organization of learning"),
  LessonPlanTemplateSection("sec6", "6. Introduction"),
  LessonPlanTemplateSection("sec7", "7. Lesson development"),
  LessonPlanTemplateSection("sec8", "8. Conclusion"),
  LessonPlanTemplateSection("sec9", "9. Extended activity"),
  LessonPlanTemplateSection("sec10", "10. Reflection"),
)

@Composable
fun LessonPlansScreen(
  state: TeacherUiState,
  onSavePlan: (plan: LessonPlan, isUpdate: Boolean, contentMap: Map<String, String>) -> Unit,
  onDeletePlan: (String) -> Unit,
  onExportPdf: (LessonPlan) -> Unit,
  onClearKicdPrefill: () -> Unit,
  onClearPlanPrefill: () -> Unit,
  onShowMessage: (String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val gradeSubjects = remember(state.selectedGrade) {
    KicdCurriculumRepository.subjectsForGrade(state.selectedGrade)
  }
  val activeStream = state.selectedStream.ifBlank {
    state.activeClass?.streamKey ?: state.selectedGrade
  }
  val sections = state.lessonPlanTemplate.ifEmpty { defaultCbcSections }

  var editingId by remember { mutableStateOf<String?>(null) }
  var learningArea by remember(state.selectedGrade) { mutableStateOf(gradeSubjects.firstOrNull()?.name ?: "Mathematics") }
  var lessonDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
  var topic by remember { mutableStateOf("") }
  var strand by remember { mutableStateOf("") }
  var hintNote by remember { mutableStateOf<String?>(null) }
  var showKicdPicker by remember { mutableStateOf(false) }

  val sectionValues = remember(sections) {
    mutableStateMapOf<String, String>().apply {
      sections.forEach { sec ->
        put(sec.key, when (sec.key) {
          "sec2", "competencies" -> "Communication and Collaboration, Critical Thinking"
          "sec5", "organization" -> "Pair & small group activities inside the classroom"
          "sec6", "introduction" -> "Review prior knowledge using oral warm-up questions."
          "sec8", "conclusion" -> "Learners summarize key takeaways and complete a quick check."
          "sec9", "extended" -> "Practice with family members at home."
          else -> ""
        })
      }
    }
  }

  fun setSectionByAliases(value: String, vararg keys: String) {
    val target = sections.firstOrNull { sec -> keys.any { k -> sec.key.equals(k, ignoreCase = true) } }?.key
      ?: keys.firstOrNull()
    if (target != null) sectionValues[target] = value
  }

  fun getSectionByAliases(vararg keys: String): String {
    for (k in keys) {
      sectionValues[k]?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return ""
  }

  LaunchedEffect(state.pendingKicdPrefill) {
    val d = state.pendingKicdPrefill ?: return@LaunchedEffect
    learningArea = d.learningArea
    strand = d.strand
    topic = d.subStrand
    setSectionByAliases(d.specificLearningOutcomes.joinToString("\n• ", prefix = "• "), "sec1", "outcomes", "specificOutcomes")
    setSectionByAliases(d.coreCompetencies.joinToString(", "), "sec2", "competencies", "coreCompetencies")
    setSectionByAliases(d.keyInquiryQuestions.joinToString("\n"), "sec3", "inquiry", "keyInquiryQuestions")
    setSectionByAliases(d.suggestedResources.joinToString(", "), "sec4", "resources", "learningResources")
    setSectionByAliases(d.learningExperiences.joinToString("\n"), "sec7", "development", "lessonDevelopment")
    hintNote = "Outcomes and inquiry questions filled from KICD design."
    onClearKicdPrefill()
  }

  LaunchedEffect(state.pendingPlanPrefill) {
    val pair = state.pendingPlanPrefill ?: return@LaunchedEffect
    topic = pair.first
    setSectionByAliases(pair.second, "sec1", "outcomes", "specificOutcomes")
    hintNote = "Topic and outcomes filled from your scheme row."
    onClearPlanPrefill()
  }

  val savedForClass = state.lessonPlans.filter { state.matchesActiveClass(it.stream, it.grade) }

  BackHandler(enabled = editingId != null) { editingId = null }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Lesson plans",
        subtitle = "${state.activeClass?.displayName ?: activeStream} · ${sections.size} CBC sections",
        onBackToServices = onBackToServices,
        action = {
          OutlinedButton(onClick = { showKicdPicker = true }) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("KICD Fill")
          }
        },
      )
    }

    if (!hintNote.isNullOrBlank()) {
      item {
        Surface(color = extra.highlighter.copy(alpha = 0.24f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
          Text(hintNote!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
        }
      }
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Learning area", style = MaterialTheme.typography.labelLarge)
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            gradeSubjects.forEach { subj ->
              val active = KicdCurriculumRepository.subjectIdForArea(state.selectedGrade, learningArea) == subj.id
              FilterChip(
                selected = active,
                onClick = { learningArea = subj.name },
                label = { Text(subj.name) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                  selectedLabelColor = MaterialTheme.colorScheme.background,
                ),
                shape = RoundedCornerShape(99.dp),
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = topic,
              onValueChange = { topic = it },
              label = { Text("Topic / Sub-strand") },
              modifier = Modifier.weight(1.4f).testTag("lesson_plan_topic_input"),
            )
            OutlinedTextField(
              value = lessonDate,
              onValueChange = { lessonDate = it },
              label = { Text("Date") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          sections.forEach { sec ->
            OutlinedTextField(
              value = sectionValues[sec.key] ?: "",
              onValueChange = { sectionValues[sec.key] = it },
              label = { Text(sec.label) },
              minLines = if (sec.key.contains("7") || sec.key.contains("dev", ignoreCase = true)) 3 else 2,
              modifier = Modifier.fillMaxWidth(),
            )
          }

          HighlighterButton(
            text = if (editingId == null) "Save plan" else "Update plan",
            onClick = {
              if (topic.isBlank()) {
                onShowMessage("Enter a Topic / Sub-strand before saving.")
                return@HighlighterButton
              }
              val contentMap = sections.associate { it.key to (sectionValues[it.key] ?: "") }
              val isUpdate = editingId != null
              onSavePlan(
                LessonPlan(
                  id = editingId ?: "lp_${UUID.randomUUID().toString().take(8)}",
                  schoolName = session.schoolName,
                  teacherName = session.fullName,
                  grade = state.selectedGrade,
                  stream = activeStream,
                  learningArea = learningArea,
                  term = session.currentTerm,
                  week = 1,
                  lessonNumber = savedForClass.size + 1,
                  date = lessonDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
                  timeSlot = "8:20 - 8:55",
                  rollCount = state.learners.count { state.matchesActiveClass(it.stream, it.grade) }.coerceAtLeast(1),
                  strand = strand.ifBlank { topic },
                  subStrand = topic,
                  topic = topic,
                  specificOutcomes = getSectionByAliases("sec1", "outcomes", "specificOutcomes"),
                  keyInquiryQuestions = getSectionByAliases("sec3", "inquiry", "keyInquiryQuestions"),
                  coreCompetencies = getSectionByAliases("sec2", "competencies", "coreCompetencies"),
                  values = "Responsibility, Unity",
                  pcis = "Life skills",
                  learningResources = getSectionByAliases("sec4", "resources", "learningResources"),
                  organizationOfLearning = getSectionByAliases("sec5", "organization"),
                  introduction = getSectionByAliases("sec6", "introduction", "intro"),
                  lessonDevelopment = getSectionByAliases("sec7", "development", "lessonDevelopment"),
                  conclusion = getSectionByAliases("sec8", "conclusion"),
                  extendedActivity = getSectionByAliases("sec9", "extended"),
                  reflection = getSectionByAliases("sec10", "reflection"),
                  updatedAt = System.currentTimeMillis(),
                  extraContent = contentMap,
                ),
                isUpdate,
                contentMap,
              )
              editingId = null
              topic = ""
              sections.forEach { sectionValues[it.key] = "" }
              hintNote = null
            },
            modifier = Modifier.fillMaxWidth().testTag("save_lesson_plan_button"),
          )
        }
      }
    }

    item {
      Text("Saved for ${state.activeClass?.displayName ?: activeStream}", style = MaterialTheme.typography.titleMedium)
    }

    if (savedForClass.isEmpty()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Nothing saved for this class yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = extra.muted,
            modifier = Modifier.padding(18.dp),
          )
        }
      }
    } else {
      items(savedForClass, key = { it.id }) { plan ->
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${plan.learningArea} · ${plan.topic.ifBlank { plan.subStrand }}", style = MaterialTheme.typography.titleSmall)
            Text(
              text = "${plan.date} · ${plan.specificOutcomes.ifBlank { plan.extraContent.values.firstOrNull { it.isNotBlank() } ?: "Lesson plan" }}",
              style = MaterialTheme.typography.bodySmall,
              color = extra.muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(onClick = {
                editingId = plan.id
                learningArea = plan.learningArea
                lessonDate = plan.date.ifBlank { lessonDate }
                topic = plan.topic.ifBlank { plan.subStrand }
                strand = plan.strand
                sections.forEach { sec ->
                  val fromExtra = plan.extraContent[sec.key]
                  if (!fromExtra.isNullOrBlank()) {
                    sectionValues[sec.key] = fromExtra
                  }
                }
                if (plan.extraContent.isEmpty()) {
                  setSectionByAliases(plan.specificOutcomes, "sec1", "outcomes")
                  setSectionByAliases(plan.coreCompetencies, "sec2", "competencies")
                  setSectionByAliases(plan.keyInquiryQuestions, "sec3", "inquiry")
                  setSectionByAliases(plan.learningResources, "sec4", "resources")
                  setSectionByAliases(plan.organizationOfLearning, "sec5", "organization")
                  setSectionByAliases(plan.introduction, "sec6", "introduction")
                  setSectionByAliases(plan.lessonDevelopment, "sec7", "development")
                  setSectionByAliases(plan.conclusion, "sec8", "conclusion")
                  setSectionByAliases(plan.extendedActivity, "sec9", "extended")
                  setSectionByAliases(plan.reflection, "sec10", "reflection")
                }
              }) { Text("Edit") }
              HighlighterButton(
                text = "Export PDF",
                onClick = { onExportPdf(plan) },
                icon = Icons.Filled.PictureAsPdf,
              )
              Spacer(Modifier.weight(1f))
              IconButton(onClick = { onDeletePlan(plan.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = extra.be)
              }
            }
          }
        }
      }
    }
  }

  if (showKicdPicker) {
    val pickerDesigns = (state.remoteKicdDesigns + KicdCurriculumRepository.kicdDesigns).distinctBy { it.id }
    AlertDialog(
      onDismissRequest = { showKicdPicker = false },
      title = { Text("Fill from KICD Design") },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          pickerDesigns.forEach { d ->
            Surface(
              color = MaterialTheme.colorScheme.background,
              shape = RoundedCornerShape(10.dp),
              onClick = {
                learningArea = d.learningArea
                strand = d.strand
                topic = d.subStrand
                setSectionByAliases(d.specificLearningOutcomes.joinToString("\n• ", prefix = "• "), "sec1", "outcomes", "specificOutcomes")
                setSectionByAliases(d.coreCompetencies.joinToString(", "), "sec2", "competencies", "coreCompetencies")
                setSectionByAliases(d.keyInquiryQuestions.joinToString("\n"), "sec3", "inquiry", "keyInquiryQuestions")
                setSectionByAliases(d.suggestedResources.joinToString(", "), "sec4", "resources", "learningResources")
                setSectionByAliases(d.learningExperiences.joinToString("\n"), "sec7", "development", "lessonDevelopment")
                showKicdPicker = false
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("${d.grade} · ${d.learningArea}", style = MaterialTheme.typography.labelMedium, color = extra.me)
                Text("${d.strand} — ${d.subStrand}", style = MaterialTheme.typography.titleSmall)
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { showKicdPicker = false }) { Text("Close") } },
    )
  }
}

@Composable
fun RecordsOfWorkScreen(
  state: TeacherUiState,
  onSaveRecord: (RecordOfWork, isUpdate: Boolean) -> Unit,
  onDeleteRecord: (String) -> Unit,
  onExportPdf: (grade: String, learningArea: String) -> Unit,
  onShowMessage: (String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val gradeSubjects = remember(state.selectedGrade) {
    KicdCurriculumRepository.subjectsForGrade(state.selectedGrade)
  }
  val activeStream = state.selectedStream.ifBlank {
    state.activeClass?.streamKey ?: state.selectedGrade
  }
  val classLearnersCount = state.learners.count { state.matchesActiveClass(it.stream, it.grade) }

  var editingId by remember { mutableStateOf<String?>(null) }
  var learningArea by remember(state.selectedGrade) { mutableStateOf(gradeSubjects.firstOrNull()?.name ?: "Mathematics") }
  var topic by remember { mutableStateOf("") }
  var weekText by remember { mutableStateOf("1") }
  var lessonText by remember { mutableStateOf("1") }
  var classSizeText by remember(classLearnersCount) { mutableStateOf(classLearnersCount.takeIf { it > 0 }?.toString() ?: "") }
  var workCovered by remember { mutableStateOf("") }
  var absencesToday by remember { mutableStateOf("") }
  var homework by remember { mutableStateOf("") }
  var remarks by remember { mutableStateOf("") }

  val savedForClass = state.recordsOfWork.filter { state.matchesActiveClass(it.stream, it.grade) }

  BackHandler(enabled = editingId != null) { editingId = null }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Record of work",
        subtitle = "${state.activeClass?.displayName ?: activeStream} · What was taught",
        onBackToServices = onBackToServices,
        action = {
          OutlinedButton(
            onClick = { onExportPdf(state.selectedGrade, "All") },
            modifier = Modifier.testTag("export_records_pdf_button"),
          ) {
            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("PDF")
          }
        },
      )
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Learning area", style = MaterialTheme.typography.labelLarge)
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            gradeSubjects.forEach { subj ->
              val active = KicdCurriculumRepository.subjectIdForArea(state.selectedGrade, learningArea) == subj.id
              FilterChip(
                selected = active,
                onClick = { learningArea = subj.name },
                label = { Text(subj.name) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                  selectedLabelColor = MaterialTheme.colorScheme.background,
                ),
                shape = RoundedCornerShape(99.dp),
              )
            }
          }
          OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = weekText,
              onValueChange = { weekText = it.filter { c -> c.isDigit() } },
              label = { Text("Week") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = lessonText,
              onValueChange = { lessonText = it.filter { c -> c.isDigit() } },
              label = { Text("Lesson") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = classSizeText,
              onValueChange = { classSizeText = it.filter { c -> c.isDigit() } },
              label = { Text("Class size") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1.2f),
            )
          }
          OutlinedTextField(value = workCovered, onValueChange = { workCovered = it }, label = { Text("Work covered") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = absencesToday, onValueChange = { absencesToday = it }, label = { Text("Absences today") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = homework, onValueChange = { homework = it }, label = { Text("Homework") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks") }, minLines = 2, modifier = Modifier.fillMaxWidth())

          HighlighterButton(
            text = if (editingId == null) "Save record" else "Update record",
            onClick = {
              if (topic.isBlank()) {
                onShowMessage("Enter a Topic before saving.")
                return@HighlighterButton
              }
              val isUpdate = editingId != null
              val initials = session.fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
              onSaveRecord(
                RecordOfWork(
                  id = editingId ?: "row_${UUID.randomUUID().toString().take(8)}",
                  schoolName = session.schoolName,
                  teacherName = session.fullName,
                  grade = state.selectedGrade,
                  stream = activeStream,
                  learningArea = learningArea,
                  term = session.currentTerm,
                  year = session.currentYear,
                  week = weekText.toIntOrNull() ?: 1,
                  date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                  strand = topic,
                  subStrand = topic,
                  workCovered = workCovered.ifBlank { "No details added" },
                  absencesToday = absencesToday,
                  homework = homework,
                  challengesRemarks = remarks,
                  teacherInitials = initials.ifBlank { "TR" },
                  updatedAt = System.currentTimeMillis(),
                  topic = topic,
                  lessonNumber = lessonText.toIntOrNull() ?: 1,
                  classSize = classSizeText.toIntOrNull() ?: classLearnersCount,
                ),
                isUpdate,
              )
              editingId = null
              topic = ""
              workCovered = ""
              absencesToday = ""
              homework = ""
              remarks = ""
            },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    item {
      Text("Saved for ${state.activeClass?.displayName ?: activeStream}", style = MaterialTheme.typography.titleMedium)
    }

    if (savedForClass.isEmpty()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Nothing saved for this class yet.", style = MaterialTheme.typography.bodyMedium, color = extra.muted, modifier = Modifier.padding(18.dp))
        }
      }
    } else {
      items(savedForClass, key = { it.id }) { rec ->
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${rec.learningArea} · ${rec.topic.ifBlank { rec.subStrand }}", style = MaterialTheme.typography.titleSmall)
                Text("Wk ${rec.week}, Lesson ${rec.lessonNumber} · ${rec.date}", style = MaterialTheme.typography.labelSmall, color = extra.me)
                Text(rec.workCovered, style = MaterialTheme.typography.bodySmall, color = extra.muted)
                if (rec.homework.isNotBlank()) {
                  Text("Homework: ${rec.homework}", style = MaterialTheme.typography.labelSmall)
                }
              }
              IconButton(onClick = { onDeleteRecord(rec.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = extra.be)
              }
            }
            OutlinedButton(onClick = {
              editingId = rec.id
              learningArea = rec.learningArea
              topic = rec.topic.ifBlank { rec.subStrand }
              weekText = rec.week.toString()
              lessonText = rec.lessonNumber.toString()
              classSizeText = rec.classSize.takeIf { it > 0 }?.toString() ?: ""
              workCovered = rec.workCovered
              absencesToday = rec.absencesToday
              homework = rec.homework
              remarks = rec.challengesRemarks
            }) {
              Text("Edit")
            }
          }
        }
      }
    }
  }
}
