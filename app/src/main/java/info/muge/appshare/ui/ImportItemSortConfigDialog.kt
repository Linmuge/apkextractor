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

public class ImportItemSortConfigDialog {

    private SortConfigDialogCallback callback;
    private SharedPreferences settings;
    private AlertDialog dialog;

    public ImportItemSortConfigDialog(@NonNull Context context, @Nullable SortConfigDialogCallback callback) {
        this.callback = callback;
        settings = SPUtil.getGlobalSharedPreferences(context);
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sort_import, null);
        
        RadioButton ra_default = dialogView.findViewById(R.id.sort_ra_default);
        RadioButton ra_name_ascending = dialogView.findViewById(R.id.sort_ra_ascending_filename);
        RadioButton ra_name_descending = dialogView.findViewById(R.id.sort_ra_descending_filename);
        RadioButton ra_size_ascending = dialogView.findViewById(R.id.sort_ra_ascending_filesize);
        RadioButton ra_size_descending = dialogView.findViewById(R.id.sort_ra_descending_filesize);
        RadioButton ra_time_ascending = dialogView.findViewById(R.id.sort_ra_ascending_modified_time);
        RadioButton ra_time_descending = dialogView.findViewById(R.id.sort_ra_descending_modified_time);
        
        int sort_config = settings.getInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, 0);
        ra_default.setChecked(sort_config == 0);
        ra_name_ascending.setChecked(sort_config == 1);
        ra_name_descending.setChecked(sort_config == 2);
        ra_size_ascending.setChecked(sort_config == 3);
        ra_size_descending.setChecked(sort_config == 4);
        ra_time_ascending.setChecked(sort_config == 5);
        ra_time_descending.setChecked(sort_config == 6);
        
        ra_default.setOnClickListener(v -> {
            int sort_config1 = 0;
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, sort_config1);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config1);
        });
        ra_name_ascending.setOnClickListener(v -> {
            int sort_config1 = 1;
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, sort_config1);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config1);
        });
        ra_name_descending.setOnClickListener(v -> {
            int sort_config1 = 2;
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, sort_config1);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config1);
        });
        ra_size_ascending.setOnClickListener(v -> {
            int sort_config1 = 3;
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, sort_config1);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config1);
        });
        ra_size_descending.setOnClickListener(v -> {
            int sort_config1 = 4;
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, sort_config1);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config1);
        });
        ra_time_ascending.setOnClickListener(v -> {
            int sort_config1 = 5;
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, sort_config1);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config1);
        });
        ra_time_descending.setOnClickListener(v -> {
            int sort_config1 = 6;
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, sort_config1);
            editor.apply();
            dialog.dismiss();
            if (callback != null) callback.onOptionSelected(sort_config1);
        });
        
        // 使用MaterialAlertDialogBuilder创建MD3风格对话框
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(context.getResources().getString(R.string.dialog_sort_import_item_title))
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
