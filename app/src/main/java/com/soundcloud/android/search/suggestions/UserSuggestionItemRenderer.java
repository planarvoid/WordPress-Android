package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;

import android.view.View;

import javax.inject.Inject;
import java.util.List;

class UserSuggestionItemRenderer extends SuggestionItemRenderer {

    @Inject
    UserSuggestionItemRenderer(ImageOperations imageOperations) {
        super(imageOperations);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestionItem> items) {
        final SearchSuggestionItem userSuggestionItem = (SearchSuggestionItem) items.get(position);
        bindView(itemView, userSuggestionItem, R.drawable.ic_search_user);
    }

    protected void loadIcon(View itemView, ImageResource imageResource) {
        imageOperations.displayCircularInAdapterView(imageResource, ApiImageSize.getListItemImageSize(itemView.getResources()), icon);
    }
}
