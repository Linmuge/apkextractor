package info.muge.appshare.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.ui.ExportRuleDialog
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.toast

/**
 * 设置页面 Fragment
 * 包含夜间模式、语言、加载选项等设置
 */
class SettingsFragment : Fragment() {

    private lateinit var settings: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SPUtil.getGlobalSharedPreferences(requireContext())

        // 设置各个选项的点击事件
        setupNightModeOption(view)
        setupLoadingOptionsOption(view)
        setupRulesOption(view)
        setupPathOption(view)
        setupAboutOption(view)
        setupLanguageOption(view)
        setupPackageNameSeparatorOption(view)

        // 刷新设置值显示
        refreshSettingValues(view)
    }

    /**
     * 设置夜间模式选项
     */
    private fun setupNightModeOption(view: View) {
        view.findViewById<View>(R.id.settings_night_mode_area).setOnClickListener {
            val editor = settings.edit()
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_night_mode, null)

            val nightMode = settings.getInt(
                Constants.PREFERENCE_NIGHT_MODE,
                Constants.PREFERENCE_NIGHT_MODE_DEFAULT
            )

            dialogView.findViewById<RadioButton>(R.id.night_mode_enabled_ra)
                .isChecked = nightMode == AppCompatDelegate.MODE_NIGHT_YES
            dialogView.findViewById<RadioButton>(R.id.night_mode_disabled_ra)
                .isChecked = nightMode == AppCompatDelegate.MODE_NIGHT_NO
            dialogView.findViewById<RadioButton>(R.id.night_mode_follow_system_ra)
                .isChecked = nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

            val dialog = MaterialAlertDialogBuilder(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle(resources.getString(R.string.activity_settings_night_mode))
                .setView(dialogView)
                .show()

            dialogView.findViewById<View>(R.id.night_mode_enabled).setOnClickListener {
                dialog.cancel()
                editor.putInt(Constants.PREFERENCE_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES)
                editor.apply()
                refreshNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            dialogView.findViewById<View>(R.id.night_mode_disabled).setOnClickListener {
                dialog.cancel()
                editor.putInt(Constants.PREFERENCE_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_NO)
                editor.apply()
                refreshNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            dialogView.findViewById<View>(R.id.night_mode_follow_system).setOnClickListener {
                dialog.cancel()
                editor.putInt(Constants.PREFERENCE_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                editor.apply()
                refreshNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    /**
     * 设置加载选项
     */
    private fun setupLoadingOptionsOption(view: View) {
        view.findViewById<View>(R.id.settings_loading_options_area).setOnClickListener {
            val editor = settings.edit()
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_loading_selection, null)

            val cbPermissions = dialogView.findViewById<CheckBox>(R.id.loading_permissions)
            val cbActivities = dialogView.findViewById<CheckBox>(R.id.loading_activities)
            val cbReceivers = dialogView.findViewById<CheckBox>(R.id.loading_receivers)
            val cbStaticLoaders = dialogView.findViewById<CheckBox>(R.id.loading_static_loaders)
            val cbSignature = dialogView.findViewById<CheckBox>(R.id.loading_apk_signature)
            val cbHash = dialogView.findViewById<CheckBox>(R.id.loading_file_hash)
            val cbService = dialogView.findViewById<CheckBox>(R.id.loading_services)
            val cbProvider = dialogView.findViewById<CheckBox>(R.id.loading_providers)

            cbPermissions.isChecked = settings.getBoolean(
                Constants.PREFERENCE_LOAD_PERMISSIONS,
                Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT
            )
            cbActivities.isChecked = settings.getBoolean(
                Constants.PREFERENCE_LOAD_ACTIVITIES,
                Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT
            )
            cbReceivers.isChecked = settings.getBoolean(
                Constants.PREFERENCE_LOAD_RECEIVERS,
                Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT
            )
            cbStaticLoaders.isChecked = settings.getBoolean(
                Constants.PREFERENCE_LOAD_STATIC_LOADERS,
                Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT
            )
            cbSignature.isChecked = settings.getBoolean(
                Constants.PREFERENCE_LOAD_APK_SIGNATURE,
                Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT
            )
            cbHash.isChecked = settings.getBoolean(
                Constants.PREFERENCE_LOAD_FILE_HASH,
                Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT
            )
            cbService.isChecked = settings.getBoolean(
                Constants.PREFERENCE_LOAD_SERVICES,
                Constants.PREFERENCE_LOAD_SERVICES_DEFAULT
            )
            cbProvider.isChecked = settings.getBoolean(
                Constants.PREFERENCE_LOAD_PROVIDERS,
                Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT
            )

            MaterialAlertDialogBuilder(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle(resources.getString(R.string.activity_settings_loading_options))
                .setView(dialogView)
                .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ ->
                    editor.putBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, cbPermissions.isChecked)
                    editor.putBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, cbActivities.isChecked)
                    editor.putBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, cbReceivers.isChecked)
                    editor.putBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, cbStaticLoaders.isChecked)
                    editor.putBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, cbSignature.isChecked)
                    editor.putBoolean(Constants.PREFERENCE_LOAD_FILE_HASH, cbHash.isChecked)
                    editor.putBoolean(Constants.PREFERENCE_LOAD_SERVICES, cbService.isChecked)
                    editor.putBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, cbProvider.isChecked)
                    editor.apply()
                    this.view?.let { refreshSettingValues(it) }
                }
                .setNegativeButton(resources.getString(R.string.dialog_button_cancel)) { _, _ -> }
                .show()
        }
    }

    /**
     * 设置规则选项
     */
    private fun setupRulesOption(view: View) {
        view.findViewById<View>(R.id.settings_rules_area).setOnClickListener {
            ExportRuleDialog(requireContext(), R.style.materialDialog).show()
        }
    }

    /**
     * 设置路径选项
     */
    private fun setupPathOption(view: View) {
        view.findViewById<View>(R.id.settings_path_area).setOnClickListener {
            "暂不支持修改".toast()
        }
    }

    /**
     * 设置关于选项
     */
    private fun setupAboutOption(view: View) {
        view.findViewById<View>(R.id.settings_about_area).setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_about, null)

            MaterialAlertDialogBuilder(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle("${EnvironmentUtil.getAppName(requireContext())}(${EnvironmentUtil.getAppVersionName(requireContext())})")
                .setIcon(R.mipmap.ic_launcher_round)
                .setCancelable(true)
                .setView(dialogView)
                .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
                .show()
        }
    }

    /**
     * 设置语言选项
     */
    private fun setupLanguageOption(view: View) {
        view.findViewById<View>(R.id.settings_language_area).setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_language, null)

            val value = SPUtil.getGlobalSharedPreferences(requireContext())
                .getInt(Constants.PREFERENCE_LANGUAGE, Constants.PREFERENCE_LANGUAGE_DEFAULT)

            dialogView.findViewById<RadioButton>(R.id.language_follow_system_ra)
                .isChecked = value == Constants.LANGUAGE_FOLLOW_SYSTEM
            dialogView.findViewById<RadioButton>(R.id.language_chinese_ra)
                .isChecked = value == Constants.LANGUAGE_CHINESE
            dialogView.findViewById<RadioButton>(R.id.language_english_ra)
                .isChecked = value == Constants.LANGUAGE_ENGLISH

            val dialog = MaterialAlertDialogBuilder(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle(resources.getString(R.string.activity_settings_language))
                .setView(dialogView)
                .show()

            dialogView.findViewById<View>(R.id.language_follow_system).setOnClickListener {
                SPUtil.getGlobalSharedPreferences(requireContext()).edit()
                    .putInt(Constants.PREFERENCE_LANGUAGE, Constants.LANGUAGE_FOLLOW_SYSTEM)
                    .apply()
                dialog.cancel()
                refreshLanguageValue()
            }

            dialogView.findViewById<View>(R.id.language_chinese).setOnClickListener {
                SPUtil.getGlobalSharedPreferences(requireContext()).edit()
                    .putInt(Constants.PREFERENCE_LANGUAGE, Constants.LANGUAGE_CHINESE)
                    .apply()
                dialog.cancel()
                refreshLanguageValue()
            }

            dialogView.findViewById<View>(R.id.language_english).setOnClickListener {
                SPUtil.getGlobalSharedPreferences(requireContext()).edit()
                    .putInt(Constants.PREFERENCE_LANGUAGE, Constants.LANGUAGE_ENGLISH)
                    .apply()
                dialog.cancel()
                refreshLanguageValue()
            }
        }
    }

    /**
     * 设置包名分隔符选项
     */
    private fun setupPackageNameSeparatorOption(view: View) {
        view.findViewById<View>(R.id.settings_package_name_separator_area).setOnClickListener {
            val editor = settings.edit()
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_package_name_split, null)

            val editText = dialogView.findViewById<EditText>(R.id.dialog_package_name_split_edit)
            editText.setText(
                settings.getString(
                    Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR,
                    Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT
                )
            )

            MaterialAlertDialogBuilder(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle(resources.getString(R.string.activity_settings_package_name_separator))
                .setView(dialogView)
                .setPositiveButton(resources.getString(R.string.action_confirm)) { _, _ ->
                    editor.putString(
                        Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR,
                        editText.text.toString()
                    ).apply()
                    this.view?.let { refreshSettingValues(it) }
                }
                .setNegativeButton(resources.getString(R.string.action_cancel)) { _, _ -> }
                .show()
        }
    }

    /**
     * 刷新夜间模式
     */
    private fun refreshNightMode(value: Int) {
        AppCompatDelegate.setDefaultNightMode(value)
        requireActivity().recreate()
    }

    /**
     * 刷新语言值
     */
    private fun refreshLanguageValue() {
        // 获取 BaseActivity 的方法来刷新语言
        requireActivity().let {
            try {
                val method = it.javaClass.getMethod("setAndRefreshLanguage")
                method.invoke(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        requireActivity().recreate()
    }

    /**
     * 刷新设置值显示
     */
    private fun refreshSettingValues(view: View) {
        // 导出路径
        view.findViewById<TextView>(R.id.settings_path_value)?.text = SPUtil.getDisplayingExportPath()

        // 夜间模式
        val nightModeValue = when (settings.getInt(
            Constants.PREFERENCE_NIGHT_MODE,
            Constants.PREFERENCE_NIGHT_MODE_DEFAULT
        )) {
            AppCompatDelegate.MODE_NIGHT_YES -> resources.getString(R.string.night_mode_enabled)
            AppCompatDelegate.MODE_NIGHT_NO -> resources.getString(R.string.night_mode_disabled)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> resources.getString(R.string.night_mode_follow_system)
            else -> ""
        }
        view.findViewById<TextView>(R.id.settings_night_mode_value)?.text = nightModeValue

        // 加载选项
        val readOptions = buildString {
            if (settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT)) {
                append(resources.getString(R.string.activity_detail_permissions))
            }
            if (settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT)) {
                if (isNotEmpty()) append(",")
                append(resources.getString(R.string.activity_detail_activities))
            }
            if (settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT)) {
                if (isNotEmpty()) append(",")
                append(resources.getString(R.string.activity_detail_services))
            }
            if (settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT)) {
                if (isNotEmpty()) append(",")
                append(resources.getString(R.string.activity_detail_receivers))
            }
            if (settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT)) {
                if (isNotEmpty()) append(",")
                append(resources.getString(R.string.activity_detail_providers))
            }
            if (settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT)) {
                if (isNotEmpty()) append(",")
                append(resources.getString(R.string.activity_detail_static_loaders))
            }
            if (settings.getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)) {
                if (isNotEmpty()) append(",")
                append(resources.getString(R.string.dialog_loading_selection_signature))
            }
            if (settings.getBoolean(Constants.PREFERENCE_LOAD_FILE_HASH, Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT)) {
                if (isNotEmpty()) append(",")
                append(resources.getString(R.string.dialog_loading_selection_file_hash))
            }
        }.ifEmpty { resources.getString(R.string.word_blank) }

        view.findViewById<TextView>(R.id.settings_loading_options_value)?.text = readOptions

        // 语言
        val languageValue = when (SPUtil.getGlobalSharedPreferences(requireContext())
            .getInt(Constants.PREFERENCE_LANGUAGE, Constants.PREFERENCE_LANGUAGE_DEFAULT)) {
            Constants.LANGUAGE_FOLLOW_SYSTEM -> resources.getString(R.string.language_follow_system)
            Constants.LANGUAGE_CHINESE -> resources.getString(R.string.language_chinese)
            Constants.LANGUAGE_ENGLISH -> resources.getString(R.string.language_english)
            else -> ""
        }
        view.findViewById<TextView>(R.id.settings_language_value)?.text = languageValue

        // 包名分隔符
        view.findViewById<TextView>(R.id.settings_package_name_separator_value)?.text = settings.getString(
            Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR,
            Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT
        )
    }

    override fun onResume() {
        super.onResume()
        // 每次返回时刷新设置值显示
        this.view?.let { refreshSettingValues(it) }
    }
}
