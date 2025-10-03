package info.muge.appshare.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.transition.TransitionManager
import info.muge.appshare.R

/**
 * 组件视图（权限、Activity、Receiver等）
 */
class AssemblyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val linearLayout_permission: LinearLayout
    val linearLayout_activity: LinearLayout
    val linearLayout_receiver: LinearLayout
    val linearLayout_loader: LinearLayout
    val linearLayout_service: LinearLayout
    val linearLayout_provider: LinearLayout

    private val permission_arrow: ImageView
    private val activity_arrow: ImageView
    private val receiver_arrow: ImageView
    private val loader_arrow: ImageView
    private val service_arrow: ImageView
    private val provider_arrow: ImageView

    val tv_permission: TextView
    val tv_activity: TextView
    val tv_receiver: TextView
    val tv_loader: TextView
    val tv_service: TextView
    val tv_provider: TextView

    init {
        inflate(context, R.layout.layout_card_assembly, this)
        
        linearLayout_permission = findViewById(R.id.detail_permission)
        linearLayout_activity = findViewById(R.id.detail_activity)
        linearLayout_receiver = findViewById(R.id.detail_receiver)
        linearLayout_loader = findViewById(R.id.detail_static_loader)
        linearLayout_service = findViewById(R.id.detail_service)
        linearLayout_provider = findViewById(R.id.detail_provider)
        
        tv_permission = findViewById(R.id.detail_permission_area_att)
        tv_activity = findViewById(R.id.detail_activity_area_att)
        tv_receiver = findViewById(R.id.detail_receiver_area_att)
        tv_loader = findViewById(R.id.detail_static_loader_area_att)
        tv_service = findViewById(R.id.detail_service_area_att)
        tv_provider = findViewById(R.id.detail_provider_area_att)
        
        permission_arrow = findViewById(R.id.detail_permission_area_arrow)
        activity_arrow = findViewById(R.id.detail_activity_area_arrow)
        receiver_arrow = findViewById(R.id.detail_receiver_area_arrow)
        loader_arrow = findViewById(R.id.detail_static_loader_area_arrow)
        service_arrow = findViewById(R.id.detail_service_area_arrow)
        provider_arrow = findViewById(R.id.detail_provider_area_arrow)

        findViewById<View>(R.id.detail_permission_area).setOnClickListener {
            if (linearLayout_permission.visibility == View.VISIBLE) {
                permission_arrow.rotation = 0f
                linearLayout_permission.visibility = View.GONE
                TransitionManager.beginDelayedTransition(this)
            } else {
                permission_arrow.rotation = 90f
                linearLayout_permission.visibility = View.VISIBLE
                TransitionManager.beginDelayedTransition(this)
            }
        }
        
        findViewById<View>(R.id.detail_activity_area).setOnClickListener {
            if (linearLayout_activity.visibility == View.VISIBLE) {
                activity_arrow.rotation = 0f
                linearLayout_activity.visibility = View.GONE
                TransitionManager.beginDelayedTransition(this)
            } else {
                activity_arrow.rotation = 90f
                linearLayout_activity.visibility = View.VISIBLE
                TransitionManager.beginDelayedTransition(this)
            }
        }
        
        findViewById<View>(R.id.detail_receiver_area).setOnClickListener {
            if (linearLayout_receiver.visibility == View.VISIBLE) {
                receiver_arrow.rotation = 0f
                linearLayout_receiver.visibility = View.GONE
                TransitionManager.beginDelayedTransition(this)
            } else {
                receiver_arrow.rotation = 90f
                linearLayout_receiver.visibility = View.VISIBLE
                TransitionManager.beginDelayedTransition(this)
            }
        }
        
        findViewById<View>(R.id.detail_static_loader_area).setOnClickListener {
            if (linearLayout_loader.visibility == View.VISIBLE) {
                loader_arrow.rotation = 0f
                linearLayout_loader.visibility = View.GONE
                TransitionManager.beginDelayedTransition(this)
            } else {
                loader_arrow.rotation = 90f
                linearLayout_loader.visibility = View.VISIBLE
                TransitionManager.beginDelayedTransition(this)
            }
        }
        
        findViewById<View>(R.id.detail_services_area).setOnClickListener {
            if (linearLayout_service.visibility == View.VISIBLE) {
                service_arrow.rotation = 0f
                linearLayout_service.visibility = View.GONE
                TransitionManager.beginDelayedTransition(this)
            } else {
                service_arrow.rotation = 90f
                linearLayout_service.visibility = View.VISIBLE
                TransitionManager.beginDelayedTransition(this)
            }
        }
        
        findViewById<View>(R.id.detail_provider_area).setOnClickListener {
            if (linearLayout_provider.visibility == View.VISIBLE) {
                provider_arrow.rotation = 0f
                linearLayout_provider.visibility = View.GONE
                TransitionManager.beginDelayedTransition(this)
            } else {
                provider_arrow.rotation = 90f
                linearLayout_provider.visibility = View.VISIBLE
                TransitionManager.beginDelayedTransition(this)
            }
        }
    }

    val isExpanded: Boolean
        get() = linearLayout_activity.visibility == VISIBLE ||
                linearLayout_permission.visibility == VISIBLE ||
                linearLayout_receiver.visibility == VISIBLE ||
                linearLayout_loader.visibility == VISIBLE ||
                linearLayout_service.visibility == VISIBLE ||
                linearLayout_provider.visibility == VISIBLE
}

