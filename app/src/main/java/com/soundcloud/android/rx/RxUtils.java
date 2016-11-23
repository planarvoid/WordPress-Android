package com.soundcloud.android.rx;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.Consts;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import rx.Notification;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

import java.util.List;

public final class RxUtils {

    public static final Func1<Boolean, Boolean> IS_TRUE = new Func1<Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean isTrue) {
            return isTrue;
        }
    };

    public static final Func1<Boolean, Boolean> IS_FALSE = new Func1<Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean isTrue) {
            return !isTrue;
        }
    };

    public static final Func1<Object, Void> TO_VOID = new Func1<Object, Void>() {
        @Override
        public Void call(Object ignore) {
            return null;
        }
    };

    public static final Func2<Object, Object, Void> ZIP_TO_VOID = new Func2<Object, Object, Void>() {
        @Override
        public Void call(Object ignored,
                         Object ignored2) {
            return null;
        }
    };

    public static final Func1<Long, Boolean> IS_VALID_TIMESTAMP = new Func1<Long, Boolean>() {
        @Override
        public Boolean call(Long ts) {
            return ts != Consts.NOT_SET;
        }
    };

    public static final Func1<Optional, Boolean> IS_PRESENT = new Func1<Optional, Boolean>() {
        @Override
        public Boolean call(Optional optional) {
            return optional.isPresent();
        }
    };

    public static final Object EMPTY_VALUE = new Object();

    public static final Func1<List, Boolean> IS_NOT_EMPTY_LIST = new Func1<List, Boolean>() {
        @Override
        public Boolean call(List list) {
            return !list.isEmpty();
        }
    };

    public static final Func1<Object, Boolean> IS_NOT_NULL = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object obj) {
            return obj != null;
        }
    };

    public static final Func1<List, Integer> TO_SIZE = new Func1<List, Integer>() {
        public Integer call(List list) {
            return list.size();
        }
    };

    public static final Func2<Boolean, Boolean, Boolean> AT_LEAST_ONE_TRUE = new Func2<Boolean, Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean first, Boolean second) {
            return first || second;
        }
    };

    /**
     * @return A Subscription that is always unsubscribed. Can use as a Null object; reference equality
     * checks are safe to perform.
     */
    public static Subscription invalidSubscription() {
        return Subscriptions.unsubscribed();
    }

    public static <T> Func1<Iterable<T>, Observable<T>> iterableToObservable() {
        return new Func1<Iterable<T>, Observable<T>>() {
            @Override
            public Observable<T> call(Iterable<T> items) {
                return Observable.from(items);
            }
        };
    }

    public static <T> Func1<Object, T> returning(final T obj) {
        return new Func1<Object, T>() {
            @Override
            public T call(Object o) {
                return obj;
            }
        };
    }

    public static <T> Func1<Object, Observable<T>> continueWith(final Observable<T> continuation) {
        return new Func1<Object, Observable<T>>() {
            @Override
            public Observable<T> call(Object o) {
                return continuation;
            }
        };
    }

    public static <T> Func1<T, Optional<T>> toOptional() {
        return new Func1<T, Optional<T>>() {
            @Override
            public Optional<T> call(T t) {
                return Optional.of(t);
            }
        };
    }

    private RxUtils() {
    }

    public static <ItemT> Observable.Transformer<List<Observable<ItemT>>, ItemT> concatEagerIgnorePartialErrors() {
        return new ConcatEagerIgnorePartialErrors<>();
    }

    private static class ConcatEagerIgnorePartialErrors<ItemT>
            implements Observable.Transformer<List<Observable<ItemT>>, ItemT> {

        private final Function<Observable<ItemT>, Observable<Notification<ItemT>>> toMaterializedObservable = new Function<Observable<ItemT>, Observable<Notification<ItemT>>>() {
            @Override
            public Observable<Notification<ItemT>> apply(Observable<ItemT> input) {
                return input.materialize();
            }
        };

        private final Func1<List<Notification<ItemT>>, Observable<Notification<ItemT>>> sanitizeNotifications = new Func1<List<Notification<ItemT>>, Observable<Notification<ItemT>>>() {
            @Override
            public Observable<Notification<ItemT>> call(List<Notification<ItemT>> notifications) {
                if (containsCompleted(notifications)) {
                    final List<Notification<ItemT>> sanitizedNotifications = removeTerminalNotifications(notifications);
                    sanitizedNotifications.add(Notification.<ItemT>createOnCompleted());
                    return Observable.from(sanitizedNotifications);
                } else {
                    return Observable.from(notifications);
                }
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
            return newArrayList(Iterables.filter(notifications1, new Predicate<Notification<ItemT>>() {
                @Override
                public boolean apply(Notification<ItemT> notification) {
                    return notification.getKind() == Notification.Kind.OnNext;
                }
            }));
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
