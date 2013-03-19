package com.soundcloud.android.rx.event;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.Subject;
import rx.util.functions.Action1;

public enum Events {

    LIKE_CHANGED,
    REPOST_CHANGED;

    private final Subject<Object> event = Subject.create();

    public void fire() {
        event.onNext(null);
    }

    public void fire(Object data) {
        event.onNext(data);
    }

    public <T> Subscription subscribe(final Observable<T> observable, final Observer<T> observer) {
        return event.subscribe(new Action1<Object>() {
            @Override
            public void call(Object data) {
                observable.subscribe(observer);
            }
        });
    }

    public <T> Subscription subscribe(final Action1<T> action) {
        return event.subscribe(new Action1<T>() {
            @Override
            public void call(T data) {
                action.call(data);
            }
        });
    }

}
