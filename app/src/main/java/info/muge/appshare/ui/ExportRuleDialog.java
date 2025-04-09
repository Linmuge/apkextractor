package info.muge.appshare.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.chip.Chip;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;

import info.muge.appshare.R;
import info.muge.appshare.Constants;
import info.muge.appshare.utils.EnvironmentUtil;
import info.muge.appshare.utils.SPUtil;

import java.util.Calendar;

public class ExportRuleDialog {

    private AlertDialog dialog;
    private MaterialAlertDialogBuilder builder;
    private TextInputEditText edit_apk,edit_zip;
    private TextView preview;
    private MaterialAutoCompleteTextView spinner;
    private Context context;
    private SharedPreferences settings;
    private View dialogView;

    /**
     * 编辑导出规则的UI，确定后会保存至SharedPreferences中
     */
    public ExportRuleDialog(Context context, int style) {
        this.context = context;
        settings = SPUtil.getGlobalSharedPreferences(context);

        // 使用 MaterialAlertDialogBuilder 创建对话框
        builder = new MaterialAlertDialogBuilder(context, style);
        
        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rule, null);
        edit_apk = dialogView.findViewById(R.id.filename_apk);
        edit_zip = dialogView.findViewById(R.id.filename_zip);
        preview = dialogView.findViewById(R.id.filename_preview);
        spinner = dialogView.findViewById(R.id.spinner_zip_level);

        ((TextView)dialogView.findViewById(R.id.filename_zip_end)).setText("."+SPUtil.getCompressingExtensionName(context));
        edit_apk.setText(settings.getString(Constants.PREFERENCE_FILENAME_FONT_APK, Constants.PREFERENCE_FILENAME_FONT_DEFAULT));
        edit_zip.setText(settings.getString(Constants.PREFERENCE_FILENAME_FONT_ZIP, Constants.PREFERENCE_FILENAME_FONT_DEFAULT));
        preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
        
        String[] zipLevels = new String[]{
            context.getResources().getString(R.string.zip_level_default),
            context.getResources().getString(R.string.zip_level_stored),
            context.getResources().getString(R.string.zip_level_low),
            context.getResources().getString(R.string.zip_level_normal),
            context.getResources().getString(R.string.zip_level_high)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, 
            android.R.layout.simple_dropdown_item_1line, zipLevels);
        spinner.setAdapter(adapter);
        spinner.setInputType(InputType.TYPE_NULL);
        spinner.setFocusable(true);
        spinner.setFocusableInTouchMode(false);
        spinner.setClickable(true);

