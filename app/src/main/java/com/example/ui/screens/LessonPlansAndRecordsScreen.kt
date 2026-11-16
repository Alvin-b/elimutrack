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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.domain.model.LessonPlan
import com.example.domain.model.RecordOfWork
import com.example.ui.components.HighlighterButton
import com.example.ui.components.SubScreenHeader
import com.example.ui.theme.LocalElimuColors
import com.example.ui.viewmodel.TeacherUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun LessonPlansScreen(
  state: TeacherUiState,
  onSavePlan: (LessonPlan) -> Unit,
  onDeletePlan: (String) -> Unit,
  onExportPdf: (LessonPlan) -> Unit,
  onClearKicdPrefill: () -> Unit,
  onClearPlanPrefill: () -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val levelAreas = KicdCurriculumRepository.cbcLearningAreasByLevel[KicdCurriculumRepository.levelForGrade(state.selectedGrade)].orEmpty()
  val activeStream = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }?.stream ?: "Delta"

  var editingId by remember { mutableStateOf<String?>(null) }
  var learningArea by remember(state.selectedGrade) { mutableStateOf(levelAreas.firstOrNull() ?: "Mathematics") }
  var topic by remember { mutableStateOf("") }
  var strand by remember { mutableStateOf("") }
  var hintNote by remember { mutableStateOf<String?>(null) }
  var showKicdPicker by remember { mutableStateOf(false) }

  // The 10 CBC Sections from prototype SECS
  var s0Outcomes by remember { mutableStateOf("") }
  var s1Competencies by remember { mutableStateOf("Communication and Collaboration, Critical Thinking") }
  var s2Inquiry by remember { mutableStateOf("") }
  var s3Resources by remember { mutableStateOf("") }
  var s4Organization by remember { mutableStateOf("Pair & small group activities inside the classroom") }
  var s5Intro by remember { mutableStateOf("Review prior knowledge using oral warm-up questions.") }
  var s6Development by remember { mutableStateOf("") }
  var s7Conclusion by remember { mutableStateOf("Learners summarize key takeaways and complete a quick check.") }
  var s8Extended by remember { mutableStateOf("Practice with family members at home.") }
  var s9Reflection by remember { mutableStateOf("") }

  LaunchedEffect(state.pendingKicdPrefill) {
    val d = state.pendingKicdPrefill ?: return@LaunchedEffect
    learningArea = d.learningArea
    strand = d.strand
    topic = d.subStrand
    s0Outcomes = d.specificLearningOutcomes.joinToString("\n• ", prefix = "• ")
    s1Competencies = d.coreCompetencies.joinToString(", ")
    s2Inquiry = d.keyInquiryQuestions.joinToString("\n")
    s3Resources = d.suggestedResources.joinToString(", ")
    s6Development = d.learningExperiences.joinToString("\n")
    hintNote = "Outcomes and inquiry questions filled from KICD design."
    onClearKicdPrefill()
  }

  LaunchedEffect(state.pendingPlanPrefill) {
    val pair = state.pendingPlanPrefill ?: return@LaunchedEffect
    topic = pair.first
    s0Outcomes = pair.second
    hintNote = "Topic and outcomes filled from your scheme row."
    onClearPlanPrefill()
  }

  val savedForClass = state.lessonPlans.filter { it.grade == state.selectedGrade }

  BackHandler(enabled = editingId != null) { editingId = null }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Lesson plans",
        subtitle = "Write a plan using the ten CBC sections",
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
            levelAreas.forEach { a ->
              FilterChip(
                selected = learningArea == a,
                onClick = { learningArea = a },
                label = { Text(a) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                  selectedLabelColor = MaterialTheme.colorScheme.background,
                ),
                shape = RoundedCornerShape(99.dp),
              )
            }
          }

          OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic / Sub-strand") }, modifier = Modifier.fillMaxWidth().testTag("lesson_plan_topic_input"))
          OutlinedTextField(value = s0Outcomes, onValueChange = { s0Outcomes = it }, label = { Text("1. Specific learning outcomes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = s1Competencies, onValueChange = { s1Competencies = it }, label = { Text("2. Core competencies") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = s2Inquiry, onValueChange = { s2Inquiry = it }, label = { Text("3. Key inquiry question") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = s3Resources, onValueChange = { s3Resources = it }, label = { Text("4. Learning resources") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = s4Organization, onValueChange = { s4Organization = it }, label = { Text("5. Organization of learning") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = s5Intro, onValueChange = { s5Intro = it }, label = { Text("6. Introduction") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = s6Development, onValueChange = { s6Development = it }, label = { Text("7. Lesson development") }, minLines = 3, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = s7Conclusion, onValueChange = { s7Conclusion = it }, label = { Text("8. Conclusion") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = s8Extended, onValueChange = { s8Extended = it }, label = { Text("9. Extended activity") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = s9Reflection, onValueChange = { s9Reflection = it }, label = { Text("10. Reflection") }, minLines = 2, modifier = Modifier.fillMaxWidth())

          HighlighterButton(
            text = if (editingId == null) "Save plan" else "Update plan",
            onClick = {
              if (topic.isBlank()) return@HighlighterButton
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
                  date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                  timeSlot = "8:20 - 8:55",
                  rollCount = state.learners.count { it.grade == state.selectedGrade }.coerceAtLeast(1),
                  strand = strand.ifBlank { topic },
                  subStrand = topic,
                  topic = topic,
                  specificOutcomes = s0Outcomes,
                  keyInquiryQuestions = s2Inquiry,
                  coreCompetencies = s1Competencies,
                  values = "Responsibility, Unity",
                  pcis = "Life skills",
                  learningResources = s3Resources,
                  organizationOfLearning = s4Organization,
                  introduction = s5Intro,
                  lessonDevelopment = s6Development,
                  conclusion = s7Conclusion,
                  extendedActivity = s8Extended,
                  reflection = s9Reflection,
                  updatedAt = System.currentTimeMillis(),
                )
              )
              editingId = null
              topic = ""
              s0Outcomes = ""
              hintNote = null
            },
            modifier = Modifier.fillMaxWidth().testTag("save_lesson_plan_button"),
          )
        }
      }
    }

    item {
      Text("Saved for ${state.selectedGrade} $activeStream", style = MaterialTheme.typography.titleMedium)
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
            Text(plan.specificOutcomes.ifBlank { "Draft plan" }, style = MaterialTheme.typography.bodySmall, color = extra.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(onClick = {
                editingId = plan.id
                learningArea = plan.learningArea
                topic = plan.topic.ifBlank { plan.subStrand }
                strand = plan.strand
                s0Outcomes = plan.specificOutcomes
                s1Competencies = plan.coreCompetencies
                s2Inquiry = plan.keyInquiryQuestions
                s3Resources = plan.learningResources
                s4Organization = plan.organizationOfLearning
                s5Intro = plan.introduction
                s6Development = plan.lessonDevelopment
                s7Conclusion = plan.conclusion
                s8Extended = plan.extendedActivity
                s9Reflection = plan.reflection
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
    AlertDialog(
      onDismissRequest = { showKicdPicker = false },
      title = { Text("Fill from KICD Design") },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          KicdCurriculumRepository.kicdDesigns.forEach { d ->
            Surface(
              color = MaterialTheme.colorScheme.background,
              shape = RoundedCornerShape(10.dp),
              onClick = {
                learningArea = d.learningArea
                strand = d.strand
                topic = d.subStrand
                s0Outcomes = d.specificLearningOutcomes.joinToString("\n• ", prefix = "• ")
                s1Competencies = d.coreCompetencies.joinToString(", ")
                s2Inquiry = d.keyInquiryQuestions.joinToString("\n")
                s3Resources = d.suggestedResources.joinToString(", ")
                s6Development = d.learningExperiences.joinToString("\n")
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
  onSaveRecord: (RecordOfWork) -> Unit,
  onDeleteRecord: (String) -> Unit,
  onExportPdf: (grade: String, learningArea: String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val levelAreas = KicdCurriculumRepository.cbcLearningAreasByLevel[KicdCurriculumRepository.levelForGrade(state.selectedGrade)].orEmpty()
  val activeStream = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }?.stream ?: "Delta"

  var learningArea by remember(state.selectedGrade) { mutableStateOf(levelAreas.firstOrNull() ?: "Mathematics") }
  var topic by remember { mutableStateOf("") }
  var workCovered by remember { mutableStateOf("") }
  var absencesToday by remember { mutableStateOf("") }
  var homework by remember { mutableStateOf("") }
  var remarks by remember { mutableStateOf("") }

  val savedForClass = state.recordsOfWork.filter { it.grade == state.selectedGrade }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Record of work",
        subtitle = "What you actually taught",
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
            levelAreas.forEach { a ->
              FilterChip(
                selected = learningArea == a,
                onClick = { learningArea = a },
                label = { Text(a) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                  selectedLabelColor = MaterialTheme.colorScheme.background,
                ),
                shape = RoundedCornerShape(99.dp),
              )
            }
          }
          OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = workCovered, onValueChange = { workCovered = it }, label = { Text("Work covered") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = absencesToday, onValueChange = { absencesToday = it }, label = { Text("Absences today") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = homework, onValueChange = { homework = it }, label = { Text("Homework") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks") }, minLines = 2, modifier = Modifier.fillMaxWidth())

          HighlighterButton(
            text = "Save record",
            onClick = {
              if (topic.isBlank()) return@HighlighterButton
              val initials = session.fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
              onSaveRecord(
                RecordOfWork(
                  id = "row_${UUID.randomUUID().toString().take(8)}",
                  schoolName = session.schoolName,
                  teacherName = session.fullName,
                  grade = state.selectedGrade,
                  stream = activeStream,
                  learningArea = learningArea,
                  term = session.currentTerm,
                  year = session.currentYear,
                  week = 1,
                  date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                  strand = topic,
                  subStrand = topic,
                  workCovered = workCovered.ifBlank { "No details added" },
                  absencesToday = absencesToday,
                  homework = homework,
                  challengesRemarks = remarks,
                  teacherInitials = initials.ifBlank { "TR" },
                  updatedAt = System.currentTimeMillis(),
                )
              )
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
      Text("Saved for ${state.selectedGrade} $activeStream", style = MaterialTheme.typography.titleMedium)
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
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text("${rec.learningArea} · ${rec.subStrand}", style = MaterialTheme.typography.titleSmall)
              Text(rec.workCovered, style = MaterialTheme.typography.bodySmall, color = extra.muted)
              if (rec.homework.isNotBlank()) {
                Text("Homework: ${rec.homework}", style = MaterialTheme.typography.labelSmall)
              }
            }
            IconButton(onClick = { onDeleteRecord(rec.id) }) {
              Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = extra.be)
            }
          }
        }
      }
    }
  }
}
