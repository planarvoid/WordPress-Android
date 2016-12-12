package com.soundcloud.android.utils;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import rx.Observable;
import rx.functions.Func2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RepoUtils {

    public static <I extends UrnHolder, E, O> Observable<List<O>> enrich(final List<I> sourceItems,
                                                                         Observable<Map<Urn, E>> entities,
                                                                         Func2<E, I, O> combiner) {
        if (sourceItems.isEmpty()) {
            return Observable.just(Collections.emptyList());
        } else {
            return entities.map(urnEntityMap -> {
                List<O> combined = new ArrayList<>(sourceItems.size());
                for (I sourceItem : sourceItems) {
                    if (urnEntityMap.containsKey(sourceItem.urn())) {
                        combined.add(combiner.call(urnEntityMap.get(sourceItem.urn()), sourceItem));
                    }
                }
                return combined;
            });
        }
    }
}
