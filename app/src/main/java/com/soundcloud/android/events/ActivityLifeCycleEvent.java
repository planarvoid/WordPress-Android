package com.soundcloud.android.events;

import android.app.Activity;

public final class ActivityLifeCycleEvent {

    public static final int ON_RESUME_EVENT = 0;
    public static final int ON_CREATE_EVENT = 1;
    public static final int ON_PAUSE_EVENT = 2;

    private final Class<? extends Activity> activityClass;
    private final int kind;

    public static ActivityLifeCycleEvent forOnCreate(Class<? extends Activity> activityClass) {
        return new ActivityLifeCycleEvent(ON_CREATE_EVENT, activityClass);
    }

    public static ActivityLifeCycleEvent forOnResume(Class<? extends Activity> activityClass) {
        return new ActivityLifeCycleEvent(ON_RESUME_EVENT, activityClass);
    }

    public static ActivityLifeCycleEvent forOnPause(Class<? extends Activity> activityClass) {
        return new ActivityLifeCycleEvent(ON_PAUSE_EVENT, activityClass);
    }

    private ActivityLifeCycleEvent(int kind, Class<? extends Activity> activityClass) {
        this.activityClass = activityClass;
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    public Class<? extends Activity> getActivityClass() {
        return activityClass;
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
            default:
                throw new IllegalStateException(
                        "Attempting to get name of unknown lifecycle method code: " + kind);
        }
    }
}
