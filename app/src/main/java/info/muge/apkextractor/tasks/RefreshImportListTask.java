package info.muge.apkextractor.tasks;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import info.muge.apkextractor.Constants;
import info.muge.apkextractor.Global;
import info.muge.apkextractor.items.FileItem;
import info.muge.apkextractor.items.ImportItem;
import info.muge.apkextractor.utils.SPUtil;
import info.muge.apkextractor.utils.StorageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RefreshImportListTask extends Thread{
    private Context context;
    //private FileItem fileItem;
    private final RefreshImportListTaskCallback callback;
    public RefreshImportListTask(Context context, RefreshImportListTaskCallback callback){
        this.context=context;
        this.callback=callback;
        //boolean isExternal= SPUtil.getIsSaved2ExternalStorage(context);
        /*if(isExternal){
            try{
                fileItem=new FileItem(context, Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context));
            }catch (Exception e){e.printStackTrace();}
        }else{
            fileItem=new FileItem(SPUtil.getInternalSavePath(context));
        }*/
    }

    @Override
    public void run(){
        final ArrayList<ImportItem> arrayList=new ArrayList<>();
        if(callback!=null) Global.handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onRefreshStarted();
            }
        });
        final int package_scope_value=SPUtil.getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_PACKAGE_SCOPE,Constants.PREFERENCE_PACKAGE_SCOPE_DEFAULT);
        FileItem fileItem=null;
        if(package_scope_value==Constants.PACKAGE_SCOPE_ALL){
            fileItem=new FileItem(StorageUtil.getMainExternalStoragePath());
        }else if(package_scope_value==Constants.PACKAGE_SCOPE_EXPORTING_PATH){
            if(SPUtil.getIsSaved2ExternalStorage(context)){
                try {
                    fileItem=new FileItem(context, Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                fileItem=new FileItem(SPUtil.getInternalSavePath(context));
            }
        }
        try{
            arrayList.addAll(getAllImportItemsFromPath(fileItem));
            if(!TextUtils.isEmpty(SPUtil.getExternalStorageUri(context))&&package_scope_value==Constants.PACKAGE_SCOPE_ALL){
                arrayList.addAll(getAllImportItemsFromPath(new FileItem(context, Uri.parse(SPUtil.getExternalStorageUri(context)), SPUtil.getSaveSegment(context))));
            }
            ImportItem.sort_config=SPUtil.getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS,0);
            Collections.sort(arrayList);
        }catch (Exception e){e.printStackTrace();}
        synchronized (Global.item_list){
            Global.item_list.clear();
            Global.item_list.addAll(arrayList);
        }
        HashTask.clearResultCache();
        GetSignatureInfoTask.clearCache();
        if(callback!=null){
            Global.handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onRefreshCompleted(arrayList);
                }
            });
        }
    }

    private ArrayList<ImportItem> getAllImportItemsFromPath(final FileItem fileItem){
        ArrayList<ImportItem>arrayList=new ArrayList<>();
        try{
            if (fileItem==null)return arrayList;
            //File file=new File(fileItem.getPath());
            if(!fileItem.isDirectory()){
                if(fileItem.getPath().trim().toLowerCase().endsWith(".apk")||fileItem.getPath().trim().toLowerCase().endsWith(".zip")
                ||fileItem.getPath().trim().toLowerCase().endsWith(".xapk")
                        ||fileItem.getPath().trim().toLowerCase().endsWith(SPUtil.getCompressingExtensionName(context).toLowerCase())){
                    if(callback!=null){
                        Global.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onProgress(fileItem);
                            }
                        });
                    }
                    arrayList.add(new ImportItem(context,fileItem));
                }
                return arrayList;
            }
            List<FileItem>fileItems=fileItem.listFileItems();
            for(final FileItem fileItem1:fileItems){
                if(fileItem1.isDirectory())arrayList.addAll(getAllImportItemsFromPath(fileItem1));
                else {
                    if(fileItem1.getPath().trim().toLowerCase().endsWith(".apk")||fileItem1.getPath().trim().toLowerCase().endsWith(".zip")
                            ||fileItem1.getPath().trim().toLowerCase().endsWith(".xapk")
                            ||fileItem1.getPath().trim().toLowerCase().endsWith(SPUtil.getCompressingExtensionName(context).toLowerCase())){
                        try{
                            if(callback!=null){
                                Global.handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onProgress(fileItem1);
                                    }
                                });
                            }
                            arrayList.add(new ImportItem(context,fileItem1));
                        }catch (Exception e){e.printStackTrace();}
                    }
                }
            }
        }catch (Exception e){e.printStackTrace();}
        return arrayList;
    }

    public interface RefreshImportListTaskCallback{
        void onRefreshStarted();
        void onProgress(@NonNull FileItem fileItem);
        void onRefreshCompleted(List<ImportItem> list);
    }
}
