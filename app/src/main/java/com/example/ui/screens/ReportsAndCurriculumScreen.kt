package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.domain.model.CbcPerformanceLevel
import com.example.domain.model.KicdDesignItem
import com.example.domain.model.Learner
import com.example.domain.model.TeacherModule
import com.example.ui.components.CbcRubricBadge
import com.example.ui.components.GradeSelectorRow
import com.example.ui.components.HighlighterButton
import com.example.ui.components.SubScreenHeader
import com.example.ui.theme.LocalElimuColors
import com.example.ui.viewmodel.TeacherUiState

private fun learnerInitials(name: String): String =
  name.trim().split(" ").filter { it.isNotBlank() }.mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnersScreen(
  state: TeacherUiState,
  onAddLearner: (adm: String, nemisUpi: String, name: String, grade: String, stream: String, gender: String, phone: String, pathway: String) -> Unit,
  onPopulateStarterRoster: (grade: String, stream: String) -> Unit,
  onNavigateModule: (TeacherModule) -> Unit,
  onExportLearnerPdf: (learner: Learner, comment: String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val context = LocalContext.current
  val activeClass = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }
  val classDisplay = activeClass?.displayName ?: state.selectedGrade
  val activeStream = activeClass?.stream ?: "Delta"

  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedLearner by remember { mutableStateOf<Learner?>(null) }

  val gradeLearners = state.learners.filter { it.grade == state.selectedGrade }
  val filtered = gradeLearners.filter { l ->
    searchQuery.isBlank() ||
      l.fullName.contains(searchQuery, ignoreCase = true) ||
      l.admissionNo.contains(searchQuery, ignoreCase = true) ||
      l.nemisUpi.contains(searchQuery, ignoreCase = true)
  }

  Scaffold(
    containerColor = Color.Transparent,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = extra.highlighter,
        contentColor = extra.highlighterInk,
        modifier = Modifier.testTag("add_learner_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add learner")
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SubScreenHeader(
          title = "Learners",
          subtitle = "$classDisplay · ${gradeLearners.size} on roll",
          onBackToServices = onBackToServices,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = { Text("Search name, Adm No or NEMIS UPI") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("learners_search_input"),
        )
      }

      if (gradeLearners.isEmpty()) {
        item {
          EmptyClassRosterCard(
            grade = state.selectedGrade,
            onLoadStarter = { onPopulateStarterRoster(state.selectedGrade, activeStream) },
          )
        }
      } else if (filtered.isEmpty()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, extra.line),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("No learner matches that search.", style = MaterialTheme.typography.bodyMedium, color = extra.muted, modifier = Modifier.padding(18.dp))
          }
        }
      } else {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, extra.line),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
              filtered.forEachIndexed { idx, l ->
                if (idx > 0) HorizontalDivider(color = extra.line)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedLearner = l }
                    .padding(vertical = 10.dp)
                    .testTag("learner_row_${l.id}"),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp, bottomEnd = 13.dp, bottomStart = 4.dp))
                      .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(learnerInitials(l.fullName), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(l.fullName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Adm. ${l.admissionNo}${if (l.nemisUpi.isNotBlank()) " · ${l.nemisUpi}" else ""}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                  }
                  Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open learner", tint = extra.muted)
                }
              }
            }
          }
        }
      }
    }
  }

  selectedLearner?.let { learner ->
    ModalBottomSheet(
      onDismissRequest = { selectedLearner = null },
      containerColor = MaterialTheme.colorScheme.background,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
              .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
          ) {
            Text(learnerInitials(learner.fullName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          Column {
            Text(learner.fullName, style = MaterialTheme.typography.headlineSmall)
            Text(classDisplay, style = MaterialTheme.typography.bodySmall, color = extra.muted)
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Admission no.", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                Text(learner.admissionNo, style = MaterialTheme.typography.titleSmall)
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("NEMIS UPI", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                Text(learner.nemisUpi.ifBlank { "-" }, style = MaterialTheme.typography.titleSmall)
              }
            }
            HorizontalDivider(color = extra.line)
            Row(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Term attendance", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                Text("${learner.termAttendancePct}%", style = MaterialTheme.typography.titleSmall)
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("Guardian", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                Text(learner.guardianPhone.ifBlank { "Not on file" }, style = MaterialTheme.typography.titleSmall)
              }
            }
          }
        }

        if (learner.guardianPhone.isNotBlank()) {
          HighlighterButton(
            text = "Call guardian",
            icon = Icons.Filled.Call,
            onClick = {
              runCatching {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${learner.guardianPhone}"))
                context.startActivity(dialIntent)
              }
            },
            modifier = Modifier.fillMaxWidth(),
          )
        }

        val allowed = session.allowedModules
        if (TeacherModule.OBSERVATIONS in allowed) {
          OutlinedButton(
            onClick = {
              selectedLearner = null
              onNavigateModule(TeacherModule.OBSERVATIONS)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Text("Add an observation", fontWeight = FontWeight.Bold)
          }
        }
        if (TeacherModule.PROGRESS in allowed) {
          OutlinedButton(
            onClick = {
              selectedLearner = null
              onNavigateModule(TeacherModule.PROGRESS)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Text("Record progress", fontWeight = FontWeight.Bold)
          }
        }
        OutlinedButton(
          onClick = {
            selectedLearner = null
            onExportLearnerPdf(learner, "Consistent engagement across term activities.")
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Export Progress Report PDF", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
      }
    }
  }

  if (showAddDialog) {
    var adm by remember { mutableStateOf("") }
    var nemis by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var stream by remember { mutableStateOf(activeStream) }
    var gender by remember { mutableStateOf("Female") }
    var phone by remember { mutableStateOf("") }
    var pathway by remember { mutableStateOf("STEM") }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Add learner to ${state.selectedGrade}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Learner full name") }, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = adm, onValueChange = { adm = it }, label = { Text("Adm No") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = nemis, onValueChange = { nemis = it }, label = { Text("NEMIS UPI") }, modifier = Modifier.weight(1f))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = stream, onValueChange = { stream = it }, label = { Text("Stream") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender") }, modifier = Modifier.weight(1f))
          }
          OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Guardian phone") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = pathway, onValueChange = { pathway = it }, label = { Text("Pathway interest") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onAddLearner(adm, nemis, name, state.selectedGrade, stream, gender, phone, pathway)
            showAddDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = extra.highlighter, contentColor = extra.highlighterInk),
        ) { Text("Add learner", fontWeight = FontWeight.Bold) }
      },
      dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObservationsScreen(
  state: TeacherUiState,
  onSaveObservation: (learner: Learner, observationType: String, competency: String, rating: CbcPerformanceLevel, note: String) -> Unit,
  onDeleteObservation: (String) -> Unit,
  onPopulateStarterRoster: (grade: String, stream: String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val activeClass = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }
  val classDisplay = activeClass?.displayName ?: state.selectedGrade
  val activeStream = activeClass?.stream ?: "Delta"
  val gradeLearners = state.learners.filter { it.grade == state.selectedGrade }
  val filtered = state.observations.filter { it.grade == state.selectedGrade }

  var selectedLearnerId by remember(state.selectedGrade, gradeLearners) { mutableStateOf(gradeLearners.firstOrNull()?.id) }
  var selectedType by remember { mutableStateOf("Strength") }
  var selectedComp by remember { mutableStateOf(KicdCurriculumRepository.cbcCoreCompetencies.first()) }
  var selectedRating by remember { mutableStateOf(CbcPerformanceLevel.ME) }
  var note by remember { mutableStateOf("") }

  val observationTypes = listOf("Strength", "Needs support", "Participation", "Wellbeing")

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Observations",
        subtitle = "Notes on how learners are doing",
        onBackToServices = onBackToServices,
      )
    }

    if (gradeLearners.isEmpty()) {
      item {
        EmptyClassRosterCard(state.selectedGrade) { onPopulateStarterRoster(state.selectedGrade, activeStream) }
      }
    } else {
      item {
        val currentLearner = gradeLearners.firstOrNull { it.id == selectedLearnerId } ?: gradeLearners.first()
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Learner", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              gradeLearners.forEach { l ->
                FilterChip(
                  selected = currentLearner.id == l.id,
                  onClick = { selectedLearnerId = l.id },
                  label = { Text(l.fullName) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                  ),
                  shape = RoundedCornerShape(99.dp),
                )
              }
            }

            Text("Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              observationTypes.forEach { t ->
                FilterChip(
                  selected = selectedType == t,
                  onClick = { selectedType = t },
                  label = { Text(t) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                  ),
                  shape = RoundedCornerShape(99.dp),
                )
              }
            }

            Text("Core competency", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              KicdCurriculumRepository.cbcCoreCompetencies.forEach { comp ->
                FilterChip(
                  selected = selectedComp == comp,
                  onClick = { selectedComp = comp },
                  label = { Text(comp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = extra.navy,
                    selectedLabelColor = Color.White,
                  ),
                  shape = RoundedCornerShape(99.dp),
                )
              }
            }

            Text("Rubric band", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              CbcPerformanceLevel.entries.forEach { lvl ->
                FilterChip(
                  selected = selectedRating == lvl,
                  onClick = { selectedRating = lvl },
                  label = { Text(lvl.code) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                  ),
                  shape = RoundedCornerShape(99.dp),
                )
              }
            }

            OutlinedTextField(
              value = note,
              onValueChange = { note = it },
              label = { Text("What did you notice?") },
              minLines = 3,
              modifier = Modifier.fillMaxWidth().testTag("observation_note_input"),
            )

            HighlighterButton(
              text = "Save observation",
              onClick = {
                onSaveObservation(currentLearner, selectedType, selectedComp, selectedRating, note)
                if (note.isNotBlank()) note = ""
              },
              modifier = Modifier.fillMaxWidth().testTag("save_observation_button"),
            )
          }
        }
      }

      item {
        Text("Saved for $classDisplay", style = MaterialTheme.typography.titleMedium)
      }

      if (filtered.isEmpty()) {
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
        items(filtered, key = { it.id }) { obs ->
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, extra.line),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${obs.learnerName} · ${obs.observationType}", style = MaterialTheme.typography.titleSmall)
                Text("${obs.competency} · ${obs.date}", style = MaterialTheme.typography.labelSmall, color = extra.me)
                Text(obs.note, style = MaterialTheme.typography.bodySmall, color = extra.muted)
              }
              CbcRubricBadge(obs.rating)
              IconButton(onClick = { onDeleteObservation(obs.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = extra.be)
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearnerProgressScreen(
  state: TeacherUiState,
  onSaveProgress: (learner: Learner, learningArea: String, stage: String, note: String) -> Unit,
  onExportLearnerPdf: (learner: Learner, comment: String) -> Unit,
  onPopulateStarterRoster: (grade: String, stream: String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val activeClass = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }
  val classDisplay = activeClass?.displayName ?: state.selectedGrade
  val activeStream = activeClass?.stream ?: "Delta"
  val gradeLearners = state.learners.filter { it.grade == state.selectedGrade }
  val levelAreas = KicdCurriculumRepository.cbcLearningAreasByLevel[KicdCurriculumRepository.levelForGrade(state.selectedGrade)].orEmpty()
  val savedEntries = state.progressEntries.filter { it.grade == state.selectedGrade }

  var selectedLearnerId by remember(state.selectedGrade, gradeLearners) { mutableStateOf(gradeLearners.firstOrNull()?.id) }
  var selectedArea by remember(state.selectedGrade) { mutableStateOf(levelAreas.firstOrNull() ?: "Mathematics") }
  var selectedStage by remember { mutableStateOf("Developing") }
  var note by remember { mutableStateOf("") }
  val stages = listOf("Beginning", "Developing", "Secure", "Exceeding")

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Learner progress",
        subtitle = "Record the stage a learner has reached",
        onBackToServices = onBackToServices,
      )
    }

    if (gradeLearners.isEmpty()) {
      item {
        EmptyClassRosterCard(state.selectedGrade) { onPopulateStarterRoster(state.selectedGrade, activeStream) }
      }
    } else {
      item {
        val currentLearner = gradeLearners.firstOrNull { it.id == selectedLearnerId } ?: gradeLearners.first()
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Learner", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              gradeLearners.forEach { l ->
                FilterChip(
                  selected = currentLearner.id == l.id,
                  onClick = { selectedLearnerId = l.id },
                  label = { Text(l.fullName) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                  ),
                  shape = RoundedCornerShape(99.dp),
                )
              }
            }

            Text("Learning area", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              levelAreas.forEach { a ->
                FilterChip(
                  selected = selectedArea == a,
                  onClick = { selectedArea = a },
                  label = { Text(a) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                  ),
                  shape = RoundedCornerShape(99.dp),
                )
              }
            }

            Text("Stage of achievement", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              stages.forEach { st ->
                FilterChip(
                  selected = selectedStage == st,
                  onClick = { selectedStage = st },
                  label = { Text(st) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                  ),
                  shape = RoundedCornerShape(99.dp),
                )
              }
            }

            OutlinedTextField(
              value = note,
              onValueChange = { note = it },
              label = { Text("Note") },
              minLines = 3,
              modifier = Modifier.fillMaxWidth(),
            )

            HighlighterButton(
              text = "Save progress",
              onClick = {
                onSaveProgress(currentLearner, selectedArea, selectedStage, note)
                if (note.isNotBlank()) note = ""
              },
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
              onClick = { onExportLearnerPdf(currentLearner, note.ifBlank { "Achieving $selectedStage stage in $selectedArea." }) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
              Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Export Progress Report PDF", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      item {
        Text("Saved for $classDisplay", style = MaterialTheme.typography.titleMedium)
      }

      if (savedEntries.isEmpty()) {
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
        items(savedEntries, key = { it.id }) { p ->
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, extra.line),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("${p.learnerName} · ${p.stage}", style = MaterialTheme.typography.titleSmall)
              Text("${p.learningArea}. ${p.note}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCardsScreen(
  state: TeacherUiState,
  onExportLearnerPdf: (learner: Learner, comment: String) -> Unit,
  onShowMessage: (String) -> Unit,
  onPopulateStarterRoster: (grade: String, stream: String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val activeClass = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }
  val classDisplay = activeClass?.displayName ?: state.selectedGrade
  val activeStream = activeClass?.stream ?: "Delta"
  val gradeLearners = state.learners.filter { it.grade == state.selectedGrade }
  val levelAreas = KicdCurriculumRepository.cbcLearningAreasByLevel[KicdCurriculumRepository.levelForGrade(state.selectedGrade)].orEmpty()

  var selectedLearner by remember { mutableStateOf<Learner?>(null) }
  val savedComments = remember { mutableStateMapOf<String, String>() }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Report cards",
        subtitle = "$classDisplay · ${session.currentTerm}, ${session.currentYear}",
        onBackToServices = onBackToServices,
      )
    }

    if (gradeLearners.isEmpty()) {
      item {
        EmptyClassRosterCard(state.selectedGrade) { onPopulateStarterRoster(state.selectedGrade, activeStream) }
      }
    } else {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            gradeLearners.forEachIndexed { idx, l ->
              if (idx > 0) HorizontalDivider(color = extra.line)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedLearner = l }
                  .padding(vertical = 10.dp)
                  .testTag("report_card_row_${l.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp, bottomEnd = 13.dp, bottomStart = 4.dp))
                    .background(MaterialTheme.colorScheme.background),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(learnerInitials(l.fullName), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(l.fullName, style = MaterialTheme.typography.titleSmall)
                  Text("Adm. ${l.admissionNo}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open report card", tint = extra.muted)
              }
            }
          }
        }
      }
    }
  }

  selectedLearner?.let { learner ->
    val learnerMarks = state.marks.filter { it.learnerId == learner.id }
    val defaultComment = savedComments[learner.id] ?: "Consistent effort and steady CBC competency growth."
    var commentText by remember(learner.id) { mutableStateOf(defaultComment) }

    ModalBottomSheet(
      onDismissRequest = { selectedLearner = null },
      containerColor = MaterialTheme.colorScheme.background,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(learner.fullName, style = MaterialTheme.typography.headlineMedium)
        Text("$classDisplay · ${session.currentTerm}, ${session.currentYear} · Attendance ${learner.termAttendancePct}%", style = MaterialTheme.typography.bodySmall, color = extra.muted)

        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            levelAreas.forEachIndexed { idx, area ->
              if (idx > 0) HorizontalDivider(color = extra.line)
              val mark = learnerMarks.firstOrNull { it.learningArea == area }
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(area, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (mark != null) {
                  Text("${mark.score.toInt()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  CbcRubricBadge(mark.level)
                } else {
                  Text("–", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = commentText,
          onValueChange = { commentText = it },
          label = { Text("Class teacher's comment") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedButton(
          onClick = {
            savedComments[learner.id] = commentText
            onShowMessage("Comment saved to this report card")
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Text("Save comment", fontWeight = FontWeight.Bold)
        }

        HighlighterButton(
          text = "Export Report Card PDF",
          icon = Icons.Filled.PictureAsPdf,
          onClick = {
            savedComments[learner.id] = commentText
            selectedLearner = null
            onExportLearnerPdf(learner, commentText)
          },
          modifier = Modifier.fillMaxWidth().testTag("export_progress_pdf_${learner.id}"),
        )
        Spacer(Modifier.height(14.dp))
      }
    }
  }
}

@Composable
fun BroadsheetScreen(
  state: TeacherUiState,
  onPopulateStarterRoster: (grade: String, stream: String) -> Unit,
  onShowMessage: (String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val activeClass = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }
  val classDisplay = activeClass?.displayName ?: state.selectedGrade
  val activeStream = activeClass?.stream ?: "Delta"
  val gradeLearners = state.learners.filter { it.grade == state.selectedGrade }
  val level = KicdCurriculumRepository.levelForGrade(state.selectedGrade)
  val areas = KicdCurriculumRepository.cbcLearningAreasByLevel[level].orEmpty()
  var showMatrix by remember { mutableStateOf(false) }

  val ranked = gradeLearners.map { learner ->
    val lMarks = state.marks.filter { it.learnerId == learner.id }
    val points = lMarks.sumOf { it.level.points }
    val mean = if (lMarks.isEmpty()) 0 else lMarks.map { it.score }.average().toInt()
    Triple(learner, points, mean)
  }.sortedWith(compareByDescending<Triple<Learner, Int, Int>> { it.second }.thenByDescending { it.third })

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Broadsheet",
        subtitle = "$classDisplay · ranked on rubric points",
        onBackToServices = onBackToServices,
        action = {
          OutlinedButton(onClick = { showMatrix = !showMatrix }) {
            Text(if (showMatrix) "Ranked" else "Matrix")
          }
        },
      )
    }

    if (gradeLearners.isEmpty()) {
      item {
        EmptyClassRosterCard(state.selectedGrade) { onPopulateStarterRoster(state.selectedGrade, activeStream) }
      }
    } else if (!showMatrix) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            ranked.forEachIndexed { idx, (learner, points, mean) ->
              if (idx > 0) HorizontalDivider(color = extra.line)
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp, bottomEnd = 13.dp, bottomStart = 4.dp))
                    .background(MaterialTheme.colorScheme.background),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("${idx + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(learner.fullName, style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = if (points == 0 && mean == 0) "No scores recorded yet" else "$points points · mean $mean",
                    style = MaterialTheme.typography.bodySmall,
                    color = extra.muted,
                  )
                }
                if (mean > 0) {
                  CbcRubricBadge(CbcPerformanceLevel.fromScore(mean.toDouble()))
                }
              }
            }
          }
        }
      }

      item {
        OutlinedButton(
          onClick = { onShowMessage("Export to CSV is a school-plan feature. Share the broadsheet from the web admin portal.") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Text("Export to CSV", fontWeight = FontWeight.Bold)
        }
      }
    } else {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("LEARNER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
              areas.forEach { area ->
                Text(area.take(10), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(68.dp))
              }
              Text("MEAN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
            }
            HorizontalDivider(color = extra.line)
            ranked.forEach { (learner, _, mean) ->
              val lMarks = state.marks.filter { it.learnerId == learner.id }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Text(learner.fullName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(130.dp))
                areas.forEach { area ->
                  val m = lMarks.firstOrNull { it.learningArea == area }
                  val txt = if (m == null) "-" else "${m.score.toInt()}(${m.level.code})"
                  Text(txt, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(68.dp))
                }
                Text(
                  text = if (lMarks.isEmpty()) "-" else "$mean%",
                  style = MaterialTheme.typography.labelMedium,
                  color = extra.me,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.width(56.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun PathwaysScreen(
  state: TeacherUiState,
  onBackToServices: () -> Unit,
) {
  val extra = LocalElimuColors.current
  val isGrade9 = state.selectedGrade.startsWith("Grade 9")
  val gradeLearners = state.learners.filter { it.grade == state.selectedGrade }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Pathways",
        subtitle = if (isGrade9) "${state.selectedGrade} · starting points for a conversation" else "Grade 9 recommendations",
        onBackToServices = onBackToServices,
      )
    }

    if (!isGrade9) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.5.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Pathways apply to Grade 9 learners. ${state.selectedGrade} isn't one. Switch to a Grade 9 class at the top if you teach one.",
            style = MaterialTheme.typography.bodyMedium,
            color = extra.muted,
            modifier = Modifier.padding(18.dp),
          )
        }
      }
    } else if (gradeLearners.isNotEmpty()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            gradeLearners.forEachIndexed { idx, l ->
              if (idx > 0) HorizontalDivider(color = extra.line)
              val lMarks = state.marks.filter { it.learnerId == l.id }
              val stemAvg = lMarks.filter { it.learningArea.contains("Math") || it.learningArea.contains("Science") || it.learningArea.contains("Agri") || it.learningArea.contains("Technical") }
                .map { it.score }.average().takeIf { !it.isNaN() }
              val socialAvg = lMarks.filter { it.learningArea.contains("Social") || it.learningArea.contains("English") || it.learningArea.contains("Kiswahili") || it.learningArea.contains("Religious") }
                .map { it.score }.average().takeIf { !it.isNaN() }
              val artsAvg = lMarks.filter { it.learningArea.contains("Creative") }
                .map { it.score }.average().takeIf { !it.isNaN() }

              val best = listOfNotNull(
                stemAvg?.let { "STEM" to it },
                socialAvg?.let { "Social Sciences" to it },
                artsAvg?.let { "Arts & Sports Science" to it },
              ).maxByOrNull { it.second } ?: (l.pathwayInterest to null)

              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp, bottomEnd = 13.dp, bottomStart = 4.dp))
                    .background(MaterialTheme.colorScheme.background),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(learnerInitials(l.fullName), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(l.fullName, style = MaterialTheme.typography.titleSmall)
                  Text("Strongest in ${best.first}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                }
                if (best.second != null) {
                  Surface(color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, extra.line), shape = RoundedCornerShape(99.dp)) {
                    Text("${best.second!!.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                  }
                }
              }
            }
          }
        }
      }
      item {
        Text(
          text = "Recommendations support a talk with each learner and family. They don't decide placement.",
          style = MaterialTheme.typography.bodySmall,
          color = extra.muted,
        )
      }
    }

    item {
      Text("KICD Senior School Pathways", style = MaterialTheme.typography.titleMedium)
    }

    items(KicdCurriculumRepository.seniorPathways, key = { it.id }) { p ->
      val interested = state.learners.count { p.name.contains(it.pathwayInterest, ignoreCase = true) }
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.AccountTree, contentDescription = null, tint = extra.ae)
            Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, extra.line), shape = RoundedCornerShape(99.dp)) {
              Text("$interested learners", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
          }
          Text("Tracks: ${p.tracks.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall)
          Text("Core Areas: ${p.coreLearningAreas.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
          Text("Careers: ${p.careerExamples.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = extra.me)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KicdDesignsScreen(
  state: TeacherUiState,
  onSelectGrade: (String) -> Unit,
  onUseInScheme: (KicdDesignItem) -> Unit,
  onUseInLessonPlan: (KicdDesignItem) -> Unit,
  onBackToServices: () -> Unit,
) {
  val extra = LocalElimuColors.current
  var searchQuery by remember { mutableStateOf("") }
  var expandedId by remember { mutableStateOf<String?>(null) }

  val designs = KicdCurriculumRepository.kicdDesigns.filter { item ->
    val matchesGrade = searchQuery.isNotBlank() || item.grade == state.selectedGrade
    val matchesSearch = searchQuery.isBlank() ||
      item.learningArea.contains(searchQuery, ignoreCase = true) ||
      item.strand.contains(searchQuery, ignoreCase = true) ||
      item.subStrand.contains(searchQuery, ignoreCase = true) ||
      item.grade.contains(searchQuery, ignoreCase = true)
    matchesGrade && matchesSearch
  }.ifEmpty {
    KicdCurriculumRepository.kicdDesigns.filter {
      searchQuery.isBlank() || it.learningArea.contains(searchQuery, ignoreCase = true) || it.subStrand.contains(searchQuery, ignoreCase = true)
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "KICD designs",
        subtitle = "${state.selectedGrade} · Curriculum design library",
        onBackToServices = onBackToServices,
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text("Search learning area, strand, or grade") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("kicd_search_input"),
      )
      Spacer(Modifier.height(8.dp))
      GradeSelectorRow(KicdCurriculumRepository.cbcGrades, state.selectedGrade, onSelectGrade)
    }

    items(designs, key = { it.id }) { design ->
      val expanded = expandedId == design.id || designs.size <= 2
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { expandedId = if (expandedId == design.id) null else design.id },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${design.grade} · ${design.learningArea}", style = MaterialTheme.typography.labelLarge, color = extra.me)
                Surface(color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, extra.line), shape = RoundedCornerShape(99.dp)) {
                  Text(design.sourceType, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
              }
              Text("${design.strand}: ${design.subStrand}", style = MaterialTheme.typography.titleMedium)
            }
            Icon(
              imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
              contentDescription = "Toggle details",
              tint = extra.muted,
            )
          }

          if (expanded) {
            HorizontalDivider(color = extra.line)
            Text("Outcomes.", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            design.specificLearningOutcomes.forEach { outcome ->
              Text("• $outcome", style = MaterialTheme.typography.bodySmall, color = extra.muted)
            }
            Text("Experiences.", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(design.learningExperiences.joinToString(" "), style = MaterialTheme.typography.bodySmall, color = extra.muted)
            Text("Key inquiry question.", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(design.keyInquiryQuestions.joinToString(" | "), style = MaterialTheme.typography.bodySmall, color = extra.muted)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              design.coreCompetencies.forEach { comp ->
                AssistChip(onClick = {}, label = { Text(comp, style = MaterialTheme.typography.labelSmall) })
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              HighlighterButton(
                text = "Use in Scheme",
                onClick = { onUseInScheme(design) },
                modifier = Modifier.weight(1f).testTag("kicd_use_scheme_${design.id}"),
              )
              OutlinedButton(
                onClick = { onUseInLessonPlan(design) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("kicd_use_plan_${design.id}"),
              ) {
                Text("Lesson Plan", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    item {
      Text("Approved KICD Textbooks & Guides", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
    }

    items(KicdCurriculumRepository.approvedResources, key = { it.id }) { res ->
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.BookmarkAdded, contentDescription = null, tint = extra.me, modifier = Modifier.size(26.dp))
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(res.title, style = MaterialTheme.typography.titleSmall)
            Text("${res.publisher} · ${res.grade} · ${res.learningArea}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
            Text("${res.resourceType} · ${res.kicdApprovalCode}", style = MaterialTheme.typography.labelSmall, color = extra.me)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeePaymentsScreen(
  state: TeacherUiState,
  onPostFeePayment: (learner: Learner, amountPaid: Double) -> Unit,
  onPopulateStarterRoster: (grade: String, stream: String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val activeClass = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }
  val classDisplay = activeClass?.displayName ?: state.selectedGrade
  val activeStream = activeClass?.stream ?: "Delta"
  val learners = state.learners.filter { it.grade == state.selectedGrade }

  val totalOutstanding = learners.sumOf { it.feeBalanceKes }.toInt()
  val totalCollected = learners.sumOf { (it.totalFeeKes - it.feeBalanceKes).coerceAtLeast(0.0) }.toInt()
  var selectedLearnerForPayment by remember { mutableStateOf<Learner?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Fee payments",
        subtitle = classDisplay,
        onBackToServices = onBackToServices,
      )
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(String.format("%,d", totalCollected), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Collected (KES)", style = MaterialTheme.typography.bodySmall, color = extra.muted)
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(String.format("%,d", totalOutstanding), style = MaterialTheme.typography.headlineMedium, color = extra.be, fontWeight = FontWeight.Bold)
            Text("Outstanding (KES)", style = MaterialTheme.typography.bodySmall, color = extra.muted)
          }
        }
      }
    }

    if (learners.isEmpty()) {
      item {
        EmptyClassRosterCard(state.selectedGrade) { onPopulateStarterRoster(state.selectedGrade, activeStream) }
      }
    } else {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            learners.forEachIndexed { idx, l ->
              if (idx > 0) HorizontalDivider(color = extra.line)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedLearnerForPayment = l }
                  .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp, bottomEnd = 13.dp, bottomStart = 4.dp))
                    .background(MaterialTheme.colorScheme.background),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(learnerInitials(l.fullName), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(l.fullName, style = MaterialTheme.typography.titleSmall)
                  Text("Adm. ${l.admissionNo}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                }
                val cleared = l.feeBalanceKes <= 0.0
                Surface(
                  color = MaterialTheme.colorScheme.background,
                  border = BorderStroke(1.dp, extra.line),
                  shape = RoundedCornerShape(99.dp),
                ) {
                  Text(
                    text = if (cleared) "Cleared" else "KES ${String.format("%,d", l.feeBalanceKes.toInt())}",
                    color = if (cleared) extra.ee else extra.be,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  selectedLearnerForPayment?.let { learner ->
    var amountInput by remember(learner.id) { mutableStateOf("") }
    ModalBottomSheet(
      onDismissRequest = { selectedLearnerForPayment = null },
      containerColor = MaterialTheme.colorScheme.background,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(learner.fullName, style = MaterialTheme.typography.headlineMedium)
        Text("Balance KES ${String.format("%,d", learner.feeBalanceKes.toInt())}", style = MaterialTheme.typography.bodyMedium, color = extra.muted)
        OutlinedTextField(
          value = amountInput,
          onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
          label = { Text("Amount received (KES)") },
          placeholder = { Text("For example, 5000") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        HighlighterButton(
          text = "Post payment",
          onClick = {
            val amount = amountInput.toDoubleOrNull() ?: 0.0
            onPostFeePayment(learner, amount)
            if (amount > 0) selectedLearnerForPayment = null
          },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppScreen(
  state: TeacherUiState,
  onSaveGuardianMessage: (learner: Learner, template: String, message: String) -> Unit,
  onPopulateStarterRoster: (grade: String, stream: String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val context = LocalContext.current
  val activeClass = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }
  val classDisplay = activeClass?.displayName ?: state.selectedGrade
  val activeStream = activeClass?.stream ?: "Delta"
  val gradeLearners = state.learners.filter { it.grade == state.selectedGrade }
  val savedMessages = state.guardianMessages.filter { it.grade == state.selectedGrade }

  val templates = listOf("Fee reminder", "Absence check", "Progress update")
  var selectedLearnerId by remember(state.selectedGrade, gradeLearners) { mutableStateOf(gradeLearners.firstOrNull()?.id) }
  var selectedTemplate by remember { mutableStateOf(templates.first()) }
  var messageText by remember { mutableStateOf("") }

  fun defaultTextForTemplate(tpl: String, learner: Learner): String = when (tpl) {
    "Fee reminder" -> "Hello guardian of ${learner.fullName}, this is a kind reminder from ${session.schoolName} regarding the outstanding balance of KES ${learner.feeBalanceKes.toInt()} for ${session.currentTerm}."
    "Absence check" -> "Hello guardian of ${learner.fullName}, we noticed ${learner.fullName} missed today's lesson in $classDisplay. Kindly let us know how they are doing."
    else -> "Hello guardian of ${learner.fullName}, we are pleased to share a positive CBC progress update for ${learner.fullName} in $classDisplay."
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "WhatsApp",
        subtitle = "Message a learner's guardian",
        onBackToServices = onBackToServices,
      )
    }

    if (gradeLearners.isEmpty()) {
      item {
        EmptyClassRosterCard(state.selectedGrade) { onPopulateStarterRoster(state.selectedGrade, activeStream) }
      }
    } else {
      item {
        val currentLearner = gradeLearners.firstOrNull { it.id == selectedLearnerId } ?: gradeLearners.first()
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Learner", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              gradeLearners.forEach { l ->
                FilterChip(
                  selected = currentLearner.id == l.id,
                  onClick = {
                    selectedLearnerId = l.id
                    if (messageText.isBlank()) messageText = defaultTextForTemplate(selectedTemplate, l)
                  },
                  label = { Text(l.fullName) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                  ),
                  shape = RoundedCornerShape(99.dp),
                )
              }
            }

            Text("Starter", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              templates.forEach { tpl ->
                FilterChip(
                  selected = selectedTemplate == tpl,
                  onClick = {
                    selectedTemplate = tpl
                    messageText = defaultTextForTemplate(tpl, currentLearner)
                  },
                  label = { Text(tpl) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                  ),
                  shape = RoundedCornerShape(99.dp),
                )
              }
            }

            OutlinedTextField(
              value = messageText,
              onValueChange = { messageText = it },
              label = { Text("Message") },
              minLines = 3,
              modifier = Modifier.fillMaxWidth(),
            )

            HighlighterButton(
              text = "Send message",
              icon = Icons.AutoMirrored.Filled.Chat,
              onClick = {
                val finalMsg = messageText.ifBlank { defaultTextForTemplate(selectedTemplate, currentLearner) }
                onSaveGuardianMessage(currentLearner, selectedTemplate, finalMsg)
                val phoneDigits = currentLearner.guardianPhone.filter { it.isDigit() }
                runCatching {
                  val url = "https://wa.me/$phoneDigits?text=${Uri.encode(finalMsg)}"
                  context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }.onFailure {
                  runCatching {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_TEXT, finalMsg)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Send guardian message"))
                  }
                }
                messageText = ""
              },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }

      item {
        Text("Saved for $classDisplay", style = MaterialTheme.typography.titleMedium)
      }

      if (savedMessages.isEmpty()) {
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
        items(savedMessages, key = { it.id }) { m ->
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, extra.line),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("${m.learnerName} · ${m.template}", style = MaterialTheme.typography.titleSmall)
              Text(m.message, style = MaterialTheme.typography.bodySmall, color = extra.muted)
            }
          }
        }
      }
    }
  }
}
