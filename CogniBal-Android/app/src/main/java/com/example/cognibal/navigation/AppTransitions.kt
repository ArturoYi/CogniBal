package com.example.cognibal.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

/**
 * 仿微信的推入：新页从末端滑入，原页让出约四分之一。没有淡入淡出，也不改容器尺寸。
 *
 * 宽屏列表-详情由场景自己排，不走这条整页滑动。窄屏一次只显示一页，才用它。
 */
internal fun wechatPush(): ContentTransform {
    val slide = pageSlide()
    return ContentTransform(
        targetContentEnter = slideInHorizontally(
            animationSpec = slide,
            initialOffsetX = { width -> width },
        ),
        initialContentExit = slideOutHorizontally(
            animationSpec = slide,
            targetOffsetX = { width -> -width / 4 },
        ),
        sizeTransform = null,
    )
}

/** 返回时方向相反，时长和曲线与进入相同。 */
internal fun wechatPop(): ContentTransform {
    val slide = pageSlide()
    return ContentTransform(
        targetContentEnter = slideInHorizontally(
            animationSpec = slide,
            initialOffsetX = { width -> -width / 4 },
        ),
        initialContentExit = slideOutHorizontally(
            animationSpec = slide,
            targetOffsetX = { width -> width },
        ),
        sizeTransform = null,
    )
}

private fun pageSlide() = tween<IntOffset>(durationMillis = 300, easing = PageSlideEasing)

private val PageSlideEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
