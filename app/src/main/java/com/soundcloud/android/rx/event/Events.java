package com.soundcloud.android.rx.event;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.Subject;
import rx.util.functions.Action1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Events {

    public static final Subject<Observable<?>> LIKE_CHANGED = Subject.create();
    public static final Subject<Observable<?>> REPOST_CHANGED = Subject.create();

    public static void fire(Subject<?> event) {
        event.onNext(null);
    }

    public static <T> Subscription subscribe(final Subject<?> event, final Observable<T> observable, final Observer<T> observer) {
        return event.subscribe(new Action1<Void>() {
            @Override
            public void call(Void nil) {
                observable.subscribe(observer);
            }
        });
    }

}
