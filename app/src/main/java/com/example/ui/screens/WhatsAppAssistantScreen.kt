package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modules.whatsapp.WhatsAppDraft
import com.example.ui.components.WhatsAppConfirmationCard
import com.example.ui.theme.LoriWhatsAppGreen
import com.example.viewmodel.LoriMainViewModel

@Composable
fun WhatsAppAssistantScreen(
    viewModel: LoriMainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingWhatsAppDraft by viewModel.pendingWhatsAppDraft.collectAsState()

    var contactName by remember { mutableStateOf("Rahul") }
    var phoneNumber by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("Shaam ko milne aa raha hoon, theek 6 baje.") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("whatsapp_assistant_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        tint = LoriWhatsAppGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WhatsApp Reply Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Lori drafts smart polite Hindi/Hinglish replies and confirms with you before sending.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Active Confirmation Dialog if pending
        if (pendingWhatsAppDraft != null) {
            item {
                WhatsAppConfirmationCard(
                    draft = pendingWhatsAppDraft!!,
                    onConfirm = { viewModel.confirmSendWhatsApp() },
                    onCancel = { viewModel.cancelWhatsAppDraft() }
                )
            }
        }

        // Draft Sandbox
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Compose / Test Voice Reply",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Recipient / Contact Name") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = LoriWhatsAppGreen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_recipient_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number (Optional)") },
                        placeholder = { Text("+91 9876543210") },
                        leadingIcon = {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = LoriWhatsAppGreen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_phone_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = instruction,
                        onValueChange = { instruction = it },
                        label = { Text("What do you want to say? (In casual Hindi/Hinglish)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_instruction_input"),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val prompt = "Lori, $contactName ko reply kar do ki $instruction"
                            viewModel.sendTextMessage(prompt)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LoriWhatsAppGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_generate_reply_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draft & Ask Lori (Voice Confirmation)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Voice Command Examples
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Voice Commands Examples / Bol kar try karein:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• “Lori, Rahul ko is message ka apne hisaab se reply kar do”", fontSize = 13.sp)
                    Text("• “Lori, Priya ko message bhejo ki main thoda late ho jaunga”", fontSize = 13.sp)
                    Text("• “Lori, Papa ko bata do main ghar pahunch gaya”", fontSize = 13.sp)
                }
            }
        }
    }
}
