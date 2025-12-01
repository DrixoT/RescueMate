package com.rescuemate.emergency.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.rescuemate.data.UserPreferences
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.emergency.data.database.EmergencyDatabaseHelper
import com.rescuemate.utils.QRCodeUtils
import org.json.JSONObject

/**
 * Emergency Contact Management Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactManagementScreen(
    onNavigateBack: () -> Unit = {},
    onAddContact: () -> Unit = {}
) {
    val context = LocalContext.current
    val dbHelper = remember { EmergencyDatabaseHelper(context) }
    val userPrefs = remember { UserPreferences(context) }
    var contacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf<EmergencyContact?>(null) }
    var showQRDialog by remember { mutableStateOf(false) }
    
    // State for handling scanned data
    var scannedData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    // QR Code Scanner Launcher
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            try {
                val json = JSONObject(result.contents)
                val name = json.optString("name", "")
                val phone = json.optString("phone", "")
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    scannedData = name to phone
                    showAddContactDialog = true
                } else {
                    // Handle invalid format
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        val result = dbHelper.getAllContacts()
        contacts = result.getOrNull() ?: emptyList()
    }

    // If we are showing the add contact dialog (triggered by scan), render it here
    if (showAddContactDialog) {
        AddEmergencyContactScreen(
            onNavigateBack = { 
                showAddContactDialog = false 
                scannedData = null
            },
            onContactAdded = {
                // Refresh list
                val result = dbHelper.getAllContacts()
                contacts = result.getOrNull() ?: emptyList()
                showAddContactDialog = false
                scannedData = null
            },
            initialName = scannedData?.first ?: "",
            initialPhone = scannedData?.second ?: ""
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Contacts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Scan QR Code
                    IconButton(onClick = {
                        val options = ScanOptions()
                        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        options.setPrompt("Scan Emergency Contact QR Code")
                        options.setBeepEnabled(true)
                        scanLauncher.launch(options)
                    }) {
                        Icon(Icons.Default.QrCodeScanner, "Scan QR")
                    }
                    
                    // Show My QR Code
                    IconButton(onClick = { showQRDialog = true }) {
                        Icon(Icons.Default.QrCode, "My QR")
                    }

                    IconButton(onClick = onAddContact) {
                        Icon(Icons.Default.Add, "Add Contact")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (contacts.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "No contacts",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No emergency contacts added",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Quick Action Buttons for Empty State
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onAddContact) {
                                Text("Add Manually")
                            }
                            
                            OutlinedButton(onClick = {
                                val options = ScanOptions()
                                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                options.setPrompt("Scan Emergency Contact QR Code")
                                options.setBeepEnabled(true)
                                scanLauncher.launch(options)
                            }) {
                                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scan QR")
                            }
                        }
                    }
                }
            } else {
                // Contact List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contacts) { contact ->
                        EmergencyContactCard(
                            contact = contact,
                            onDelete = { showDeleteDialog = contact },
                            onEdit = { /* TODO: Navigate to edit */ }
                        )
                    }
                }
            }
        }
    }

    // My QR Code Dialog
    if (showQRDialog) {
        val userName = userPrefs.getUserName() ?: "Unknown User"
        val userPhone = userPrefs.getUserPhone() ?: ""
        
        // Create JSON content
        val qrContent = JSONObject().apply {
            put("name", userName)
            put("phone", userPhone)
            put("type", "emergency_contact_share")
        }.toString()

        Dialog(onDismissRequest = { showQRDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Share Contact Info",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Others can scan this to add you as an emergency contact.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // QR Code Image
                    val bitmap = remember(qrContent) {
                        QRCodeUtils.generateQRCode(qrContent)
                    }
                    
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userPhone,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { showQRDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { contact ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Contact?") },
            text = { Text("Remove ${contact.name} from emergency contacts?") },
            confirmButton = {
                Button(
                    onClick = {
                        dbHelper.deleteContact(contact.id)
                        val result = dbHelper.getAllContacts()
                        contacts = result.getOrNull() ?: emptyList()
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Emergency Contact Card
 */
@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Contact",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Contact Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (contact.isPrimaryContact) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "PRIMARY",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = "${contact.relationship} • Priority ${contact.priority}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Notification Preferences
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (contact.canReceiveVoiceCall()) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Voice",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (contact.canReceiveSMS()) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "SMS",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (contact.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Actions
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Add Emergency Contact Screen - WITH VALIDATION & PROPER SAVING
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmergencyContactScreen(
    onNavigateBack: () -> Unit = {},
    onContactAdded: () -> Unit = {},
    initialName: String = "",
    initialPhone: String = ""
) {
    val context = LocalContext.current
    val dbHelper = remember { EmergencyDatabaseHelper(context) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(initialName) }
    var phoneNumber by remember { mutableStateOf(initialPhone) }
    var relationship by remember { mutableStateOf("Select") }
    var priority by remember { mutableStateOf("1") }
    var isPrimary by remember { mutableStateOf(false) }
    var notificationPref by remember { mutableStateOf(EmergencyContact.NotificationPreference.ALL) }
    
    // Update state if initial values change
    LaunchedEffect(initialName, initialPhone) {
        if (initialName.isNotEmpty()) name = initialName
        if (initialPhone.isNotEmpty()) phoneNumber = initialPhone
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Emergency Contact") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Done, "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name Field with Validation
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, "Name") },
                isError = name.isBlank(),
                supportingText = {
                    if (name.isBlank()) {
                        Text("Name is required", color = MaterialTheme.colorScheme.error)
                    } else if (name.length < 2) {
                        Text("Name should be at least 2 characters", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            // Phone Number Field with Validation
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == '-' } },
                label = { Text("Phone Number (e.g., +1234567890) *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                placeholder = { Text("+1234567890") },
                isError = phoneNumber.isNotBlank() && !isValidPhoneNumber(phoneNumber),
                supportingText = {
                    when {
                        phoneNumber.isBlank() -> Text("Phone number is required", color = MaterialTheme.colorScheme.error)
                        !isValidPhoneNumber(phoneNumber) -> Text("Invalid phone format (use +1234567890)", color = MaterialTheme.colorScheme.error)
                        else -> Text("Valid phone number")
                    }
                }
            )

            // Relationship Dropdown
            DropdownMenuField(
                label = "Relationship *",
                selectedValue = relationship,
                options = listOf("Family", "Friend", "Colleague", "Doctor", "Other"),
                onValueChanged = { relationship = it },
                isError = relationship == "Select"
            )

            // Priority Dropdown
            DropdownMenuField(
                label = "Priority (1 = Highest) *",
                selectedValue = priority,
                options = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
                onValueChanged = { priority = it },
                isError = false
            )

            // Notification Preference Dropdown
            DropdownMenuField(
                label = "Notification Method",
                selectedValue = notificationPref.toString(),
                options = listOf("ALL", "VOICE_SMS", "SMS_ONLY", "VOICE_ONLY"),
                onValueChanged = { value ->
                    notificationPref = EmergencyContact.NotificationPreference.valueOf(value)
                }
            )

            // Primary Contact Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isPrimary,
                    onCheckedChange = { isPrimary = it }
                )
                Text("Set as primary contact")
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Contact?") },
            text = { Text("Add $name to emergency contacts?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Validate all fields
                        if (name.isBlank() || name.length < 2) {
                            errorMessage = "Please enter a valid name (at least 2 characters)"
                            showErrorDialog = true
                            showSaveDialog = false
                            return@Button
                        }
                        if (phoneNumber.isBlank()) {
                            errorMessage = "Phone number is required"
                            showErrorDialog = true
                            showSaveDialog = false
                            return@Button
                        }
                        if (!isValidPhoneNumber(phoneNumber)) {
                            errorMessage = "Invalid phone number format (use +1234567890)"
                            showErrorDialog = true
                            showSaveDialog = false
                            return@Button
                        }
                        if (relationship == "Select") {
                            errorMessage = "Please select a relationship"
                            showErrorDialog = true
                            showSaveDialog = false
                            return@Button
                        }

                        // Create and save contact
                        val contact = EmergencyContact(
                            name = name.trim(),
                            phoneNumber = phoneNumber.trim(),
                            relationship = relationship,
                            priority = priority.toIntOrNull() ?: 1,
                            isPrimaryContact = isPrimary,
                            isVerified = false,
                            notificationPreference = notificationPref
                        )

                        val result = dbHelper.insertContact(contact)
                        showSaveDialog = false

                        if (result.getOrDefault(0L) > 0) {
                            showSuccessDialog = true
                        } else {
                            errorMessage = "Failed to save contact"
                            showErrorDialog = true
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success!") },
            text = { Text("Contact '$name' has been saved successfully!") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onContactAdded()
                        onNavigateBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Validation Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Validate phone number format
 */
private fun isValidPhoneNumber(phone: String): Boolean {
    // Allow +, digits, and - characters
    if (!phone.matches(Regex("^[+]?[0-9-]{7,15}$"))) {
        return false
    }
    return phone.length >= 10
}

/**
 * Reusable Dropdown Menu Field Component
 */
@Composable
fun DropdownMenuField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChanged: (String) -> Unit,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown"
                )
            },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("$label is required", color = MaterialTheme.colorScheme.error)
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChanged(option)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
