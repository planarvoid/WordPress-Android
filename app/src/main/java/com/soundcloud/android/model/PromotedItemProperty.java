package com.soundcloud.android.model;

import com.google.common.base.Optional;
import com.soundcloud.propeller.Property;
import com.soundcloud.propeller.guava.OptionalProperty;

import java.util.List;

public class PromotedItemProperty extends PlayableProperty {
    public static final Property<String> AD_URN = Property.of(PromotedItemProperty.class, String.class);
    public static final Property<Optional<Urn>> PROMOTER_URN = OptionalProperty.of(PromotedItemProperty.class, Urn.class);
    public static final Property<Optional<String>> PROMOTER_NAME = OptionalProperty.of(PromotedItemProperty.class, String.class);
    public static final Property<List<String>> PROMOTER_CLICKED_URLS = Property.ofList(PromotedItemProperty.class, String.class);
    public static final Property<List<String>> TRACK_PLAYED_URLS = Property.ofList(PromotedItemProperty.class, String.class);
    public static final Property<List<String>> TRACK_CLICKED_URLS = Property.ofList(PromotedItemProperty.class, String.class);
    public static final Property<List<String>> TRACK_IMPRESSION_URLS = Property.ofList(PromotedItemProperty.class, String.class);
}
