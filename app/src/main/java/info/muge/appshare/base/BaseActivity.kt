package info.muge.appshare.base

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.BarUtils
import info.muge.appshare.R
import info.muge.appshare.utils.resToColor
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity(), BaseViewBinding<VB>{


    internal val binding: VB by lazy(mode = LazyThreadSafetyMode.NONE) {
        getViewBinding(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window
        BarUtils.setStatusBarLightMode(this,resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK=== Configuration.UI_MODE_NIGHT_NO)
        BarUtils.transparentStatusBar(this)
        BarUtils.setStatusBarColor(this,R.color.titleViewBgColor.resToColor(this))

        setContentView(binding.root)

        binding.initView()
    }


    fun <VB: ViewBinding> Any.getViewBinding(inflater: LayoutInflater): VB {
        val vbClass =  (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<VB>>()
        val inflate = vbClass[0].getDeclaredMethod("inflate", LayoutInflater::class.java)
        return  inflate.invoke(null, inflater) as VB
    }

}

interface BaseViewBinding<VB : ViewBinding> {
    fun VB.initView()
}