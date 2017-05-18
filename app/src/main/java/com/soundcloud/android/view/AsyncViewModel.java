package com.soundcloud.android.view;

import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@Deprecated // use CollectionLoaderState
@AutoValue
public abstract class AsyncViewModel<ViewModelType> {

    public static <T> AsyncViewModel<T> create(Optional<T> optData, boolean isLoadingNextPage, boolean isRefreshing, Optional<ViewError> viewErrorOptional) {
        return new AutoValue_AsyncViewModel<>(optData, isLoadingNextPage, isRefreshing, viewErrorOptional);
    }

    public abstract Optional<ViewModelType> data();

    public abstract boolean isLoadingNextPage();

    public abstract boolean isRefreshing();

    public abstract Optional<ViewError> error();

    public AsyncViewModel<ViewModelType> withNewData(ViewModelType data){
        return AsyncViewModel.create(of(data), isLoadingNextPage(), isRefreshing(), error());
    }

}
