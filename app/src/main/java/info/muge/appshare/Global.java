package info.muge.appshare;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import info.muge.appshare.items.AppItem;
import info.muge.appshare.items.FileItem;
import info.muge.appshare.items.ImportItem;
import info.muge.appshare.tasks.ExportTask;
import info.muge.appshare.tasks.GetImportLengthAndDuplicateInfoTask;
import info.muge.appshare.tasks.ImportTask;
import info.muge.appshare.ui.DataObbDialog;
import info.muge.appshare.ui.ExportingDialog;
import info.muge.appshare.ui.ImportingDialog;
import info.muge.appshare.ui.ToastManager;
import info.muge.appshare.utils.DocumentFileUtil;
import info.muge.appshare.utils.OutputUtil;
import info.muge.appshare.utils.SPUtil;
import info.muge.appshare.utils.ViewExtsKt;
import info.muge.appshare.utils.ZipFileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Global {

    /**
     * 全局Handler，用于向主UI线程发送消息
     */
    public static final Handler handler=new Handler(Looper.getMainLooper());

    /**
     * 用于持有对读取出的list的引用
     */
    public static final List<AppItem> app_list=new ArrayList<>();

    /**
     * 导出目录下的文件list引用
     */
    public static final List<ImportItem> item_list=new ArrayList<>();

    public static void showRequestingWritePermissionSnackBar(@NonNull final Activity activity){
        Snackbar snackbar=Snackbar.make(activity.findViewById(android.R.id.content),activity.getResources().getString(R.string.permission_write),Snackbar.LENGTH_SHORT);
        snackbar.setAction(activity.getResources().getString(R.string.permission_grant), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
                activity.startActivity(intent);
            }
        });
        snackbar.show();
    }

    public interface ExportTaskFinishedListener{
        void onFinished(@NonNull String error_message);
    }

    /**
     * 选择data,obb项，确认重复文件，导出list集合中的应用，并向activity显示一个dialog，传入接口来监听完成回调（在主线程）
     * @param list AppItem的副本，当check_data_obb值为true时无需初始，false时须提前设置好data,obb值
     * @param check_data_obb 传入true 则会执行一次data,obb检查（list中没有设置data,obb值）
     */
    public static void checkAndExportCertainAppItemsToSetPathWithoutShare(@NonNull final Activity activity, @NonNull final List<AppItem>list, boolean check_data_obb,@Nullable final ExportTaskFinishedListener listener){
        if(list.size()==0)return;
        if(check_data_obb){
            DataObbDialog dialog=new DataObbDialog(activity, list, new DataObbDialog.DialogDataObbConfirmedCallback() {
                @Override
                public void onDialogDataObbConfirmed(@NonNull final List<AppItem> export_list) {
                    String dulplicated_info=getDuplicatedFileInfo(activity,export_list);
                    if(!dulplicated_info.trim().equals("")){
                        new AlertDialog.Builder(activity)
                                .setTitle(activity.getResources().getString(R.string.dialog_duplicate_title))
                                .setMessage(activity.getResources().getString(R.string.dialog_duplicate_msg)+dulplicated_info)
                                .setPositiveButton(activity.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        exportCertainAppItemsToSetPathAndShare(activity, export_list,listener);
                                    }
                                })
                                .setNegativeButton(activity.getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {}
                                })
                                .show();
                        return;
                    }
                    exportCertainAppItemsToSetPathAndShare(activity, export_list,listener);
                }
            });
            dialog.show();
        }else{
            String dulplicated_info=getDuplicatedFileInfo(activity,list);
            if(!dulplicated_info.trim().equals("")){
                new AlertDialog.Builder(activity)
                        .setTitle(activity.getResources().getString(R.string.dialog_duplicate_title))
                        .setMessage(activity.getResources().getString(R.string.dialog_duplicate_msg)+dulplicated_info)
                        .setPositiveButton(activity.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                exportCertainAppItemsToSetPathAndShare(activity, list,listener);
                            }
                        })
                        .setNegativeButton(activity.getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .show();
                return;
            }
            exportCertainAppItemsToSetPathAndShare(activity, list,listener);
        }

    }

    /**
     * 导出list集合中的应用，并向activity显示一个dialog，传入接口来监听完成回调（在主线程）
     */
    private static void exportCertainAppItemsToSetPathAndShare(@NonNull final Activity activity, @NonNull List<AppItem>export_list, @Nullable final ExportTaskFinishedListener listener){
        final ExportingDialog dialog=new ExportingDialog(activity);
        final ExportTask task=new ExportTask(activity,export_list,null);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getResources().getString(R.string.dialog_export_stop), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                task.setInterrupted();
                dialog.cancel();
            }
        });
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        task.setExportProgressListener(new ExportTask.ExportProgressListener() {
            @Override
            public void onExportAppItemStarted(int order, AppItem item, int total, String write_path) {
                dialog.setProgressOfApp(order,total,item,write_path);
            }

            @Override
            public void onExportProgressUpdated(long current, long total, String write_path) {
                dialog.setProgressOfWriteBytes(current,total);
            }

            @Override
            public void onExportZipProgressUpdated(String write_path) {
                dialog.setProgressOfCurrentZipFile(write_path);
            }

            @Override
            public void onExportSpeedUpdated(long speed) {
                dialog.setSpeed(speed);
            }

            @Override
            public void onExportTaskFinished(List<FileItem> fileItems, String error_message) {
                dialog.cancel();
                if(listener!=null)listener.onFinished(error_message);

            }
        });
        task.start();
    }

    /**
     * 通过包名获取指定list中的item
     * @param list 要遍历的list
     * @param package_name 要定位的包名
     * @return 查询到的AppItem
     */
    public static @Nullable AppItem getAppItemByPackageNameFromList(@NonNull List<AppItem>list,@NonNull String package_name){
        for(AppItem item:list){
            try {
                if(item.getPackageName().trim().toLowerCase().equals(package_name.trim().toLowerCase()))return item;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 通过FileItem的path从指定list中取出ImportItem
     * @param list 要遍历的list
     * @param path FileItem的path，参考{@link FileItem#getPath()}
     * @return 指定的ImportItem
     */
    public static @Nullable ImportItem getImportItemByFileItemPath(@NonNull List<ImportItem>list,@NonNull String path){
        for(ImportItem importItem:list){
            try {
                if(importItem.getFileItem().getPath().equalsIgnoreCase(path))return importItem;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static String getDuplicatedFileInfo(@NonNull Context context,@NonNull List<AppItem>items){
        if(items.size()==0)return "";
        StringBuilder builder=new StringBuilder();
        boolean external=SPUtil.getIsSaved2ExternalStorage(context);
        if(external){
            for(int i=0;i<items.size();i++){
                final AppItem item=items.get(i);
                try{
                    DocumentFile searchFile=OutputUtil.getExportPathDocumentFile(context).findFile(OutputUtil.getWriteFileNameForAppItem(context,item,(item.exportData||item.exportObb)?
                            SPUtil.getCompressingExtensionName(context):"apk",i));
                    if(searchFile!=null){
                        builder.append(DocumentFileUtil.getDisplayPathForDocumentFile(context,searchFile));
                        builder.append("\n\n");
                    }
                }catch (Exception e){e.printStackTrace();}
            }
        }else {
            for(int i=0;i<items.size();i++){
                final AppItem item=items.get(i);
                File file=new File(OutputUtil.getAbsoluteWritePath(context,item,(item.exportData||item.exportObb)?SPUtil.getCompressingExtensionName(context):"apk",i+1));
                if(file.exists()){
                    builder.append(file.getAbsolutePath());
                    builder.append("\n\n");
                }
            }
        }

        return builder.toString();
    }

    /**
     * 分享指定item应用
     * @param items 传入AppItem的副本，data obb为false
     */
    public static void shareCertainAppsByItems(@NonNull final Activity activity,@NonNull final List<AppItem>items){
        ViewExtsKt.toast("分享应用List");
    }


    /**
     * 展示查重对话框，启动导入流程
     */
    public static void showCheckingDuplicationDialogAndStartImporting(@NonNull final Activity activity,
                                                                      @NonNull final List<ImportItem>importItems,
                                                                      @NonNull final List<ZipFileUtil.ZipFileInfo>zipFileInfos,
                                                                      @Nullable final ImportTaskFinishedCallback callback){
        final AlertDialog dialog_duplication_wait=new AlertDialog.Builder(activity)
                .setTitle(activity.getResources().getString(R.string.dialog_wait))
                .setView(LayoutInflater.from(activity).inflate(R.layout.dialog_duplication_file,null))
                .setNegativeButton(activity.getResources().getString(R.string.dialog_button_cancel), null)
                .setCancelable(false)
                .show();
        final GetImportLengthAndDuplicateInfoTask infoTask=new GetImportLengthAndDuplicateInfoTask(importItems,zipFileInfos,new GetImportLengthAndDuplicateInfoTask.GetImportLengthAndDuplicateInfoCallback(){
            @Override
            public void onCheckingFinished(@NonNull List<String>duplication_infos, long total) {
                dialog_duplication_wait.cancel();
                final ImportingDialog importingDialog=new ImportingDialog(activity,total);
                final ImportTask.ImportTaskCallback importTaskCallback=new ImportTask.ImportTaskCallback() {
                    @Override
                    public void onImportTaskStarted() {}

                    @Override
                    public void onRefreshSpeed(long speed) {
                        importingDialog.setSpeed(speed);
                    }

                    @Override
                    public void onImportTaskProgress(@NonNull String writePath, long progress) {
                        importingDialog.setProgress(progress);
                        importingDialog.setCurrentWritingName(writePath);
                    }

                    @Override
                    public void onImportTaskFinished(@NonNull String errorMessage) {
                        importingDialog.cancel();
                        if(callback!=null)callback.onImportFinished(errorMessage);
                    }
                };
                final ImportTask importTask=new ImportTask(activity, importItems,importTaskCallback);
                importingDialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getResources().getString(R.string.word_stop), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importTask.setInterrupted();
                        importingDialog.cancel();
                    }
                });
                if(duplication_infos.size()==0){
                    importingDialog.show();
                    importTask.start();
                }else{
                    StringBuilder stringBuilder=new StringBuilder();
                    int checkingIndex=duplication_infos.size();
                    int unListed=0;
                    if(checkingIndex>100){
                        unListed=checkingIndex-100;
                        checkingIndex=100;
                    }
                    for(int i=0;i<checkingIndex;i++){
                        stringBuilder.append(duplication_infos.get(i));
                        stringBuilder.append("\n\n");
                    }
                    if(unListed>0){
                        stringBuilder.append("+");
                        stringBuilder.append(unListed);
                        stringBuilder.append(activity.getResources().getString(R.string.dialog_import_duplicate_more));
                    }
                    new AlertDialog.Builder(activity)
                            .setTitle(activity.getResources().getString(R.string.dialog_import_duplicate_title))
                            .setMessage(activity.getResources().getString(R.string.dialog_import_duplicate_message)+stringBuilder.toString())
                            .setPositiveButton(activity.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    importingDialog.show();
                                    importTask.start();
                                }
                            })
                            .setNegativeButton(activity.getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {}
                            })
                            .show();
                }
            }
        });
        infoTask.start();
        dialog_duplication_wait.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_duplication_wait.cancel();
                infoTask.setInterrupted();
            }
        });
    }

    public interface ImportTaskFinishedCallback{
        void onImportFinished(String error_message);
    }

    public static void shareImportItems(@NonNull Activity activity,@NonNull List<ImportItem>importItems){
        ViewExtsKt.toast("分享应用");
    }

    /**
     * 执行分享应用操作
     */
    public static void shareCertainFiles(@NonNull Context context, @NonNull List<Uri>uris, @NonNull String title){
        if(uris.size()==0)return;
        Intent intent=new Intent();
        //intent.setType("application/vnd.android.package-archive");
        intent.setType("application/x-zip-compressed");
        if(uris.size()>1){
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            //ArrayList<Uri> uris=new ArrayList<>();
            //for(String path:paths) uris.add(Uri.fromFile(new File(path)));
            intent.putExtra(Intent.EXTRA_STREAM, new ArrayList<>(uris));
        }else{
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM,uris.get(0));
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, title);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{
            context.startActivity(Intent.createChooser(intent,title));
        }catch (Exception e){
            e.printStackTrace();
            ToastManager.showToast(context,e.toString(),Toast.LENGTH_SHORT);
        }
    }

    /**
     * 通过系统分享接口分享本应用
     */
    public static void shareThisApp(@NonNull Context context){
        try{
            ApplicationInfo applicationInfo=context.getPackageManager().getApplicationInfo(context.getPackageName(),0);
            final String title=context.getResources().getString(R.string.share_title);
            Intent intent=new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.android.package-archive");
            intent.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(new File(applicationInfo.sourceDir)));
            intent.putExtra(Intent.EXTRA_SUBJECT,title);
            intent.putExtra(Intent.EXTRA_TEXT,title);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent,title));
        }catch (Exception e){
            e.printStackTrace();
            ToastManager.showToast(context,e.toString(),Toast.LENGTH_SHORT);
        }
    }


}
