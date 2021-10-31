package info.muge.apkextractor.tasks;

import androidx.annotation.NonNull;

import info.muge.apkextractor.Global;
import info.muge.apkextractor.items.AppItem;
import info.muge.apkextractor.utils.PinyinUtil;

import java.util.ArrayList;
import java.util.List;

public class SearchAppItemTask extends Thread {

    private volatile boolean isInterrupted=false;
    private final String search_info;
    private final List<AppItem> appItemList=new ArrayList<>();
    private final ArrayList<AppItem> result_appItems=new ArrayList<>();
    private final SearchTaskCompletedCallback callback;


    public SearchAppItemTask(List<AppItem>appItems,@NonNull String info,@NonNull SearchTaskCompletedCallback callback){
        this.search_info=info.trim().toLowerCase();
        this.appItemList.addAll(appItems);
        this.callback=callback;
    }

    @Override
    public void run() {
        super.run();
        for(AppItem item: appItemList){
            if(isInterrupted){
                break;
            }
            try{
                boolean b=(getFormatString(item.getAppName()).contains(search_info)
                        ||getFormatString(item.getPackageName()).contains(search_info)
                        ||getFormatString(item.getVersionName()).contains(search_info)
                        ||getFormatString(PinyinUtil.getFirstSpell(item.getAppName())).contains(search_info)
                        ||getFormatString(PinyinUtil.getFullSpell(item.getAppName())).contains(search_info)
                        ||getFormatString(PinyinUtil.getPinYin(item.getAppName())).contains(search_info))&&!search_info.trim().equals("");
                if(b) result_appItems.add(item);
            }catch (Exception e){e.printStackTrace();}
        }
        Global.handler.post(new Runnable() {
            @Override
            public void run() {
                if(!isInterrupted)callback.onSearchTaskCompleted(result_appItems,search_info);
            }
        });
    }

    public void setInterrupted(){
        isInterrupted=true;
    }

    private String getFormatString(@NonNull String s){
        return s.trim().toLowerCase();
    }

    public interface SearchTaskCompletedCallback{
        void onSearchTaskCompleted(@NonNull List<AppItem> appItems,@NonNull String keyword);
    }
}
