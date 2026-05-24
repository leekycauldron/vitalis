package com.vitalis.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vitalis.ui.common.AppHeader
import com.vitalis.ui.common.Title
import com.vitalis.ui.common.VPrimaryButton
import com.vitalis.ui.theme.VColors

@Composable
fun CreateAccountScreen(
    initialEmail: String,
    onBack: () -> Unit,
    onCreate: (email: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
  var email by remember { mutableStateOf(initialEmail) }
  var password by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }

  Column(
      modifier = modifier.fillMaxSize().background(VColors.Bg).navigationBarsPadding(),
  ) {
    AppHeader(onBack = onBack)
    Title(text = "Create account", subtitle = "Sign up with email or your favourite account.")

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SocialButton(label = "Continue with Apple")
      SocialButton(label = "Continue with Google")
      OrDivider()

      VInput(
          value = name,
          onValueChange = { name = it },
          label = "Name",
      )
      VInput(
          value = email,
          onValueChange = { email = it },
          label = "Email",
          keyboardType = KeyboardType.Email,
      )
      VInput(
          value = password,
          onValueChange = { password = it },
          label = "Password",
          keyboardType = KeyboardType.Password,
          isPassword = true,
      )
      Text(
          text = "8 characters minimum · one uppercase · one number",
          color = VColors.InkLo,
          style = MaterialTheme.typography.bodySmall,
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
      VPrimaryButton(
          text = "Create Account",
          onClick = { onCreate(email.trim(), name.trim()) },
          enabled = email.isNotBlank(),
      )
    }
  }
}

@Composable
private fun SocialButton(label: String) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(52.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(VColors.Card)
              .border(1.dp, VColors.Border, RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center,
  ) {
    Text(text = label, color = VColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
  }
}

@Composable
private fun OrDivider() {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Box(modifier = Modifier.weight(1f).height(1.dp).background(VColors.Border))
    Text(
        text = "or",
        color = VColors.InkLo,
        modifier = Modifier.padding(horizontal = 12.dp),
        style = MaterialTheme.typography.bodySmall,
    )
    Box(modifier = Modifier.weight(1f).height(1.dp).background(VColors.Border))
  }
}

@Composable
fun VInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth(),
      label = { Text(label, color = VColors.InkMd) },
      singleLine = singleLine,
      visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
      colors =
          OutlinedTextFieldDefaults.colors(
              focusedTextColor = VColors.Ink,
              unfocusedTextColor = VColors.Ink,
              focusedBorderColor = VColors.Purple,
              unfocusedBorderColor = VColors.Border,
              focusedContainerColor = VColors.Card,
              unfocusedContainerColor = VColors.Card,
              cursorColor = VColors.Purple,
              focusedLabelColor = VColors.PurpleL,
              unfocusedLabelColor = VColors.InkMd,
          ),
      shape = RoundedCornerShape(12.dp),
  )
}
