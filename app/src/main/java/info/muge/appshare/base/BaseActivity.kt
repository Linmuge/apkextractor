package info.muge.appshare.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity(), BaseViewBinding<VB>{


    internal val binding: VB by lazy(mode = LazyThreadSafetyMode.NONE) {
        getViewBinding(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window
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