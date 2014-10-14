package com.soundcloud.android.ads;

import com.soundcloud.propeller.Property;

import android.net.Uri;

import java.util.List;

public class VisualAdProperty {
    public static final Property<String> IMAGE_URL = Property.of(VisualAdProperty.class, String.class);
    public static final Property<Uri> CLICK_THROUGH_URL = Property.of(VisualAdProperty.class, Uri.class);
    public static final Property<List<String>> TRACKING_IMPRESSION_URLS = Property.of(VisualAdProperty.class, (Class<List<String>>)(Class<?>) List.class);
    public static final Property<List<String>> TRACKING_CLICK_URLS = Property.of(VisualAdProperty.class, (Class<List<String>>)(Class<?>) List.class);
    public static final Property<Boolean> META_AD_COMPLETED = Property.of(VisualAdProperty.class, Boolean.class);
    public static final Property<Boolean> META_AD_CLICKED = Property.of(VisualAdProperty.class, Boolean.class);
}
