package info.muge.apkextractor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import info.muge.apkextractor.Constants;
import info.muge.apkextractor.R;

public class DocumentFileUtil {

    /**
     * 通过segment片段定位到parent的指定文件夹，如果没有则尝试创建
     */
    public static @NonNull DocumentFile getDocumentFileBySegments(@NonNull DocumentFile parent, @Nullable String segment) throws Exception{
        if(segment==null)return parent;
        String[]segments=segment.split("/");
        DocumentFile documentFile=parent;
        for(int i=0;i<segments.length;i++){
            DocumentFile lookup=documentFile.findFile(segments[i]);
            if(lookup==null){
                lookup=documentFile.createDirectory(segments[i]);
            }
            if(lookup==null){
                throw new Exception("Can not create folder "+segments[i]);
            }
            documentFile=lookup;
        }
        return documentFile;
    }

    /**
     * 将segments数组转换为string
     */
    public static @NonNull String toSegmentString(@NonNull Object[]segments){
        StringBuilder builder=new StringBuilder();
        for(int i=0;i<segments.length;i++){
            builder.append(segments[i]);
            if(i<segments.length-1)builder.append("/");
        }
        return builder.toString();
    }


    /**
     * 获取一个documentFile用于展示的路径
     */
    public static @NonNull String getDisplayPathForDocumentFile(@NonNull Context context,@NonNull DocumentFile documentFile){
        String uriPath= documentFile.getUri().getPath();
        if(uriPath==null)return "";
        int index=uriPath.lastIndexOf(":")+1;
        if(index<=uriPath.length())return context.getResources().getString(R.string.external_storage)+"/"+uriPath.substring(index);
        return "";
    }

}