        int level_set=settings.getInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT);
        try{
            switch(level_set){
                default:spinner.setText(zipLevels[0], false);break;
                case Constants.ZIP_LEVEL_STORED:spinner.setText(zipLevels[1], false);break;
                case Constants.ZIP_LEVEL_LOW:spinner.setText(zipLevels[2], false);break;
                case Constants.ZIP_LEVEL_NORMAL:spinner.setText(zipLevels[3], false);break;
                case Constants.ZIP_LEVEL_HIGH:spinner.setText(zipLevels[4], false);break;
            }
        }catch(Exception e){e.printStackTrace();}

        if(!edit_apk.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                &&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
            dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.VISIBLE);
        }else{
            dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.GONE);
        }

        if(!edit_zip.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                &&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONCODE)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_VERSIONNAME)){
            dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.VISIBLE);
        }else{
            dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.GONE);
        }
        
        // 配置对话框
        builder.setTitle(context.getResources().getString(R.string.dialog_filename_title));
        builder.setView(dialogView);
        builder.setPositiveButton(context.getResources().getString(R.string.dialog_button_confirm), null);
        builder.setNegativeButton(context.getResources().getString(R.string.dialog_button_cancel), null);

        edit_apk.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
                if(!edit_apk.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_apk.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                        &&!edit_apk.getText().toString().contains(Constants.FONT_AUTO_SEQUENCE_NUMBER)){
                    dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.VISIBLE);
                }else{
                    dialogView.findViewById(R.id.filename_apk_warn).setVisibility(View.GONE);
                }
            }

        });
        edit_zip.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                preview.setText(getFormatedExportFileName(edit_apk.getText().toString(),edit_zip.getText().toString()));
                if(!edit_zip.getText().toString().contains(Constants.FONT_APP_NAME)&&!edit_zip.getText().toString().contains(Constants.FONT_APP_PACKAGE_NAME)
                &&!edit_zip.getText().toString().contains(Constants.FONT_AUTO_SEQUENCE_NUMBER)){
                    dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.VISIBLE);
                }else{
                    dialogView.findViewById(R.id.filename_zip_warn).setVisibility(View.GONE);
                }
            }

        });

        setupChipClickListener(dialogView, R.id.filename_appname, Constants.FONT_APP_NAME);
        setupChipClickListener(dialogView, R.id.filename_packagename, Constants.FONT_APP_PACKAGE_NAME);
        setupChipClickListener(dialogView, R.id.filename_version, Constants.FONT_APP_VERSIONNAME);
        setupChipClickListener(dialogView, R.id.filename_versioncode, Constants.FONT_APP_VERSIONCODE);
        setupChipClickListener(dialogView, R.id.filename_connector, "-");
        setupChipClickListener(dialogView, R.id.filename_underline, "_");
        setupChipClickListener(dialogView, R.id.filename_year, Constants.FONT_YEAR);
        setupChipClickListener(dialogView, R.id.filename_month, Constants.FONT_MONTH);
        setupChipClickListener(dialogView, R.id.filename_day_of_month, Constants.FONT_DAY_OF_MONTH);
        setupChipClickListener(dialogView, R.id.filename_hour_of_day, Constants.FONT_HOUR_OF_DAY);
        setupChipClickListener(dialogView, R.id.filename_minute, Constants.FONT_MINUTE);
        setupChipClickListener(dialogView, R.id.filename_second, Constants.FONT_SECOND);
        setupChipClickListener(dialogView, R.id.filename_sequence_number, Constants.FONT_AUTO_SEQUENCE_NUMBER);
        
        // 创建对话框，但不立即显示
        dialog = builder.create();
        
        // 在显示前设置窗口属性，避免闪烁问题
        if (dialog.getWindow() != null) {
            // 设置对话框宽度
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
            
            // 创建圆角背景并立即应用
            float cornerSize = context.getResources().getDimension(R.dimen.md3_card_corner_radius_large);
            ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel.Builder()
                    .setAllCorners(CornerFamily.ROUNDED, cornerSize)
                    .build();
            
            MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable(shapeAppearanceModel);
            shapeDrawable.setFillColor(android.content.res.ColorStateList.valueOf(
                    context.getResources().getColor(android.R.color.white)));
            
            dialog.getWindow().setBackgroundDrawable(shapeDrawable);
        }
    }
    
    private void setupChipClickListener(View rootView, int chipId, String insertText) {
        Chip chip = rootView.findViewById(chipId);
        chip.setOnClickListener(v -> {
            if(edit_apk.hasFocus()){
                int position = edit_apk.getSelectionStart();
                edit_apk.getText().insert(position, insertText);
            }
            if(edit_zip.hasFocus()){
                int position = edit_zip.getSelectionStart();
                edit_zip.getText().insert(position, insertText);
            }
        });
    }

    public void show(){
        dialog.show();
        
        // 在对话框显示后设置按钮点击监听器
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if(edit_apk.getText().toString().trim().equals("")||edit_zip.getText().toString().trim().equals("")){
                ToastManager.showToast(context, context.getResources().getString(R.string.dialog_filename_toast_blank), Toast.LENGTH_SHORT);
                return;
            }

            final String apk_replaced_variables=EnvironmentUtil.getEmptyVariableString(edit_apk.getText().toString());
            final String zip_replaced_variables=EnvironmentUtil.getEmptyVariableString(edit_zip.getText().toString());
            if(!EnvironmentUtil.isALegalFileName(apk_replaced_variables)||!EnvironmentUtil.isALegalFileName(zip_replaced_variables)){
                ToastManager.showToast(context, context.getResources().getString(R.string.file_invalid_name), Toast.LENGTH_SHORT);
                return;
            }

            SharedPreferences.Editor editor=settings.edit();
            editor.putString(Constants.PREFERENCE_FILENAME_FONT_APK, edit_apk.getText().toString());
            editor.putString(Constants.PREFERENCE_FILENAME_FONT_ZIP, edit_zip.getText().toString());
            
            String selectedZipLevel = spinner.getText().toString();
            if(selectedZipLevel.equals(context.getResources().getString(R.string.zip_level_default))) {
                editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT);
            } else if(selectedZipLevel.equals(context.getResources().getString(R.string.zip_level_stored))) {
                editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_STORED);
            } else if(selectedZipLevel.equals(context.getResources().getString(R.string.zip_level_low))) {
                editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_LOW);
            } else if(selectedZipLevel.equals(context.getResources().getString(R.string.zip_level_normal))) {
                editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_NORMAL);
            } else if(selectedZipLevel.equals(context.getResources().getString(R.string.zip_level_high))) {
                editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_HIGH);
            }
            
            editor.apply();
            dialog.dismiss();
        });
    }
    
    public void cancel() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private String getFormatedExportFileName(String apk, String zip){
        return context.getResources().getString(R.string.word_preview)+"\n\nAPK:  "+getReplacedString(apk)+".apk\n\n"
                +context.getResources().getString(R.string.word_compressed)+":  "+getReplacedString(zip)+"."
                +SPUtil.getCompressingExtensionName(context);
    }

    private String getReplacedString(String value){
        final String PREVIEW_APPNAME=context.getResources().getString(R.string.dialog_filename_preview_appname);
        final String PREVIEW_PACKAGENAME=context.getResources().getString(R.string.dialog_filename_preview_packagename);
        final String PREVIEW_VERSION=context.getResources().getString(R.string.dialog_filename_preview_version);
        final String PREVIEW_VERSIONCODE=context.getResources().getString(R.string.dialog_filename_preview_versioncode);
        value=value.replace(Constants.FONT_APP_NAME, PREVIEW_APPNAME);
        value=value.replace(Constants.FONT_APP_PACKAGE_NAME, PREVIEW_PACKAGENAME);
        value=value.replace(Constants.FONT_APP_VERSIONNAME, PREVIEW_VERSION);
        value=value.replace(Constants.FONT_APP_VERSIONCODE, PREVIEW_VERSIONCODE);
        value=value.replace(Constants.FONT_YEAR,EnvironmentUtil.getCurrentTimeValue(Calendar.YEAR));
        value=value.replace(Constants.FONT_MONTH,EnvironmentUtil.getCurrentTimeValue(Calendar.MONTH));
        value=value.replace(Constants.FONT_DAY_OF_MONTH,EnvironmentUtil.getCurrentTimeValue(Calendar.DAY_OF_MONTH));
        value=value.replace(Constants.FONT_HOUR_OF_DAY,EnvironmentUtil.getCurrentTimeValue(Calendar.HOUR_OF_DAY));
        value=value.replace(Constants.FONT_MINUTE,EnvironmentUtil.getCurrentTimeValue(Calendar.MINUTE));
        value=value.replace(Constants.FONT_SECOND,EnvironmentUtil.getCurrentTimeValue(Calendar.SECOND));
        value=value.replace(Constants.FONT_AUTO_SEQUENCE_NUMBER,String.valueOf(2));
        return value;
    }
}
