package info.muge.appshare.ui;

import android.content.Context;
import android.view.View;

import info.muge.appshare.R;

public class LoadingListDialog extends ProgressDialog {

    public LoadingListDialog(Context context){
        super(context,context.getResources().getString(R.string.dialog_loading_title));
        att.setVisibility(View.GONE);
        progressBar.setMax(100);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    public void setProgress(int progress,int total){
        progressBar.setMax(total);
        if(progress>progressBar.getMax())return;
        progressBar.setProgress(progress);
        att_left.setText(progress+"/"+progressBar.getMax());
        att_right.setText((int)((float)progress/total*100)+"%");
    }



}
