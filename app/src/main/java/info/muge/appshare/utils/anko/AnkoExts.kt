package info.muge.appshare.utils.anko

import android.content.Context
import info.muge.appshare.MyApplication


//returns dip(dp) dimension value in pixels
fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
fun Context.dip(value: Float): Int = (value * resources.displayMetrics.density).toInt()

val Int.dp:Int
get() = MyApplication.instance.dip(this)
val Float.dp:Int
get() = MyApplication.instance.dip(this)

//return sp dimension value in pixels
fun Context.sp(value: Int): Float = (value * resources.displayMetrics.scaledDensity)
fun Context.sp(value: Float): Float = (value * resources.displayMetrics.scaledDensity)


val Int.sp:Float
    get() = MyApplication.instance.sp(this)
val Float.sp:Float
    get() = MyApplication.instance.sp(this)