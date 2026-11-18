package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.CbcPerformanceLevel
import com.example.ui.components.CbcRubricBadge
import com.example.ui.components.HighlighterButton
import com.example.ui.components.SubScreenHeader
import com.example.ui.theme.LocalElimuColors
import com.example.ui.viewmodel.TeacherUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun initialsOf(name: String): String =
  name.trim().split(" ").filter { it.isNotBlank() }.mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")

private data class WeekDayInfo(
  val shortDay: String,
  val dayInitial: String,
  val dateStr: String,
  val dayOfMonth: String,
)

private fun termAnchorMonday(term: String, year: Int): Calendar {
  val month = when {
    term.contains("1") -> Calendar.JANUARY
    term.contains("2") -> Calendar.MAY
    else -> Calendar.SEPTEMBER
  }
  return Calendar.getInstance().apply {
    clear()
    set(year, month, 1, 0, 0, 0)
    while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
      add(Calendar.DAY_OF_MONTH, 1)
    }
  }
}

private fun weekDaysForTermWeek(term: String, year: Int, weekNumber: Int): List<WeekDayInfo> {
  val fmtIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  val fmtDayNum = SimpleDateFormat("d", Locale.US)
  val cal = termAnchorMonday(term, year).apply {
    add(Calendar.WEEK_OF_YEAR, (weekNumber - 1).coerceIn(0, 14))
  }
  val labels = listOf("Mon" to "M", "Tue" to "T", "Wed" to "W", "Thu" to "T", "Fri" to "F")
  return labels.map { (short, initial) ->
    val info = WeekDayInfo(
      shortDay = short,
      dayInitial = initial,
      dateStr = fmtIso.format(cal.time),
      dayOfMonth = fmtDayNum.format(cal.time),
    )
    cal.add(Calendar.DAY_OF_MONTH, 1)
    info
  }
}

private fun initialWeekForToday(term: String, year: Int): Int {
  val anchor = termAnchorMonday(term, year).timeInMillis
  val now = Calendar.getInstance().timeInMillis
  val diffWeeks = ((now - anchor) / (7L * 24L * 3600L * 1000L)).toInt() + 1
  return diffWeeks.coerceIn(1, 12)
}

