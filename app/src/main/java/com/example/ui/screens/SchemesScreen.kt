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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.domain.model.KicdDesignItem
import com.example.domain.model.SchemeOfWork
import com.example.domain.model.SchemeRow
import com.example.ui.components.HighlighterButton
import com.example.ui.components.SubScreenHeader
import com.example.ui.theme.LocalElimuColors
import com.example.ui.viewmodel.TeacherUiState

@Composable
fun SchemesScreen(
  state: TeacherUiState,
  onSaveScheme: (existingId: String?, title: String, grade: String, learningArea: String, term: String, year: Int, rows: List<SchemeRow>) -> Unit,
  onDeleteScheme: (SchemeOfWork) -> Unit,
  onExportPdf: (SchemeOfWork) -> Unit,
  onPlanLessonRow: (topic: String, outcomes: String) -> Unit,
  onClearKicdPrefill: () -> Unit,
  onShowMessage: (String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val extra = LocalElimuColors.current
  val classDisplay = state.activeClass?.displayName ?: state.selectedStream.ifBlank { state.selectedGrade }
  var editingScheme by remember { mutableStateOf<SchemeOfWork?>(null) }
  var isCreating by remember { mutableStateOf(false) }
  var showKicdPicker by remember { mutableStateOf(false) }

  val gradeSubjects = remember(state.selectedGrade) {
    KicdCurriculumRepository.subjectsForGrade(state.selectedGrade)
  }
  var selectedSubjectId by remember(state.selectedGrade) {
    mutableStateOf(gradeSubjects.firstOrNull()?.id ?: "math")
  }
  val selectedAreaName = remember(selectedSubjectId, gradeSubjects) {
    gradeSubjects.firstOrNull { it.id == selectedSubjectId }?.name
      ?: KicdCurriculumRepository.areaNameForSubjectId(selectedSubjectId)
  }

  var title by remember { mutableStateOf("") }
  var learningArea by remember { mutableStateOf(selectedAreaName) }
  var term by remember { mutableStateOf(state.session?.currentTerm ?: "Term 1") }
  var yearText by remember { mutableStateOf((state.session?.currentYear ?: 2026).toString()) }
  var rows by remember { mutableStateOf<List<SchemeRow>>(emptyList()) }

  fun openNewEditor(prefill: KicdDesignItem? = null) {
    val defaultArea = prefill?.learningArea ?: selectedAreaName
    editingScheme = null
    learningArea = defaultArea
    term = state.session?.currentTerm ?: "Term 1"
    yearText = (state.session?.currentYear ?: 2026).toString()
    title = "$classDisplay · $defaultArea ($term)"
    rows = if (prefill != null) {
      listOf(
        SchemeRow(
          week = 1,
          lesson = 1,
          strand = prefill.strand,
          subStrand = prefill.subStrand,
          specificOutcomes = prefill.specificLearningOutcomes.joinToString("\n• ", prefix = "• "),
          keyInquiryQuestion = prefill.keyInquiryQuestions.joinToString(" "),
          learningExperiences = prefill.learningExperiences.joinToString("\n• ", prefix = "• "),
          learningResources = prefill.suggestedResources.joinToString(", "),
          assessmentMethods = "Observation, Oral questions, Rubric check",
        )
      )
    } else {
      listOf(SchemeRow(1, 1, "", "", "", "", "", "", "Observation, Oral questions"))
    }
    isCreating = true
  }

  LaunchedEffect(state.pendingKicdPrefill) {
    val prefill = state.pendingKicdPrefill
    if (prefill != null) {
      openNewEditor(prefill)
      onClearKicdPrefill()
    }
  }

  if (isCreating) {
    BackHandler { isCreating = false; editingScheme = null }
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { isCreating = false; editingScheme = null }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
          Column {
            Text(if (editingScheme == null) "New Scheme of Work" else "Edit Scheme of Work", style = MaterialTheme.typography.titleLarge)
            Text(classDisplay, style = MaterialTheme.typography.bodySmall, color = extra.muted)
          }
        }
        OutlinedButton(onClick = { showKicdPicker = true }) {
          Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("KICD Fill")
        }
      }

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Scheme Title") },
        modifier = Modifier.fillMaxWidth().testTag("scheme_title_input"),
      )

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

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = learningArea, onValueChange = { learningArea = it }, label = { Text("Learning Area") }, modifier = Modifier.weight(1.4f))
        OutlinedTextField(value = term, onValueChange = { term = it }, label = { Text("Term") }, modifier = Modifier.weight(0.8f))
        OutlinedTextField(value = yearText, onValueChange = { yearText = it }, label = { Text("Year") }, modifier = Modifier.weight(0.7f))
      }

      rows.forEachIndexed { idx, row ->
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("Week ${row.week}, Lesson ${row.lesson}", style = MaterialTheme.typography.titleSmall)
              if (rows.size > 1) {
                IconButton(onClick = { rows = rows.toMutableList().also { it.removeAt(idx) } }) {
                  Icon(Icons.Filled.Delete, contentDescription = "Remove row", tint = extra.be)
                }
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = row.week.toString(),
                onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(week = v.toIntOrNull() ?: 1) } },
                label = { Text("Week") },
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = row.lesson.toString(),
                onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(lesson = v.toIntOrNull() ?: 1) } },
                label = { Text("Lesson") },
                modifier = Modifier.weight(1f),
              )
            }
            OutlinedTextField(value = row.strand, onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(strand = v) } }, label = { Text("Strand") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = row.subStrand, onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(subStrand = v) } }, label = { Text("Sub-Strand") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = row.specificOutcomes, onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(specificOutcomes = v) } }, label = { Text("Specific Learning Outcomes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = row.keyInquiryQuestion, onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(keyInquiryQuestion = v) } }, label = { Text("Key Inquiry Question") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = row.learningExperiences, onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(learningExperiences = v) } }, label = { Text("Learning Experiences") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = row.learningResources, onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(learningResources = v) } }, label = { Text("Learning Resources") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = row.assessmentMethods, onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(assessmentMethods = v) } }, label = { Text("Assessment Methods") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = row.reflection, onValueChange = { v -> rows = rows.toMutableList().also { it[idx] = row.copy(reflection = v) } }, label = { Text("Remarks") }, modifier = Modifier.fillMaxWidth())
          }
        }
      }

      OutlinedButton(
        onClick = {
          val nextWeek = (rows.lastOrNull()?.week ?: 1) + 1
          rows = rows + SchemeRow(nextWeek, 1, rows.lastOrNull()?.strand ?: "", "", "", "", "", "", "Observation, Oral questions")
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Add Week / Lesson Row")
      }

      HighlighterButton(
        text = "Save scheme",
        onClick = {
          if (learningArea.isBlank()) {
            onShowMessage("Select or enter a learning area.")
            return@HighlighterButton
          }
          val invalidRow = rows.firstOrNull { it.subStrand.isBlank() || it.specificOutcomes.isBlank() }
          if (invalidRow != null) {
            onShowMessage("Week ${invalidRow.week} needs both Sub-Strand and Specific Learning Outcomes.")
            return@HighlighterButton
          }
          onSaveScheme(editingScheme?.id, title, state.selectedGrade, learningArea, term, yearText.toIntOrNull() ?: 2026, rows)
          isCreating = false
          editingScheme = null
        },
        modifier = Modifier.fillMaxWidth().testTag("save_scheme_button"),
      )
      Spacer(Modifier.height(24.dp))
    }

    if (showKicdPicker) {
      val pickerDesigns = (state.remoteKicdDesigns + KicdCurriculumRepository.kicdDesigns).distinctBy { it.id }
      AlertDialog(
        onDismissRequest = { showKicdPicker = false },
        title = { Text("Auto-Fill from KICD Design") },
        text = {
          Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pickerDesigns.forEach { d ->
              Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(10.dp),
                onClick = {
                  learningArea = d.learningArea
                  title = "$classDisplay · ${d.learningArea} ($term)"
                  val newRow = SchemeRow(
                    week = rows.size + 1,
                    lesson = 1,
                    strand = d.strand,
                    subStrand = d.subStrand,
                    specificOutcomes = d.specificLearningOutcomes.joinToString("\n• ", prefix = "• "),
                    keyInquiryQuestion = d.keyInquiryQuestions.joinToString(" "),
                    learningExperiences = d.learningExperiences.joinToString("\n• ", prefix = "• "),
                    learningResources = d.suggestedResources.joinToString(", "),
                    assessmentMethods = "Rubric observation, Practical tasks",
                  )
                  rows = if (rows.size == 1 && rows.first().strand.isBlank()) listOf(newRow.copy(week = 1)) else rows + newRow
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
    return
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { openNewEditor(null) },
        containerColor = extra.highlighter,
        contentColor = extra.highlighterInk,
        modifier = Modifier.testTag("add_scheme_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Create scheme")
      }
    },
  ) { innerPadding ->
    val targetGrade = KicdCurriculumRepository.normalizeGrade(state.selectedGrade) ?: state.selectedGrade
    val schemesForGrade = state.schemes.filter {
      val g = KicdCurriculumRepository.normalizeGrade(it.grade) ?: it.grade
      g.equals(targetGrade, ignoreCase = true)
    }.ifEmpty { state.schemes }
    val saved = schemesForGrade.filter {
      KicdCurriculumRepository.subjectIdForArea(state.selectedGrade, it.learningArea) == selectedSubjectId ||
        it.learningArea.equals(selectedAreaName, ignoreCase = true)
    }.ifEmpty { schemesForGrade }

    val sampleRows = KicdCurriculumRepository.kicdDesigns.filter {
      (KicdCurriculumRepository.normalizeGrade(it.grade) ?: it.grade).equals(targetGrade, ignoreCase = true)
    }.ifEmpty { KicdCurriculumRepository.kicdDesigns.take(3) }

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SubScreenHeader(
          title = "Schemes of work",
          subtitle = "$classDisplay · $selectedAreaName",
          onBackToServices = onBackToServices,
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          gradeSubjects.forEach { subj ->
            FilterChip(
              selected = selectedSubjectId == subj.id,
              onClick = { selectedSubjectId = subj.id },
              label = { Text(subj.name) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                selectedLabelColor = MaterialTheme.colorScheme.background,
              ),
              shape = RoundedCornerShape(99.dp),
            )
          }
        }
      }

      if (saved.isNotEmpty()) {
        items(saved, key = { it.id }) { scheme ->
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, extra.line),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(scheme.title, style = MaterialTheme.typography.titleMedium)
                  Text("${scheme.grade} · ${scheme.learningArea} · ${scheme.term}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                }
                Surface(color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, extra.line), shape = RoundedCornerShape(99.dp)) {
                  Text("${scheme.rows.size} rows", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                  editingScheme = scheme
                  title = scheme.title
                  learningArea = scheme.learningArea
                  term = scheme.term
                  yearText = scheme.year.toString()
                  rows = scheme.rows
                  isCreating = true
                }) { Text("Edit") }
                HighlighterButton(
                  text = "Export PDF",
                  onClick = { onExportPdf(scheme) },
                  icon = Icons.Filled.PictureAsPdf,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onDeleteScheme(scheme) }) {
                  Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = extra.be)
                }
              }
            }
          }
        }
      }

      item {
        Surface(
          color = extra.highlighter.copy(alpha = 0.24f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "KICD scheme templates below can be turned into a lesson plan in one tap, or tap + to build & export your full scheme PDF.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      items(sampleRows, key = { it.id }) { design ->
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(design.learningArea, style = MaterialTheme.typography.titleSmall)
              Surface(color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, extra.line), shape = RoundedCornerShape(99.dp)) {
                Text(design.strand, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
              }
            }
            Text(design.subStrand, style = MaterialTheme.typography.titleMedium)
            Text(design.specificLearningOutcomes.firstOrNull().orEmpty(), style = MaterialTheme.typography.bodySmall, color = extra.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = {
                  onPlanLessonRow(
                    design.subStrand,
                    design.specificLearningOutcomes.joinToString("\n• ", prefix = "• "),
                  )
                },
              ) {
                Text("Plan this lesson")
              }
              OutlinedButton(onClick = { openNewEditor(design) }) {
                Text("Add to scheme")
              }
            }
          }
        }
      }
    }
  }
}
