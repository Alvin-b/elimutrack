package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.domain.model.AllocatedClass
import com.example.domain.model.CbcPerformanceLevel
import com.example.domain.model.ServiceGroup
import com.example.domain.model.TeacherModule
import com.example.domain.model.TeacherSession
import com.example.pdf.ExportedPdf
import com.example.ui.theme.LocalElimuColors

fun Modifier.ruledNotebookBackground(ruleColor: Color): Modifier = this.drawBehind {
  val stepPx = 28.dp.toPx()
  var y = stepPx
  while (y < size.height) {
    drawLine(
      color = ruleColor,
      start = Offset(0f, y),
      end = Offset(size.width, y),
      strokeWidth = 1.dp.toPx(),
    )
    y += stepPx
  }
}

fun moduleIcon(module: TeacherModule): ImageVector = when (module) {
  TeacherModule.DASHBOARD -> Icons.Filled.Home
  TeacherModule.MARKSBOOK -> Icons.AutoMirrored.Filled.FactCheck
  TeacherModule.LESSON_PLANS -> Icons.AutoMirrored.Filled.MenuBook
  TeacherModule.SCHEMES -> Icons.Filled.CalendarMonth
  TeacherModule.TIMETABLE -> Icons.Filled.Schedule
  TeacherModule.RECORD_OF_WORK -> Icons.Filled.EditNote
  TeacherModule.PROGRESS -> Icons.AutoMirrored.Filled.TrendingUp
  TeacherModule.KICD_DESIGNS -> Icons.AutoMirrored.Filled.LibraryBooks
  TeacherModule.OBSERVATIONS -> Icons.Filled.Visibility
  TeacherModule.ATTENDANCE -> Icons.Filled.EventAvailable
  TeacherModule.BROADSHEET -> Icons.Filled.GridOn
  TeacherModule.LEARNERS -> Icons.Filled.Groups
  TeacherModule.REPORT_CARDS -> Icons.Filled.WorkspacePremium
  TeacherModule.PATHWAYS -> Icons.Filled.AccountTree
  TeacherModule.FEE_PAYMENTS -> Icons.Filled.AccountBalanceWallet
  TeacherModule.WHATSAPP -> Icons.AutoMirrored.Filled.Chat
}

@Composable
fun groupAccentColor(group: ServiceGroup): Color {
  val extra = LocalElimuColors.current
  return when (group) {
    ServiceGroup.OVERVIEW -> extra.navy
    ServiceGroup.TEACHING -> extra.me
    ServiceGroup.LEARNERS -> extra.ee
    ServiceGroup.ASSESSMENT -> extra.ae
    ServiceGroup.SCHOOL -> extra.be
  }
}

