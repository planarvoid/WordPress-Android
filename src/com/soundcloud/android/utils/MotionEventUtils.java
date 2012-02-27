/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;

/**
 * A utility class that emulates multitouch APIs available in Android 2.0+.
 */
public final class MotionEventUtils {
    public static final int ACTION_MASK = 0xff;
    public static final int ACTION_POINTER_UP = 0x6;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;

    public static boolean sMultiTouchApiAvailable;

    private MotionEventUtils() {}

    static {
        try {
            MotionEvent.class.getMethod("getPointerId", new Class[]{int.class});
            sMultiTouchApiAvailable = true;
        } catch (NoSuchMethodException nsme) {
            sMultiTouchApiAvailable = false;
        }
    }

    public static float getX(MotionEvent ev, int pointerIndex) {
        if (sMultiTouchApiAvailable) {
            return WrappedStaticMotionEvent.getX(ev, pointerIndex);
        } else {
            return ev.getX();
        }
    }

    public static float getY(MotionEvent ev, int pointerIndex) {
        if (sMultiTouchApiAvailable) {
            return WrappedStaticMotionEvent.getY(ev, pointerIndex);
        } else {
            return ev.getY();
        }
    }

    public static int getPointerId(MotionEvent ev, int pointerIndex) {
        if (sMultiTouchApiAvailable) {
            return WrappedStaticMotionEvent.getPointerId(ev, pointerIndex);
        } else {
            return 0;
        }
    }

    public static int findPointerIndex(MotionEvent ev, int pointerId) {
        if (sMultiTouchApiAvailable) {
            return WrappedStaticMotionEvent.findPointerIndex(ev, pointerId);
        } else {
            return (pointerId == 0) ? 0 : -1;
        }
    }


    public static boolean isOutOfBounds(MotionEvent event, Dialog d) {
        return isOutOfBounds(d.getContext(), event, d.getWindow());
    }

    public static boolean isOutOfBounds(Context context, MotionEvent event, Window window) {
        if (context != null) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final int slop = ViewConfiguration.get(context).getScaledWindowTouchSlop();
            final View decorView = window.getDecorView();
            return (x < -slop) ||
                    (y < -slop) ||
                    (x > (decorView.getWidth() + slop)) ||
                    (y > (decorView.getHeight() + slop));
        } else {
            return true;
        }
    }

    // Show an event in the LogCat view, for debugging
    @SuppressWarnings("UnusedDeclaration")
    public static void dumpMotionEvent(MotionEvent event) {
        String names[] = {
                "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP", "7?",
                "8?", "9?"
        };
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);
        if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }
        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }
        sb.append("]");
        Log.d(TAG, sb.toString());
    }

    /**
     * A wrapper around newer (SDK level 5) MotionEvent APIs. This class only gets loaded
     * if it is determined that these new APIs exist on the device.
     */
    private static class WrappedStaticMotionEvent {
        public static float getX(MotionEvent ev, int pointerIndex) {
            return ev.getX(pointerIndex);
        }

        public static float getY(MotionEvent ev, int pointerIndex) {
            return ev.getY(pointerIndex);
        }

        public static int getPointerId(MotionEvent ev, int pointerIndex) {
            return ev.getPointerId(pointerIndex);
        }

        public static int findPointerIndex(MotionEvent ev, int pointerId) {
            return ev.findPointerIndex(pointerId);
        }
    }
}