package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.ruledNotebookBackground
import com.example.ui.theme.LocalElimuColors

@Composable
fun LoginScreen(
  isLoggingIn: Boolean,
  loginError: String?,
  onLogin: (username: String, password: String) -> Unit,
  onClearError: () -> Unit = {},
) {
  val extra = LocalElimuColors.current
  val focusManager = LocalFocusManager.current

  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  val submitLogin = {
    focusManager.clearFocus()
    if (!isLoggingIn) {
      onLogin(username, password)
    }
  }

  val darkFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = extra.highlighter,
    unfocusedBorderColor = extra.line,
    focusedLabelColor = extra.highlighter,
    unfocusedLabelColor = extra.muted,
    focusedLeadingIconColor = extra.highlighter,
    unfocusedLeadingIconColor = extra.muted,
    focusedTrailingIconColor = extra.highlighter,
    unfocusedTrailingIconColor = extra.muted,
    cursorColor = extra.highlighter,
    focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
    unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
  )

  val surfaceGlowColor = extra.navy.copy(alpha = 0.38f)
  val accentGlowColor = extra.me.copy(alpha = 0.14f)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .drawBehind {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(surfaceGlowColor, Color.Transparent),
            center = Offset(size.width * 0.25f, size.height * 0.18f),
            radius = size.maxDimension * 0.55f,
          ),
          radius = size.maxDimension * 0.55f,
          center = Offset(size.width * 0.25f, size.height * 0.18f),
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(accentGlowColor, Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.82f),
            radius = size.maxDimension * 0.45f,
          ),
          radius = size.maxDimension * 0.45f,
          center = Offset(size.width * 0.85f, size.height * 0.82f),
        )
      }
      .ruledNotebookBackground(extra.ruleLine)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 460.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Exercise Book Cover Header
      Surface(
        color = extra.navy,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
        border = BorderStroke(1.dp, extra.line),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 3.dp))
                  .background(extra.highlighter),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.School,
                  contentDescription = "ElimuTrack Logo",
                  tint = extra.highlighterInk,
                  modifier = Modifier.size(20.dp),
                )
              }
              Column {
                Text(
                  text = "ElimuTrack",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text = "TEACHER PORTAL",
                  style = MaterialTheme.typography.labelSmall,
                  color = extra.highlighter,
                )
              }
            }
            Surface(
              color = Color.White.copy(alpha = 0.12f),
              shape = RoundedCornerShape(99.dp),
            ) {
              Text(
                text = "KICD CBC",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, extra.line),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
              Box(
                modifier = Modifier
                  .width(4.dp)
                  .fillMaxHeight()
                  .background(extra.penRed),
              )
              Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Text(
                  text = "CBC School Management System",
                  style = MaterialTheme.typography.labelMedium,
                  color = extra.me,
                )
                Text(
                  text = "Sign In",
                  style = MaterialTheme.typography.headlineMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Use the username and password issued by your school administrator.",
                  style = MaterialTheme.typography.bodySmall,
                  color = extra.muted,
                )
              }
            }
          }
        }
      }

      // Sign-In Form Card
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, extra.line),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          OutlinedTextField(
            value = username,
            onValueChange = {
              username = it
              if (!loginError.isNullOrBlank()) onClearError()
            },
            label = { Text("Username") },
            placeholder = { Text("e.g. alvina") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Username") },
            singleLine = true,
            colors = darkFieldColors,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Text,
              imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
              onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_identifier_input"),
          )

          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it
              if (!loginError.isNullOrBlank()) onClearError()
            },
            label = { Text("Password") },
            placeholder = { Text("Enter your password") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
            trailingIcon = {
              IconButton(
                onClick = { showPassword = !showPassword },
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
              ) {
                Icon(
                  imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                  contentDescription = if (showPassword) "Hide password" else "Show password",
                )
              }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            colors = darkFieldColors,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
              onDone = { submitLogin() }
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_password_input"),
          )

          AnimatedVisibility(
            visible = !loginError.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
          ) {
            Surface(
              color = extra.be.copy(alpha = 0.16f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, extra.be.copy(alpha = 0.45f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("login_error_banner"),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.ErrorOutline,
                  contentDescription = "Sign-in error",
                  tint = extra.be,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = loginError.orEmpty(),
                  color = extra.be,
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          }

          Button(
            onClick = submitLogin,
            enabled = !isLoggingIn,
            colors = ButtonDefaults.buttonColors(
              containerColor = extra.highlighter,
              contentColor = extra.highlighterInk,
              disabledContainerColor = extra.highlighter.copy(alpha = 0.5f),
              disabledContentColor = extra.highlighterInk.copy(alpha = 0.6f),
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("login_submit_button"),
          ) {
            if (isLoggingIn) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = extra.highlighterInk,
                strokeWidth = 2.dp,
              )
              Spacer(Modifier.width(8.dp))
              Text("Signing In...", fontWeight = FontWeight.Bold)
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text("Sign In", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      Text(
        text = "Accounts are provisioned by your school administrator.",
        style = MaterialTheme.typography.bodySmall,
        color = extra.muted,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )
    }
  }
}
