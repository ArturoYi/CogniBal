package com.cyr.cognobal.navigation

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
import androidx.navigation3.scene.NavigationBackHandler
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneState
import androidx.navigation3.scene.rememberNavigationEventState
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import com.cyr.cognobal.ui.background.PageSolidBackground
import com.cyr.cognobal.ui.insets.HomeTabChrome

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberAppSceneState(navigator: AppNavigator): SceneState<NavKey> {
    val provider = entryProvider<NavKey> { appRoutes(navigator) }
    val billEntries = rememberHomeEntries(navigator.billStack, provider)
    val statisticsEntries = rememberHomeEntries(navigator.statisticsStack, provider)
    val assetsEntries = rememberHomeEntries(navigator.assetsStack, provider)
    val settingsEntries = rememberHomeEntries(navigator.settingsStack, provider)
    val entries = when (navigator.homeTab) {
        HomeTab.Bill -> billEntries
        HomeTab.Statistics -> statisticsEntries
        HomeTab.Assets -> assetsEntries
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

@Composable
private fun <T : Any> rememberPageSolidBackgroundDecorator(): NavEntryDecorator<T> {
    return remember {
        NavEntryDecorator { entry ->
            HomeTabChrome(isHomeTab = entry.metadata[HomeTabMetadata] == true) {
                PageSolidBackground {
                    entry.Content()
                }
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

internal fun Scene<NavKey>.showsHomeTab(): Boolean =
    entries.any { entry -> entry.metadata[HomeTabMetadata] == true }

private fun Scene<NavKey>.homeContentKey(): Any? =
    entries.firstOrNull { entry -> entry.metadata[HomeTabMetadata] == true }?.contentKey

private fun AnimatedContentTransitionScope<Scene<NavKey>>.switchesHomeTab(): Boolean {
    val from = initialState.homeContentKey()
    val to = targetState.homeContentKey()
    return from != null && to != null && from != to
}
