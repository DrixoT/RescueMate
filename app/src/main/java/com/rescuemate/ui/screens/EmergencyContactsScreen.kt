package com.rescuemate.ui.screens

import android.util.Log
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.rescuemate.R
import com.rescuemate.data.UserPreferences
import com.rescuemate.data.repository.EmergencyRepository
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.ui.theme.*
import com.rescuemate.utils.QRCodeUtils
import com.rescuemate.utils.EncryptionUtils
import org.json.JSONObject

@Composable
fun EmergencyContactsScreen(
    onBack: () -> Unit,
    onAddContact: (String?, String?) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { EmergencyRepository(context) }
    val userPrefs = remember { UserPreferences(context) }

    // Use mutableStateOf instead of mutableStateListOf for better reactivity
    var contacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog States
    var showAddMethodDialog by remember { mutableStateOf(false) }
    var showMyQRDialog by remember { mutableStateOf(false) }

    // QR Code Scanner Launcher
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            try {
                val decryptedContent = EncryptionUtils.decrypt(result.contents)
                if (decryptedContent == null) {
                    Toast.makeText(context, "Invalid QR Code (Decryption failed)", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
                val json = JSONObject(decryptedContent)
                val name = json.optString("name", "")
                val phone = json.optString("phone", "")
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    Log.d("EmergencyContactsScreen", "✅ QR Scanned: $name, $phone")
                    onAddContact(name, phone)
                } else {
                    Toast.makeText(context, "Invalid QR Code format", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error parsing QR Code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load contacts when screen appears
    LaunchedEffect(Unit) {
        Log.d("EmergencyContactsScreen", "🎨 Screen loaded - Loading contacts from database")
        isLoading = true
        contacts = repository.getAllContacts()
        isLoading = false
        Log.d("EmergencyContactsScreen", "✅ Loaded ${contacts.size} contacts from database")
    }

    // Refresh contacts when returning from add contact screen
    DisposableEffect(Unit) {
        onDispose {
            Log.d("EmergencyContactsScreen", "🔄 Screen disposing")
        }
    }

    com.rescuemate.ui.components.CosmicScaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = CosmicTextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    com.rescuemate.ui.components.CosmicHeader(
                        text = stringResource(R.string.emergency_contacts)
                    )
                    com.rescuemate.ui.components.CosmicSubHeader(
                        text = stringResource(R.string.your_safety_network)
                    )
                }
                
                // My QR Code Button
                IconButton(onClick = { showMyQRDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "My QR Code",
                        tint = CosmicPrimary
                    )
                }
                
                // Add Contact Button (Triggers Dialog)
                IconButton(onClick = { showAddMethodDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Contact",
                        tint = CosmicPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            // Show loading, empty state or contacts list
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CosmicPrimary)
                    }
                }
                contacts.isEmpty() -> {
                    EmptyContactsState(
                        onAddContact = { showAddMethodDialog = true }
                    )
                }
                else -> {
                    Log.d("EmergencyContactsScreen", "📋 Displaying ${contacts.size} contacts")

                    // Contacts List
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(contacts) { index, contact ->
                            ContactCard(
                                contact = contact,
                                modifier = Modifier.fillMaxWidth(),
                                onDelete = {
                                    Log.d("EmergencyContactsScreen", "🗑️ Delete requested for: ${contact.name}")
                                    val success = repository.deleteContact(contact.id)
                                    if (success) {
                                        // Refresh list
                                        contacts = repository.getAllContacts()
                                        Log.d("EmergencyContactsScreen", "✅ Contact deleted, list refreshed")
                                    }
                                },
                                onUpdate = { updatedContact ->
                                    Log.d("EmergencyContactsScreen", "🔄 Update requested for: ${updatedContact.name}")
                                    val success = repository.addContact(updatedContact)
                                    if (success) {
                                        contacts = repository.getAllContacts()
                                        Log.d("EmergencyContactsScreen", "✅ Contact updated")
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Note
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicCardHover.copy(alpha = 0.5f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.auto_alert),
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicTextSecondary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.auto_alert_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Contact Method Dialog
    if (showAddMethodDialog) {
        AlertDialog(
            onDismissRequest = { showAddMethodDialog = false },
            title = { 
                Text(
                    "Add Emergency Contact",
                    style = MaterialTheme.typography.titleLarge,
                    color = CosmicTextPrimary
                ) 
            },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Choose how you want to add a new contact:",
                        color = CosmicTextSecondary
                    )
                    
                    // Scan QR Option
                    Card(
                        onClick = { 
                            showAddMethodDialog = false
                            val options = ScanOptions()
                            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            options.setPrompt("Scan Emergency Contact QR Code")
                            options.setBeepEnabled(true)
                            scanLauncher.launch(options)
                        },
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = CosmicPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    "Scan QR Code",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CosmicTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Scan another user's code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicTextSecondary
                                )
                            }
                        }
                    }
                    
                    // Manual Entry Option
                    Card(
                        onClick = { 
                            showAddMethodDialog = false
                            onAddContact(null, null)
                        },
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = CosmicPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    "Enter Manually",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CosmicTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Type name and phone number",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicTextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddMethodDialog = false }) {
                    Text("Cancel", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicBackground,
            textContentColor = CosmicTextPrimary
        )
    }

    // My QR Code Dialog
    if (showMyQRDialog) {
        val userName = userPrefs.getUserName() ?: "Unknown User"
        val userPhone = userPrefs.getUserPhone() ?: ""
        
        // Create JSON content
        val qrContent = JSONObject().apply {
            put("name", userName)
            put("phone", userPhone)
            put("type", "emergency_contact_share")
        }.toString()

        Dialog(onDismissRequest = { showMyQRDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "My Contact QR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Let others scan this to add you as an emergency contact.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // QR Code Image
                    val bitmap = remember(qrContent) {
                        val encryptedContent = EncryptionUtils.encrypt(qrContent)
                        QRCodeUtils.generateQRCode(encryptedContent)
                    }
                    
                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .background(Color.White)
                                .padding(16.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = userPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicTextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { showMyQRDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyContactsState(
    onAddContact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Empty state icon
        Icon(
            imageVector = Icons.Default.PersonAddAlt,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = CosmicTextSecondary.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "No Emergency Contacts",
            style = MaterialTheme.typography.headlineSmall,
            color = CosmicTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Add trusted contacts who will be notified\nin case of an emergency",
            style = MaterialTheme.typography.bodyLarge,
            color = CosmicTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        com.rescuemate.ui.components.CosmicButton(
            text = "Add Your First Contact",
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth(),
            isPrimary = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CosmicCardHover.copy(alpha = 0.5f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = CosmicPrimary
                )
                Column {
                    Text(
                        text = "Emergency Services (911)",
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "911 is always available as your default emergency contact",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ContactCard(
    contact: EmergencyContact,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
    onUpdate: (EmergencyContact) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isPreferenceExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = CosmicCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = CosmicTextPrimary
                        )
                        if (contact.isPrimaryContact) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Primary Contact",
                                modifier = Modifier.size(14.dp),
                                tint = CosmicPrimary
                            )
                        }
                    }
                    Text(
                        text = contact.relationship,
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextPrimary.copy(alpha = 0.7f)
                    )
                    if (contact.email != null) {
                        Text(
                            text = contact.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextPrimary.copy(alpha = 0.7f)
                        )
                    }

                    // Notification Preference Indicators
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Preference Dropdown
                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { isPreferenceExpanded = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val (icon, text) = when (contact.notificationPreference) {
                                            EmergencyContact.NotificationPreference.ALL -> Icons.Default.NotificationsActive to "All"
                                            EmergencyContact.NotificationPreference.VOICE_SMS -> Icons.Default.PermPhoneMsg to "Voice & SMS"
                                            EmergencyContact.NotificationPreference.SMS_ONLY -> Icons.Default.Message to "SMS Only"
                                            EmergencyContact.NotificationPreference.VOICE_ONLY -> Icons.Default.Phone to "Voice Only"
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = CosmicPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CosmicTextSecondary,
                                            fontSize = 10.sp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = CosmicTextSecondary
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CosmicCardHover,
                                    selectedLabelColor = CosmicTextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = true,
                                    borderColor = CosmicBorder
                                )
                            )
                            
                            DropdownMenu(
                                expanded = isPreferenceExpanded,
                                onDismissRequest = { isPreferenceExpanded = false },
                                modifier = Modifier.background(CosmicCard)
                            ) {
                                EmergencyContact.NotificationPreference.values().forEach { pref ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                when (pref) {
                                                    EmergencyContact.NotificationPreference.ALL -> "All (Voice, SMS, Email)"
                                                    EmergencyContact.NotificationPreference.VOICE_SMS -> "Voice & SMS"
                                                    EmergencyContact.NotificationPreference.SMS_ONLY -> "SMS Only"
                                                    EmergencyContact.NotificationPreference.VOICE_ONLY -> "Voice Only"
                                                },
                                                color = CosmicTextPrimary
                                            ) 
                                        },
                                        onClick = {
                                            onUpdate(contact.copy(notificationPreference = pref))
                                            isPreferenceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Delete button
                IconButton(
                    onClick = {
                        showDeleteDialog = true
                        Log.d("ContactCard", "🗑️ Delete icon clicked for: ${contact.name}")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Contact",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Log.d("ContactCard", "📞 Call button clicked for: ${contact.phoneNumber}")
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${contact.phoneNumber}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("ContactCard", "Error starting call", e)
                            Toast.makeText(context, "Unable to make call", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.call),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                OutlinedButton(
                    onClick = {
                        Log.d("ContactCard", "💬 Message button clicked for: ${contact.phoneNumber}")
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${contact.phoneNumber}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("ContactCard", "Error sending message", e)
                            Toast.makeText(context, "Unable to send message", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CosmicTextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.message),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete ${contact.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d("ContactCard", "✅ Delete confirmed for: ${contact.name}")
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    Log.d("ContactCard", "❌ Delete cancelled")
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
