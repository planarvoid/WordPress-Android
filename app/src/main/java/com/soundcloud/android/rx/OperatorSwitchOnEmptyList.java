package com.soundcloud.android.rx;

import rx.Observable;
import rx.Subscriber;

import java.util.List;

// TODO: When we bump the RxJava lib, we can use a combination of filter and OperatorSwitchIfEmpty (experimental at the time of this writing) instead
public class OperatorSwitchOnEmptyList<T> implements Observable.Operator<List<T>, List<T>> {
    private final Observable<List<T>> onEmptyResults;

    public OperatorSwitchOnEmptyList(Observable<List<T>> onEmptyResults) {
        this.onEmptyResults = onEmptyResults;
    }

    @Override
    public Subscriber<? super List<T>> call(final Subscriber<? super List<T>> child) {
        return new SwitchOnEmptyListSubscriber(child);
    }

    private class SwitchOnEmptyListSubscriber extends Subscriber<List<T>> {

        private final Subscriber<? super List<T>> child;
        private boolean isOriginalSource = true;

        public SwitchOnEmptyListSubscriber(Subscriber<? super List<T>> child) {
            super(child);
            this.child = child;
        }

        @Override
        public void onCompleted() {
            if (isOriginalSource) {
                child.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (isOriginalSource) {
                child.onError(e);
            }
        }

        @Override
        public void onNext(List<T> results) {
            if (isOriginalSource) {
                if (results != null && results.isEmpty()) {
                    add(onEmptyResults.subscribe(child));
                    isOriginalSource = false;
                } else {
                    child.onNext(results);
                }
            }
        }
    }
}
