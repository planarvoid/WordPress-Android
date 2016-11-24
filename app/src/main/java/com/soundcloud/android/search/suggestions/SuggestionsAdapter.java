package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class SuggestionsAdapter extends RecyclerItemAdapter<SuggestionItem, RecyclerView.ViewHolder> {
    @Inject
    SuggestionsAdapter(AutocompletionItemRenderer autocompletionItemRenderer,
                       SearchSuggestionItemRenderer searchItemRenderer,
                       TrackSuggestionItemRenderer trackItemRenderer,
                       UserSuggestionItemRenderer userItemRenderer,
                       PlaylistSuggestionItemRenderer playlistItemRenderer) {
        super(new CellRendererBinding<>(SuggestionItem.Kind.AutocompletionItem.ordinal(), autocompletionItemRenderer),
              new CellRendererBinding<>(SuggestionItem.Kind.SearchItem.ordinal(), searchItemRenderer),
              new CellRendererBinding<>(SuggestionItem.Kind.TrackItem.ordinal(), trackItemRenderer),
              new CellRendererBinding<>(SuggestionItem.Kind.UserItem.ordinal(), userItemRenderer),
              new CellRendererBinding<>(SuggestionItem.Kind.PlaylistItem.ordinal(), playlistItemRenderer));
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).kind().ordinal();
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
