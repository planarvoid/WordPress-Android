package com.soundcloud.android.utils;

import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.settings.Settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class VersionMigrator {
    private final SharedPreferences mSharedPreferences;
    private final int mCurrentVersionCode;

    public VersionMigrator(Context context){
        this(AndroidUtils.getAppVersionCode(context, 0), PreferenceManager.getDefaultSharedPreferences(context));
    }

    public VersionMigrator(int currentAppVersionCode, SharedPreferences sharedPreferences) {
        mCurrentVersionCode = currentAppVersionCode;
        mSharedPreferences = sharedPreferences;
    }

    public boolean migrate(){

        if (mCurrentVersionCode > 0 && mCurrentVersionCode > mSharedPreferences.getInt(Consts.PrefKeys.VERSION_KEY, 0)) {
            final SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putInt(Consts.PrefKeys.VERSION_KEY, mCurrentVersionCode);
            SharedPreferencesUtils.apply(editor);

            switch (mCurrentVersionCode){
                case 68 :
                    editor.putBoolean(Settings.ANALYTICS, mSharedPreferences.getBoolean(Settings.ACRA_ENABLE, true));
                    editor.putBoolean(Settings.ACRA_ENABLE, mSharedPreferences.getBoolean(Settings.CRASHLOGS, true));
                    editor.commit();
                    break;
            }
            return true;
        } else {
            return false;
        }
    }
}
