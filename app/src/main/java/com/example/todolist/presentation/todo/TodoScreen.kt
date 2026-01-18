package com.example.todolist.presentation.todo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.todolist.data.UserData
import com.example.todolist.data.model.Priority
import com.example.todolist.data.model.Todo

// Palette Warna Premium (Eye-Friendly)
val BackgroundLight = Color(0xFFF8F9FE)
val SurfaceColor = Color(0xFFFFFFFF)
val PrimaryBlue = Color(0xFF3F72AF)
val TextDark = Color(0xFF112D4E)
val TextGray = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    userData: UserData?,
    viewModel: TodoViewModel,
    onSignOut: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    var todoText by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    val todos by viewModel.todos.collectAsState()

    LaunchedEffect(key1 = userData?.userId) {
        userData?.userId?.let { viewModel.observeTodos(it) }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            // Top Bar yang lebih clean dan lapang
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundLight
                ),
                title = {
                    Text(
                        "My Tasks",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                actions = {
                    userData?.let {
                        AsyncImage(
                            model = it.profilePictureUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 20.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { onSignOut() }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. Progress Dashboard Section
            // Menggunakan diagram progres untuk memberikan umpan balik visual instan bagi pengguna

            PremiumDashboard(todos)

            // 2. Search & Filter Section
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                ModernSearchBar(viewModel)

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Quick Input Field (Sangat Ringkas)
                QuickAddCard(
                    text = todoText,
                    onTextChange = { todoText = it },
                    currentPriority = selectedPriority,
                    onPriorityChange = { selectedPriority = it },
                    onAddClick = {
                        if (todoText.isNotBlank()) {
                            userData?.userId?.let { viewModel.add(it, todoText, selectedPriority.name) }
                            todoText = ""
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Tasks List
                Text(
                    "Ongoing Tasks",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = TextGray,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(todos, key = { it.id }) { todo ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    userData?.userId?.let { uid -> viewModel.delete(uid, todo.id) }
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                SwipeDeleteBackground(dismissState)
                            }
                        ) {
                            TaskItemPremium(todo, onNavigateToEdit) {
                                userData?.userId?.let { uid -> viewModel.toggle(uid, todo) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumDashboard(todos: List<Todo>) {
    val completed = todos.count { it.isCompleted }
    val total = todos.size
    val progress = if (total > 0) completed.toFloat() / total else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.horizontalGradient(listOf(Color(0xFF3F72AF), Color(0xFF112D4E)))
            )
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Daily Goal", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Text(
                    if (progress == 1f) "All tasks done! 🎉" else "$completed of $total tasks",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    color = Color.White,
                    strokeWidth = 6.dp,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(60.dp)
                )
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ModernSearchBar(viewModel: TodoViewModel) {
    TextField(
        value = viewModel.searchQuery.value,
        onValueChange = {
            viewModel.searchQuery.value = it
            viewModel.updateFilteredList()
        },
        placeholder = { Text("Search tasks...", color = TextGray) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextGray) },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@Composable
fun QuickAddCard(
    text: String,
    onTextChange: (String) -> Unit,
    currentPriority: Priority,
    onPriorityChange: (Priority) -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("What needs to be done?", fontSize = 16.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = onAddClick,
                    enabled = text.isNotBlank(),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (text.isNotBlank()) PrimaryBlue else BackgroundLight)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }

            Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp)) {
                Priority.entries.forEach { p ->
                    val isSelected = currentPriority == p
                    val pColor = p.color

                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onPriorityChange(p) },
                        color = if (isSelected) pColor.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BackgroundLight)
                    ) {
                        Text(
                            p.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) pColor else TextGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemPremium(todo: Todo, onEdit: (String) -> Unit, onToggle: () -> Unit) {
    val p = Priority.fromString(todo.priority)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(todo.id) },
        color = SurfaceColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indikator Prioritas (Titik Lembut)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(p.color)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Custom Checkbox UX
            IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (todo.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (todo.isCompleted) PrimaryBlue else TextGray.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = todo.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                    color = if (todo.isCompleted) TextGray.copy(alpha = 0.6f) else TextDark,
                    fontWeight = FontWeight.Medium
                )
            )

            Icon(Icons.Default.ChevronRight, null, tint = BackgroundLight, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDeleteBackground(state: SwipeToDismissBoxState) {
    val color = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart)
        Color(0xFFFFEBEB) else Color.Transparent

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(Icons.Default.DeleteSweep, "Delete", tint = Color.Red.copy(alpha = 0.7f))
    }
}