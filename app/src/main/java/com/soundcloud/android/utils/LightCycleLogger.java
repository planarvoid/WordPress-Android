package com.soundcloud.android.utils;

import static android.util.Log.INFO;
import static com.soundcloud.android.utils.ErrorUtils.log;

import com.soundcloud.lightcycle.ActivityLightCycle;
import com.soundcloud.lightcycle.SupportFragmentLightCycle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;

public class LightCycleLogger {

    public static SupportFragmentLightCycle<Fragment> forSupportFragment(String tag) {

        return new SupportFragmentLightCycle<Fragment>() {
            @Override
            public void onAttach(Fragment fragment, Activity activity) {
                log(INFO, tag, "[LIFE_CYCLE] onAttach Fragment = [" + fragment + "], activity = [" + activity + "]");
            }

            @Override
            public void onCreate(Fragment fragment, Bundle bundle) {
                log(INFO, tag, "[LIFE_CYCLE] onCreate  [" + fragment + "], bundle = [" + bundle + "]");
            }

            @Override
            public void onViewCreated(Fragment fragment, View view, Bundle bundle) {
                log(INFO, tag, "[LIFE_CYCLE] onViewCreated  Fragment = [" + fragment + "], view = [" + view + "], bundle = [" + bundle + "]");
            }

            @Override
            public void onActivityCreated(Fragment fragment, Bundle bundle) {
                log(INFO, tag, "[LIFE_CYCLE] onActivityCreated  Fragment = [" + fragment + "], bundle = [" + bundle + "]");
            }

            @Override
            public void onStart(Fragment fragment) {
                log(INFO, tag, "[LIFE_CYCLE] onStart fragment = [" + fragment + "]");
            }

            @Override
            public void onResume(Fragment fragment) {
                log(INFO, tag, "[LIFE_CYCLE] onResume fragment = [" + fragment + "]");
            }

            @Override
            public boolean onOptionsItemSelected(Fragment fragment, MenuItem menuItem) {
                return false;
            }

            @Override
            public void onPause(Fragment fragment) {
                log(INFO, tag, "[LIFE_CYCLE] onPause fragment = [" + fragment + "]");
            }

            @Override
            public void onStop(Fragment fragment) {
                log(INFO, tag, "[LIFE_CYCLE] onStop fragment = [" + fragment + "]");
            }

            @Override
            public void onSaveInstanceState(Fragment fragment, Bundle bundle) {
                log(INFO, tag, "[LIFE_CYCLE] onSaveInstanceState Fragment = [" + fragment + "], bundle = [" + bundle + "]");
            }

            @Override
            public void onDestroyView(Fragment fragment) {
                log(INFO, tag, "[LIFE_CYCLE] onDestroyView fragment = [" + fragment + "]");
            }

            @Override
            public void onDestroy(Fragment fragment) {
                log(INFO, tag, "[LIFE_CYCLE] onDestroy fragment = [" + fragment + "]");
            }

            @Override
            public void onDetach(Fragment fragment) {
                log(INFO, tag, "[LIFE_CYCLE] onDetach fragment = [" + fragment + "]");
            }
        };
    }

    public static ActivityLightCycle<Activity> forActivity(String tag) {
        return new ActivityLightCycle<Activity>() {
            @Override
            public void onCreate(Activity activity, Bundle bundle) {
                log(INFO, tag, "[LIFE_CYCLE] onCreate activity = [" + activity + "], bundle = [" + bundle + "]");
            }

            @Override
            public void onNewIntent(Activity activity, Intent intent) {
                log(INFO, tag, "[LIFE_CYCLE] onNewIntent activity = [" + activity + "], intent = [" + intent + "]");
            }

            @Override
            public void onStart(Activity activity) {
                log(INFO, tag, "[LIFE_CYCLE] onStart activity = [" + activity + "]");
            }

            @Override
            public void onResume(Activity activity) {
                log(INFO, tag, "[LIFE_CYCLE] onResume activity = [" + activity + "]");
            }

            @Override
            public boolean onOptionsItemSelected(Activity activity, MenuItem menuItem) {
                return false;
            }

            @Override
            public void onPause(Activity activity) {
                log(INFO, tag, "[LIFE_CYCLE] onPause activity = [" + activity + "]");
            }

            @Override
            public void onStop(Activity activity) {
                log(INFO, tag, "[LIFE_CYCLE] onStop activity = [" + activity + "]");
            }

            @Override
            public void onSaveInstanceState(Activity activity, Bundle bundle) {
                log(INFO, tag, "[LIFE_CYCLE] onSaveInstanceState activity = [" + activity + "], bundle = [" + bundle + "]");
            }

            @Override
            public void onRestoreInstanceState(Activity activity, Bundle bundle) {
                log(INFO, tag, "[LIFE_CYCLE] onRestoreInstanceState activity = [" + activity + "], bundle = [" + bundle + "]");
            }

            @Override
            public void onDestroy(Activity activity) {
                log(INFO, tag, "[LIFE_CYCLE] onDestroy activity = [" + activity + "]");
            }
        };
    }
}
