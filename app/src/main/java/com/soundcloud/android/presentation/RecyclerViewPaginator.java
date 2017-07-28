package com.soundcloud.android.presentation;

import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;

public final class RecyclerViewPaginator {
    
    private final @NonNull RecyclerView recyclerView;
    private final @NonNull Action nextPage;
    private Disposable disposable;

    public RecyclerViewPaginator(final @NonNull RecyclerView recyclerView, final @NonNull Action nextPage) {
        this.recyclerView = recyclerView;
        this.nextPage = nextPage;
        start();
    }

    public void start() {
        stop();

        disposable = RxRecyclerView.scrollEvents(recyclerView)
                                   .map(__ -> recyclerView.getLayoutManager())
                                   .ofType(LinearLayoutManager.class)
                                   .map(this::displayedItemFromLinearLayout)
                                   .filter(item -> item.second != 0)
                                   .filter(this::visibleItemIsCloseToBottom)
                                   .distinctUntilChanged()
                                   .subscribe(integerIntegerPair -> nextPage.run());
    }

    public void stop() {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
    }

    private Pair<Integer, Integer> displayedItemFromLinearLayout(final @NonNull LinearLayoutManager manager) {
        return new Pair<>(manager.findLastVisibleItemPosition(), manager.getItemCount());
    }

    private boolean visibleItemIsCloseToBottom(final @NonNull Pair<Integer, Integer> visibleItemOfTotal) {
        return visibleItemOfTotal.first == visibleItemOfTotal.second - 1;
    }
}
