package com.soundcloud.android.view.status;

import static com.soundcloud.android.utils.ViewUtils.blendColors;
import static com.soundcloud.android.view.status.StatusBarUtils.isLightStatusBar;

import com.soundcloud.android.R;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StatusBarColorController extends DefaultActivityLightCycle<AppCompatActivity> {

    private int statusBarColor;
    private int expandedStatusColor;
    private boolean setLightStatusBar;
    private boolean expandOnResume;
    private boolean playerIsExpanded;
    private AppCompatActivity activity;

    @Inject
    public StatusBarColorController() {
        // dagger
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        super.onResume(activity);
        this.activity = activity;
        this.setLightStatusBar = isLightStatusBar(getView());

        statusBarColor = StatusBarUtils.getStatusBarColor(activity);
        expandedStatusColor = activity.getResources().getColor(R.color.primary_darker);
        showPlayerExpandedIfNecessary();
    }

    void showPlayerExpandedIfNecessary() {
        if (expandOnResume) {
            onPlayerExpanded();
        }
        expandOnResume = false;
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        this.activity = null;
    }

    public void setLightStatusBar() {
        if (activity != null && !setLightStatusBar) {
            this.setLightStatusBar = true;
            StatusBarUtils.setLightStatusBar(getView());
        }
    }

    public void clearLightStatusBar() {
        if (activity != null && setLightStatusBar) {
            this.setLightStatusBar = false;
            StatusBarUtils.clearLightStatusBar(getView());
        }
    }

    public void setStatusBarColor(int value) {
        if (activity == null) {
            return;
        }

        statusBarColor = value;
        if (!playerIsExpanded) {
            StatusBarUtils.setStatusBarColor(activity, statusBarColor);
        }
    }

    private View getView() {
        return activity.findViewById(android.R.id.content);
    }

    public void onPlayerExpanded() {
        playerIsExpanded = true;
        if (activity != null) {
            StatusBarUtils.setStatusBarColor(activity, expandedStatusColor);
            StatusBarUtils.clearLightStatusBar(getView());
        } else {
            expandOnResume = true;
        }

    }

    public void onPlayerSlide(float slideOffset) {
        if (activity != null) {
            StatusBarUtils.setStatusBarColor(activity, blendColors(statusBarColor, expandedStatusColor, slideOffset));
        }
    }

    public void onPlayerCollapsed() {
        playerIsExpanded = false;
        if (activity != null) {
            StatusBarUtils.setStatusBarColor(activity, statusBarColor);
            if (setLightStatusBar) {
                StatusBarUtils.setLightStatusBar(activity.findViewById(android.R.id.content));
            } else {
                StatusBarUtils.clearLightStatusBar(activity.findViewById(android.R.id.content));
            }
        }
    }

}
