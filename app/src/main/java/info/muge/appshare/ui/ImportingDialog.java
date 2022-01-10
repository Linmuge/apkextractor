package info.muge.appshare.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.format.Formatter;

import info.muge.appshare.R;

import java.text.DecimalFormat;

public class ImportingDialog extends ProgressDialog {
    private final long total;

    public ImportingDialog(@NonNull Context context,long total) {
        super(context, context.getResources().getString(R.string.dialog_import_title));
        this.total=total;
        progressBar.setMax((int) (total/1024));
        setCancelable(false);
    }

    public void setCurrentWritingName(String filename){
        att.setText(getContext().getResources().getString(R.string.dialog_import_msg)+filename);
    }

    public void setProgress(long progress){
        progressBar.setProgress((int)(progress/1024));
        DecimalFormat dm=new DecimalFormat("#.00");
        int percent=(int)(Double.valueOf(dm.format((double)progress/total))*100);
        att_right.setText(Formatter.formatFileSize(getContext(),progress)+"/"+Formatter.formatFileSize(getContext(),total)+"("+percent+"%)");
    }

    public void setSpeed(long speed){
        att_left.setText(Formatter.formatFileSize(getContext(),speed)+"/s");
    }
}
