package com.soundcloud.android.api.model;

import com.soundcloud.android.commands.Command;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;

import java.util.Iterator;

public abstract class PagedCollection<T> implements Iterable<T> {
    private final ModelCollection<T> items;

    protected PagedCollection(ModelCollection<T> items) {
        this.items = items;
    }

    public static <T extends PagedCollection> Pager.PagingFunction<T> pagingFunction(
            final Command<String, Observable<T>> nextPage,
            final Scheduler scheduler) {
        return new Pager.PagingFunction<T>() {
            @Override
            public Observable<T> call(T collection) {
                final Optional<Link> nextLink = collection.nextLink();
                if (nextLink.isPresent()) {
                    return nextPage.call(nextLink.get().getHref()).subscribeOn(scheduler);
                } else {
                    return Pager.finish();
                }
            }
        };
    }

    public Optional<Link> nextLink() {
        return items.getNextLink();
    }

    public Optional<String> nextPageLink(){
        return nextLink().transform(new Function<Link, String>() {
            public String apply(Link input) {
                return input.getHref();
            }
        });
    }

    public ModelCollection<T> items() {
        return items;
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    public boolean lastPage() {
        return !nextLink().isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PagedCollection<?> that = (PagedCollection<?>) o;

        if (items != null ? !items.equals(that.items) : that.items != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return items != null ? items.hashCode() : 0;
    }
}
