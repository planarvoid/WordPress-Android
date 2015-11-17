package com.soundcloud.android.utils;

import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ArrayBlockingQueue;

public class InputObject {
    public static final byte EVENT_TYPE_KEY = 1;
    public static final byte EVENT_TYPE_TOUCH = 2;
    public static final int ACTION_KEY_DOWN = 1;
    public static final int ACTION_KEY_UP = 2;
    public static final int ACTION_TOUCH_DOWN = 3;
    public static final int ACTION_TOUCH_MOVE = 4;
    public static final int ACTION_TOUCH_UP = 5;
    public static final int ACTION_TOUCH_POINTER_DOWN = 6;
    public static final int ACTION_TOUCH_POINTER_UP = 7;

    public ArrayBlockingQueue<InputObject> pool;

    public byte eventType;
    public long time;
    public int action;
    public int x;
    public int y;
    public int pointerX;
    public int pointerY;
    public View view;
    public int actionIndex;

    public InputObject(ArrayBlockingQueue<InputObject> pool) {
        this.pool = pool;
    }

    public void useEvent(View v, MotionEvent event) {
        view = v;
        eventType = EVENT_TYPE_TOUCH;

        int actionCode = event.getAction() & MotionEvent.ACTION_MASK;
        switch (actionCode) {
            case MotionEvent.ACTION_DOWN:
                action = ACTION_TOUCH_DOWN;
                break;
            case MotionEvent.ACTION_MOVE:
                action = ACTION_TOUCH_MOVE;
                break;
            case MotionEvent.ACTION_UP:
                action = ACTION_TOUCH_UP;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                action = ACTION_TOUCH_POINTER_DOWN;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                action = ACTION_TOUCH_POINTER_UP;
                break;
            default:
                action = 0;
        }

        actionIndex = event.getActionIndex();
        time = event.getEventTime();
        x = (int) event.getX();
        y = (int) event.getY();

        if (event.getPointerCount() > 1) {
            pointerX = (int) event.getX(1);
            pointerY = (int) event.getY(1);
        }

    }

    public void useEventHistory(View v, MotionEvent event, int historyItem) {
        view = v;
        eventType = EVENT_TYPE_TOUCH;
        action = ACTION_TOUCH_MOVE;
        time = event.getHistoricalEventTime(historyItem);
        x = (int) event.getHistoricalX(historyItem);
        y = (int) event.getHistoricalY(historyItem);
    }

    public void returnToPool() {
        pool.add(this);
    }
}
