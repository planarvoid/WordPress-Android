package com.soundcloud.android.rx;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.Consts;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import rx.Notification;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RxUtils {

    public static final Func1<Boolean, Boolean> IS_TRUE = isTrue -> isTrue;

    public static final Func1<Boolean, Boolean> IS_FALSE = isTrue -> !isTrue;

    public static final Func1<Object, Void> TO_VOID = ignore -> null;

    public static final Func2<Object, Object, Void> ZIP_TO_VOID = (ignored, ignored2) -> null;

    public static final Func1<Long, Boolean> IS_VALID_TIMESTAMP = ts -> ts != Consts.NOT_SET;

    public static final Func1<Optional, Boolean> IS_PRESENT = optional -> optional.isPresent();

    public static final Object EMPTY_VALUE = new Object();

    public static final Func1<List, Boolean> IS_NOT_EMPTY_LIST = list -> !list.isEmpty();

    public static final Func1<Object, Boolean> IS_NOT_NULL = obj -> obj != null;

    public static final Func1<List, Integer> TO_SIZE = list -> list.size();

    public static final Func2<Boolean, Boolean, Boolean> AT_LEAST_ONE_TRUE = (first, second) -> first || second;

    /**
     * @return A Subscription that is always unsubscribed. Can use as a Null object; reference equality
     * checks are safe to perform.
     */
    public static Subscription invalidSubscription() {
        return Subscriptions.unsubscribed();
    }

    public static <T> Func1<Iterable<T>, Observable<T>> iterableToObservable() {
        return items -> Observable.from(items);
    }

    public static <T> Func1<Object, T> returning(final T obj) {
        return o -> obj;
    }

    public static <T> Func1<Object, Observable<T>> continueWith(final Observable<T> continuation) {
        return o -> continuation;
    }

    public static <T> Func1<T, Optional<T>> toOptional() {
        return t -> Optional.of(t);
    }

    public static <T, P> Func1<Map<T, P>, List<P>> toOrderedList(final List<T> list,
                                                                 final Optional<P> defaultValue) {
        return map -> {
            List<P> result = new ArrayList<>(map.size());
            for (T key : list) {
                if (map.containsKey(key)) {
                    result.add(map.get(key));
                } else if (defaultValue.isPresent()) {
                    result.add(defaultValue.get());
                }
            }
            return result;
        };
    }

    private RxUtils() {
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
                sanitizedNotifications.add(Notification.<ItemT>createOnCompleted());
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
