package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import rx.subjects.PublishSubject;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

@AutoFactory
class SearchItemAdapter extends PagingRecyclerItemAdapter<SearchItem, RecyclerView.ViewHolder> {

    @Inject
    SearchItemAdapter(@Provided SearchTrackRendererFactory searchTrackRendererFactory,
                      @Provided SearchPlaylistRendererFactory searchPlaylistRendererFactory,
                      @Provided SearchUserRendererFactory searchUserRendererFactory,
                      PublishSubject<SearchItem> searchItemClicked) {
        super(new CellRendererBinding<>(SearchItem.Kind.TRACK.ordinal(), searchTrackRendererFactory.create(searchItemClicked)),
              new CellRendererBinding<>(SearchItem.Kind.PLAYLIST.ordinal(), searchPlaylistRendererFactory.create(searchItemClicked)),
              new CellRendererBinding<>(SearchItem.Kind.USER.ordinal(), searchUserRendererFactory.create(searchItemClicked)));
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).kind().ordinal();
    }

    public void setItems(List<SearchItem> searchItems) {
        clear();
        onNext(searchItems);
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

}
