package com.soundcloud.android.model;

import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

import java.util.List;

public class PromotedItemProperty extends PlayableProperty {
    public static final Property<String> AD_URN = Property.of(PromotedItemProperty.class, String.class);
    public static final Property<Optional<Urn>> PROMOTER_URN = Property.ofOptional(PromotedItemProperty.class, Urn.class);
    public static final Property<Optional<String>> PROMOTER_NAME = Property.ofOptional(PromotedItemProperty.class, String.class);
    public static final Property<List<String>> PROMOTER_CLICKED_URLS = Property.ofList(PromotedItemProperty.class, String.class);
    public static final Property<List<String>> TRACK_PLAYED_URLS = Property.ofList(PromotedItemProperty.class, String.class);
    public static final Property<List<String>> TRACK_CLICKED_URLS = Property.ofList(PromotedItemProperty.class, String.class);
    public static final Property<List<String>> TRACK_IMPRESSION_URLS = Property.ofList(PromotedItemProperty.class, String.class);
}
