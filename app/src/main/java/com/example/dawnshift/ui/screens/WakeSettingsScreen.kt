package com.example.dawnshift.ui.screens

import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dawnshift.ui.viewmodel.ItemStatus
import com.example.dawnshift.ui.viewmodel.ScheduleItemData
import com.example.dawnshift.ui.viewmodel.WakeSettingsViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WakeSettingsScreen(
    viewModel: WakeSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    // State for TimePicker Dialogs
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showTargetTimePicker by remember { mutableStateOf(false) }

    // Permissions logic
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission handled */ }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            viewModel.updateAlarmSound(uri?.toString() ?: "")
        }
    }

    var ringtoneTitle by remember { mutableStateOf("Default") }
    LaunchedEffect(uiState.alarmSoundUri) {
        if (uiState.alarmSoundUri.isNotEmpty()) {
            val ringtone = RingtoneManager.getRingtone(context, uiState.alarmSoundUri.toUri())
            ringtoneTitle = ringtone?.getTitle(context) ?: "Unknown"
        } else {
            ringtoneTitle = "Default"
        }
    }



    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                     hasNotificationPermission = ContextCompat.checkSelfPermission(
                         context,
                         Manifest.permission.POST_NOTIFICATIONS
                     ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    // Header Metrics
    val density = LocalDensity.current

    // Scroll State
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    
    // Edit plan state
    var isEditingPlan by remember { mutableStateOf(false) }

    // Cancel confirmation state
    var showCancelConfirmation by remember { mutableStateOf(false) }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text("Cancel Plan") },
            text = { Text("Are you sure you want to cancel the current plan? All scheduled alarms will be removed and your progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelAllAlarms()
                        isEditingPlan = false // Reset UI to home state
                        showCancelConfirmation = false
                        Toast.makeText(context, "All alarms cancelled", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Cancel", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("No", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
    




    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Floating Pill Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = !uiState.alarmsSet && !isEditingPlan,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    Button(
                        onClick = {
                            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                return@Button
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@Button
                            }
                            scope.launch {
                                // Race condition: Animation vs Monitor
                                val scrollJob = launch {
                                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                                    val avgItemSize = if (visibleItems.isNotEmpty()) {
                                        visibleItems.sumOf { it.size } / visibleItems.size.toFloat()
                                    } else {
                                        with(density) { 80.dp.toPx() }
                                    }
                                    
                                    val estimatedDistance = listState.firstVisibleItemScrollOffset + (listState.firstVisibleItemIndex * avgItemSize)
                                    val targetScroll = estimatedDistance + 500f // Reduced buffer
                                    val durationMs = (targetScroll / 8f).toInt().coerceIn(300, 1000)
                                    
                                    listState.animateScrollBy(
                                        value = -targetScroll,
                                        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
                                    )
                                }
                                
                                // Wait until we hit the top (index 0, offset 0)
                                androidx.compose.runtime.snapshotFlow { 
                                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 
                                }.filter { it }.first()
                                
                                scrollJob.cancel()
                                listState.scrollToItem(0)
                                
                                viewModel.scheduleAllAlarms()
                                Toast.makeText(context, "Alarms scheduled!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("Set Alarms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(top = 64.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Spacer for header expansion (Max Height 100dp - Min Height 64dp = 36dp)
                item { Spacer(modifier = Modifier.height(36.dp)) }

                item { Spacer(modifier = Modifier.height(20.dp)) }

                item {
                    val editModeStartTime = remember(uiState) {
                        val today = java.time.LocalDate.now()
                        uiState.activeSchedule.find { it.date == today }?.time ?: uiState.startTime
                    }
                    
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                         AnimatedVisibility(
                             visible = isEditingPlan,
                             enter = expandVertically() + fadeIn(),
                             exit = shrinkVertically() + fadeOut()
                        ) {
                             Column(modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)) {
                                 Text(
                                     "Edit current configuration for remaining days",
                                     style = MaterialTheme.typography.labelMedium,
                                     color = MaterialTheme.colorScheme.primary
                                 )
                                 Text(
                                    "Your progress will be saved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                             }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = if (isEditingPlan) "edit" else if (uiState.alarmsSet) "progress" else "config",
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith 
                                     fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)))
                                        .using(
                                            SizeTransform(
                                                clip = false,
                                                sizeAnimationSpec = { _, _ ->
                                                    tween(300, easing = FastOutSlowInEasing)
                                                }
                                            )
                                        )
                                },
                                label = "card_content"
                            ) { state ->
                                when (state) {
                                    "progress" -> {
                                        Row(
                                            modifier = Modifier
                                                .clickable { isEditingPlan = true }
                                                .padding(20.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Active Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("${uiState.startTime.format(timeFormatter)} → ${uiState.targetTime.format(timeFormatter)}, in ${uiState.days} days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("${uiState.completedCount} of ${uiState.days} days completed", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Box(contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    progress = { if (uiState.days > 0) uiState.completedCount / uiState.days.toFloat() else 0f },
                                                    modifier = Modifier.size(56.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    strokeWidth = 6.dp
                                                )
                                                Text(
                                                    text = "${((if (uiState.days > 0) uiState.completedCount / uiState.days.toFloat() else 0f) * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    "edit", "config" -> {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TimeInputRow(
                                                "Current Wake Time",
                                                if (state == "edit") editModeStartTime else uiState.startTime,
                                                { showStartTimePicker = true },
                                                timeFormatter,
                                                enabled = state != "edit",
                                                subtitle = if (state == "edit") "Today's wake up time" else null
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            TimeInputRow(
                                                if (state == "edit") "New target wake time" else "Target Wake Time",
                                                uiState.targetTime,
                                                { showTargetTimePicker = true },
                                                timeFormatter
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            DaysPickerRow(
                                                uiState.days,
                                                { viewModel.updateDays(it.toString()) },
                                                label = if (state == "edit") "Duration (Days left)" else "Duration (Days)"
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                            Row(
                                                modifier = Modifier.fillMaxWidth().height(40.dp).clickable {
                                                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                                        val currentUri = if (uiState.alarmSoundUri.isNotEmpty()) uiState.alarmSoundUri.toUri() else null
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                                    }
                                                    ringtoneLauncher.launch(intent)
                                                },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Alarm Sound", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                                Text(ringtoneTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                            }
                                            
                                            // Edit Mode Buttons (Inside Card for smooth expansion)
                                            if (state == "edit") {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { isEditingPlan = false },
                                                        shape = RoundedCornerShape(28.dp),
                                                        modifier = Modifier.weight(1f).height(48.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                                    ) {
                                                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            viewModel.updateActivePlan()
                                                            isEditingPlan = false
                                                            Toast.makeText(context, "Plan updated", Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RoundedCornerShape(28.dp),
                                                        modifier = Modifier.weight(1f).height(48.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Text("Save Changes", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                // Cancel Plan Button
                                                Button(
                                                    onClick = { showCancelConfirmation = true },
                                                    shape = RoundedCornerShape(28.dp),
                                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color.Red,
                                                        contentColor = Color.White
                                                    ),
                                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                                ) {
                                                    Text("Cancel Plan", fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                // Permissions Warning - Notifications only
                if (!hasNotificationPermission) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Permissions Required",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    
                                    // Notification Permission
                                    Text(
                                        text = "To ensure alarms ring reliably, please allow Notifications.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Button(
                                        onClick = {
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Allow Notifications", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // Add spacing before schedule
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Schedule List
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Column {
                            Text(
                                text = if (uiState.alarmsSet && uiState.activeSchedule.isNotEmpty()) "Your Alarm Schedule" else "Preview Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                            val list =
                                if (uiState.alarmsSet && uiState.activeSchedule.isNotEmpty()) uiState.activeSchedule else uiState.schedule
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                if (list.isEmpty()) {
                                    Text(
                                        "No schedule generated yet.",
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    list.forEachIndexed { index, item ->
                                        val isSnoozed = uiState.alarmsSet && 
                                            uiState.snoozedAlarmIndex == index && 
                                            uiState.snoozeUntilEpochMillis > System.currentTimeMillis()
                                        ScheduleRow(
                                            item = item,
                                            formatter = timeFormatter,
                                            isSnoozed = isSnoozed,
                                            snoozeUntilMillis = uiState.snoozeUntilEpochMillis,
                                            onDismissSnooze = { viewModel.dismissSnooze() }
                                        )
                                        if (index < list.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.5f
                                                ),
                                                modifier = Modifier.padding(vertical = 8.dp)
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

            // Overlay Header
            val maxScrollPx = with(density) { 36.dp.toPx() } // Expansion range (100 - 64)
            
            val scrollProgress = remember { derivedStateOf { 
                if (listState.firstVisibleItemIndex == 0) {
                    (listState.firstVisibleItemScrollOffset / maxScrollPx).coerceIn(0f, 1f)
                } else {
                    1f
                }
            } }
            
            val collapseFraction by remember { scrollProgress }
            
            // Direct interpolation for smooth response (no snapping)
            val headerHeight = androidx.compose.ui.unit.lerp(100.dp, 64.dp, collapseFraction)
            val titleSize = androidx.compose.ui.util.lerp(36f, 20f, collapseFraction)
            
            // Alpha transitions
            val subtitleAlpha = (1f - collapseFraction * 2.5f).coerceIn(0f, 1f)
            // Smoother fade in for progress (starts earlier: 0.5 to 1.0)
            val collapsedAlpha = ((collapseFraction - 0.5f) * 2f).coerceIn(0f, 1f)
            val subtitleContainerHeight = androidx.compose.ui.unit.lerp(30.dp, 0.dp, collapseFraction)

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(headerHeight),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = if (collapseFraction > 0.9f) 3.dp else 0.dp
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Dawn Shift 🌅",
                            fontSize = titleSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Subtitle container with animated height for smooth layout
                        Column(
                            modifier = Modifier
                                .graphicsLayer { alpha = subtitleAlpha }
                                .height(subtitleContainerHeight)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Wake up earlier, bit by bit.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                    if (collapsedAlpha > 0.01f && uiState.alarmsSet) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.align(Alignment.CenterEnd).alpha(collapsedAlpha)
                        ) {
                            CircularProgressIndicator(
                                progress = { if (uiState.days > 0) uiState.completedCount / uiState.days.toFloat() else 0f },
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 4.dp
                            )
                        }
                    }
                }
            }
        }

        if (showStartTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showStartTimePicker = false },
                onConfirm = { viewModel.updateStartTime(it); showStartTimePicker = false },
                initialTime = uiState.startTime
            )
        }
        if (showTargetTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showTargetTimePicker = false },
                onConfirm = { viewModel.updateTargetTime(it); showTargetTimePicker = false },
                initialTime = uiState.targetTime
            )
        }
    }
}

@Composable
fun ScheduleRow(
    item: ScheduleItemData,
    formatter: DateTimeFormatter,
    isSnoozed: Boolean,
    snoozeUntilMillis: Long,
    onDismissSnooze: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val isNext = item.status == ItemStatus.NEXT
    val isCompleted = item.status == ItemStatus.COMPLETED

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isCompleted -> MaterialTheme.colorScheme.tertiaryContainer
                            isNext -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "${item.dayIndex}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.date.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSnoozed) {
                    val snoozeTime = java.time.Instant.ofEpochMilli(snoozeUntilMillis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime()
                    Text(
                        "Snoozed until ${snoozeTime.format(formatter)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.time.format(formatter),
                style = if (isNext) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

                if (isSnoozed) {
                    TextButton(
                        onClick = onDismissSnooze,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
}


@Composable
fun TimeInputRow(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
    formatter: DateTimeFormatter,
    enabled: Boolean = true,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle != null) 60.dp else 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            time.format(formatter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DaysPickerRow(days: Int, onDaysChanged: (Int) -> Unit, label: String = "Duration (Days)") {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (days > 1) onDaysChanged(days - 1) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.Remove, null, modifier = Modifier.size(20.dp))
            }
            Text(
                "$days",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { if (days < 120) onDaysChanged(days + 1) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    initialTime: LocalTime
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    LocalTime.of(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                )
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } },
        text = { TimePicker(state = timePickerState) }
    )
}

