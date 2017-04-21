package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import rx.subjects.PublishSubject;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory
class TopResultsAdapter extends RecyclerItemAdapter<TopResultsBucketViewModel, RecyclerView.ViewHolder> {

    TopResultsAdapter(@Provided BucketRendererFactory bucketRendererFactory,
                      PublishSubject<SearchItem.Track> trackClick,
                      PublishSubject<SearchItem.Playlist> playlistClick,
                      PublishSubject<SearchItem.User> userClick,
                      PublishSubject<TopResults.Bucket.Kind> viewAllClick,
                      PublishSubject<Void> helpClick) {
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
