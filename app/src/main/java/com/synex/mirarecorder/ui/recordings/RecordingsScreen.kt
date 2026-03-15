package com.synex.mirarecorder.ui.recordings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(viewModel: RecordingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recordings") },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Streamers") },
                            onClick = {
                                viewModel.setSelectedStreamer(null)
                                showFilterMenu = false
                            },
                        )
                        state.uniqueStreamers.forEach { streamer ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = streamer,
                                        fontWeight = if (streamer == state.selectedStreamer) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    viewModel.setSelectedStreamer(streamer)
                                    showFilterMenu = false
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Stats bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                StatItem(
                    icon = Icons.Default.VideoLibrary,
                    value = "${state.totalVideos}",
                    label = "Videos",
                )
                StatItem(
                    icon = Icons.Default.AccessTime,
                    value = String.format("%.1f", state.totalHours),
                    label = "Hours",
                )
            }

            // Search bar
            TextField(
                value = state.searchText,
                onValueChange = { viewModel.setSearchText(it) },
                placeholder = { Text("Search recordings...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Selected streamer chip
            state.selectedStreamer?.let { streamer ->
                Text(
                    text = "Filtered: $streamer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadRecordings() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = state.filteredRecordings,
                        key = { it.filename },
                    ) { recording ->
                        RecordingCard(
                            recording = recording,
                            thumbnailUrl = viewModel.thumbnailUrl(recording.filename),
                            avatarUrl = viewModel.avatarUrl(recording.displayName),
                            isLive = state.isLive(recording),
                            onDownloadClick = {
                                val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                val request = android.app.DownloadManager.Request(android.net.Uri.parse(viewModel.videoUrl(recording.filename)))
                                    .setTitle(recording.filename)
                                    .setDescription("Downloading video")
                                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MOVIES, recording.filename) // Require WRITE_EXTERNAL_STORAGE for older APIs, but scoped storage doesn't need it.
                                downloadManager.enqueue(request)
                            },
                            onClick = {
                                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                    putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, viewModel.videoUrl(recording.filename))
                                    putExtra(VideoPlayerActivity.EXTRA_TITLE, recording.displayName)
                                }
                                context.startActivity(intent)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}
