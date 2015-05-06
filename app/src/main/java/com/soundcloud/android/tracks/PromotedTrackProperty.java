package com.soundcloud.android.tracks;

import com.google.common.base.Optional;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.Property;
import com.soundcloud.propeller.guava.OptionalProperty;

import java.util.List;

public class PromotedTrackProperty extends TrackProperty {
    public static final Property<String> AD_URN = Property.of(PromotedTrackProperty.class, String.class);
    public static final Property<Optional<Urn>> PROMOTER_URN = OptionalProperty.of(PromotedTrackProperty.class, Urn.class);
    public static final Property<Optional<String>> PROMOTER_NAME = OptionalProperty.of(PromotedTrackProperty.class, String.class);
    public static final Property<List<String>> TRACK_CLICKED_URLS = Property.ofList(PromotedTrackProperty.class, String.class);
    public static final Property<List<String>> PROMOTER_CLICKED_URLS = Property.ofList(PromotedTrackProperty.class, String.class);
    public static final Property<List<String>> TRACK_PLAYED_URLS = Property.ofList(PromotedTrackProperty.class, String.class);
    public static final Property<List<String>> TRACK_IMPRESSION_URLS = Property.ofList(PromotedTrackProperty.class, String.class);
}
