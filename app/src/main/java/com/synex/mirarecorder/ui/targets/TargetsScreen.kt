package com.synex.mirarecorder.ui.targets

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.synex.mirarecorder.data.model.Target
import com.synex.mirarecorder.ui.theme.LiveRed
import com.synex.mirarecorder.util.colorHash

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetsScreen(viewModel: TargetsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Targets") })
        },
        floatingActionButton = {
            if (state.isUnlocked) {
                FloatingActionButton(onClick = { viewModel.setShowAddDialog(true) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add target")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search bar
            TextField(
                value = state.searchText,
                onValueChange = { viewModel.setSearchText(it) },
                placeholder = { Text("Search targets...") },
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

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadTargets() },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.filteredTargets.isEmpty() && !state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (state.targets.isEmpty()) "No targets added yet" else "No matching targets",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.filteredTargets,
                            key = { it.userId },
                        ) { target ->
                            TargetRow(
                                target = target,
                                avatarUrl = viewModel.avatarUrl(target),
                                isUnlocked = state.isUnlocked,
                                onToggle = { enabled -> viewModel.toggleTarget(target, enabled) },
                                onDelete = { viewModel.deleteTarget(target) },
                            )
                        }
                    }
                }
            }

            // Error message
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = LiveRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    if (state.showAddDialog) {
        AddTargetDialog(
            onDismiss = { viewModel.setShowAddDialog(false) },
            onAdd = { userId -> viewModel.addTarget(userId) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetRow(
    target: Target,
    avatarUrl: String?,
    isUnlocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LiveRed)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colorHash(target.name)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = target.name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = target.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (target.isLiveRecording) {
                        Spacer(modifier = Modifier.width(8.dp))
                        LiveIndicator()
                    }
                }
                Text(
                    text = target.userId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            if (isUnlocked) {
                Switch(
                    checked = target.enabled,
                    onCheckedChange = onToggle,
                )
            }
        }
    }
}

@Composable
private fun LiveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveAlpha",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(LiveRed.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .alpha(alpha),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(LiveRed),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "LIVE",
            color = LiveRed,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
