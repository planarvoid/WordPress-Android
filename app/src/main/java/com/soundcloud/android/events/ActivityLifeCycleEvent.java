package com.soundcloud.android.events;

import com.google.common.annotations.VisibleForTesting;

import android.app.Activity;

import java.util.Collections;

public final class ActivityLifeCycleEvent extends Event {

    private static final int ON_RESUME_EVENT = 0;
    private static final int ON_CREATE_EVENT = 1;
    private static final int ON_PAUSE_EVENT = 2;

    private Class<? extends Activity> mActivityClass;
    private int mLifeCycleMethod;

    public static ActivityLifeCycleEvent forOnCreate(Class<? extends Activity> activityClass) {
        return new ActivityLifeCycleEvent(activityClass, ON_CREATE_EVENT);
    }

    public static ActivityLifeCycleEvent forOnResume(Class<? extends Activity> activityClass) {
        return new ActivityLifeCycleEvent(activityClass, ON_RESUME_EVENT);
    }

    public static ActivityLifeCycleEvent forOnPause(Class<? extends Activity> activityClass) {
        return new ActivityLifeCycleEvent(activityClass, ON_PAUSE_EVENT);
    }

    @VisibleForTesting
    ActivityLifeCycleEvent(Class<? extends Activity> activityClass, int lifeCycleMethod) {
        super(lifeCycleMethod, Collections.<String, String>emptyMap());
        mActivityClass = activityClass;
        mLifeCycleMethod = lifeCycleMethod;
    }

    public boolean isResumeEvent() {
        return mLifeCycleMethod == ON_RESUME_EVENT;
    }

    public boolean isCreateEvent() {
        return mLifeCycleMethod == ON_CREATE_EVENT;
    }

    public boolean isPauseEvent() {
        return mLifeCycleMethod == ON_PAUSE_EVENT;
    }

    public Class<? extends Activity> getActivityClass() {
        return mActivityClass;
    }

    @Override
    public String toString() {
        return mActivityClass.getSimpleName() + "#" + lifeCycleMethodName();
    }

    private String lifeCycleMethodName() {
        switch (mLifeCycleMethod) {
            case ON_CREATE_EVENT:
                return "onCreate";
            case ON_RESUME_EVENT:
                return "onResume";
            case ON_PAUSE_EVENT:
                return "onPause";
            default:
                throw new IllegalStateException(
                        "Attempting to get name of unknown lifecycle method code: " + mLifeCycleMethod);
        }
    }
}
