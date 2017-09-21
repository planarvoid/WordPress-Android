package com.soundcloud.android.utils

import com.soundcloud.android.model.Urn
import com.soundcloud.android.model.UrnHolder
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.util.ArrayList

fun <Entity : UrnHolder, Properties, Aggregate> enrichItemsWithProperties(sourceItems: List<Entity>,
                                                                          entities: Single<Map<Urn, Properties>>,
                                                                          combiner: BiFunction<Properties, Entity, Aggregate>): Single<List<Aggregate>> {
    return if (sourceItems.isEmpty()) {
        Single.just(emptyList())
    } else {
        entities
                .map { urnEntityMap ->
                    val combined = ArrayList<Aggregate>(sourceItems.size)
                    sourceItems
                            .filter { urnEntityMap.containsKey(it.urn()) }
                            .map { sourceItem -> combined.add(combiner.apply(urnEntityMap.getValue(sourceItem.urn()), sourceItem)) }
                    combined
                }
    }
}
