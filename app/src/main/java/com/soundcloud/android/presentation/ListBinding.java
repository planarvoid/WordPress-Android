package com.soundcloud.android.presentation;

import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ItemAdapter;
import rx.Observable;
import rx.Observer;
import rx.android.Pager;
import rx.functions.Func1;

import java.util.List;

public class ListBinding<DataT, ViewT> extends DataBinding<List<DataT>, List<ViewT>> {

    private final ItemAdapter<ViewT> adapter;
    private Pager<List<DataT>> pager;

    ListBinding(Observable<List<DataT>> source, ItemAdapter<ViewT> adapter, Func1<List<DataT>, List<ViewT>> itemTransformer) {
        super(source, itemTransformer);
        this.adapter = adapter;
    }

    ListBinding(Observable<List<DataT>> source, EndlessAdapter<ViewT> adapter, Pager<List<DataT>> pager,
                Func1<List<DataT>, List<ViewT>> itemTransformer) {
        this(pager.page(source), adapter, itemTransformer);
        this.pager = pager;
    }

    @Override
    public ListBinding<DataT, ViewT> addViewObserver(Observer<? super List<ViewT>> observer) {
        return (ListBinding<DataT, ViewT>) super.addViewObserver(observer);
    }

    public ItemAdapter<ViewT> getAdapter() {
        return adapter;
    }

    public Pager<List<DataT>> getPager() {
        return pager;
    }

    void setPager(Pager<List<DataT>> pager) {
        this.pager = pager;
    }

    boolean isPaged() {
        return pager != null;
    }
}
