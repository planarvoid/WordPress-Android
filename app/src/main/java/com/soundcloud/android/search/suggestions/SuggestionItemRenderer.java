package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionHighlighter.findHighlight;
import static com.soundcloud.android.search.suggestions.SuggestionHighlighter.setHighlightSpans;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

abstract class SuggestionItemRenderer implements CellRenderer<SuggestionItem> {

    protected final ImageOperations imageOperations;

    protected SuggestionItemRenderer(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public abstract View createItemView(ViewGroup viewGroup);

    protected void bindView(View itemView,
                            SearchSuggestionItem suggestionItem,
                            int iconRes) {

        TextView titleText = ButterKnife.findById(itemView, R.id.title);
        Context context = itemView.getContext();
        titleText.setText(highlight(suggestionItem, context));
        ButterKnife.<ImageView>findById(itemView, R.id.iv_search_type).setImageResource(iconRes);
        ImageView icon = ButterKnife.findById(itemView, R.id.icon);
        loadIcon(icon, suggestionItem, context.getResources());
    }

    protected abstract void loadIcon(ImageView icon, ImageResource imageResource, Resources resources);

    private Spanned highlight(SearchSuggestionItem suggestionItem, Context context) {
        final SuggestionHighlight suggestionHighlight =
                suggestionItem.suggestionHighlight().or(findHighlight(
                        suggestionItem.userQuery(), suggestionItem.displayedText()));
        final SpannableString spanned = new SpannableString(suggestionItem.displayedText());
        setHighlightSpans(context, spanned, suggestionHighlight);
        return spanned;
    }
}
