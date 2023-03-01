package info.muge.appshare.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import info.muge.appshare.Constants;
import info.muge.appshare.R;
import info.muge.appshare.utils.SPUtil;

public class ImportItemSortConfigDialog extends AlertDialog{

    private SortConfigDialogCallback callback;
    private SharedPreferences settings;

    public ImportItemSortConfigDialog(@NonNull Context context, @Nullable SortConfigDialogCallback callback) {
        super(context);
        this.callback=callback;
        settings= SPUtil.getGlobalSharedPreferences(context);
        View dialogView= LayoutInflater.from(context).inflate(R.layout.dialog_sort_import,null);
        setView(dialogView);
        setTitle(context.getResources().getString(R.string.dialog_sort_import_item_title));
        RadioButton ra_default=dialogView.findViewById(R.id.sort_ra_default);
        RadioButton ra_name_ascending=dialogView.findViewById(R.id.sort_ra_ascending_filename);
        RadioButton ra_name_descending=dialogView.findViewById(R.id.sort_ra_descending_filename);
        RadioButton ra_size_ascending=dialogView.findViewById(R.id.sort_ra_ascending_filesize);
        RadioButton ra_size_descending=dialogView.findViewById(R.id.sort_ra_descending_filesize);
        RadioButton ra_time_ascending=dialogView.findViewById(R.id.sort_ra_ascending_modified_time);
        RadioButton ra_time_descending=dialogView.findViewById(R.id.sort_ra_descending_modified_time);
        int sort_config=settings.getInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,0);
        ra_default.setChecked(sort_config==0);
        ra_name_ascending.setChecked(sort_config==1);
        ra_name_descending.setChecked(sort_config==2);
        ra_size_ascending.setChecked(sort_config==3);
        ra_size_descending.setChecked(sort_config==4);
        ra_time_ascending.setChecked(sort_config==5);
        ra_time_descending.setChecked(sort_config==6);
        ra_default.setOnClickListener(v->{
            int sort_config1 = 0;
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,sort_config1);
            editor.apply();
            cancel();
            if(callback!=null)callback.onOptionSelected(sort_config1);
        });
        ra_name_ascending.setOnClickListener(v->{
            int sort_config1 = 1;
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,sort_config1);
            editor.apply();
            cancel();
            if(callback!=null)callback.onOptionSelected(sort_config1);
        });
        ra_name_descending.setOnClickListener(v->{
            int sort_config1 = 2;
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,sort_config1);
            editor.apply();
            cancel();
            if(callback!=null)callback.onOptionSelected(sort_config1);
        });
        ra_size_ascending.setOnClickListener(v->{
            int sort_config1 = 3;
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,sort_config1);
            editor.apply();
            cancel();
            if(callback!=null)callback.onOptionSelected(sort_config1);
        });
        ra_size_descending.setOnClickListener(v->{
            int sort_config1 = 4;
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,sort_config1);
            editor.apply();
            cancel();
            if(callback!=null)callback.onOptionSelected(sort_config1);
        });
        ra_time_ascending.setOnClickListener(v->{
            int sort_config1 = 5;
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,sort_config1);
            editor.apply();
            cancel();
            if(callback!=null)callback.onOptionSelected(sort_config1);
        });
        ra_time_descending.setOnClickListener(v->{
            int sort_config1 = 6;
            SharedPreferences.Editor editor=settings.edit();
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,sort_config1);
            editor.apply();
            cancel();
            if(callback!=null)callback.onOptionSelected(sort_config1);
        });
        setButton(AlertDialog.BUTTON_NEGATIVE, context.getResources().getString(R.string.dialog_button_cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
    }

}
