package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.ViewTypes;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class SuggestionsAdapter extends RecyclerItemAdapter<SuggestionItem, RecyclerView.ViewHolder> {

    final static int TYPE_SEARCH = ViewTypes.DEFAULT_VIEW_TYPE;
    final static int TYPE_TRACK = ViewTypes.DEFAULT_VIEW_TYPE + 1;
    final static int TYPE_USER = ViewTypes.DEFAULT_VIEW_TYPE + 2;
    final static int TYPE_PLAYLIST = ViewTypes.DEFAULT_VIEW_TYPE + 3;

    @Inject
    SuggestionsAdapter(SearchSuggestionItemRenderer searchItemRenderer,
                       TrackSuggestionItemRenderer trackItemRenderer,
                       UserSuggestionItemRenderer userItemRenderer,
                       PlaylistSuggestionItemRenderer playlistItemRenderer) {
        super(new CellRendererBinding<>(TYPE_SEARCH, searchItemRenderer),
              new CellRendererBinding<>(TYPE_TRACK, trackItemRenderer),
              new CellRendererBinding<>(TYPE_USER, userItemRenderer),
              new CellRendererBinding<>(TYPE_PLAYLIST, playlistItemRenderer)
        );
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        switch (getItem(position).kind()) {
            case SearchItem:
                return TYPE_SEARCH;
            case TrackItem:
                return TYPE_TRACK;
            case UserItem:
                return TYPE_USER;
            case PlaylistItem:
                return TYPE_PLAYLIST;
            default:
                throw new IllegalArgumentException("Unhandled suggestion item kind " + getItem(position).kind());
        }
    }

    @Override
    public void onNext(Iterable<SuggestionItem> items) {
        //We need to clear the adapter at this point because we
        //want to keep the existing items in it (search suggestions)
        //until the last moment, in order to replace them exactly
        //when they arrive (so we avoid blinking effect).
        //It is a bit hacky solution but this is gonna be removed when
        //we swap from serving content to serving search queries.
        clear();
        super.onNext(items);
    }
}
