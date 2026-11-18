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
import com.example.data.curriculum.DefaultLessonPlanTemplate
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

private fun normalizeLabel(value: String): String =
  value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

private fun List<LessonPlanTemplateSection>.keyForAliases(vararg aliases: String): String? {
  val normalized = aliases.map(::normalizeLabel).filter { it.isNotBlank() }
  return firstOrNull { section ->
    val label = normalizeLabel(section.label)
    normalized.any { alias ->
      label == alias || label.contains(alias) || (alias.length > 4 && alias.contains(label) && label.length > 3)
    }
  }?.key ?: aliases.firstOrNull { key -> any { it.key.equals(key, ignoreCase = true) } }
}

private fun LessonPlan.valueForSection(section: LessonPlanTemplateSection): String {
  val saved = extraContent[section.key]
  if (!saved.isNullOrBlank()) return saved
  val label = normalizeLabel(section.label)
  return when {
    "outcome" in label -> specificOutcomes
    "competenc" in label -> coreCompetencies
    "inquiry" in label -> keyInquiryQuestions
    "resource" in label -> learningResources
    "organization" in label || "organisation" in label -> organizationOfLearning
    "introduction" in label -> introduction
    "development" in label -> lessonDevelopment
    "conclusion" in label -> conclusion
    "extended" in label -> extendedActivity
    "reflection" in label -> reflection
    else -> ""
  }
}

