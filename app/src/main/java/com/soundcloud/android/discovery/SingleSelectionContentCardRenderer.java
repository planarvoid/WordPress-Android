package com.soundcloud.android.discovery;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class SingleSelectionContentCardRenderer implements CellRenderer<DiscoveryCard.SingleContentSelectionCard> {
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final Navigator navigator;

    @Inject
    SingleSelectionContentCardRenderer(ImageOperations imageOperations, Resources resources, Navigator navigator) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.discovery_single_selection_card, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View view, List<DiscoveryCard.SingleContentSelectionCard> list) {
        final DiscoveryCard.SingleContentSelectionCard singleContentSelectionCard = list.get(position);
        bindText(view, R.id.single_card_title, singleContentSelectionCard.title());
        bindText(view, R.id.single_card_description, singleContentSelectionCard.description());
        bindSelectionItem(view, R.id.single_card_artwork, singleContentSelectionCard.selectionItem());
        bindSocialProof(view, singleContentSelectionCard);
        bindClickHandling(view, singleContentSelectionCard.selectionItem());
    }

    private void bindText(View parentView, int resource, Optional<String> value) {
        final TextView view = (TextView) parentView.findViewById(resource);
        if (value.isPresent()) {
            view.setVisibility(View.VISIBLE);
            view.setText(value.get());
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void bindSelectionItem(View parentView, int resource, SelectionItem selectionItem) {
        final ImageView view = (ImageView) parentView.findViewById(resource);
        imageOperations.displayInAdapterView(
                String.valueOf(selectionItem.hashCode()), selectionItem.artworkUrlTemplate(), ApiImageSize.getFullImageSize(resources), view);

        bindText(parentView, R.id.single_card_track_count, selectionItem.count().transform(String::valueOf));
    }

    private void bindSocialProof(View itemView, DiscoveryCard.SingleContentSelectionCard singleContentSelectionCard) {
        View container = itemView.findViewById(R.id.single_card_social_proof_container);
        boolean boundText = bindSocialProofText(container, singleContentSelectionCard);
        boolean boundAvatars = bindSocialProofAvatars(container, singleContentSelectionCard);
        int visibility = boundText || boundAvatars ? View.VISIBLE : View.GONE;
        container.setVisibility(visibility);
    }

    private boolean bindSocialProofText(View container, DiscoveryCard.SingleContentSelectionCard singleContentSelectionCard) {
        bindText(container, R.id.single_card_social_proof, singleContentSelectionCard.socialProof());
        return singleContentSelectionCard.socialProof().isPresent();
    }

    private boolean bindSocialProofAvatars(View container, DiscoveryCard.SingleContentSelectionCard singleContentSelectionCard) {
        List<String> imageUrls = singleContentSelectionCard.socialProofAvatarUrlTemplates().or(new ArrayList<>());
        bindSocialProofUserArtwork(container, R.id.single_card_user_artwork_1, imageUrls, 0);
        bindSocialProofUserArtwork(container, R.id.single_card_user_artwork_2, imageUrls, 1);
        bindSocialProofUserArtwork(container, R.id.single_card_user_artwork_3, imageUrls, 2);
        bindSocialProofUserArtwork(container, R.id.single_card_user_artwork_4, imageUrls, 3);
        bindSocialProofUserArtwork(container, R.id.single_card_user_artwork_5, imageUrls, 4);
        return singleContentSelectionCard.socialProofAvatarUrlTemplates().isPresent();
    }

    private void bindSocialProofUserArtwork(View itemView, int resId, List<String> userArtworkUrls, int position) {
        ImageView imageView = (ImageView) itemView.findViewById(resId);

        if (userArtworkUrls.size() > position) {
            imageView.setVisibility(View.VISIBLE);
            imageOperations.displayCircularWithPlaceholder(String.valueOf(userArtworkUrls.get(position).hashCode()),
                                                           Optional.of(userArtworkUrls.get(position)),
                                                           ApiImageSize.getListItemImageSize(resources),
                                                           imageView);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    private void bindClickHandling(View view, final SelectionItem selectionItem) {
        view.setOnClickListener(selectionItem.onClickListener(navigator));
    }
}