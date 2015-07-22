package com.soundcloud.android.ads;

import com.soundcloud.java.collections.Property;

import android.net.Uri;

import java.util.List;

public class AdOverlayProperty {
    public static final Property<String> IMAGE_URL = Property.of(AdOverlayProperty.class, String.class);
    public static final Property<Uri> CLICK_THROUGH_URL = Property.of(AdOverlayProperty.class, Uri.class);
    public static final Property<List<String>> TRACKING_IMPRESSION_URLS = Property.ofList(AdOverlayProperty.class, String.class);
    public static final Property<List<String>> TRACKING_CLICK_URLS = Property.ofList(AdOverlayProperty.class, String.class);
    public static final Property<Boolean> META_AD_COMPLETED = Property.of(AdOverlayProperty.class, Boolean.class);
    public static final Property<Boolean> META_AD_CLICKED = Property.of(AdOverlayProperty.class, Boolean.class);
    public static final Property<Boolean> META_AD_DISMISSED = Property.of(AdOverlayProperty.class, Boolean.class);
}
