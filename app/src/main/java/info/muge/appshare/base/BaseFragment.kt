package info.muge.appshare.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<VB : ViewDataBinding> : Fragment(),BaseBinding<VB> {

    protected lateinit var binding:VB
        private set
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = getViewBinding(inflater,container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.initView()
    }


    override fun onDestroy() {
        super.onDestroy()
        //此处记得取消绑定，避免内存泄露
        if (::binding.isInitialized) {
            binding.unbind()
        }
    }

    inline fun <VB:ViewBinding> Any.getViewBinding(inflater: LayoutInflater, container: ViewGroup?):VB{
        // 获取泛型父类
        val genericSuperclass = javaClass.genericSuperclass

        // 检查是否为 ParameterizedType，如果不是则尝试从父类链中查找
        val parameterizedType = if (genericSuperclass is ParameterizedType) {
            genericSuperclass
        } else {
            // 如果直接父类不是 ParameterizedType，尝试查找父类链
            var currentClass: Class<*>? = javaClass.superclass
            var foundType: ParameterizedType? = null

            while (currentClass != null && foundType == null) {
                val superType = currentClass.genericSuperclass
                if (superType is ParameterizedType) {
                    foundType = superType
                    break
                }
                currentClass = currentClass.superclass
            }

            foundType ?: throw IllegalStateException(
                "无法获取 ViewBinding 泛型参数。请确保 ${javaClass.simpleName} 正确继承了 BaseFragment<VB>"
            )
        }

        // 获取泛型参数
        val vbClass = parameterizedType.actualTypeArguments
            .filterIsInstance<Class<VB>>()

        if (vbClass.isEmpty()) {
            throw IllegalStateException(
                "无法找到 ViewBinding 类型参数。请确保 ${javaClass.simpleName} 正确指定了泛型参数"
            )
        }

        // 反射调用 inflate 方法
        val inflate = vbClass[0].getDeclaredMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
        return inflate.invoke(null, inflater, container, false) as VB
    }

}

interface BaseBinding<VB : ViewDataBinding> {
    fun VB.initView()
}