package info.muge.appshare.tasks

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.ui.AssemblyView
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.utils.SPUtil

/**
 * 获取包信息视图任务
 */
class GetPackageInfoViewTask(
    private val activity: Activity,
    private val packageInfo: PackageInfo,
    private val static_receiver_bundle: Bundle,
    private val assemblyView: AssemblyView,
    private val callback: CompletedCallback
) : Thread() {

    override fun run() {
        super.run()
        
        val settings: SharedPreferences = SPUtil.getGlobalSharedPreferences(activity)
        val permissions = packageInfo.requestedPermissions
        val activities = packageInfo.activities
        val receivers = packageInfo.receivers
        val services = packageInfo.services
        val providers = packageInfo.providers

        val get_permissions = settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT)
        val get_activities = settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT)
        val get_receivers = settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT)
        val get_static_loaders = settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT)
        val get_services = settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT)
        val get_providers = settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT)

        val permission_child_views = ArrayList<View>()
        val activity_child_views = ArrayList<View>()
        val receiver_child_views = ArrayList<View>()
        val loaders_child_views = ArrayList<View>()
        val service_child_views = ArrayList<View>()
        val provider_child_views = ArrayList<View>()

        // 处理权限
        if (permissions != null && get_permissions) {
            for (s in permissions) {
                if (s == null) continue
                permission_child_views.add(
                    getSingleItemView(
                        assemblyView.linearLayout_permission,
                        s,
                        { clip2ClipboardAndShowSnackbar(s) },
                        null
                    )
                )
            }
        }

        // 处理Activity
        if (activities != null && get_activities) {
            for (info in activities) {
                activity_child_views.add(
                    getSingleItemView(
                        assemblyView.linearLayout_activity,
                        info.name,
                        { clip2ClipboardAndShowSnackbar(info.name) },
                        {
                            try {
                                val intent = Intent()
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.setClassName(info.packageName, info.name)
                                activity.startActivity(intent)
                            } catch (e: Exception) {
                                ToastManager.showToast(activity, e.toString(), Toast.LENGTH_SHORT)
                            }
                            true
                        }
                    )
                )
            }
        }

        // 处理Receiver
        if (receivers != null && get_receivers) {
            for (activityInfo in receivers) {
                receiver_child_views.add(
                    getSingleItemView(
                        assemblyView.linearLayout_receiver,
                        activityInfo.name,
                        { clip2ClipboardAndShowSnackbar(activityInfo.name) },
                        null
                    )
                )
            }
        }

        // 处理Service
        if (services != null && get_services) {
            for (serviceInfo in services) {
                service_child_views.add(
                    getSingleItemView(
                        assemblyView.linearLayout_service,
                        serviceInfo.name,
                        { clip2ClipboardAndShowSnackbar(serviceInfo.name) },
                        {
                            try {
                                val intent = Intent()
                                intent.setClassName(serviceInfo.packageName, serviceInfo.name)
                                activity.startService(intent)
                            } catch (e: Exception) {
                                ToastManager.showToast(activity, e.toString(), Toast.LENGTH_SHORT)
                            }
                            true
                        }
                    )
                )
            }
        }

        // 处理Provider
        if (providers != null && get_providers) {
            for (providerInfo in providers) {
                provider_child_views.add(
                    getSingleItemView(
                        assemblyView.linearLayout_provider,
                        providerInfo.name,
                        { clip2ClipboardAndShowSnackbar(providerInfo.name) },
                        null
                    )
                )
            }
        }

        // 处理静态加载器
        val keys = static_receiver_bundle.keySet()
        if (get_static_loaders) {
            for (s in keys) {
                val static_loader_item_view = LayoutInflater.from(activity)
                    .inflate(R.layout.item_static_loader, assemblyView.linearLayout_loader, false)
                static_loader_item_view.findViewById<TextView>(R.id.static_loader_name).text = s
                static_loader_item_view.setOnClickListener { clip2ClipboardAndShowSnackbar(s) }
                
                val filter_views = static_loader_item_view.findViewById<ViewGroup>(R.id.static_loader_intents)
                val filters = static_receiver_bundle.getStringArrayList(s)
                if (filters != null) {
                    for (filter in filters) {
                        val itemView = LayoutInflater.from(activity)
                            .inflate(R.layout.item_single_textview, filter_views, false)
                        itemView.findViewById<TextView>(R.id.item_textview).text = filter
                        itemView.setOnClickListener { clip2ClipboardAndShowSnackbar(filter) }
                        filter_views.addView(itemView)
                    }
                }
                loaders_child_views.add(static_loader_item_view)
            }
        }

        // 在主线程更新UI
        Global.handler.post {
            if (get_permissions) {
                for (view in permission_child_views) assemblyView.linearLayout_permission.addView(view)
                val att_permission = assemblyView.tv_permission
                att_permission.text = "${activity.resources.getString(R.string.activity_detail_permissions)}" +
                        "(${permission_child_views.size}${activity.resources.getString(R.string.unit_item)})"
                assemblyView.findViewById<View>(R.id.detail_card_permissions).visibility = View.VISIBLE
            }
            if (get_activities) {
                for (view in activity_child_views) assemblyView.linearLayout_activity.addView(view)
                val att_activity = assemblyView.tv_activity
                att_activity.text = "${activity.resources.getString(R.string.activity_detail_activities)}" +
                        "(${activity_child_views.size}${activity.resources.getString(R.string.unit_item)})"
                assemblyView.findViewById<View>(R.id.detail_card_activities).visibility = View.VISIBLE
            }
            if (get_receivers) {
                for (view in receiver_child_views) assemblyView.linearLayout_receiver.addView(view)
                val att_receiver = assemblyView.tv_receiver
                att_receiver.text = "${activity.resources.getString(R.string.activity_detail_receivers)}" +
                        "(${receiver_child_views.size}${activity.resources.getString(R.string.unit_item)})"
                assemblyView.findViewById<View>(R.id.detail_card_receivers).visibility = View.VISIBLE
            }
            if (get_static_loaders) {
                for (view in loaders_child_views) assemblyView.linearLayout_loader.addView(view)
                val att_static_loader = assemblyView.tv_loader
                att_static_loader.text = "${activity.resources.getString(R.string.activity_detail_static_loaders)}" +
                        "(${keys.size}${activity.resources.getString(R.string.unit_item)})"
                assemblyView.findViewById<View>(R.id.detail_card_static_loaders).visibility = View.VISIBLE
            }
            if (get_services) {
                for (view in service_child_views) assemblyView.linearLayout_service.addView(view)
                val att_service = assemblyView.tv_service
                att_service.text = "${activity.resources.getString(R.string.activity_detail_services)}" +
                        "(${service_child_views.size}${activity.resources.getString(R.string.unit_item)})"
                assemblyView.findViewById<View>(R.id.detail_card_services).visibility = View.VISIBLE
            }
            if (get_providers) {
                for (view in provider_child_views) assemblyView.linearLayout_provider.addView(view)
                val att_providers = assemblyView.tv_provider
                att_providers.text = "${activity.resources.getString(R.string.activity_detail_providers)}" +
                        "(${provider_child_views.size}${activity.resources.getString(R.string.unit_item)})"
                assemblyView.findViewById<View>(R.id.detail_card_providers).visibility = View.VISIBLE
            }
            callback.onViewsCreated()
        }
    }

    private fun getSingleItemView(
        group: ViewGroup,
        text: String,
        clickListener: View.OnClickListener?,
        longClickListener: View.OnLongClickListener?
    ): View {
        val view = LayoutInflater.from(activity).inflate(R.layout.item_single_textview, group, false)
        view.findViewById<TextView>(R.id.item_textview).text = text
        view.setOnClickListener(clickListener)
        view.setOnLongClickListener(longClickListener)
        return view
    }

    private fun clip2ClipboardAndShowSnackbar(s: String) {
        try {
            val manager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("message", s))
            Snackbar.make(
                activity.findViewById(android.R.id.content),
                activity.resources.getString(R.string.snack_bar_clipboard),
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface CompletedCallback {
        fun onViewsCreated()
    }
}

