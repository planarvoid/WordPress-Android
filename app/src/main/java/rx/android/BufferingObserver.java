package rx.android;

import rx.Observer;

import java.util.ArrayList;
import java.util.List;

/**
 * A buffering observer caches all items received via onNext until onCompleted is called,
 * then emits all cached items on the wrapped observer and calls onCompleted on it too.
 * This behavior is useful if you want to know whether a stream of items could be
 * received successfully in its entirety before actually emitting these items to the
 * final receiver.
 *
 * @param <T>
 */
public class BufferingObserver<T> implements Observer<T> {

    private final Observer<T> wrapped;
    private final List<T> items;

    public BufferingObserver(Observer<T> wrapped) {
        this.wrapped = wrapped;
        this.items = new ArrayList<T>();
    }

    public BufferingObserver(Observer<T> wrapped, int expectedItemCount) {
        this.wrapped = wrapped;
        this.items = new ArrayList<T>(expectedItemCount);
    }

    @Override
    public void onCompleted() {
        for (T item : items) {
            wrapped.onNext(item);
        }
        wrapped.onCompleted();
    }

    @Override
    public void onError(Exception e) {
        wrapped.onError(e);
    }

    @Override
    public void onNext(T item) {
        items.add(item);
    }
}
