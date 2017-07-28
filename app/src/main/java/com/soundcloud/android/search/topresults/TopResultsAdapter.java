package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import io.reactivex.subjects.PublishSubject;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory
class TopResultsAdapter extends PagingRecyclerItemAdapter<TopResultsBucketViewModel, RecyclerView.ViewHolder> {

    TopResultsAdapter(@Provided BucketRendererFactory bucketRendererFactory,
                      PublishSubject<UiAction.TrackClick> trackClick,
                      PublishSubject<UiAction.PlaylistClick> playlistClick,
                      PublishSubject<UiAction.UserClick> userClick,
                      PublishSubject<UiAction.ViewAllClick> viewAllClick,
                      PublishSubject<UiAction.HelpClick> helpClick) {
        super(bucketRendererFactory.create(trackClick, playlistClick, userClick, viewAllClick, helpClick));
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
