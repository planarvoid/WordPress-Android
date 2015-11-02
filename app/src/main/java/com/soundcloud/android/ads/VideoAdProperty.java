package com.soundcloud.android.ads;

import com.soundcloud.java.collections.Property;

import java.util.List;

class VideoAdProperty extends AdProperty {
    public static final Property<List<String>> AD_START_URLS = Property.ofList(VideoAdProperty.class, String.class);
    public static final Property<List<String>> AD_FIRST_QUARTILE_URLS = Property.ofList(VideoAdProperty.class, String.class);
    public static final Property<List<String>> AD_SECOND_QUARTILE_URLS = Property.ofList(VideoAdProperty.class, String.class);
    public static final Property<List<String>> AD_THIRD_QUARTILE_URLS = Property.ofList(VideoAdProperty.class, String.class);

    public static final Property<List<String>> AD_PAUSE_URLS = Property.ofList(VideoAdProperty.class, String.class);
    public static final Property<List<String>> AD_RESUME_URLS = Property.ofList(VideoAdProperty.class, String.class);
    public static final Property<List<String>> AD_FULLSCREEN_URLS = Property.ofList(VideoAdProperty.class, String.class);
    public static final Property<List<String>> AD_EXIT_FULLSCREEN_URLS = Property.ofList(VideoAdProperty.class, String.class);
}
