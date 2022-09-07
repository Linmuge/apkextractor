package info.muge.appshare.utils

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import info.muge.appshare.MyApplication


val Int.resToColor : Int @ColorInt
get() = ContextCompat.getColor(MyApplication.instance,this)

fun Int.resToColor(context: Context) : Int{
    return ContextCompat.getColor(context,this)
}

fun Int.resToDrawable(context: Context) : Drawable{
    return ContextCompat.getDrawable(context,this)!!
}

val Int.resToDrawable : Drawable?
get() = ContextCompat.getDrawable(MyApplication.instance,this)

