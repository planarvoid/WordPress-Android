package com.soundcloud.android.rx;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.Consts;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import rx.Notification;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

import java.util.List;

public final class RxUtils {

    public static final Func1<Boolean, Boolean> IS_TRUE = isTrue -> isTrue;

    public static final Func1<Boolean, Boolean> IS_FALSE = isTrue -> !isTrue;

    public static final Func1<Object, Void> TO_VOID = ignore -> null;

    public static final Func2<Object, Object, Void> ZIP_TO_VOID = (ignored, ignored2) -> null;

    public static final Func1<Long, Boolean> IS_VALID_TIMESTAMP = ts -> ts != Consts.NOT_SET;

    public static final Object EMPTY_VALUE = new Object();

    public static final Func1<List, Boolean> IS_NOT_EMPTY_LIST = list -> !list.isEmpty();

    public static final Func1<Object, Boolean> IS_NOT_NULL = obj -> obj != null;

    private RxUtils() {
    }

    /**
     * @return A Subscription that is always unsubscribed. Can use as a Null object; reference equality
     * checks are safe to perform.
     */
    public static Subscription invalidSubscription() {
        return Subscriptions.unsubscribed();
    }

    public static <ItemT> Observable.Transformer<List<Observable<ItemT>>, ItemT> concatEagerIgnorePartialErrors() {
        return new ConcatEagerIgnorePartialErrors<>();
    }

    private static class ConcatEagerIgnorePartialErrors<ItemT>
            implements Observable.Transformer<List<Observable<ItemT>>, ItemT> {

        private final Function<Observable<ItemT>, Observable<Notification<ItemT>>> toMaterializedObservable = input -> input.materialize();

        private final Func1<List<Notification<ItemT>>, Observable<Notification<ItemT>>> sanitizeNotifications = notifications -> {
            if (containsCompleted(notifications)) {
                final List<Notification<ItemT>> sanitizedNotifications = removeTerminalNotifications(notifications);
                sanitizedNotifications.add(Notification.createOnCompleted());
                return Observable.from(sanitizedNotifications);
            } else {
                return Observable.from(notifications);
            }
        };

        @Override
        public Observable<ItemT> call(Observable<List<Observable<ItemT>>> listObservable) {
            return listObservable.flatMap(new Func1<List<Observable<ItemT>>, Observable<ItemT>>() {
                @Override
                public Observable<ItemT> call(List<Observable<ItemT>> observables) {
                    return Observable
                            .concatEager(Lists.transform(observables, toMaterializedObservable))
                            .toList()
                            .flatMap(sanitizeNotifications)
                            .dematerialize();
                }
            });
        }


        private List<Notification<ItemT>> removeTerminalNotifications(List<Notification<ItemT>> notifications1) {
            return newArrayList(Iterables.filter(notifications1, notification -> notification.getKind() == Notification.Kind.OnNext));
        }

        private boolean containsCompleted(List<Notification<ItemT>> notifications1) {
            for (Notification<ItemT> notification : notifications1) {
                if (notification.getKind() == Notification.Kind.OnCompleted) {
                    return true;
                }
            }
            return false;
        }
    }
}