@Composable
fun CbcRubricBadge(
  level: CbcPerformanceLevel,
  modifier: Modifier = Modifier,
) {
  val extra = LocalElimuColors.current
  val bg = when (level) {
    CbcPerformanceLevel.EE -> extra.ee
    CbcPerformanceLevel.ME -> extra.me
    CbcPerformanceLevel.AE -> extra.ae
    CbcPerformanceLevel.BE -> extra.be
  }
  val fg = if (bg.luminance() > 0.4f) Color(0xFF0A1424) else Color.White
  Surface(
    modifier = modifier,
    color = bg,
    shape = RoundedCornerShape(7.dp),
  ) {
    Text(
      text = level.code,
      color = fg,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun ElimuTopChrome(
  session: TeacherSession,
  selectedClassDisplay: String,
  pendingSyncCount: Int,
  isSyncing: Boolean,
  onSyncClick: () -> Unit,
  onOpenClassPicker: () -> Unit,
) {
  val extra = LocalElimuColors.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(9.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomEnd = 9.dp, bottomStart = 2.dp))
            .background(extra.navy),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = "ElimuTrack",
            tint = extra.highlighter,
            modifier = Modifier.size(17.dp),
          )
        }
        Text(
          text = "ElimuTrack",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      if (session.mustChangePassword) {
        Surface(
          shape = RoundedCornerShape(99.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, extra.line),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(13.dp), tint = extra.muted)
            Text("Login required", style = MaterialTheme.typography.labelMedium)
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(99.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, extra.line),
          modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .clickable(onClick = onSyncClick)
            .testTag("top_sync_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (pendingSyncCount > 0 || isSyncing) extra.ae else extra.ee),
            )
            Text(
              text = when {
                isSyncing -> "Syncing..."
                pendingSyncCount > 0 -> "$pendingSyncCount to sync"
                else -> "All synced"
              },
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    if (!session.mustChangePassword) {
      Surface(
        color = extra.navy,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .clickable(onClick = onOpenClassPicker)
          .testTag("class_picker_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Working in",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.75f),
            )
            Text(
              text = selectedClassDisplay,
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
          }
          Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = "Choose class",
            tint = Color.White,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
}

enum class BottomNavTab(val label: String, val icon: ImageVector) {
  HOME("Today", Icons.Filled.Home),
  REGISTER("Register", Icons.Filled.EventAvailable),
  MARKS("Marks", Icons.AutoMirrored.Filled.FactCheck),
  SERVICES("Services", Icons.Filled.GridView),
  ME("Me", Icons.Filled.Person),
}

@Composable
fun ElimuBottomBar(
  activeTab: BottomNavTab,
  showRegister: Boolean,
  showMarks: Boolean,
  onSelectTab: (BottomNavTab) -> Unit,
) {
  val extra = LocalElimuColors.current
  val tabs = BottomNavTab.entries.filter { tab ->
    when (tab) {
      BottomNavTab.REGISTER -> showRegister
      BottomNavTab.MARKS -> showMarks
      else -> true
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, extra.line),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
      tabs.forEach { tab ->
        val selected = tab == activeTab
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelectTab(tab) }
            .testTag("bottom_tab_${tab.name.lowercase()}"),
          contentAlignment = Alignment.TopCenter,
        ) {
          if (selected) {
            Box(
              modifier = Modifier
                .width(28.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(extra.highlighter),
            )
          }
          Column(
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Icon(
              imageVector = tab.icon,
              contentDescription = tab.label,
              tint = if (selected) MaterialTheme.colorScheme.onSurface else extra.muted,
              modifier = Modifier.size(22.dp),
            )
            Text(
              text = tab.label,
              style = MaterialTheme.typography.labelMedium,
              color = if (selected) MaterialTheme.colorScheme.onSurface else extra.muted,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
fun SubScreenHeader(
  title: String,
  subtitle: String,
  onBackToServices: (() -> Unit)? = null,
  action: (@Composable () -> Unit)? = null,
) {
  val extra = LocalElimuColors.current
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    if (onBackToServices != null) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onBackToServices)
          .padding(vertical = 6.dp, horizontal = 2.dp)
          .testTag("back_to_services_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "All services",
          tint = extra.muted,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "All services",
          style = MaterialTheme.typography.labelLarge,
          color = extra.muted,
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.displaySmall)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = extra.muted)
      }
      if (action != null) action()
    }
  }
}

@Composable
fun HighlighterButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
) {
  val extra = LocalElimuColors.current
  Button(
    onClick = onClick,
    enabled = enabled,
    colors = ButtonDefaults.buttonColors(
      containerColor = extra.highlighter,
      contentColor = extra.highlighterInk,
      disabledContainerColor = extra.highlighter.copy(alpha = 0.45f),
      disabledContentColor = extra.highlighterInk.copy(alpha = 0.5f),
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.height(48.dp),
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun GradeSelectorRow(
  grades: List<String>,
  selectedGrade: String,
  onSelectGrade: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    grades.forEach { grade ->
      val selected = grade == selectedGrade
      FilterChip(
        selected = selected,
        onClick = { onSelectGrade(grade) },
        label = { Text(grade, style = MaterialTheme.typography.labelLarge) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.onBackground,
          selectedLabelColor = MaterialTheme.colorScheme.background,
        ),
        shape = RoundedCornerShape(99.dp),
        modifier = Modifier.testTag("grade_chip_${grade.lowercase().replace(" ", "_")}"),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassPickerSheet(
  allocatedClasses: List<AllocatedClass>,
  currentStream: String,
  learnerCountByStream: Map<String, Int>,
  onSelectClass: (AllocatedClass) -> Unit,
  onDismiss: () -> Unit,
) {
  val extra = LocalElimuColors.current
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text("Choose a class", style = MaterialTheme.typography.headlineMedium)
      Spacer(Modifier.height(4.dp))
      allocatedClasses.forEach { cls ->
        val isCurrent = cls.streamKey.equals(currentStream, ignoreCase = true) ||
          cls.displayName.equals(currentStream, ignoreCase = true)
        val count = learnerCountByStream[cls.streamKey] ?: cls.learnerCount
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, if (isCurrent) extra.navy else extra.line),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
              onSelectClass(cls)
              onDismiss()
            },
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(cls.displayName, style = MaterialTheme.typography.titleMedium)
              Text(
                text = if (count > 0) "${cls.learningAreas.size} learning areas · $count learners" else "${cls.learningAreas.size} learning areas",
                style = MaterialTheme.typography.bodySmall,
                color = extra.muted,
              )
            }
            if (isCurrent) {
              Surface(
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, extra.line),
                shape = RoundedCornerShape(99.dp),
              ) {
                Text(
                  text = "Current",
                  style = MaterialTheme.typography.labelMedium,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
fun PdfExportReadyDialog(
  exported: ExportedPdf,
  onDismiss: () -> Unit,
  onOpenPdf: () -> Unit,
  onSharePdf: () -> Unit,
  onSaveCopy: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.PictureAsPdf,
        contentDescription = "PDF Ready",
        tint = LocalElimuColors.current.navy,
        modifier = Modifier.size(36.dp),
      )
    },
    title = { Text("PDF Export Ready") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(exported.title, style = MaterialTheme.typography.titleSmall)
        Text(exported.file.name, style = MaterialTheme.typography.labelSmall, color = LocalElimuColors.current.muted)
        Text(
          text = "Open, share, or save this official KICD PDF document.",
          style = MaterialTheme.typography.bodySmall,
        )
      }
    },
    confirmButton = {
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HighlighterButton(
          text = "View PDF",
          onClick = onOpenPdf,
          icon = Icons.Filled.Description,
          modifier = Modifier.fillMaxWidth().testTag("pdf_open_button"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(onClick = onSharePdf, modifier = Modifier.weight(1f).testTag("pdf_share_button")) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Share")
          }
          OutlinedButton(onClick = onSaveCopy, modifier = Modifier.weight(1f).testTag("pdf_save_button")) {
            Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save")
          }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
      }
    },
  )
}
