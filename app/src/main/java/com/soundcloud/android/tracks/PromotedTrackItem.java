package com.soundcloud.android.tracks;

import static com.soundcloud.android.presentation.TypedListItem.Kind.PROMOTED;

import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.List;

public class PromotedTrackItem extends TrackItem implements PromotedListItem {

    public final PromotedProperties promotedProperties;

    public static PromotedTrackItem from(PropertySet source) {
        return new PromotedTrackItem(source,
                                     PromotedProperties.create(source.get(PromotedItemProperty.AD_URN),
                                                               source.get(PromotedItemProperty.TRACK_CLICKED_URLS),
                                                               source.get(PromotedItemProperty.TRACK_IMPRESSION_URLS),
                                                               source.get(PromotedItemProperty.TRACK_PLAYED_URLS),
                                                               source.get(PromotedItemProperty.PROMOTER_CLICKED_URLS),
                                                               source.get(PromotedItemProperty.PROMOTER_URN),
                                                               source.get(PromotedItemProperty.PROMOTER_NAME)));
    }

    public static PromotedTrackItem from(Track track, StreamEntity streamEntity, PromotedProperties promotedProperties) {
        return new PromotedTrackItem(toPropertySet(track, streamEntity), promotedProperties);
    }

    PromotedTrackItem(PropertySet source, PromotedProperties promotedProperties) {
        super(source);
        this.promotedProperties = promotedProperties;
    }

    @Override
    public String getAdUrn() {
        return promotedProperties.adUrn();
    }

    @Override
    public boolean hasPromoter() {
        return promotedProperties.promoterUrn().isPresent();
    }

    @Override
    public Optional<String> getPromoterName() {
        return promotedProperties.promoterName();
    }

    @Override
    public Optional<Urn> getPromoterUrn() {
        return promotedProperties.promoterUrn();
    }

    @Override
    public List<String> getClickUrls() {
        return promotedProperties.trackClickedUrls();
    }

    @Override
    public List<String> getImpressionUrls() {
        return promotedProperties.trackImpressionUrls();
    }

    @Override
    public List<String> getPromoterClickUrls() {
        return promotedProperties.promoterClickedUrls();
    }

    @Override
    public List<String> getPlayUrls() {
        return promotedProperties.trackPlayedUrls();
    }

    @Override
    public Kind getKind() {
        return PROMOTED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PromotedTrackItem that = (PromotedTrackItem) o;

        return promotedProperties != null ? promotedProperties.equals(that.promotedProperties) : that.promotedProperties == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (promotedProperties != null ? promotedProperties.hashCode() : 0);
        return result;
    }
}