@Composable
fun LessonPlansScreen(
  state: TeacherUiState,
  onSavePlan: (plan: LessonPlan, isUpdate: Boolean, contentMap: Map<String, String>) -> Unit,
  onDeletePlan: (String) -> Unit,
  onExportPdf: (LessonPlan) -> Unit,
  onRecordPlan: (LessonPlan) -> Unit,
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
  val sections = state.lessonPlanTemplate.ifEmpty { DefaultLessonPlanTemplate.sections }

  var editingId by remember { mutableStateOf<String?>(null) }
  var learningArea by remember(state.selectedGrade) { mutableStateOf(gradeSubjects.firstOrNull()?.name ?: "Mathematics") }
  var lessonDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
  var topic by remember { mutableStateOf("") }
  var strand by remember { mutableStateOf("") }
  var schemeId by remember { mutableStateOf("") }
  var schemeLocalId by remember { mutableStateOf("") }
  var planWeek by remember { mutableStateOf(1) }
  var planLesson by remember { mutableStateOf(1) }
  var planTerm by remember { mutableStateOf(session.currentTerm) }
  var hintNote by remember { mutableStateOf<String?>(null) }
  var showKicdPicker by remember { mutableStateOf(false) }

  val sectionValues = remember(sections) {
    mutableStateMapOf<String, String>().apply {
      sections.forEach { sec ->
        val label = normalizeLabel(sec.label)
        put(sec.key, when {
          "competenc" in label -> "Communication and Collaboration, Critical Thinking"
          "organization" in label || "organisation" in label -> "Pair & small group activities inside the classroom"
          "introduction" in label -> "Review prior knowledge using oral warm-up questions."
          "conclusion" in label -> "Learners summarize key takeaways and complete a quick check."
          "extended" in label -> "Practice with family members at home."
          else -> ""
        })
      }
    }
  }

  fun setSectionByAliases(value: String, vararg keys: String) {
    val target = sections.keyForAliases(*keys)
    if (target != null) sectionValues[target] = value
  }

  fun getSectionByAliases(vararg keys: String): String {
    val target = sections.keyForAliases(*keys)
    if (target != null) sectionValues[target]?.takeIf { it.isNotBlank() }?.let { return it }
    return ""
  }

  LaunchedEffect(state.pendingKicdPrefill) {
    val d = state.pendingKicdPrefill ?: return@LaunchedEffect
    learningArea = d.learningArea
    strand = d.strand
    topic = d.subStrand
    setSectionByAliases(d.specificLearningOutcomes.joinToString("\n• ", prefix = "• "), "Specific Learning Outcomes", "Outcomes", "sec1", "outcomes", "specificOutcomes")
    setSectionByAliases(d.coreCompetencies.joinToString(", "), "Core Competencies", "Competencies", "sec3", "competencies", "coreCompetencies")
    setSectionByAliases(d.keyInquiryQuestions.joinToString("\n"), "Key Inquiry Question", "Inquiry", "sec2", "inquiry", "keyInquiryQuestions")
    setSectionByAliases(d.suggestedResources.joinToString(", "), "Learning Resources", "Resources", "sec6", "sec4", "resources", "learningResources")
    setSectionByAliases(d.learningExperiences.joinToString("\n"), "Lesson Development", "Development", "sec8", "sec7", "development", "lessonDevelopment")
    hintNote = "Outcomes and inquiry questions filled from KICD design."
    onClearKicdPrefill()
  }

  LaunchedEffect(state.pendingPlanPrefill) {
    val prefill = state.pendingPlanPrefill ?: return@LaunchedEffect
    if (prefill.learningArea.isNotBlank()) learningArea = prefill.learningArea
    if (prefill.term.isNotBlank()) planTerm = prefill.term
    schemeId = prefill.schemeId
    schemeLocalId = prefill.schemeLocalId
    planWeek = prefill.week
    planLesson = prefill.lesson
    strand = prefill.strand
    topic = prefill.subStrand
    setSectionByAliases(prefill.outcomes, "Specific Learning Outcomes", "Outcomes", "sec1", "outcomes", "specificOutcomes")
    setSectionByAliases(prefill.keyInquiryQuestion, "Key Inquiry Question", "Inquiry", "sec2", "sec3", "inquiry", "keyInquiryQuestions")
    setSectionByAliases(prefill.learningExperiences, "Lesson Development", "Development", "sec8", "sec7", "development", "lessonDevelopment")
    setSectionByAliases(prefill.learningResources, "Learning Resources", "Resources", "sec6", "sec4", "resources", "learningResources")
    hintNote = "Week ${prefill.week}, lesson ${prefill.lesson} filled from your scheme of work. Complete the plan, then create its record of work."
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
                  term = planTerm.ifBlank { session.currentTerm },
                  week = planWeek,
                  lessonNumber = planLesson,
                  date = lessonDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
                  timeSlot = "8:20 - 8:55",
                  rollCount = state.learners.count { state.matchesActiveClass(it.stream, it.grade) }.coerceAtLeast(1),
                  strand = strand.ifBlank { topic },
                  subStrand = topic,
                  topic = topic,
                  specificOutcomes = getSectionByAliases("Specific Learning Outcomes", "Outcomes", "sec1", "outcomes", "specificOutcomes"),
                  keyInquiryQuestions = getSectionByAliases("Key Inquiry Question", "Inquiry", "sec2", "sec3", "inquiry", "keyInquiryQuestions"),
                  coreCompetencies = getSectionByAliases("Core Competencies", "Competencies", "sec3", "sec2", "competencies", "coreCompetencies"),
                  values = "Responsibility, Unity",
                  pcis = "Life skills",
                  learningResources = getSectionByAliases("Learning Resources", "Resources", "sec6", "sec4", "resources", "learningResources"),
                  organizationOfLearning = getSectionByAliases("Organization of Learning", "Organisation of Learning", "sec13", "sec5", "organization"),
                  introduction = getSectionByAliases("Introduction", "sec7", "sec6", "introduction", "intro"),
                  lessonDevelopment = getSectionByAliases("Lesson Development", "Development", "sec8", "sec7", "development", "lessonDevelopment"),
                  conclusion = getSectionByAliases("Conclusion", "sec9", "sec8", "conclusion"),
                  extendedActivity = getSectionByAliases("Extended Activity", "Extended Activities", "sec10", "sec9", "extended"),
                  reflection = getSectionByAliases("Reflection", "Teacher's Reflection", "sec12", "sec10", "reflection"),
                  updatedAt = System.currentTimeMillis(),
                  extraContent = contentMap,
                  schemeId = schemeId,
                  schemeLocalId = schemeLocalId,
                ),
                isUpdate,
                contentMap,
              )
              editingId = null
              topic = ""
              strand = ""
              schemeId = ""
              schemeLocalId = ""
              planWeek = 1
              planLesson = 1
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
            Text(
              text = "Wk ${plan.week}, Lesson ${plan.lessonNumber}" +
                (if (plan.schemeId.isNotBlank() || plan.schemeLocalId.isNotBlank()) " · linked to scheme" else "") +
                (if (!plan.synced) " · pending sync" else ""),
              style = MaterialTheme.typography.labelSmall,
              color = extra.me,
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
              OutlinedButton(onClick = {
                editingId = plan.id
                learningArea = plan.learningArea
                lessonDate = plan.date.ifBlank { lessonDate }
                topic = plan.topic.ifBlank { plan.subStrand }
                strand = plan.strand
                schemeId = plan.schemeId
                schemeLocalId = plan.schemeLocalId
                planWeek = plan.week
                planLesson = plan.lessonNumber
                planTerm = plan.term
                sections.forEach { sec -> sectionValues[sec.key] = plan.valueForSection(sec) }
              }) { Text("Edit") }
              OutlinedButton(onClick = { onRecordPlan(plan) }) { Text("Record work") }
              HighlighterButton(
                text = "Export PDF",
                onClick = { onExportPdf(plan) },
                icon = Icons.Filled.PictureAsPdf,
              )
              Spacer(Modifier.width(4.dp))
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
    val pickerDesigns = remember(
      state.remoteKicdDesigns,
      state.bundledKicdDesigns,
      state.selectedGrade,
      learningArea,
    ) {
      val all = (state.bundledKicdDesigns + state.remoteKicdDesigns + KicdCurriculumRepository.kicdDesigns)
        .distinctBy { it.id }
      val subjectId = KicdCurriculumRepository.subjectIdForArea(state.selectedGrade, learningArea)
      all.filter { d ->
        d.grade.equals(state.selectedGrade, ignoreCase = true) &&
          KicdCurriculumRepository.subjectIdForDesign(d.grade, d.subjectId, d.learningArea) == subjectId
      }.ifEmpty {
        all.filter { it.grade.equals(state.selectedGrade, ignoreCase = true) }
      }
    }
    AlertDialog(
      onDismissRequest = { showKicdPicker = false },
      title = { Text("Fill from KICD Design") },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pickerDesigns.isEmpty()) {
              Text(
                "No KICD templates found for $learningArea · ${state.selectedGrade}.",
                style = MaterialTheme.typography.bodyMedium,
                color = extra.muted,
              )
            }
          pickerDesigns.forEach { d ->
            Surface(
              color = MaterialTheme.colorScheme.background,
              shape = RoundedCornerShape(10.dp),
              onClick = {
                learningArea = d.learningArea
                strand = d.strand
                topic = d.subStrand
                setSectionByAliases(d.specificLearningOutcomes.joinToString("\n• ", prefix = "• "), "Specific Learning Outcomes", "Outcomes", "sec1", "outcomes", "specificOutcomes")
                setSectionByAliases(d.coreCompetencies.joinToString(", "), "Core Competencies", "Competencies", "sec3", "competencies", "coreCompetencies")
                setSectionByAliases(d.keyInquiryQuestions.joinToString("\n"), "Key Inquiry Question", "Inquiry", "sec2", "inquiry", "keyInquiryQuestions")
                setSectionByAliases(d.suggestedResources.joinToString(", "), "Learning Resources", "Resources", "sec6", "sec4", "resources", "learningResources")
                setSectionByAliases(d.learningExperiences.joinToString("\n"), "Lesson Development", "Development", "sec8", "sec7", "development", "lessonDevelopment")
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
  onClearRecordPrefill: () -> Unit,
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
  var strand by remember { mutableStateOf("") }
  var schemeId by remember { mutableStateOf("") }
  var schemeLocalId by remember { mutableStateOf("") }
  var lessonPlanId by remember { mutableStateOf("") }
  var lessonPlanLocalId by remember { mutableStateOf("") }
  var recordTerm by remember { mutableStateOf(session.currentTerm) }
  var weekText by remember { mutableStateOf("1") }
  var lessonText by remember { mutableStateOf("1") }
  var classSizeText by remember(classLearnersCount) { mutableStateOf(classLearnersCount.takeIf { it > 0 }?.toString() ?: "") }
  var workCovered by remember { mutableStateOf("") }
  var absencesToday by remember { mutableStateOf("") }
  var homework by remember { mutableStateOf("") }
  var remarks by remember { mutableStateOf("") }

  LaunchedEffect(state.pendingRecordPrefill) {
    val prefill = state.pendingRecordPrefill ?: return@LaunchedEffect
    if (prefill.learningArea.isNotBlank()) learningArea = prefill.learningArea
    if (prefill.term.isNotBlank()) recordTerm = prefill.term
    schemeId = prefill.schemeId
    schemeLocalId = prefill.schemeLocalId
    lessonPlanId = prefill.lessonPlanId
    lessonPlanLocalId = prefill.lessonPlanLocalId
    strand = prefill.strand
    topic = prefill.topic.ifBlank { prefill.subStrand }
    workCovered = prefill.workCovered.ifBlank { prefill.topic.ifBlank { prefill.subStrand } }
    weekText = prefill.week.toString()
    lessonText = prefill.lesson.toString()
    if (prefill.classSize > 0) classSizeText = prefill.classSize.toString()
    onClearRecordPrefill()
  }

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
                  term = recordTerm.ifBlank { session.currentTerm },
                  year = session.currentYear,
                  week = weekText.toIntOrNull() ?: 1,
                  date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                  strand = strand.ifBlank { topic },
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
                  schemeId = schemeId,
                  schemeLocalId = schemeLocalId,
                  lessonPlanId = lessonPlanId,
                  lessonPlanLocalId = lessonPlanLocalId,
                ),
                isUpdate,
              )
              editingId = null
              topic = ""
              strand = ""
              schemeId = ""
              schemeLocalId = ""
              lessonPlanId = ""
              lessonPlanLocalId = ""
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
              strand = rec.strand
              schemeId = rec.schemeId
              schemeLocalId = rec.schemeLocalId
              lessonPlanId = rec.lessonPlanId
              lessonPlanLocalId = rec.lessonPlanLocalId
              recordTerm = rec.term
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
