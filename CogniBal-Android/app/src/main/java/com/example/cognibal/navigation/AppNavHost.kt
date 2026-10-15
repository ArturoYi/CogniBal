package com.example.cognibal.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.example.cognibal.ui.background.PageSolidBackground
import androidx.navigation3.scene.NavigationBackHandler
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneState
import androidx.navigation3.scene.rememberNavigationEventState
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay

/**
 * 全局导航宿主。
 *
 * 宽屏用列表-详情并排，窄屏把场景让给单页，这样微信式滑动只发生在手机上。
 * 不要给宿主再垫安全区，每个目的地自己处理状态栏和导航栏。
 *
 * 两条首页栈都会装饰并记住状态。切走的那条不交给 [NavDisplay]，回来时滚动和表单还在。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberAppSceneState(navigator: AppNavigator): SceneState<NavKey> {
    val provider = entryProvider<NavKey> { appRoutes(navigator) }
    val billEntries = rememberHomeEntries(navigator.billStack, provider)
    val settingsEntries = rememberHomeEntries(navigator.settingsStack, provider)
    val entries = when (navigator.homeTab) {
        HomeTab.Bill -> billEntries
        HomeTab.Settings -> settingsEntries
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    return rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(listDetailStrategy),
        onBack = navigator::back,
    )
}

@Composable
private fun rememberHomeEntries(
    backStack: List<NavKey>,
    provider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> =
    rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(
            rememberPageSolidBackgroundDecorator(),
            rememberSaveableStateHolderNavEntryDecorator(),
        ),
        entryProvider = provider,
    )

/**
 * 每个目的地最外层铺主题纯色。侧滑时这一层跟着页面走，当前页不会透出上一页。
 */
@Composable
private fun <T : Any> rememberPageSolidBackgroundDecorator(): NavEntryDecorator<T> {
    return remember {
        NavEntryDecorator { entry ->
            PageSolidBackground {
                entry.Content()
            }
        }
    }
}

@Composable
internal fun AppNavHost(
    sceneState: SceneState<NavKey>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationEventState = rememberNavigationEventState(sceneState)
    NavigationBackHandler(
        sceneState = sceneState,
        state = navigationEventState,
        onBackCompleted = onBack,
    )
    NavDisplay(
        sceneState = sceneState,
        navigationEventState = navigationEventState,
        modifier = modifier,
        transitionSpec = {
            if (switchesHomeTab()) noPageTransition() else wechatPush()
        },
        popTransitionSpec = {
            if (switchesHomeTab()) noPageTransition() else wechatPop()
        },
        predictivePopTransitionSpec = {
            if (switchesHomeTab()) noPageTransition() else wechatPop()
        },
    )
}

/** 当前场景里还有账单或设置列表。窄屏二级页和记一笔都不是。 */
internal fun Scene<NavKey>.showsHomeTab(): Boolean =
    entries.any { entry -> entry.metadata[HomeTabMetadata] == true }

private fun Scene<NavKey>.homeContentKey(): Any? =
    entries.firstOrNull { entry -> entry.metadata[HomeTabMetadata] == true }?.contentKey

/** 两个首页标签对调。这种切换不滑整页。 */
private fun AnimatedContentTransitionScope<Scene<NavKey>>.switchesHomeTab(): Boolean {
    val from = initialState.homeContentKey()
    val to = targetState.homeContentKey()
    return from != null && to != null && from != to
}
