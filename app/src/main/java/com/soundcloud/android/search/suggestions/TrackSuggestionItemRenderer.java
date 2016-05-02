package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;

import android.view.View;

import javax.inject.Inject;
import java.util.List;

class TrackSuggestionItemRenderer extends SuggestionItemRenderer {

    @Inject
    TrackSuggestionItemRenderer(ImageOperations imageOperations) {
        super(imageOperations);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestionItem> items) {
        final SearchSuggestionItem trackSuggestionItem = (SearchSuggestionItem) items.get(position);
        bindView(itemView, trackSuggestionItem, R.drawable.ic_search_sound);
    }

    protected void loadIcon(View itemView, ImageResource imageResource) {
        imageOperations.displayInAdapterView(imageResource, ApiImageSize.getListItemImageSize(itemView.getResources()), icon);
    }
}
