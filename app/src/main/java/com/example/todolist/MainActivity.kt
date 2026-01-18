package com.example.todolist // Sesuaikan dengan package asli Anda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset // PERBAIKAN: Import wajib untuk IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.todolist.data.GoogleAuthUiClient
import com.example.todolist.presentation.sign_in.SignInScreen
import com.example.todolist.presentation.sign_in.SignInViewModel
import com.example.todolist.presentation.todo.EditTodoScreen
import com.example.todolist.presentation.todo.TodoScreen
import com.example.todolist.presentation.todo.TodoViewModel
import com.example.todolist.ui.theme.ToDoListTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(context = applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val todoViewModel = viewModel<TodoViewModel>()

                    // Definisi animasi transisi premium
                    val animSpec = tween<IntOffset>(durationMillis = 600, easing = FastOutSlowInEasing)
                    val fadeSpec = tween<Float>(durationMillis = 600)

                    NavHost(
                        navController = navController,
                        startDestination = "sign_in",
                        // PERMINTAAN: Animasi transisi antar page super mulus
                        enterTransition = { slideInHorizontally(animSpec) { it } + fadeIn(fadeSpec) },
                        exitTransition = { slideOutHorizontally(animSpec) { -it / 3 } + fadeOut(fadeSpec) },
                        popEnterTransition = { slideInHorizontally(animSpec) { -it } + fadeIn(fadeSpec) },
                        popExitTransition = { slideOutHorizontally(animSpec) { it / 3 } + fadeOut(fadeSpec) }
                    ) {
                        // --- PROSES LOGIN (DIPERTAHANKAN) ---
                        composable("sign_in") {
                            val viewModel = viewModel<SignInViewModel>()
                            val state by viewModel.state.collectAsStateWithLifecycle()

                            LaunchedEffect(key1 = state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    navController.navigate("todo_list") {
                                        popUpTo("sign_in") { inclusive = true }
                                    }
                                }
                            }
                            SignInScreen(
                                state = state,
                                onSignInClick = {
                                    lifecycleScope.launch {
                                        val result = googleAuthUiClient.signIn()
                                        viewModel.onSignInResult(result)
                                    }
                                }
                            )
                        }

                        // --- HALAMAN UTAMA TODO ---
                        composable("todo_list") {
                            TodoScreen(
                                userData = googleAuthUiClient.getSignedInUser(),
                                viewModel = todoViewModel,
                                onSignOut = {
                                    lifecycleScope.launch {
                                        googleAuthUiClient.signOut()
                                        navController.navigate("sign_in") {
                                            popUpTo("todo_list") { inclusive = true }
                                        }
                                    }
                                },
                                onNavigateToEdit = { todoId ->
                                    navController.navigate("edit_todo/$todoId")
                                }
                            )
                        }

                        // --- HALAMAN EDIT ---
                        composable(
                            route = "edit_todo/{todoId}",
                            arguments = listOf(navArgument("todoId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val todoId = backStackEntry.arguments?.getString("todoId") ?: ""
                            val todos by todoViewModel.todos.collectAsStateWithLifecycle()
                            val todo = todos.find { it.id == todoId }
                            val userId = googleAuthUiClient.getSignedInUser()?.userId ?: ""

                            todo?.let {
                                EditTodoScreen(
                                    todo = it,
                                    onSave = { newTitle, newPriority ->
                                        todoViewModel.update(userId, todoId, newTitle, newPriority)
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}