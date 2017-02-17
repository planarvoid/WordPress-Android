package com.soundcloud.android.rx;

import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.DefaultEventBus;
import com.soundcloud.rx.eventbus.Queue;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.Subject;

public class LoggingDefaultEventBus extends DefaultEventBus {

    private static final String TAG = "EventBus";
    private static final int MAX_LENGTH = 100;

    public LoggingDefaultEventBus(Scheduler defaultScheduler) {
        super(defaultScheduler);
    }

    @Override
    public <E> Subject<E, E> queue(Queue<E> queue) {
        Log.d(TAG, "queue(" + queue + ")");
        return super.queue(queue);
    }

    @Override
    public <E> Subscription subscribe(Queue<E> queue, Observer<E> observer) {
        Log.d(TAG, "subscribe(" + queue + ")");
        return super.subscribe(queue, observer);
    }

    @Override
    public <E> Subscription subscribeImmediate(Queue<E> queue, Observer<E> observer) {
        Log.d(TAG, "subscribeImmediate(" + queue + ")");
        return super.subscribeImmediate(queue, observer);
    }

    @Override
    public <E> void publish(Queue<E> queue, E event) {
        Log.d(TAG, "publish(" + queue + ", " + limit(event.toString()) + ")");
        super.publish(queue, event);
    }

    @Override
    public <E> Action0 publishAction0(Queue<E> queue, E event) {
        Log.d(TAG, "publishAction0(" + queue + ", " + limit(event.toString()) + ")");
        return super.publishAction0(queue, event);
    }

    @Override
    public <E, T> Action1<T> publishAction1(Queue<E> queue, E event) {
        Log.d(TAG, "publishAction1(" + queue + ", " + limit(event.toString()) + ")");
        return super.publishAction1(queue, event);
    }

    private static String limit(String message) {
        return message.length() <= MAX_LENGTH ? message : (message.substring(0, MAX_LENGTH) + "[...]");
    }

}
