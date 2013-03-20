package com.soundcloud.android.rx.event;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.Subject;
import rx.util.functions.Action1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Event {

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

    public static SubscriptionBuilder anyOf(Event... events) {
        return new SubscriptionBuilder(Arrays.asList(events));
    }

    public static class SubscriptionBuilder {
        private List<Event> mEvents;
        private List<Subscription> mSubscriptions;

        private Subscription mCollectiveSubscription = new Subscription() {
            @Override
            public void unsubscribe() {
                for (Subscription s : mSubscriptions) {
                    s.unsubscribe();
                }
            }
        };

        public SubscriptionBuilder(List<Event> events) {
            mEvents = events;
            mSubscriptions = new ArrayList<Subscription>(mEvents.size());
        }

        public <T> Subscription subscribe(final Observable<T> observable, final Observer<T> observer) {
            for (Event e : mEvents) {
                mSubscriptions.add(e.subscribe(observable, observer));
            }
            return mCollectiveSubscription;
        }

        public <T> Subscription subscribe(final Action1<T> action) {
            for (Event e : mEvents) {
                mSubscriptions.add(e.subscribe(action));
            }
            return mCollectiveSubscription;
        }
    }
}
