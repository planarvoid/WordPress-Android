package com.soundcloud.android.view.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.AsyncLoadingState;

import java.util.List;

@AutoValue
public abstract class CollectionRendererState<ItemT> {

    public abstract AsyncLoadingState collectionLoadingState();

    public abstract List<ItemT> items();

    public static <ItemT> CollectionRendererState<ItemT> create(AsyncLoadingState loadingState, List<ItemT> items) {
        return new AutoValue_CollectionRendererState<>(loadingState, items);
    }
}
