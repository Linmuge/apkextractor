package info.muge.appshare.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Material Design 3 形状定义
 */
val AppShapes = Shapes(
    small = RoundedCornerShape(AppDimens.Radius.sm),
    medium = RoundedCornerShape(AppDimens.Radius.md),
    large = RoundedCornerShape(AppDimens.Radius.lg),
    extraLarge = RoundedCornerShape(AppDimens.Radius.xl)
)
