package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.domain.model.TimetableSlot
import com.example.ui.components.GradeSelectorRow
import com.example.ui.components.HighlighterButton
import com.example.ui.components.SubScreenHeader
import com.example.ui.theme.LocalElimuColors
import com.example.ui.theme.TimetableBannerPeach
import com.example.ui.theme.TimetableBannerText
import com.example.ui.theme.TimetableBlueHeader
import com.example.ui.theme.TimetableCellCream
import com.example.ui.viewmodel.TeacherUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimetableScreen(
  state: TeacherUiState,
  onSelectGrade: (String) -> Unit,
  onUpdateSlot: (TimetableSlot) -> Unit,
  onResetGradeTimetable: (String) -> Unit,
  onExportTimetablePdf: (String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  var showFullGrid by remember { mutableStateOf(false) }
  var isEditMode by remember { mutableStateOf(false) }
  var selectedDay by remember { mutableStateOf("MON") }
  var editingSlot by remember { mutableStateOf<TimetableSlot?>(null) }

  val days = listOf("MON" to "Mon", "TUE" to "Tue", "WED" to "Wed", "THU" to "Thu", "FRI" to "Fri")
  val gradeSlots = state.timetable.filter { it.grade == state.selectedGrade }
  val activeClass = session.allocatedClasses.firstOrNull { it.grade == state.selectedGrade }
  val classDisplay = activeClass?.displayName ?: state.selectedGrade

  val level = KicdCurriculumRepository.levelForGrade(state.selectedGrade)
  val levelAreas = KicdCurriculumRepository.cbcLearningAreasByLevel[level].orEmpty()
  val expectedTimeSlots = KicdCurriculumRepository.timeSlotsForGrade(state.selectedGrade)
  val formatTitle = KicdCurriculumRepository.timetableFormatTitle(state.selectedGrade)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Timetable",
        subtitle = "$classDisplay · $formatTitle",
        onBackToServices = onBackToServices,
      )
      Spacer(Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = { showFullGrid = !showFullGrid },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.height(44.dp).testTag("timetable_view_toggle"),
        ) {
          Icon(
            imageVector = if (showFullGrid) Icons.Filled.ViewAgenda else Icons.Filled.TableChart,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(if (showFullGrid) "Day list" else "Full grid")
        }

        OutlinedButton(
          onClick = { isEditMode = !isEditMode },
          colors = if (isEditMode) {
            ButtonDefaults.outlinedButtonColors(
              containerColor = extra.navy,
              contentColor = Color.White,
            )
          } else {
            ButtonDefaults.outlinedButtonColors()
          },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.height(44.dp).testTag("timetable_edit_toggle"),
        ) {
          Icon(
            imageVector = if (isEditMode) Icons.Filled.Check else Icons.Filled.Edit,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(if (isEditMode) "Done editing" else "Edit timetable")
        }

        Button(
          onClick = { onExportTimetablePdf(state.selectedGrade) },
          colors = ButtonDefaults.buttonColors(containerColor = extra.highlighter, contentColor = extra.highlighterInk),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier.height(44.dp).testTag("timetable_export_pdf_button"),
        ) {
          Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("PDF", fontWeight = FontWeight.Bold)
        }
      }
    }

    item {
      GradeSelectorRow(KicdCurriculumRepository.cbcGrades, state.selectedGrade, onSelectGrade)
    }

    // Format badge card clarifying Lower vs Upper/Junior structure
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isEditMode) extra.highlighter else extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = when (level) {
                "Lower Primary" -> "Lower Primary Format · ${expectedTimeSlots.size} slots (Morning only)"
                "Upper Primary" -> "Upper Primary Format · ${expectedTimeSlots.size} slots (Includes Lunch & Afternoon)"
                else -> "Junior School Format · ${expectedTimeSlots.size} slots (Includes Lunch & Afternoon)"
              },
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = if (isEditMode) "Tap any period below to edit subject, break, time or venue." else "Tap Edit timetable or any slot to customize lessons.",
              style = MaterialTheme.typography.bodySmall,
              color = extra.muted,
            )
          }
          if (isEditMode) {
            TextButton(onClick = { onResetGradeTimetable(state.selectedGrade) }) {
              Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Reset")
            }
          }
        }
      }
    }

    if (!showFullGrid) {
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          days.forEach { (code, label) ->
            FilterChip(
              selected = selectedDay == code,
              onClick = { selectedDay = code },
              label = { Text(label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                selectedLabelColor = MaterialTheme.colorScheme.background,
              ),
              shape = RoundedCornerShape(99.dp),
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      item {
        val daySlots = gradeSlots.filter { it.dayOfWeek == selectedDay }.sortedBy { it.slotIndex }
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            expectedTimeSlots.forEachIndexed { idx, (slotIdx, defaultTime) ->
              if (idx > 0) HorizontalDivider(color = extra.line)
              val slot = daySlots.firstOrNull { it.slotIndex == slotIdx } ?: TimetableSlot(
                id = "${state.selectedGrade}_${selectedDay}_$slotIdx",
                grade = state.selectedGrade,
                dayOfWeek = selectedDay,
                slotIndex = slotIdx,
                timeRange = defaultTime,
                learningArea = "FREE PERIOD",
                isBreak = false,
                room = "${state.selectedGrade} Room",
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { editingSlot = slot }
                  .padding(vertical = 10.dp)
                  .testTag("timetable_slot_${selectedDay}_$slotIdx"),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = slot.timeRange,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (slot.isBreak) extra.muted else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.width(92.dp),
                )
                if (slot.isBreak) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = slot.learningArea,
                      style = MaterialTheme.typography.labelLarge,
                      color = extra.ae,
                    )
                  }
                } else {
                  Row(modifier = Modifier.weight(1f).height(IntrinsicSize.Min)) {
                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(extra.me))
                    Spacer(Modifier.width(9.dp))
                    Column {
                      Text(slot.learningArea, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      Text(
                        text = if (slot.room.isNotBlank()) "$classDisplay · ${slot.room}" else classDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = extra.muted,
                      )
                    }
                  }
                }
                if (isEditMode) {
                  Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit slot",
                    tint = extra.highlighter,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }
      }
    } else {
      // Full horizontal grid differentiating Lower (8 slots) vs Upper Primary (11 slots) vs Junior School (12 slots)
      item {
        Surface(
          color = Color.White,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Box(
              modifier = Modifier.fillMaxWidth().background(TimetableBannerPeach).padding(vertical = 10.dp, horizontal = 12.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = formatTitle,
                color = TimetableBannerText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
              )
            }
            Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
              val dayWidth = 58.dp
              val cellWidth = 118.dp
              val borderColor = Color(0xFF94A3B8)
              Row {
                Box(
                  modifier = Modifier.width(dayWidth).height(34.dp).background(TimetableBlueHeader).border(0.5.dp, borderColor),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("DAY", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E3A8A))
                }
                expectedTimeSlots.forEach { (idx, defaultRange) ->
                  val headerTime = gradeSlots.firstOrNull { it.slotIndex == idx }?.timeRange ?: defaultRange
                  Box(
                    modifier = Modifier.width(cellWidth).height(34.dp).background(TimetableBlueHeader).border(0.5.dp, borderColor),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(headerTime, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E3A8A))
                  }
                }
              }
              days.forEach { (code, _) ->
                val daySlots = gradeSlots.filter { it.dayOfWeek == code }.sortedBy { it.slotIndex }
                Row {
                  Box(
                    modifier = Modifier.width(dayWidth).height(64.dp).background(Color(0xFFF1F5F9)).border(0.5.dp, borderColor),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(code, color = extra.penRed, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  }
                  expectedTimeSlots.forEach { (i, defaultRange) ->
                    val slot = daySlots.firstOrNull { it.slotIndex == i } ?: TimetableSlot(
                      id = "${state.selectedGrade}_${code}_$i",
                      grade = state.selectedGrade,
                      dayOfWeek = code,
                      slotIndex = i,
                      timeRange = defaultRange,
                      learningArea = "-",
                      isBreak = false,
                      room = "${state.selectedGrade} Room",
                    )
                    val isBreak = slot.isBreak
                    Box(
                      modifier = Modifier
                        .width(cellWidth)
                        .height(64.dp)
                        .background(if (isBreak) Color(0xFFD4DAE4) else TimetableCellCream)
                        .border(0.5.dp, borderColor)
                        .clickable { editingSlot = slot }
                        .padding(6.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = slot.learningArea,
                        color = Color(0xFF0F172A),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isBreak) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  editingSlot?.let { slot ->
    var subjectText by remember(slot.id) { mutableStateOf(slot.learningArea) }
    var timeRangeText by remember(slot.id) { mutableStateOf(slot.timeRange) }
    var roomText by remember(slot.id) { mutableStateOf(slot.room) }
    var isBreakSlot by remember(slot.id) { mutableStateOf(slot.isBreak) }

    val quickOptions = levelAreas.map { it.uppercase() } + listOf(
      "GAMES & SPORTS",
      "CLUBS & SOCIETIES",
      "PREP / GUIDED STUDY",
      "FREE PERIOD",
      "BREAK",
      "LUNCH BREAK",
    )

    ModalBottomSheet(
      onDismissRequest = { editingSlot = null },
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
        Text("Edit period · ${slot.dayOfWeek}", style = MaterialTheme.typography.headlineMedium)
        Text("${slot.grade} · Period ${slot.slotIndex + 1}", style = MaterialTheme.typography.bodySmall, color = extra.muted)

        OutlinedTextField(
          value = timeRangeText,
          onValueChange = { timeRangeText = it },
          label = { Text("Time range") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("Break / Lunch period", style = MaterialTheme.typography.titleSmall)
            Text("Mark this slot as BREAK or LUNCH BREAK", style = MaterialTheme.typography.bodySmall, color = extra.muted)
          }
          Switch(
            checked = isBreakSlot,
            onCheckedChange = { checked ->
              isBreakSlot = checked
              if (checked && !subjectText.contains("BREAK", ignoreCase = true)) {
                subjectText = "BREAK"
              } else if (!checked && subjectText.contains("BREAK", ignoreCase = true)) {
                subjectText = levelAreas.firstOrNull()?.uppercase() ?: "MATHEMATICS"
              }
            },
          )
        }

        Text("Pick KICD learning area or activity", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          quickOptions.forEach { option ->
            val selected = subjectText.equals(option, ignoreCase = true)
            FilterChip(
              selected = selected,
              onClick = {
                subjectText = option
                isBreakSlot = option == "BREAK" || option == "LUNCH BREAK"
              },
              label = { Text(option, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                selectedLabelColor = MaterialTheme.colorScheme.background,
              ),
              shape = RoundedCornerShape(99.dp),
            )
          }
        }

        OutlinedTextField(
          value = subjectText,
          onValueChange = { subjectText = it },
          label = { Text("Subject / Activity title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("edit_slot_subject_input"),
        )

        if (!isBreakSlot) {
          OutlinedTextField(
            value = roomText,
            onValueChange = { roomText = it },
            label = { Text("Room / Venue") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        HighlighterButton(
          text = "Save period",
          onClick = {
            onUpdateSlot(
              slot.copy(
                timeRange = timeRangeText.trim().ifBlank { slot.timeRange },
                learningArea = subjectText.trim().ifBlank { if (isBreakSlot) "BREAK" else "FREE PERIOD" },
                isBreak = isBreakSlot,
                room = if (isBreakSlot) "" else roomText.trim(),
              )
            )
            editingSlot = null
          },
          modifier = Modifier.fillMaxWidth().testTag("save_timetable_slot_button"),
        )
        Spacer(Modifier.height(14.dp))
      }
    }
  }
}
