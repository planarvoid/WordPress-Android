package com.soundcloud.android.stream;

import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.Pager;
import static rx.android.OperatorPaged.pagedWith;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.PropertySet;
import rx.Observable;
import rx.android.OperatorPaged;

import javax.inject.Inject;
import java.util.List;

class SoundStreamOperations {

    private static final int DEFAULT_LIMIT = Consts.LIST_PAGE_SIZE;

    private final SoundStreamStorage soundStreamStorage;

    @Inject
    SoundStreamOperations(SoundStreamStorage soundStreamStorage) {
        this.soundStreamStorage = soundStreamStorage;
    }

    public Observable<Page<List<PropertySet>>> getStreamItems() {
        final Urn currentUserUrn = Urn.forUser(123); // TODO
        return loadPagedStreamItems(currentUserUrn, 0);
    }

    private Observable<Page<List<PropertySet>>> loadPagedStreamItems(Urn userUrn, int offset) {
        Observable<PropertySet> source = soundStreamStorage.loadStreamItemsAsync(userUrn, DEFAULT_LIMIT, offset);
        return source.toList().lift(pagedWith(streamItemPager(userUrn, offset)));
    }

    private Pager<List<PropertySet>> streamItemPager(final Urn userUrn, final int offset) {
        return new Pager<List<PropertySet>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(List<PropertySet> propertySet) {
                if (propertySet.size() == DEFAULT_LIMIT) {
                    return loadPagedStreamItems(userUrn, offset + DEFAULT_LIMIT);
                } else {
                    return OperatorPaged.emptyPageObservable();
                }
            }
        };
    }

}
