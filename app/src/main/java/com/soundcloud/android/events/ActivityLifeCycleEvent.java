package com.soundcloud.android.events;

import android.app.Activity;

public final class ActivityLifeCycleEvent implements Event {

    public static final int ON_RESUME_EVENT = 0;
    public static final int ON_CREATE_EVENT = 1;
    public static final int ON_PAUSE_EVENT = 2;

    private final Class<? extends Activity> mActivityClass;
    private final int mKind;

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
        mActivityClass = activityClass;
        mKind = kind;
    }

    @Override
    public int getKind() {
        return mKind;
    }

    public Class<? extends Activity> getActivityClass() {
        return mActivityClass;
    }

    @Override
    public String toString() {
        return mActivityClass.getSimpleName() + "#" + lifeCycleMethodName();
    }

    private String lifeCycleMethodName() {
        switch (mKind) {
            case ON_CREATE_EVENT:
                return "onCreate";
            case ON_RESUME_EVENT:
                return "onResume";
            case ON_PAUSE_EVENT:
                return "onPause";
            default:
                throw new IllegalStateException(
                        "Attempting to get name of unknown lifecycle method code: " + mKind);
        }
    }
}
