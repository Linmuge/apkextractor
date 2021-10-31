package info.muge.apkextractor.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public interface OperationCallback {
    void onItemLongClickedAndMultiSelectModeOpened(@NonNull Fragment fragment);
}
