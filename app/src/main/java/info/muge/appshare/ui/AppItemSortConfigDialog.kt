package info.muge.appshare.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.graphics.drawable.GradientDrawable;
import android.view.Window;
import android.os.Build;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AlertDialog;

import info.muge.appshare.Constants;
import info.muge.appshare.R;
import info.muge.appshare.utils.SPUtil;

public class AppItemSortConfigDialog {

    private SharedPreferences settings;
    private SortConfigDialogCallback callback;
    private AlertDialog dialog;

    public AppItemSortConfigDialog(@NonNull Context context, @Nullable SortConfigDialogCallback callback) {
        this.callback = callback;
        settings = SPUtil.getGlobalSharedPreferences(context);

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sort, null);
        
        int sort = settings.getInt(Constants.PREFERENCE_SORT_CONFIG, 0);
        RadioButton ra_default = dialogView.findViewById(R.id.sort_ra_default);
        RadioButton ra_name_ascend = dialogView.findViewById(R.id.sort_ra_ascending_appname);
        RadioButton ra_name_descend = dialogView.findViewById(R.id.sort_ra_descending_appname);
        RadioButton ra_size_ascend = dialogView.findViewById(R.id.sort_ra_ascending_appsize);
        RadioButton ra_size_descend = dialogView.findViewById(R.id.sort_ra_descending_appsize);
        RadioButton ra_update_time_ascend = dialogView.findViewById(R.id.sort_ra_ascending_date);
        RadioButton ra_update_time_descend = dialogView.findViewById(R.id.sort_ra_descending_date);
        RadioButton ra_install_time_ascend = dialogView.findViewById(R.id.sort_ra_ascending_install_date);
        RadioButton ra_install_time_descend = dialogView.findViewById(R.id.sort_ra_descending_install_date);
        RadioButton ra_package_name_ascend = dialogView.findViewById(R.id.sort_ra_ascending_package_name);
        RadioButton ra_package_name_descend = dialogView.findViewById(R.id.sort_ra_descending_package_name);

        ra_default.setChecked(sort == 0);
        ra_name_ascend.setChecked(sort == 1);
        ra_name_descend.setChecked(sort == 2);
        ra_size_ascend.setChecked(sort == 3);
        ra_size_descend.setChecked(sort == 4);
        ra_update_time_ascend.setChecked(sort == 5);
        ra_update_time_descend.setChecked(sort == 6);
        ra_install_time_ascend.setChecked(sort == 7);
        ra_install_time_descend.setChecked(sort == 8);
        ra_package_name_ascend.setChecked(sort == 9);
        ra_package_name_descend.setChecked(sort == 10);

        ra_default.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 0;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_name_ascend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 1;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_name_descend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 2;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_size_ascend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 3;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_size_descend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 4;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_update_time_ascend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 5;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_update_time_descend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 6;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_install_time_ascend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 7;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_install_time_descend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 8;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_package_name_ascend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 9;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });
        ra_package_name_descend.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();
            int sort_config = 10;
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sort_config);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config);
        });

        // 使用MaterialAlertDialogBuilder创建MD3风格对话框
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(context.getResources().getString(R.string.dialog_sort_appitem_title))
            .setView(dialogView)
            .setNegativeButton(context.getResources().getString(R.string.dialog_button_cancel), (dialog, which) -> {});
            
        dialog = builder.create();
        
        // 设置圆角和其他MD3样式
        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();
            if (window != null) {
                // 应用MD3大圆角
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.setBackgroundDrawableResource(com.google.android.material.R.drawable.mtrl_dialog_background);
                    // 增加圆角大小 - 如果默认圆角不够大
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setCornerRadius(32f); // 设置更大的圆角半径
                    drawable.setColor(context.getResources().getColor(com.google.android.material.R.color.mtrl_btn_bg_color_selector));
                    window.setBackgroundDrawable(drawable);
                } else {
                    window.setBackgroundDrawableResource(com.google.android.material.R.drawable.mtrl_dialog_background);
                }
            }
        });
    }
    
    public void show() {
        dialog.show();
    }
    
    public void cancel() {
        dialog.dismiss();
    }
}

