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

    public static <Entity extends UrnHolder, Properties, Aggregate> Observable<List<Aggregate>> enrich(final List<Entity> sourceItems,
                                                                                                       Observable<Map<Urn, Properties>> entities,
                                                                                                       Func2<Properties, Entity, Aggregate> combiner) {
        if (sourceItems.isEmpty()) {
            return Observable.just(Collections.emptyList());
        } else {
            return entities.map(urnEntityMap -> {
                final List<Aggregate> combined = new ArrayList<>(sourceItems.size());
                for (Entity sourceItem : sourceItems) {
                    if (urnEntityMap.containsKey(sourceItem.urn())) {
                        combined.add(combiner.call(urnEntityMap.get(sourceItem.urn()), sourceItem));
                    }
                }
                return combined;
            });
        }
    }

}
