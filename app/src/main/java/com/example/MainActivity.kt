package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.notifications.ReminderReceiver
import com.example.services.BiometricLockHelper
import com.example.services.SpeechVoiceManager
import com.example.services.configureAppCheck
import com.example.ui.components.BadgeUnlockGlassToast
import com.example.ui.components.BottomNavTab
import com.example.ui.components.GlassBottomBar
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidBackdrop
import com.example.ui.components.UndoGlassBanner
import com.example.ui.screens.AgentScreen
import com.example.ui.screens.AppLockOverlay
import com.example.ui.screens.BadgesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewEntryModal
import com.example.ui.screens.PlansLoansScreen
import com.example.ui.screens.RecurringScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransferModal
import com.example.ui.screens.WalletsScreen
import com.example.ui.screens.WeeklyRecapScreen
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.TakaFlowTheme
import com.example.ui.viewmodel.AgentViewModel
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val agentViewModel: AgentViewModel by viewModels()
    private lateinit var speechManager: SpeechVoiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModel work
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        speechManager = SpeechVoiceManager(applicationContext)

        val initialRoute = intent?.getStringExtra(ReminderReceiver.EXTRA_ROUTE)

        setContent {
            val state by appViewModel.uiState.collectAsStateWithLifecycle()
            val agentState by agentViewModel.state.collectAsStateWithLifecycle()

            TakaFlowTheme(themeMode = state.settings.themeMode) {
                TakaFlowAppShell(
                    appViewModel = appViewModel,
                    agentViewModel = agentViewModel,
                    state = state,
                    agentState = agentState,
                    speechManager = speechManager,
                    initialDeepLink = initialRoute,
                    onRequestBiometric = { onError ->
                        if (BiometricLockHelper.canAuthenticate(this)) {
                            BiometricLockHelper.authenticate(
                                activity = this,
                                onSuccess = { appViewModel.unlockApp() },
                                onError = onError,
                            )
                        } else {
                            appViewModel.unlockApp()
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.getStringExtra(ReminderReceiver.EXTRA_ROUTE)) {
            "add" -> appViewModel.openNewEntryManual()
        }
    }

    override fun onResume() {
        super.onResume()
        appViewModel.onAppForegrounded()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechManager.isInitialized) {
            speechManager.release()
        }
    }
}

@Composable
private fun TakaFlowAppShell(
    appViewModel: AppViewModel,
    agentViewModel: AgentViewModel,
    state: com.example.ui.viewmodel.MainUiState,
    agentState: com.example.ui.viewmodel.AgentUiState,
    speechManager: SpeechVoiceManager,
    initialDeepLink: String?,
    onRequestBiometric: (onError: (String) -> Unit) -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    LaunchedEffect(initialDeepLink) {
        when (initialDeepLink) {
            "add" -> appViewModel.openNewEntryManual()
            "recurring" -> navController.navigate("recurring")
            "plans" -> navController.navigate(BottomNavTab.PLANS.route)
        }
    }

    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage != null) {
            delay(2_800L)
            appViewModel.dismissStatus()
        }
    }

    fun navigateTopTab(tab: BottomNavTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (state.isLocked) {
        AppLockOverlay(
            onRequestBiometric = onRequestBiometric,
            onFallbackUnlock = { appViewModel.unlockApp() },
        )
        return
    }

    val showBottomBar = currentRoute in setOf(
        BottomNavTab.HOME.route,
        BottomNavTab.HISTORY.route,
        BottomNavTab.PLANS.route,
        BottomNavTab.SETTINGS.route,
    )

    LiquidBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavTab.HOME.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(BottomNavTab.HOME.route) {
                    HomeScreen(
                        state = state,
                        onOpenWallets = { navController.navigate("wallets") },
                        onOpenVoiceEntry = { appViewModel.openNewEntryVoice() },
                        onOpenReport = { navController.navigate("report") },
                        onOpenRecap = { navController.navigate("recap") },
                        onOpenBadges = { navController.navigate("badges") },
                        onOpenRecurring = { navController.navigate("recurring") },
                        onOpenAgent = { navController.navigate("agent") },
                        onToggleTotalBalanceHidden = { appViewModel.toggleTotalBalanceHidden() },
                        onSelectPeriodPreset = { appViewModel.selectPeriodPreset(it) },
                        onSetCustomRange = { appViewModel.setCustomPeriodRange(it) },
                        onMarkNothingSpent = { appViewModel.markNothingSpentToday() },
                        onUndoCheckinToday = { appViewModel.undoCheckinToday() },
                        onSetBudget = { cat, amt -> appViewModel.setBudget(cat, amt) },
                        onRemoveBudget = { appViewModel.removeBudget(it) },
                    )
                }

                composable(BottomNavTab.HISTORY.route) {
                    HistoryScreen(
                        state = state,
                        onEditTransaction = { appViewModel.openNewEntryManual(it) },
                        onEditTransfer = { appViewModel.openTransferSheet(it) },
                        onDeleteTransaction = { appViewModel.deleteTransactionWithUndo(it) },
                    )
                }

                composable(BottomNavTab.PLANS.route) {
                    PlansLoansScreen(
                        state = state,
                        onSavePlan = { appViewModel.savePlan(it) },
                        onDeletePlan = { appViewModel.deletePlanWithUndo(it) },
                        onCompleteItemPlan = { plan, amt, wId -> appViewModel.completeItemPlan(plan, amt, wId) },
                        onReopenPlan = { appViewModel.reopenPlan(it) },
                        onAddRepayment = { loan, amt, note -> appViewModel.addLoanRepayment(loan, amt, note) },
                        onRemoveRepayment = { loan, repId -> appViewModel.removeLoanRepayment(loan, repId) },
                        onSettleLoan = { appViewModel.settleLoan(it) },
                    )
                }

                composable(BottomNavTab.SETTINGS.route) {
                    SettingsScreen(state = state, viewModel = appViewModel)
                }

                composable("report") {
                    ReportScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable("wallets") {
                    WalletsScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onOpenTransfer = { appViewModel.openTransferSheet() },
                        onToggleTotalHidden = { appViewModel.toggleTotalBalanceHidden() },
                        onToggleWalletHidden = { appViewModel.toggleWalletHidden(it) },
                        onAddWallet = { n, b, c, i -> appViewModel.addWallet(n, b, c, i) },
                        onUpdateWallet = { appViewModel.saveWallet(it) },
                        onDeleteWallet = { appViewModel.deleteWalletWithUndo(it) },
                    )
                }

                composable("recurring") {
                    RecurringScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onSaveRule = { appViewModel.saveRecurringRule(it) },
                        onToggleActive = { appViewModel.toggleRecurringActive(it) },
                        onDeleteRule = { appViewModel.deleteRecurringWithUndo(it) },
                    )
                }

                composable("agent") {
                    AgentScreen(
                        appState = state,
                        agentState = agentState,
                        agentViewModel = agentViewModel,
                        settingsStore = appViewModel.settingsStore,
                        speechManager = speechManager,
                        onToggleVoiceReply = { appViewModel.setAiVoiceReplyEnabled(it) },
                        onExecuteAction = { appViewModel.executeAgentAction(it) },
                        onBackExit = { navController.popBackStack() },
                    )
                }

                composable("recap") {
                    WeeklyRecapScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable("badges") {
                    BadgesScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            if (showBottomBar) {
                GlassBottomBar(
                    currentRoute = currentRoute,
                    onSelectTab = ::navigateTopTab,
                    onOpenQuickAdd = { appViewModel.openNewEntryManual() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Badge unlock toast at top
            BadgeUnlockGlassToast(
                badge = state.badgeToast,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            // Status toast
            val P = LocalGlassPalette.current
            AnimatedVisibility(
                visible = state.statusMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                state.statusMessage?.let { msg ->
                    GlassCard(
                        tint = P.obsidian,
                        cornerRadius = 20.dp,
                        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = P.pureWhite)
                    }
                }
            }

            // Undo banner above bottom bar
            UndoGlassBanner(
                message = state.undoBanner?.message,
                onUndo = { state.undoBanner?.onUndo?.invoke() },
                onDismiss = { appViewModel.dismissUndo() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (showBottomBar) 92.dp else 20.dp,
                    ),
            )
        }
    }

    // Global New Entry Modal
    NewEntryModal(
        sheetState = state.newEntrySheet,
        wallets = state.wallets,
        categories = state.categories,
        favorites = state.favorites,
        speechManager = speechManager,
        onDismiss = { appViewModel.closeNewEntry() },
        onSaveManual = { ed, t, a, c, d, w, ts ->
            appViewModel.saveTransaction(ed, t, a, c, d, w, ts)
        },
        onSaveDrafts = { drafts, wId, ts ->
            appViewModel.saveDrafts(drafts, wId, ts)
        },
        onTriggerFavorite = { appViewModel.triggerFavorite(it) },
        onAddFavorite = { lbl, amt, t, cat, wId -> appViewModel.saveFavorite(lbl, amt, t, cat, wId) },
        onDeleteFavorite = { appViewModel.deleteFavorite(it) },
        onAddCategory = { name, type -> appViewModel.addCategory(name, type) },
    )

    // Global Transfer Modal
    TransferModal(
        sheetState = state.transferSheet,
        wallets = state.wallets,
        onDismiss = { appViewModel.closeTransferSheet() },
        onSave = { ed, fromId, toId, amt, note, ts ->
            appViewModel.saveTransfer(ed, fromId, toId, amt, note, ts)
        },
    )
}
