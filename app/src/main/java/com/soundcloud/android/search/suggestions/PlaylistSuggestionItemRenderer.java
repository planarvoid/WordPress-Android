package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

class PlaylistSuggestionItemRenderer extends SuggestionItemRenderer {

    @Inject
    PlaylistSuggestionItemRenderer(ImageOperations imageOperations) {
        super(imageOperations);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestionItem> items) {
        final SearchSuggestionItem playlistSuggestionItem = (SearchSuggestionItem) items.get(position);
        bindView(itemView, playlistSuggestionItem, R.drawable.ic_search_playlist);
    }

    protected void loadIcon(ImageView icon, ImageResource imageResource, Resources resources) {
        imageOperations.displayInAdapterView(imageResource,
                                             ApiImageSize.getListItemImageSize(resources),
                                             icon);
    }
}
