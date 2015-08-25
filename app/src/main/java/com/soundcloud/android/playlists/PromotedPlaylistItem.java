package com.soundcloud.android.playlists;

import static com.soundcloud.android.stream.StreamItem.Kind.PROMOTED;

import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.List;

public class PromotedPlaylistItem extends PlaylistItem implements PromotedListItem {

    public static PromotedPlaylistItem from(PropertySet source) {
        return new PromotedPlaylistItem(source);
    }

    PromotedPlaylistItem(PropertySet source) {
        super(source);
    }

    @Override
    public String getAdUrn() {
        return source.get(PromotedItemProperty.AD_URN);
    }

    @Override
    public boolean hasPromoter() {
        return source.get(PromotedItemProperty.PROMOTER_URN).isPresent();
    }

    @Override
    public Optional<String> getPromoterName() {
        return source.get(PromotedItemProperty.PROMOTER_NAME);
    }

    @Override
    public Optional<Urn> getPromoterUrn() {
        return source.get(PromotedItemProperty.PROMOTER_URN);
    }

    @Override
    public List<String> getClickUrls() {
        return source.get(PromotedItemProperty.TRACK_CLICKED_URLS);
    }

    @Override
    public List<String> getImpressionUrls() {
        return source.get(PromotedItemProperty.TRACK_IMPRESSION_URLS);
    }

    @Override
    public List<String> getPromoterClickUrls() {
        return source.get(PromotedItemProperty.PROMOTER_CLICKED_URLS);
    }

    @Override
    public List<String> getPlayUrls() {
        return source.get(PromotedItemProperty.TRACK_PLAYED_URLS);
    }

    @Override
    public Kind getKind() {
        return PROMOTED;
    }

}
