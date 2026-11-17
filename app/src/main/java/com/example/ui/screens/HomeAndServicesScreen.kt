package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ServiceGroup
import com.example.domain.model.TeacherModule
import com.example.ui.components.HighlighterButton
import com.example.ui.components.SubScreenHeader
import com.example.ui.components.groupAccentColor
import com.example.ui.components.moduleIcon
import com.example.ui.theme.LocalElimuColors
import com.example.ui.viewmodel.TeacherUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  state: TeacherUiState,
  onNavigateModule: (TeacherModule) -> Unit,
  onSelectClassAndRegister: (streamKey: String) -> Unit,
  onRefresh: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val allowed = session.allowedModules
  val activeClass = state.activeClass
  val classDisplay = activeClass?.displayName ?: state.selectedStream.ifBlank { state.selectedGrade }

  val dayCode = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.TUESDAY -> "TUE"
    Calendar.WEDNESDAY -> "WED"
    Calendar.THURSDAY -> "THU"
    Calendar.FRIDAY -> "FRI"
    else -> "MON"
  }
  val todayDateLabel = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date())
  val todayLessons = state.timetable
    .filter { it.grade == state.selectedGrade && it.dayOfWeek == dayCode && !it.isBreak }
    .sortedBy { it.slotIndex }
  val nextLesson = todayLessons.firstOrNull()

  val classLearners = state.learners.filter { state.matchesActiveClass(it.stream, it.grade) }
  val classMarks = state.marks.filter { state.matchesActiveClass(it.stream, it.grade) }
  val summary = state.dashboardSummary?.takeIf {
    it.stream.isBlank() || it.stream.equals(state.selectedStream, ignoreCase = true)
  }

  val totalLearners = summary?.learnerCount ?: classLearners.size
  val avgAtt = summary?.attendancePct
    ?: if (classLearners.isEmpty()) 0 else classLearners.map { it.termAttendancePct }.average().toInt()
  val classMean = summary?.meanScore?.toString()
    ?: if (classMarks.isEmpty()) "--" else "${classMarks.map { it.score }.average().toInt()}"
  val feesOutstanding = summary?.feeOutstandingKes ?: classLearners.sumOf { it.feeBalanceKes }
  val plansCount = summary?.plansCount ?: state.lessonPlans.count { state.matchesActiveClass(it.stream, it.grade) }
  val obsCount = summary?.observationsCount ?: state.observations.count { it.grade == state.selectedGrade }

  val quickActions = listOf(
    Triple(TeacherModule.ATTENDANCE, "Take the register", TeacherModule.ATTENDANCE),
    Triple(TeacherModule.MARKSBOOK, "Enter marks", TeacherModule.MARKSBOOK),
    Triple(TeacherModule.LESSON_PLANS, "Write a lesson plan", TeacherModule.LESSON_PLANS),
    Triple(TeacherModule.OBSERVATIONS, "Add an observation", TeacherModule.OBSERVATIONS),
    Triple(TeacherModule.SCHEMES, "Schemes of work", TeacherModule.SCHEMES),
    Triple(TeacherModule.RECORD_OF_WORK, "Record work covered", TeacherModule.RECORD_OF_WORK),
    Triple(TeacherModule.PROGRESS, "Record progress", TeacherModule.PROGRESS),
  ).filter { it.first in allowed }.take(4)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Exercise Book Cover Card with Ruled Label & Red Margin Stripe
    item {
      Surface(
        color = extra.navy,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 5.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
              Box(
                modifier = Modifier
                  .width(3.5.dp)
                  .fillMaxHeight()
                  .background(extra.penRed),
              )
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .drawBehind {
                    val step = 25.dp.toPx()
                    var y = step
                    while (y < size.height) {
                      drawLine(extra.line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                      y += step
                    }
                  }
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Text("Teacher · ${session.schoolName}", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                Text(
                  text = session.fullName,
                  style = MaterialTheme.typography.headlineMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "$todayDateLabel · ${session.currentTerm}, ${session.currentYear}",
                  style = MaterialTheme.typography.bodySmall,
                  color = extra.muted,
                )
              }
            }
          }

          if (TeacherModule.TIMETABLE in allowed && nextLesson != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Next lesson · ${nextLesson.timeRange}",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                  text = nextLesson.learningArea,
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = classDisplay,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.8f),
                )
              }
              if (TeacherModule.ATTENDANCE in allowed && activeClass != null) {
                Button(
                  onClick = { onSelectClassAndRegister(activeClass.streamKey) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = extra.highlighter,
                    contentColor = extra.highlighterInk,
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("home_take_register_button"),
                ) {
                  Text("Take register", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }

    // Overview Stats (visible when Dashboard module is enabled)
    if (TeacherModule.DASHBOARD in allowed) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NotebookStatCard(
              value = "$totalLearners",
              label = "Learners in $classDisplay",
              modifier = Modifier.weight(1f),
            )
            NotebookStatCard(
              value = "$avgAtt%",
              label = "Attendance ($classDisplay)",
              modifier = Modifier.weight(1f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NotebookStatCard(
              value = classMean,
              label = "Class mean ($classDisplay)",
              modifier = Modifier.weight(1f),
            )
            if (TeacherModule.FEE_PAYMENTS in allowed) {
              NotebookStatCard(
                value = if (feesOutstanding >= 1000) "${(feesOutstanding / 1000).toInt()}k" else "${feesOutstanding.toInt()}",
                label = "Fees outstanding (KES)",
                modifier = Modifier.weight(1f),
              )
            } else {
              NotebookStatCard(
                value = "${plansCount + obsCount}",
                label = "Plans & observations",
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }

    // Do Now Quick Actions (2x2)
    if (quickActions.isNotEmpty()) {
      item {
        Text("Do now", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(6.dp))
        FlowRow(
          maxItemsInEachRow = 2,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          quickActions.forEach { (mod, title, target) ->
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, extra.line),
              modifier = Modifier
                .weight(1f)
                .height(86.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onNavigateModule(target) }
                .testTag("quick_action_${mod.key}"),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
              ) {
                Icon(
                  imageVector = moduleIcon(mod),
                  contentDescription = title,
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(22.dp),
                )
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
              }
            }
          }
        }
      }
    }

    // Today's Lessons Card
    if (TeacherModule.TIMETABLE in allowed) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Today's lessons ($dayCode)", style = MaterialTheme.typography.titleMedium)
          Surface(
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, extra.line),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(99.dp))
              .clickable { onNavigateModule(TeacherModule.TIMETABLE) },
          ) {
            Text(
              text = "Full week",
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
        Spacer(Modifier.height(6.dp))
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          if (todayLessons.isEmpty()) {
            Text(
              text = "No timetable slots published for $dayCode yet.",
              style = MaterialTheme.typography.bodySmall,
              color = extra.muted,
              modifier = Modifier.padding(14.dp),
            )
          } else {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
              todayLessons.forEachIndexed { idx, slot ->
                if (idx > 0) HorizontalDivider(color = extra.line)
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = slot.timeRange,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(94.dp),
                  )
                  Row(modifier = Modifier.weight(1f).height(IntrinsicSize.Min)) {
                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(extra.me))
                    Spacer(Modifier.width(9.dp))
                    Column {
                      Text(slot.learningArea, style = MaterialTheme.typography.titleSmall)
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

    if (quickActions.isEmpty() && TeacherModule.TIMETABLE !in allowed && TeacherModule.DASHBOARD !in allowed) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Your school administrator hasn't switched on any services yet. Ask them to enable modules in Staff & Access.",
              style = MaterialTheme.typography.bodyMedium,
              color = extra.muted,
            )
            OutlinedButton(onClick = onRefresh) {
              Text("Refresh permissions")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NotebookStatCard(
  value: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  val extra = LocalElimuColors.current
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, extra.line),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
      Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      Text(label, style = MaterialTheme.typography.bodySmall, color = extra.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServicesHubScreen(
  state: TeacherUiState,
  onNavigateModule: (TeacherModule) -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  val allowed = session.allowedModules
  val grouped = listOf(
    ServiceGroup.TEACHING,
    ServiceGroup.LEARNERS,
    ServiceGroup.ASSESSMENT,
    ServiceGroup.SCHOOL,
  ).mapNotNull { grp ->
    val items = TeacherModule.entries.filter { it.group == grp && it in allowed }
    if (items.isEmpty()) null else grp to items
  }
  val enabledCount = grouped.sumOf { it.second.size }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Services",
        subtitle = "$enabledCount switched on by your admin",
      )
    }

    if (grouped.isEmpty()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "No services yet. Your admin chooses what appears here.",
            style = MaterialTheme.typography.bodyMedium,
            color = extra.muted,
            modifier = Modifier.padding(18.dp),
          )
        }
      }
    } else {
      grouped.forEach { (grp, modules) ->
        item(key = grp.name) {
          val accent = groupAccentColor(grp)
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(grp.label, style = MaterialTheme.typography.titleMedium)
            FlowRow(
              maxItemsInEachRow = 2,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              modules.forEach { mod ->
                Surface(
                  color = MaterialTheme.colorScheme.surface,
                  shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 14.dp, bottomEnd = 14.dp),
                  border = BorderStroke(1.dp, extra.line),
                  modifier = Modifier
                    .weight(1f)
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 14.dp, bottomEnd = 14.dp))
                    .clickable { onNavigateModule(mod) }
                    .testTag("service_tile_${mod.key}"),
                ) {
                  Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(accent))
                    Column(
                      modifier = Modifier.padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector = moduleIcon(mod),
                        contentDescription = mod.label,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                      )
                      Text(mod.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      Text(
                        text = mod.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = extra.muted,
                        maxLines = 2,
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

@Composable
fun MeScreen(
  state: TeacherUiState,
  onToggleDarkMode: (Boolean) -> Unit,
  onChangeLogin: (username: String, currentPassword: String, newPassword: String) -> Unit,
  onSaveServerUrl: (String) -> Unit,
  onSyncNow: () -> Unit,
  onLogout: () -> Unit,
) {
  val session = state.session ?: return
  val extra = LocalElimuColors.current
  var username by remember(session.username) { mutableStateOf(session.username) }
  var currentPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var serverUrl by remember(session.backendUrl) { mutableStateOf(session.backendUrl) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SubScreenHeader(
        title = "Me",
        subtitle = "${session.fullName} · ${session.schoolName}",
      )
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Assigned classes", style = MaterialTheme.typography.titleSmall)
          if (session.allocatedClasses.isEmpty()) {
            Text(
              text = "No streams assigned yet.",
              style = MaterialTheme.typography.bodySmall,
              color = extra.muted,
            )
          } else {
            session.allocatedClasses.forEachIndexed { idx, cls ->
              if (idx > 0) HorizontalDivider(color = extra.line)
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(cls.displayName, style = MaterialTheme.typography.titleSmall)
                Surface(
                  color = MaterialTheme.colorScheme.background,
                  border = BorderStroke(1.dp, extra.line),
                  shape = RoundedCornerShape(99.dp),
                ) {
                  Text(
                    text = "${cls.learningAreas.size} learning areas",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                  )
                }
              }
            }
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Your services", style = MaterialTheme.typography.titleSmall)
          Text(
            text = TeacherModule.entries.filter { it in session.allowedModules }.joinToString(", ") { it.label }.ifBlank { "None yet" },
            style = MaterialTheme.typography.bodySmall,
            color = extra.muted,
          )
        }
      }
    }

    item {
      Text("My login", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(6.dp))
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text("Current password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New password (min 6 chars)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          HighlighterButton(
            text = "Update login",
            onClick = {
              onChangeLogin(username, currentPassword, newPassword)
              currentPassword = ""
              newPassword = ""
            },
            modifier = Modifier.fillMaxWidth(),
          )
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
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Dark mode", style = MaterialTheme.typography.titleSmall)
            Switch(checked = state.darkMode, onCheckedChange = onToggleDarkMode)
          }
          HorizontalDivider(color = extra.line)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onSyncNow)
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Sync with server", style = MaterialTheme.typography.titleSmall)
              Text(session.backendUrl, style = MaterialTheme.typography.labelSmall, color = extra.muted)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = extra.muted)
          }
          OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("School server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedButton(
            onClick = { onSaveServerUrl(serverUrl) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Save server URL")
          }
        }
      }
    }

    item {
      OutlinedButton(
        onClick = onLogout,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("me_sign_out_button"),
      ) {
        Text("Sign out", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun FirstSignInLockScreen(
  defaultUsername: String,
  onSaveAndContinue: (username: String, currentPassword: String, newPassword: String) -> Unit,
  onLogout: () -> Unit,
) {
  val extra = LocalElimuColors.current
  var username by remember { mutableStateOf(defaultUsername) }
  var currentPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      color = extra.navy,
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 5.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(3.5.dp).fillMaxHeight().background(extra.penRed))
            Column(modifier = Modifier.padding(12.dp)) {
              Text("Welcome", style = MaterialTheme.typography.bodySmall, color = extra.muted)
              Text("Set your own login", style = MaterialTheme.typography.headlineMedium)
            }
          }
        }
      }
    }

    Surface(
      color = extra.highlighter.copy(alpha = 0.24f),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = "Your admin created this account. Confirm your current password and set a new password to open your dashboard.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(12.dp),
      )
    }

    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, extra.line),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = username,
          onValueChange = { username = it },
          label = { Text("Username") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = currentPassword,
          onValueChange = { currentPassword = it },
          label = { Text("Current password") },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = newPassword,
          onValueChange = { newPassword = it },
          label = { Text("New password (min 6 chars)") },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        HighlighterButton(
          text = "Save and continue",
          onClick = { onSaveAndContinue(username, currentPassword, newPassword) },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
          onClick = onLogout,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Sign out")
        }
      }
    }
  }
}
