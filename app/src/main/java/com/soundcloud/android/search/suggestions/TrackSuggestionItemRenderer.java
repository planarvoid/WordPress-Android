package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

class TrackSuggestionItemRenderer extends SuggestionItemRenderer {

    @Inject
    TrackSuggestionItemRenderer(ImageOperations imageOperations) {
        super(imageOperations);
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_suggestion_default, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestionItem> items) {
        final SearchSuggestionItem trackSuggestionItem = (SearchSuggestionItem) items.get(position);
        bindView(itemView, trackSuggestionItem, R.drawable.ic_search_sound);
    }

    protected void loadIcon(ImageView icon, ImageResource imageResource, Resources resources) {
        imageOperations.displayInAdapterView(imageResource.getUrn(),
                                             imageResource.getImageUrlTemplate(),
                                             ApiImageSize.getListItemImageSize(resources),
                                             icon,
                                             ImageOperations.DisplayType.DEFAULT);
    }
}
