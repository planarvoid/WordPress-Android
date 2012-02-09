package com.soundcloud.android.utils.analytics;

import android.os.AsyncTask;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * http://blog.tomgibara.com/post/5598222730/improving-android-google-analytics-performance
 * <p/>
 * got rid of unused stuff
 *
 * @author Tom Gibara
 */

public class GATracker {

    public enum Type {
        PAGE_VIEW,
        EVENT
    }

    // fields
    private final EnumSet<Type> mEnabled = EnumSet.allOf(Type.class);
    private final GoogleAnalyticsTracker mTracker;
    private final ArrayList<Event> mQueue = new ArrayList<Event>();

    private boolean mQueueFlushing = false;
    private Event[] mEvents = null; // temporary working set, held globally to avoid pointless repeated allocations


    public GATracker(GoogleAnalyticsTracker tracker) {
        if (tracker == null) throw new IllegalArgumentException("null tracker");
        mTracker = tracker;
    }

    public void trackPageView(String pagePath) {
        if (!mEnabled.contains(Type.PAGE_VIEW)) return;
        enqueue(new Event(Type.PAGE_VIEW, null, pagePath, null, 0));
    }

    public void trackEvent(String category, String action, String label, int value) {
        track(Type.EVENT, category, action, label, value);
    }


    public void setCustomVar(int i, String s, String s1) {
        mTracker.setCustomVar(i, s, s1);
    }


    // private utility methods

    private void track(Type type, String category, String action, String label, int value) {
        if (!mEnabled.contains(type)) return;
        enqueue(new Event(type, category, action, label, value));
    }

    private void enqueue(Event event) {
        synchronized (mQueue) {
            mQueue.add(event);
            if (!mQueueFlushing) {
                new EventTask().execute();
            }
        }
    }

    private void track(String page) {
        mTracker.trackPageView(page);
    }

    private void track(String category, String action, String label, int value) {
        mTracker.trackEvent(category, action, label, value);
    }

    // inner classes

    private class EventTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            synchronized (mQueue) {
                if (mQueueFlushing) return null;
                mQueueFlushing = true;
            }
            try {
                while (true) {
                    final int length;
                    synchronized (mQueue) {
                        length = mQueue.size();
                        if (length == 0) return null;
                        if (mEvents == null || mEvents.length < length) {
                            mEvents = new Event[mQueue.size()];
                        }
                        mQueue.toArray(mEvents);
                        mQueue.clear();
                    }
                    for (int i = 0; i < length; i++) {
                        Event event = mEvents[i];
                        if (event.mType == Type.PAGE_VIEW) {
                            track(event.mAction);
                        } else {
                            track(event.mCategory, event.mAction, event.mLabel, event.mValue);
                        }
                    }
                    Arrays.fill(mEvents, null);
                }
            } finally {
                synchronized (mQueue) {
                    mQueueFlushing = false;
                }
            }
        }

    }

    private static class Event {

        Type mType;
        String mCategory;
        String mAction;
        String mLabel;
        int mValue;

        Event(Type type, String category, String action, String label, int value) {
            mType = type;
            mCategory = category;
            mAction = action;
            mLabel = label;
            mValue = value;
        }

    }

}