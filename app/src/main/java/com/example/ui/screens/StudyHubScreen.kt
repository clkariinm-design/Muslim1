package com.nuur.muslim.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuur.muslim.data.local.AyahEntity
import com.nuur.muslim.data.local.BookmarkWithAyah
import com.nuur.muslim.data.local.NoteWithAyah
import com.nuur.muslim.data.local.ReadingPlanEntity
import com.nuur.muslim.data.local.SurahEntity
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.localization.AppStrings
import com.nuur.muslim.ui.theme.AmiriFontFamily

@Composable
fun StudyHubScreen(
    settings: UserSettings,
    strings: AppStrings,
    surahs: List<SurahEntity>,
    bookmarks: List<BookmarkWithAyah>,
    notes: List<NoteWithAyah>,
    readingPlans: List<ReadingPlanEntity>,
    downloadedCounts: Map<Int, Int>,
    onOpenReader: (Int, Int) -> Unit,
    onRemoveBookmark: (Int, Int, Int) -> Unit,
    onSaveEditedNote: (NoteWithAyah, String, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onCreatePlan: (String, Int) -> Unit,
    onDeletePlan: (Long) -> Unit,
    onResetKhatam: () -> Unit,
    onDeleteOfflineSurah: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingNote by remember { mutableStateOf<NoteWithAyah?>(null) }
    var showCreatePlanDialog by remember { mutableStateOf(false) }

    val tabs = listOf(strings.bookmarksTab, strings.notesTab, strings.plansTab, strings.offlineTab)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("study_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    if (bookmarks.isEmpty()) {
                        EmptyStudyState(strings.noBookmarksYet)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(bookmarks, key = { it.bookmark.id }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onOpenReader(item.bookmark.surahNumber, item.bookmark.ayahNumber)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Bookmark,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    "${item.surahEnglishName} (${item.bookmark.surahNumber}:${item.bookmark.ayahNumber})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    onRemoveBookmark(
                                                        item.bookmark.globalAyahNumber,
                                                        item.bookmark.surahNumber,
                                                        item.bookmark.ayahNumber
                                                    )
                                                }
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete)
                                            }
                                        }
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                            Text(
                                                text = item.textUthmani,
                                                fontFamily = AmiriFontFamily,
                                                fontSize = 20.sp,
                                                lineHeight = 34.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.textSomali,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (notes.isEmpty()) {
                        EmptyStudyState(strings.noNotesYet)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(notes, key = { it.note.id }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onOpenReader(item.note.surahNumber, item.note.ayahNumber)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${item.surahEnglishName} (${item.note.surahNumber}:${item.note.ayahNumber})",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row {
                                                IconButton(onClick = { editingNote = item }) {
                                                    Icon(Icons.Default.Edit, contentDescription = strings.editNote)
                                                }
                                                IconButton(onClick = { onDeleteNote(item.note.id) }) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete)
                                                }
                                            }
                                        }
                                        if (item.note.title.isNotBlank()) {
                                            Text(
                                                text = item.note.title,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        Text(
                                            text = item.note.body,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    val readCount = settings.khatamAyahsRead.coerceIn(0, 6236)
                    val progress = readCount / 6236f
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = strings.khatamProgress,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("$readCount / 6,236 ${strings.ayahsReadLabel} (${(progress * 100).toInt()}%)")
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { showCreatePlanDialog = true },
                                            modifier = Modifier.testTag("create_plan_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(strings.createReadingPlan)
                                        }
                                        OutlinedButton(onClick = onResetKhatam) {
                                            Text(strings.resetKhatam)
                                        }
                                    }
                                }
                            }
                        }
                        items(readingPlans, key = { it.id }) { plan ->
                            val remaining = (6236 - readCount).coerceAtLeast(0)
                            val daily = (remaining / plan.targetDays.coerceAtLeast(1)).coerceAtLeast(1)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(plan.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${plan.targetDays} ${strings.daysWord} • ${strings.dailyTargetLabel}: $daily ${strings.ayahsWord}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onDeletePlan(plan.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    val offlineSurahs = surahs.filter { (downloadedCounts[it.number] ?: 0) > 0 }
                    if (offlineSurahs.isEmpty()) {
                        EmptyStudyState(strings.noOfflineAudioYet)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(offlineSurahs, key = { it.number }) { surah ->
                                val dl = downloadedCounts[surah.number] ?: 0
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenReader(surah.number, 1) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(surah.englishName, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "$dl / ${surah.ayahCount} ${strings.ayahsWord}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDeleteOfflineSurah(surah.number) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = strings.deleteOfflineAudio)
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

    editingNote?.let { noteItem ->
        NoteEditorDialog(
            surahNumber = noteItem.note.surahNumber,
            ayahNumber = noteItem.note.ayahNumber,
            initialTitle = noteItem.note.title,
            initialBody = noteItem.note.body,
            strings = strings,
            onSave = { title, body ->
                onSaveEditedNote(noteItem, title, body)
                editingNote = null
            },
            onDismiss = { editingNote = null }
        )
    }

    if (showCreatePlanDialog) {
        CreateReadingPlanDialog(
            strings = strings,
            onCreate = { title, days ->
                onCreatePlan(title, days)
                showCreatePlanDialog = false
            },
            onDismiss = { showCreatePlanDialog = false }
        )
    }
}

@Composable
private fun EmptyStudyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun CreateReadingPlanDialog(
    strings: AppStrings,
    onCreate: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("Khatam 30") }
    var daysStr by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.createReadingPlan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.readingPlanTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = daysStr,
                    onValueChange = { daysStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.planDaysLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90).forEach { preset ->
                        OutlinedButton(onClick = {
                            daysStr = preset.toString()
                            title = "Khatam $preset"
                        }) {
                            Text("${preset}d", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val days = daysStr.toIntOrNull()?.coerceIn(7, 365) ?: 30
                onCreate(title, days)
            }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
