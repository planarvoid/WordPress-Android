package com.soundcloud.android.ads;

import com.soundcloud.propeller.Property;

import android.net.Uri;

import java.util.List;

public class LeaveBehindProperty {
    public static final Property<String> LEAVE_BEHIND_URN = Property.of(LeaveBehind.class, String.class);
    public static final Property<Uri> IMAGE_URL = Property.of(LeaveBehind.class, Uri.class);
    public static final Property<Uri> CLICK_THROUGH_URL = Property.of(LeaveBehind.class, Uri.class);
    public static final Property<List<String>> TRACKING_IMPRESSION_URLS = Property.of(LeaveBehind.class, (Class<List<String>>)(Class<?>) List.class);
    public static final Property<List<String>> TRACKING_CLICK_URLS = Property.of(LeaveBehind.class, (Class<List<String>>)(Class<?>) List.class);
}
