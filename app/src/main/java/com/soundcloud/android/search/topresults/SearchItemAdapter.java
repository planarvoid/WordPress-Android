package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import io.reactivex.subjects.PublishSubject;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

@AutoFactory
class SearchItemAdapter extends PagingRecyclerItemAdapter<SearchItem, RecyclerView.ViewHolder> {

    SearchItemAdapter(@Provided SearchTrackRendererFactory searchTrackRendererFactory,
                      @Provided SearchPlaylistRendererFactory searchPlaylistRendererFactory,
                      @Provided SearchUserRendererFactory searchUserRendererFactory,
                      PublishSubject<SearchItem.Track> trackClick,
                      PublishSubject<SearchItem.Playlist> playlistClick,
                      PublishSubject<SearchItem.User> userClick) {
        super(new CellRendererBinding<>(SearchItem.Kind.TRACK.ordinal(), searchTrackRendererFactory.create(trackClick)),
              new CellRendererBinding<>(SearchItem.Kind.PLAYLIST.ordinal(), searchPlaylistRendererFactory.create(playlistClick)),
              new CellRendererBinding<>(SearchItem.Kind.USER.ordinal(), searchUserRendererFactory.create(userClick)));
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
