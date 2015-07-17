package com.soundcloud.android.recommendations;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.view.EmptyView;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class RecommendationsView {

    @InjectView(R.id.ak_recycler_view) RecyclerView recyclerView;
    @InjectView(android.R.id.empty) EmptyView emptyView;

    private LinearLayoutManager linearLayoutManager;

    @Inject
    public RecommendationsView() {
    }

    public void bindViews(Context context, View viewRoot) {
        ButterKnife.inject(this, viewRoot);
        setupRecyclerView(context);
    }

    public void unbindViews() {
        ButterKnife.reset(this);
    }

    private void setupRecyclerView(Context context) {
        if(this.recyclerView == null) {
            throw new IllegalStateException("Expected to find RecyclerView with ID R.id.recycler_view");
        } else {
            this.linearLayoutManager = new LinearLayoutManager(context);
            this.recyclerView.setLayoutManager(this.linearLayoutManager);
        }
    }
}
