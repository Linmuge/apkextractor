package info.muge.appshare.utils

import android.content.Context
import android.util.TypedValue
import androidx.annotation.ColorInt
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import info.muge.appshare.R

val Context.colorPrimary :Int @ColorInt
get(){
    return getThemeColor(R.attr.colorPrimary)
}
val Context.colorOnPrimary :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
}
val Context.colorSurface :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorSurface)
}
val Context.colorOnSurface :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorOnSurface)
}
val Context.colorOnSurfaceVariant :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
}
val Context.colorSurfaceContainer :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorSurfaceContainer)
}
val Context.colorSurfaceContainerHigh :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorSurfaceContainerHigh)
}
val Context.colorOutlineVariant :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorOutlineVariant)
}
val Context.colorSurfaceContainerLow :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorSurfaceContainerLow)
}
val Context.colorOnPrimaryContainer :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)
}
val Context.colorPrimaryContainer :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer)
}
val Context.colorSecondary :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorSecondary)
}
val Context.colorSecondaryContainer :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorSecondaryContainer)
}
val Context.colorOnSecondaryContainer :Int @ColorInt
get(){
    return getThemeColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
}
val Context.colorError :Int @ColorInt
get(){
    return getThemeColor(R.attr.colorError)
}
@ColorInt
private fun Context.getThemeColor(colorInt: Int):Int{
    val typedValue = TypedValue()
    theme.resolveAttribute(colorInt, typedValue, true)
    val defaultColor =  typedValue.data
    return if (DynamicColors.isDynamicColorAvailable()){
        MaterialColors.getColor(this, colorInt,defaultColor)
    }else{
        defaultColor
    }
}