private fun formatReadableDate(isoDate: String): String {
  val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  val outFmt = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
  return runCatching { outFmt.format(inFmt.parse(isoDate) ?: Date()) }.getOrDefault(isoDate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
  state: TeacherUiState,
  onFetchAttendanceForDay: (String) -> Unit,
  onSaveAttendance: (date: String, grade: String, learningArea: String, lessonSlot: String, statuses: Map<String, Pair<AttendanceStatus, String>>) -> Unit,
  onRefreshRoster: () -> Unit,
  onBackToServices: (() -> Unit)? = null,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val classDisplay = state.activeClass?.displayName ?: state.selectedStream.ifBlank { state.selectedGrade }

  val classLearners = state.learners.filter { state.matchesActiveClass(it.stream, it.grade) }
  val sortedLearners = remember(classLearners) {
    classLearners.sortedBy { it.fullName.lowercase(Locale.getDefault()) }
  }

  val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
  val defaultWeek = remember(session.currentTerm, session.currentYear) {
    initialWeekForToday(session.currentTerm, session.currentYear)
  }

  var selectedWeek by remember { mutableIntStateOf(defaultWeek) }
  val weekDays = remember(session.currentTerm, session.currentYear, selectedWeek) {
    weekDaysForTermWeek(session.currentTerm, session.currentYear, selectedWeek)
  }
  var selectedDate by remember {
    mutableStateOf(
      if (weekDays.any { it.dateStr == todayIso }) todayIso else weekDays.first().dateStr
    )
  }
  var showDatePicker by remember { mutableStateOf(false) }
  var isMarkingMode by remember { mutableStateOf(false) }
  var rollCallStartIndex by remember { mutableIntStateOf(0) }

  LaunchedEffect(state.selectedStream, selectedDate) {
    onFetchAttendanceForDay(selectedDate)
  }

  val gradeAttendanceByKey = remember(state.attendance, state.selectedStream, state.selectedGrade) {
    val map = HashMap<Pair<String, String>, AttendanceStatus>()
    state.attendance.filter { state.matchesActiveClass(it.stream, it.grade) }.forEach { rec ->
      map.putIfAbsent(rec.learnerId to rec.date, rec.status)
    }
    map
  }

  if (isMarkingMode && sortedLearners.isNotEmpty()) {
    BackHandler { isMarkingMode = false }

    val statusMap = remember(state.selectedStream, selectedDate, sortedLearners) {
      mutableStateMapOf<String, AttendanceStatus?>().apply {
        sortedLearners.forEach { learner ->
          put(learner.id, gradeAttendanceByKey[learner.id to selectedDate])
        }
      }
    }

    var activeIndex by remember(selectedDate, sortedLearners, rollCallStartIndex) {
      mutableIntStateOf(
        rollCallStartIndex.coerceIn(0, sortedLearners.lastIndex)
      )
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(rollCallStartIndex, selectedDate) {
      if (rollCallStartIndex in sortedLearners.indices) {
        listState.scrollToItem(rollCallStartIndex)
      }
    }

    val presentCount = statusMap.values.count { it == AttendanceStatus.PRESENT || it == AttendanceStatus.LATE }
    val absentCount = statusMap.values.count { it == AttendanceStatus.ABSENT }
    val markedCount = presentCount + absentCount

    Column(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { isMarkingMode = false }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to register",
            tint = extra.muted,
            modifier = Modifier.size(18.dp),
          )
          Text("Back to register", style = MaterialTheme.typography.labelLarge, color = extra.muted)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Mark attendance", style = MaterialTheme.typography.headlineMedium)
            Text(
              text = "$classDisplay · ${formatReadableDate(selectedDate)} (A–Z)",
              style = MaterialTheme.typography.bodySmall,
              color = extra.muted,
            )
          }
          OutlinedButton(
            onClick = {
              sortedLearners.forEach { l -> statusMap[l.id] = AttendanceStatus.PRESENT }
              activeIndex = sortedLearners.lastIndex
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("mark_all_present_button"),
          ) {
            Text("All present")
          }
        }

        LinearProgressIndicator(
          progress = { if (sortedLearners.isEmpty()) 0f else markedCount.toFloat() / sortedLearners.size.toFloat() },
          color = extra.ee,
          trackColor = extra.line,
          modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
        )
        Text(
          text = "Marked $markedCount of ${sortedLearners.size} · Tap Present or Absent to advance (scroll freely to edit any learner)",
          style = MaterialTheme.typography.bodySmall,
          color = extra.muted,
        )
      }

      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        itemsIndexed(sortedLearners, key = { _, l -> l.id }) { idx, learner ->
          val currentStatus = statusMap[learner.id]
          val isCurrent = idx == activeIndex

          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) extra.highlighter else extra.line),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { activeIndex = idx }
              .testTag("rollcall_card_${learner.id}"),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 4.dp))
                    .background(if (isCurrent) extra.highlighter else MaterialTheme.colorScheme.background),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "${idx + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) extra.highlighterInk else MaterialTheme.colorScheme.onSurface,
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(learner.fullName, style = MaterialTheme.typography.titleMedium)
                  Text("Adm. ${learner.admissionNo}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                }
                if (currentStatus == AttendanceStatus.PRESENT) {
                  Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(extra.ee),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      Icons.Filled.Check,
                      contentDescription = "Marked present",
                      tint = if (extra.ee.luminance() > 0.4f) Color(0xFF0A1424) else Color.White,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                } else if (currentStatus == AttendanceStatus.ABSENT) {
                  Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(extra.be),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      Icons.Filled.Close,
                      contentDescription = "Marked absent",
                      tint = if (extra.be.luminance() > 0.4f) Color(0xFF0A1424) else Color.White,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                val isPresent = currentStatus == AttendanceStatus.PRESENT
                val isAbsent = currentStatus == AttendanceStatus.ABSENT
                val presentFg = if (isPresent) {
                  if (extra.ee.luminance() > 0.4f) Color(0xFF0A1424) else Color.White
                } else extra.ee
                val absentFg = if (isAbsent) {
                  if (extra.be.luminance() > 0.4f) Color(0xFF0A1424) else Color.White
                } else extra.be

                Button(
                  onClick = {
                    statusMap[learner.id] = AttendanceStatus.PRESENT
                    if (idx < sortedLearners.lastIndex) {
                      val next = idx + 1
                      activeIndex = next
                      coroutineScope.launch {
                        delay(110)
                        listState.animateScrollToItem(next)
                      }
                    }
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPresent) extra.ee else extra.ee.copy(alpha = 0.14f),
                    contentColor = presentFg,
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("rollcall_present_${learner.id}"),
                ) {
                  Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Present", fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = {
                    statusMap[learner.id] = AttendanceStatus.ABSENT
                    if (idx < sortedLearners.lastIndex) {
                      val next = idx + 1
                      activeIndex = next
                      coroutineScope.launch {
                        delay(110)
                        listState.animateScrollToItem(next)
                      }
                    }
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAbsent) extra.be else extra.be.copy(alpha = 0.14f),
                    contentColor = absentFg,
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("rollcall_absent_${learner.id}"),
                ) {
                  Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Absent", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(modifier = Modifier.padding(16.dp)) {
          HighlighterButton(
            text = if (markedCount == sortedLearners.size) {
              "Save register ($markedCount/${sortedLearners.size})"
            } else {
              "Save register ($markedCount/${sortedLearners.size} marked)"
            },
            enabled = markedCount > 0,
            onClick = {
              val payload = sortedLearners.mapNotNull { l ->
                val st = statusMap[l.id] ?: return@mapNotNull null
                l.id to (st to "")
              }.toMap()
              onSaveAttendance(
                selectedDate,
                state.selectedGrade,
                "Class Register",
                "Daily",
                payload,
              )
              isMarkingMode = false
            },
            modifier = Modifier.fillMaxWidth().testTag("save_attendance_button"),
          )
        }
      }
    }
    return
  }

  val presentSelectedCount = sortedLearners.count {
    val st = gradeAttendanceByKey[it.id to selectedDate]
    st == AttendanceStatus.PRESENT || st == AttendanceStatus.LATE
  }
  val absentSelectedCount = sortedLearners.count {
    gradeAttendanceByKey[it.id to selectedDate] == AttendanceStatus.ABSENT
  }
  val unmarkedSelectedCount = (sortedLearners.size - presentSelectedCount - absentSelectedCount).coerceAtLeast(0)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Register",
        subtitle = "$classDisplay · Week $selectedWeek (${formatReadableDate(selectedDate)})",
        onBackToServices = onBackToServices,
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Term weeks", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          (1..12).forEach { weekNum ->
            val selected = weekNum == selectedWeek
            FilterChip(
              selected = selected,
              onClick = {
                selectedWeek = weekNum
                val newDays = weekDaysForTermWeek(session.currentTerm, session.currentYear, weekNum)
                selectedDate = newDays.first().dateStr
              },
              label = { Text("Week $weekNum") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = extra.navy,
                selectedLabelColor = Color.White,
              ),
              shape = RoundedCornerShape(99.dp),
              modifier = Modifier.testTag("register_week_$weekNum"),
            )
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        weekDays.forEach { day ->
          val active = selectedDate == day.dateStr
          FilterChip(
            selected = active,
            onClick = { selectedDate = day.dateStr },
            label = { Text("${day.shortDay} ${day.dayOfMonth}") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = extra.highlighter,
              selectedLabelColor = extra.highlighterInk,
            ),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.testTag("register_day_${day.dateStr}"),
          )
        }
        if (weekDays.none { it.dateStr == selectedDate }) {
          FilterChip(
            selected = true,
            onClick = { showDatePicker = true },
            label = { Text(formatReadableDate(selectedDate)) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = extra.highlighter,
              selectedLabelColor = extra.highlighterInk,
            ),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.testTag("register_day_custom"),
          )
        }
        OutlinedButton(
          onClick = { showDatePicker = true },
          shape = RoundedCornerShape(99.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.height(36.dp).testTag("register_pick_date_button"),
        ) {
          Icon(Icons.Filled.CalendarToday, contentDescription = "Choose date", modifier = Modifier.size(14.dp))
          Spacer(Modifier.width(5.dp))
          Text("Pick date", style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TallyBox(value = presentSelectedCount.toString(), label = "Present", valueColor = extra.ee, modifier = Modifier.weight(1f))
        TallyBox(value = absentSelectedCount.toString(), label = "Absent", valueColor = extra.be, modifier = Modifier.weight(1f))
        TallyBox(value = unmarkedSelectedCount.toString(), label = "Not marked", valueColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
      }
    }

    if (sortedLearners.isEmpty()) {
      item {
        EmptyClassRosterCard(
          classLabel = classDisplay,
          onRefresh = onRefreshRoster,
        )
      }
    } else {
      item {
        HighlighterButton(
          text = "Mark attendance",
          icon = Icons.Filled.HowToReg,
          onClick = {
            val firstUnmarked = sortedLearners.indexOfFirst { (it.id to selectedDate) !in gradeAttendanceByKey }
            rollCallStartIndex = if (firstUnmarked >= 0) firstUnmarked else 0
            isMarkingMode = true
          },
          modifier = Modifier.fillMaxWidth().testTag("open_mark_attendance_button"),
        )
      }

      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Learners (${sortedLearners.size}) · A–Z",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
              )
              weekDays.forEach { d ->
                val isSelectedDay = d.dateStr == selectedDate
                Box(
                  modifier = Modifier
                    .width(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelectedDay) extra.highlighter else Color.Transparent)
                    .clickable { selectedDate = d.dateStr }
                    .padding(vertical = 4.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = d.dayInitial,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelectedDay) extra.highlighterInk else extra.muted,
                  )
                }
              }
            }
            HorizontalDivider(color = extra.line)

            sortedLearners.forEachIndexed { idx, learner ->
              if (idx > 0) HorizontalDivider(color = extra.line)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    rollCallStartIndex = idx
                    isMarkingMode = true
                  }
                  .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp, bottomEnd = 11.dp, bottomStart = 4.dp))
                    .background(MaterialTheme.colorScheme.background),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(initialsOf(learner.fullName), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(learner.fullName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text("Adm. ${learner.admissionNo}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                }

                weekDays.forEach { d ->
                  val dayStatus = gradeAttendanceByKey[learner.id to d.dateStr]
                  val isSelectedDay = d.dateStr == selectedDate
                  Box(
                    modifier = Modifier
                      .width(30.dp)
                      .height(30.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (isSelectedDay) extra.highlighter.copy(alpha = 0.14f) else Color.Transparent),
                    contentAlignment = Alignment.Center,
                  ) {
                    when (dayStatus) {
                      AttendanceStatus.PRESENT, AttendanceStatus.LATE -> {
                        Box(
                          modifier = Modifier.size(22.dp).clip(CircleShape).background(extra.ee),
                          contentAlignment = Alignment.Center,
                        ) {
                          Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Present",
                            tint = if (extra.ee.luminance() > 0.4f) Color(0xFF0A1424) else Color.White,
                            modifier = Modifier.size(14.dp),
                          )
                        }
                      }
                      AttendanceStatus.ABSENT -> {
                        Box(
                          modifier = Modifier.size(22.dp).clip(CircleShape).background(extra.be),
                          contentAlignment = Alignment.Center,
                        ) {
                          Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Absent",
                            tint = if (extra.be.luminance() > 0.4f) Color(0xFF0A1424) else Color.White,
                            modifier = Modifier.size(14.dp),
                          )
                        }
                      }
                      else -> {
                        Text("–", style = MaterialTheme.typography.labelMedium, color = extra.muted)
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

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { utcMillis ->
              val utcFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
              }
              val pickedIso = utcFmt.format(Date(utcMillis))
              selectedDate = pickedIso
              val anchor = termAnchorMonday(session.currentTerm, session.currentYear).timeInMillis
              val diffWeeks = ((utcMillis - anchor) / (7L * 24L * 3600L * 1000L)).toInt() + 1
              if (diffWeeks in 1..12) {
                selectedWeek = diffWeeks
              }
            }
            showDatePicker = false
          },
        ) {
          Text("Use date")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }
}

@Composable
private fun TallyBox(
  value: String,
  label: String,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  val extra = LocalElimuColors.current
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, extra.line),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
      Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor, fontWeight = FontWeight.Bold)
      Text(label, style = MaterialTheme.typography.bodySmall, color = extra.muted, maxLines = 1)
    }
  }
}

