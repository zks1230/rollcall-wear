package com.juhao.rollcall

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.juhao.rollcall.data.DateFormats
import com.juhao.rollcall.navigation.AppKey
import com.juhao.rollcall.navigation.DayKey
import com.juhao.rollcall.navigation.HistoryKey
import com.juhao.rollcall.navigation.HomeKey
import com.juhao.rollcall.navigation.ImportKey
import com.juhao.rollcall.navigation.RosterKey
import com.juhao.rollcall.navigation.SettingsKey
import com.juhao.rollcall.navigation.StudentEditKey
import com.juhao.rollcall.state.RollCallController
import com.juhao.rollcall.theme.RollCallTheme
import com.juhao.rollcall.theme.accentForCounts
import com.juhao.rollcall.ui.components.AppButton
import com.juhao.rollcall.ui.components.HintText
import com.juhao.rollcall.ui.main.HomeScreen
import com.juhao.rollcall.ui.settings.SettingsScreen
import com.juhao.rollcall.utils.LocalScreenShape
import com.juhao.rollcall.utils.rememberScreenShape
import kotlinx.coroutines.delay

/**
 * 应用壳：主题、顶部时间胶囊、NavDisplay 路由。
 * 主题强调色由“当前班级状态”决定，全到=绿、有人请假迟到=黄、有人缺勤=红。
 */
@Composable
fun RollCallApp(controller: RollCallController) {
    val state by controller.state.collectAsStateWithLifecycle()
    val currentState = rememberUpdatedState(state)
    val context = LocalContext.current
    val backStack = rememberNavBackStack(HomeKey)
    val screenShape = rememberScreenShape()
    val accent = accentForCounts(state.counts)
    val scopeTag = remember { Any() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { controller.refreshToday() }

    // 二级页面支持系统返回键 / 侧滑返回。
    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    LaunchedEffect(controller) {
        controller.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val entryProvider = remember(backStack, scopeTag) {
        entryProvider<NavKey> {
            entry<HomeKey> {
                HomeScreen(
                    state = currentState.value,
                    onCycleAttendance = controller::cycleAttendance,
                    onClearAttendance = controller::clearTodayAttendance,
                    onModeChange = controller::setMode,
                    onToggleHomework = controller::toggleHomework,
                    onAllHomeworkSubmitted = controller::markAllHomeworkSubmitted,
                    onAllHomeworkMissing = controller::markAllHomeworkMissing,
                    onExport = controller::exportCsv,
                    onOpenHistory = { backStack.add(HistoryKey) },
                    onOpenRoster = { backStack.add(RosterKey) },
                    onOpenSettings = { backStack.add(SettingsKey) },
                )
            }

            entry<HistoryKey> {
                com.juhao.rollcall.ui.history.HistoryScreen(
                    state = currentState.value,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenDay = { date -> backStack.add(DayKey(date)) },
                )
            }

            entry<DayKey> { key ->
                val day = currentState.value.history.firstOrNull { it.date == key.date }
                if (day == null) {
                    FallbackScreen(text = "没有 ${key.date} 的记录") {
                        backStack.removeLastOrNull()
                    }
                } else {
                    com.juhao.rollcall.ui.history.DayDetailScreen(
                        day = day,
                        students = currentState.value.students,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
            }

            entry<RosterKey> {
                com.juhao.rollcall.ui.roster.RosterScreen(
                    state = currentState.value,
                    onBack = { backStack.removeLastOrNull() },
                    onAddStudent = { backStack.add(StudentEditKey(-1L)) },
                    onEditStudent = { id -> backStack.add(StudentEditKey(id)) },
                    onOpenImport = { backStack.add(ImportKey) },
                )
            }

            entry<StudentEditKey> { key ->
                val student = currentState.value.students.firstOrNull { it.id == key.studentId }
                com.juhao.rollcall.ui.roster.StudentEditScreen(
                    student = student,
                    onBack = { backStack.removeLastOrNull() },
                    onSave = { name ->
                        if (student == null) controller.addStudent(name) else controller.renameStudent(student.id, name)
                        backStack.removeLastOrNull()
                    },
                    onDelete = {
                        if (student != null) {
                            controller.deleteStudent(student.id)
                            backStack.removeLastOrNull()
                        }
                    },
                    onMoveUp = { student?.let { controller.moveStudent(it.id, -1) } },
                    onMoveDown = { student?.let { controller.moveStudent(it.id, 1) } },
                )
            }

            entry<ImportKey> {
                com.juhao.rollcall.ui.roster.ImportScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onImport = { names, replace ->
                        controller.importRoster(names, replace)
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<SettingsKey> {
                SettingsScreen(
                    state = currentState.value,
                    onBack = { backStack.removeLastOrNull() },
                    onDayModeChange = controller::setDayMode,
                    onExport = controller::exportCsv,
                    onRestoreSeedRoster = controller::restoreSeedRoster,
                    onUiScaleChange = controller::setUiScale,
                    onCornerRadiusChange = controller::setCornerRadius,
                )
            }
        }
    }

    val swipeDismissableSceneStrategy = rememberSwipeDismissableSceneStrategy<NavKey>()

    // 界面缩放：直接改 Compose 的 density，dp 和字号一起放大，立即生效、不用重启。
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, state.uiScale) {
        Density(
            density = baseDensity.density * state.uiScale.coerceIn(0.8f, 1.6f),
            fontScale = baseDensity.fontScale,
        )
    }
    // 圆角按“物理 dp”算：不跟着界面缩放一起放大，否则 60dp 会被放大成 75dp。
    val cornerRadiusPx = with(baseDensity) { state.cornerRadiusDp.dp.toPx() }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        RollCallTheme(accent = accent, isDay = state.dayMode) {
            CompositionLocalProvider(LocalScreenShape provides screenShape) {
                val cornerDp = with(scaledDensity) { cornerRadiusPx.toDp() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerDp))
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    AppScaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                        timeText = { ClockTimeText() },
                    ) {
                        NavDisplay(
                            backStack = backStack,
                            entryProvider = entryProvider,
                            sceneStrategies = listOf(swipeDismissableSceneStrategy),
                        )
                    }
                }
            }
        }
    }
}

/** 方屏上的时间胶囊：每分钟跟一次，顶部居中。 */
@Composable
private fun ClockTimeText() {
    var clock by remember { mutableStateOf(DateFormats.nowClock()) }

    LaunchedEffect(Unit) {
        while (true) {
            clock = DateFormats.nowClock()
            val delayMillis = (60_000L - System.currentTimeMillis() % 60_000L).coerceAtLeast(1L)
            delay(delayMillis)
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = clock,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                .padding(horizontal = 8.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun FallbackScreen(text: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HintText(text)
        AppButton(text = "返回", onClick = onBack, modifier = Modifier.padding(top = 10.dp))
    }
}
