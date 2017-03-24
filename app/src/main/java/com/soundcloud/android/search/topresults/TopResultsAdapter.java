package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import rx.subjects.PublishSubject;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

@AutoFactory
class TopResultsAdapter extends RecyclerItemAdapter<TopResultsBucketViewModel, RecyclerView.ViewHolder> {

    @Inject
    TopResultsAdapter(PublishSubject<SearchItem> searchItemClicked,
                      PublishSubject<TopResultsViewAllArgs> viewAllClicked,
                      PublishSubject<Void> helpClicked,
                      @Provided BucketRendererFactory bucketRendererFactory) {
        super(bucketRendererFactory.create(searchItemClicked, viewAllClicked, helpClicked));
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return 0;
    }
}