@Composable
fun MarksbookScreen(
  state: TeacherUiState,
  onSaveMarksBatch: (learningAreaOrId: String, term: String, assessmentType: String, scores: Map<String, Double>) -> Unit,
  onRefreshRoster: () -> Unit,
  onBackToServices: (() -> Unit)? = null,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val classDisplay = state.activeClass?.displayName ?: state.selectedStream.ifBlank { state.selectedGrade }

  val classLearners = state.learners.filter { state.matchesActiveClass(it.stream, it.grade) }
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
  var selectedAssessment by remember { mutableStateOf("CAT 1") }
  val assessmentTypes = listOf("CAT 1", "Mid-Term", "End-Term", "SBA")

  val scoreInputs = remember(state.selectedStream, selectedSubjectId, selectedAssessment, classLearners, state.marks) {
    mutableStateMapOf<String, String>().apply {
      classLearners.forEach { l ->
        val existing = state.marks.firstOrNull { m ->
          m.learnerId == l.id &&
            (KicdCurriculumRepository.subjectIdForArea(state.selectedGrade, m.learningArea) == selectedSubjectId ||
              m.learningArea.equals(selectedAreaName, ignoreCase = true))
        }
        put(l.id, existing?.score?.toInt()?.toString() ?: "")
      }
    }
  }

  val numericScores = classLearners.mapNotNull { l ->
    scoreInputs[l.id]?.toDoubleOrNull()?.coerceIn(0.0, 100.0)
  }
  val eeCount = numericScores.count { CbcPerformanceLevel.fromScore(it) == CbcPerformanceLevel.EE }
  val meCount = numericScores.count { CbcPerformanceLevel.fromScore(it) == CbcPerformanceLevel.ME }
  val aeCount = numericScores.count { CbcPerformanceLevel.fromScore(it) == CbcPerformanceLevel.AE }
  val beCount = numericScores.count { CbcPerformanceLevel.fromScore(it) == CbcPerformanceLevel.BE }
  val meanLabel = if (numericScores.isEmpty()) "No scores yet" else "Mean ${numericScores.average().toInt()}"

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Marksbook",
        subtitle = "$classDisplay · $selectedAreaName, ${session.currentTerm}",
        onBackToServices = onBackToServices,
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          assessmentTypes.forEach { t ->
            FilterChip(
              selected = selectedAssessment == t,
              onClick = { selectedAssessment = t },
              label = { Text(t) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = extra.navy,
                selectedLabelColor = Color.White,
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
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(9.dp))
              .background(extra.line),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            if (eeCount > 0) Box(modifier = Modifier.weight(eeCount.toFloat()).height(10.dp).background(extra.ee))
            if (meCount > 0) Box(modifier = Modifier.weight(meCount.toFloat()).height(10.dp).background(extra.me))
            if (aeCount > 0) Box(modifier = Modifier.weight(aeCount.toFloat()).height(10.dp).background(extra.ae))
            if (beCount > 0) Box(modifier = Modifier.weight(beCount.toFloat()).height(10.dp).background(extra.be))
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("EE $eeCount", color = extra.ee, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              Text("ME $meCount", color = extra.me, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              Text("AE $aeCount", color = extra.ae, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              Text("BE $beCount", color = extra.be, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Text(meanLabel, color = extra.muted, style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    if (classLearners.isEmpty()) {
      item {
        EmptyClassRosterCard(
          classLabel = classDisplay,
          onRefresh = onRefreshRoster,
        )
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
            classLearners.forEachIndexed { idx, learner ->
              if (idx > 0) HorizontalDivider(color = extra.line)
              val rawText = scoreInputs[learner.id] ?: ""
              val parsed = rawText.toDoubleOrNull()
              val previewLevel = parsed?.let { CbcPerformanceLevel.fromScore(it) }

              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp, bottomEnd = 13.dp, bottomStart = 4.dp))
                    .background(MaterialTheme.colorScheme.background),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(initialsOf(learner.fullName), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(learner.fullName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text("Out of 100", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                }

                if (previewLevel != null) {
                  CbcRubricBadge(previewLevel)
                } else {
                  Surface(color = extra.line, shape = RoundedCornerShape(7.dp), modifier = Modifier.width(34.dp)) {
                    Text(
                      text = "–",
                      color = Color.White,
                      textAlign = TextAlign.Center,
                      style = MaterialTheme.typography.labelMedium,
                      modifier = Modifier.padding(vertical = 3.dp),
                    )
                  }
                }

                OutlinedTextField(
                  value = rawText,
                  onValueChange = { txt ->
                    val filtered = txt.filter { it.isDigit() }
                    val clamped = if (filtered.isEmpty()) "" else filtered.toIntOrNull()?.coerceIn(0, 100)?.toString() ?: ""
                    scoreInputs[learner.id] = clamped
                  },
                  placeholder = { Text("–") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.width(74.dp).testTag("mark_input_${learner.id}"),
                )
              }
            }
          }
        }
      }

      item {
        Text(
          text = "EE 80 and above · ME 60 to 79 · AE 40 to 59 · BE below 40. Bands update as you type.",
          style = MaterialTheme.typography.bodySmall,
          color = extra.muted,
        )
      }

      item {
        HighlighterButton(
          text = "Save scores",
          enabled = numericScores.isNotEmpty(),
          onClick = {
            val map = classLearners.mapNotNull { l ->
              val v = scoreInputs[l.id]?.toDoubleOrNull() ?: return@mapNotNull null
              l.id to v.coerceIn(0.0, 100.0)
            }.toMap()
            onSaveMarksBatch(selectedSubjectId, session.currentTerm, selectedAssessment, map)
          },
          modifier = Modifier.fillMaxWidth().testTag("save_marks_button"),
        )
      }
    }
  }
}

@Composable
fun EmptyClassRosterCard(
  classLabel: String,
  onRefresh: () -> Unit,
) {
  val extra = LocalElimuColors.current
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.5.dp, extra.line),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(22.dp).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(Icons.Filled.Groups, contentDescription = null, tint = extra.navy, modifier = Modifier.size(38.dp))
      Text("No learners found in $classLabel", style = MaterialTheme.typography.titleMedium)
      Text(
        text = "Refresh to load your latest class list, or add a learner from the Learners module if permitted.",
        style = MaterialTheme.typography.bodySmall,
        color = extra.muted,
        textAlign = TextAlign.Center,
      )
      HighlighterButton(
        text = "Refresh roster",
        onClick = onRefresh,
        modifier = Modifier.fillMaxWidth().testTag("refresh_roster_button"),
      )
    }
  }
}
