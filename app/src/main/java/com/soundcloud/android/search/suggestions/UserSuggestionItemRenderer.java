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

class UserSuggestionItemRenderer extends SuggestionItemRenderer {

    @Inject
    UserSuggestionItemRenderer(ImageOperations imageOperations) {
        super(imageOperations);
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_suggestion_user, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestionItem> items) {
        final SearchSuggestionItem userSuggestionItem = (SearchSuggestionItem) items.get(position);
        bindView(itemView, userSuggestionItem, R.drawable.ic_search_user);
        showProBadge(itemView.findViewById(R.id.pro_badge), userSuggestionItem);
    }

    private void showProBadge(View proBadge, SearchSuggestionItem userSuggestionItem) {
        if (userSuggestionItem.isPro()) {
            proBadge.setVisibility(View.VISIBLE);
        }
    }

    protected void loadIcon(ImageView icon, ImageResource imageResource, Resources resources) {
        imageOperations.displayCircularInAdapterView(imageResource,
                                                     ApiImageSize.getListItemImageSize(resources),
                                                     icon);
    }
}
