package com.soundcloud.android.events;

import android.app.Activity;
import android.support.annotation.Nullable;

public final class ActivityLifeCycleEvent {

    public static final int ON_RESUME_EVENT = 0;
    public static final int ON_CREATE_EVENT = 1;
    public static final int ON_PAUSE_EVENT = 2;
    public static final int ON_START_EVENT = 3;
    public static final int ON_STOP_EVENT = 4;

    private final Class<? extends Activity> activityClass;
    private final Activity activity;
    private final int kind;

    public static ActivityLifeCycleEvent forOnCreate(Activity activity) {
        return new ActivityLifeCycleEvent(ON_CREATE_EVENT, activity);
    }

    public static ActivityLifeCycleEvent forOnResume(Activity activity) {
        return new ActivityLifeCycleEvent(ON_RESUME_EVENT, activity);
    }

    public static ActivityLifeCycleEvent forOnStart(Activity activity) {
        return new ActivityLifeCycleEvent(ON_START_EVENT, activity);
    }

    public static ActivityLifeCycleEvent forOnStop(Activity activity) {
        return new ActivityLifeCycleEvent(ON_STOP_EVENT, activity);
    }

    public static ActivityLifeCycleEvent forOnPause(Activity activity) {
        return new ActivityLifeCycleEvent(ON_PAUSE_EVENT, activity);
    }

    private ActivityLifeCycleEvent(int kind, Activity activity) {
        this.activityClass = activity.getClass();
        this.activity = activity;
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    public Class<? extends Activity> getActivityClass() {
        return activityClass;
    }

    @Nullable
    public Activity getActivity() {
        return activity;
    }

    @Override
    public String toString() {
        return activityClass.getSimpleName() + "#" + lifeCycleMethodName();
    }

    private String lifeCycleMethodName() {
        switch (kind) {
            case ON_CREATE_EVENT:
                return "onCreate";
            case ON_RESUME_EVENT:
                return "onResume";
            case ON_PAUSE_EVENT:
                return "onPause";
            case ON_START_EVENT:
                return "onStart";
            case ON_STOP_EVENT:
                return "onStop";
            default:
                throw new IllegalStateException(
                        "Attempting to get name of unknown lifecycle method code: " + kind);
        }
    }
}
