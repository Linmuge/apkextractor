package info.muge.appshare.adapters;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import info.muge.appshare.R;

public class MyPagerAdapter extends FragmentPagerAdapter {

    private final Activity activity;
    private final Fragment[]fragments;

    public MyPagerAdapter(@NonNull Activity activity,@NonNull FragmentManager fm,Fragment ... fragments) {
        super(fm);
        this.activity = activity;
        this.fragments=fragments;
    }

    @Override
    public Fragment getItem(int i) {
        return fragments[i];
    }

    @Override
    public int getCount() {
        if(fragments==null)return 0;
        return fragments.length;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            default:break;
            case 0:return activity.getResources().getString(R.string.main_page_export);
        }
        return "";
    }
}
