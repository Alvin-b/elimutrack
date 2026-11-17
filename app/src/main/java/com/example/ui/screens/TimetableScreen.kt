package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.domain.model.TimetableSlot
import com.example.ui.components.SubScreenHeader
import com.example.ui.theme.LocalElimuColors
import com.example.ui.theme.TimetableBannerPeach
import com.example.ui.theme.TimetableBannerText
import com.example.ui.theme.TimetableBlueHeader
import com.example.ui.theme.TimetableCellCream
import com.example.ui.viewmodel.TeacherUiState

@Composable
fun TimetableScreen(
  state: TeacherUiState,
  onSelectStream: (String) -> Unit,
  onRefresh: () -> Unit,
  onExportTimetablePdf: (String) -> Unit,
  onBackToServices: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  var showFullGrid by remember { mutableStateOf(false) }
  var selectedDay by remember { mutableStateOf("MON") }

  val days = listOf("MON" to "Mon", "TUE" to "Tue", "WED" to "Wed", "THU" to "Thu", "FRI" to "Fri")
  val activeClass = state.activeClass
  val classDisplay = activeClass?.displayName ?: state.selectedStream.ifBlank { state.selectedGrade }

  val gradeSlots = state.timetable.filter { slot ->
    slot.room.equals(state.selectedStream, ignoreCase = true) ||
      slot.grade.equals(state.selectedGrade, ignoreCase = true)
  }

  val level = KicdCurriculumRepository.levelForGrade(state.selectedGrade)
  val expectedTimeSlots = remember(gradeSlots, state.selectedGrade) {
    val fromServer = gradeSlots
      .distinctBy { it.slotIndex }
      .sortedBy { it.slotIndex }
      .map { it.slotIndex to it.timeRange }
    fromServer.ifEmpty { KicdCurriculumRepository.timeSlotsForGrade(state.selectedGrade) }
  }
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
          onClick = onRefresh,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.height(44.dp),
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Refresh")
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

    if (session.allocatedClasses.size > 1) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          session.allocatedClasses.forEach { cls ->
            val selected = cls.streamKey.equals(state.selectedStream, ignoreCase = true)
            FilterChip(
              selected = selected,
              onClick = { onSelectStream(cls.streamKey) },
              label = { Text(cls.displayName, style = MaterialTheme.typography.labelLarge) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                selectedLabelColor = MaterialTheme.colorScheme.background,
              ),
              shape = RoundedCornerShape(99.dp),
            )
          }
        }
      }
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = when (level) {
              "Lower Primary" -> "Lower Primary Format · ${expectedTimeSlots.size} slots (Morning only)"
              "Upper Primary" -> "Upper Primary Format · ${expectedTimeSlots.size} slots (Includes Lunch & Afternoon)"
              else -> "Junior School Format · ${expectedTimeSlots.size} slots (Includes Lunch & Afternoon)"
            },
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = "Published by your school administrator (read-only).",
            style = MaterialTheme.typography.bodySmall,
            color = extra.muted,
          )
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
          if (daySlots.isEmpty() && gradeSlots.isEmpty()) {
            Text(
              text = "No timetable entries published for $classDisplay yet.",
              style = MaterialTheme.typography.bodyMedium,
              color = extra.muted,
              modifier = Modifier.padding(18.dp),
            )
          } else {
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
                  room = classDisplay,
                )
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
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
                          text = if (slot.room.isNotBlank()) slot.room else classDisplay,
                          style = MaterialTheme.typography.bodySmall,
                          color = extra.muted,
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
    } else {
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
                      room = classDisplay,
                    )
                    val isBreak = slot.isBreak
                    Box(
                      modifier = Modifier
                        .width(cellWidth)
                        .height(64.dp)
                        .background(if (isBreak) Color(0xFFD4DAE4) else TimetableCellCream)
                        .border(0.5.dp, borderColor)
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
}
