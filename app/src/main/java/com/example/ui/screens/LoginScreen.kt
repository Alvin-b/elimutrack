package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.KicdCurriculumRepository
import com.example.domain.model.TeacherSession
import com.example.ui.components.ruledNotebookBackground
import com.example.ui.theme.LocalElimuColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
  isLoggingIn: Boolean,
  loginError: String?,
  onBackendLogin: (backendUrl: String, username: String, password: String) -> Unit,
  onOfflineWorkspace: (teacherName: String, tscNumber: String, schoolName: String, backendUrl: String, grades: List<String>, username: String) -> Unit,
) {
  val extra = LocalElimuColors.current
  var selectedTab by remember { mutableIntStateOf(0) }
  var backendUrl by remember { mutableStateOf(TeacherSession.DEFAULT_BACKEND_URL) }
  var showServerSettings by remember { mutableStateOf(false) }

  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  var offlineUsername by remember { mutableStateOf("alvina") }
  var teacherName by remember { mutableStateOf("Alvin Ojiambo") }
  var tscNumber by remember { mutableStateOf("TSC-482910") }
  var schoolName by remember { mutableStateOf("Fret's School") }
  var selectedGrades by remember { mutableStateOf(setOf("Grade 4", "Grade 7", "Grade 9")) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .ruledNotebookBackground(extra.ruleLine)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 460.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Exercise Book Cover Header
      Surface(
        color = extra.navy,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 5.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 2.dp))
                .background(extra.highlighter),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Filled.Home, contentDescription = null, tint = extra.highlighterInk, modifier = Modifier.size(16.dp))
            }
            Text("ElimuTrack Teacher", style = MaterialTheme.typography.titleLarge, color = Color.White)
          }

          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
              Box(modifier = Modifier.width(3.5.dp).fillMaxHeight().background(extra.penRed))
              Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text("CBC School Management System", style = MaterialTheme.typography.bodySmall, color = extra.muted)
                Text("Sign In", style = MaterialTheme.typography.headlineMedium)
                Text("Enter your teacher username and password", style = MaterialTheme.typography.bodySmall, color = extra.muted)
              }
            }
          }
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, extra.line),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = { Text("Sign In") },
              modifier = Modifier.testTag("tab_backend_signin"),
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = { Text("Offline Workspace") },
              modifier = Modifier.testTag("tab_offline_workspace"),
            )
          }

          if (selectedTab == 0) {
            Text(
              text = "Sign in with the username and password issued by your school admin.",
              style = MaterialTheme.typography.bodySmall,
              color = extra.muted,
            )

            OutlinedTextField(
              value = username,
              onValueChange = { username = it },
              label = { Text("Username") },
              placeholder = { Text("e.g. alvina") },
              leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Username") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
              ),
              modifier = Modifier.fillMaxWidth().testTag("login_identifier_input"),
            )

            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Password") },
              placeholder = { Text("Enter your password") },
              leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
              trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                  Icon(
                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Toggle password",
                  )
                }
              },
              visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
              ),
              keyboardActions = KeyboardActions(
                onDone = { if (!isLoggingIn) onBackendLogin(backendUrl, username, password) }
              ),
              modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
            )

            if (!loginError.isNullOrBlank()) {
              Surface(
                color = extra.be.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = loginError,
                  color = extra.be,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(10.dp),
                )
              }
            }

            Button(
              onClick = { onBackendLogin(backendUrl, username, password) },
              enabled = !isLoggingIn,
              colors = ButtonDefaults.buttonColors(containerColor = extra.highlighter, contentColor = extra.highlighterInk),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("login_submit_button"),
            ) {
              if (isLoggingIn) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = extra.highlighterInk, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
              }
              Text("Sign In", fontWeight = FontWeight.Bold)
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { showServerSettings = !showServerSettings }
                .padding(horizontal = 4.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.CloudSync, contentDescription = null, tint = extra.muted, modifier = Modifier.size(16.dp))
                Text(
                  text = "School server URL",
                  style = MaterialTheme.typography.labelMedium,
                  color = extra.muted,
                )
              }
              Icon(
                imageVector = if (showServerSettings) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = "Toggle server URL settings",
                tint = extra.muted,
              )
            }

            AnimatedVisibility(visible = showServerSettings) {
              OutlinedTextField(
                value = backendUrl,
                onValueChange = { backendUrl = it },
                label = { Text("Backend API URL") },
                leadingIcon = { Icon(Icons.Filled.CloudSync, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("login_backend_url_input"),
              )
            }
          } else {
            Text(
              text = "Open an offline-first teacher workspace backed by local storage. You can preview Admin service allocations anytime under Me -> Preview as admin.",
              style = MaterialTheme.typography.bodySmall,
              color = extra.muted,
            )

            OutlinedTextField(
              value = offlineUsername,
              onValueChange = { offlineUsername = it },
              label = { Text("Username") },
              leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Username") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("offline_username_input"),
            )

            OutlinedTextField(
              value = teacherName,
              onValueChange = { teacherName = it },
              label = { Text("Teacher Full Name") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("offline_teacher_name_input"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              OutlinedTextField(
                value = tscNumber,
                onValueChange = { tscNumber = it },
                label = { Text("TSC No.") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("School Name") },
                singleLine = true,
                modifier = Modifier.weight(1.3f),
              )
            }

            Text("Assigned classes", style = MaterialTheme.typography.titleSmall)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              KicdCurriculumRepository.cbcGrades.forEach { grade ->
                val active = grade in selectedGrades
                FilterChip(
                  selected = active,
                  onClick = {
                    selectedGrades = if (active && selectedGrades.size > 1) selectedGrades - grade else selectedGrades + grade
                  },
                  label = { Text(grade) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                  ),
                )
              }
            }

            Button(
              onClick = {
                onOfflineWorkspace(teacherName, tscNumber, schoolName, backendUrl, selectedGrades.sorted(), offlineUsername)
              },
              colors = ButtonDefaults.buttonColors(containerColor = extra.highlighter, contentColor = extra.highlighterInk),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("offline_enter_button"),
            ) {
              Text("Open Teacher Workspace", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
