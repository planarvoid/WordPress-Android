package com.soundcloud.android.view.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.CollectionLoadingState;

import java.util.List;

@AutoValue
public abstract class CollectionRendererState<ItemT> {

    public abstract CollectionLoadingState collectionLoadingState();

    public abstract List<ItemT> items();

    public static <ItemT> CollectionRendererState<ItemT> create(CollectionLoadingState loadingState, List<ItemT> items) {
        return new AutoValue_CollectionRendererState<>(loadingState, items);
    }
}
