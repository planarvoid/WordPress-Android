package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

import android.net.Uri;

import java.util.List;

public class AdProperty {
    public static final String AD_TYPE_AUDIO = "audio_ad";
    public static final String AD_TYPE_VIDEO = "video_ad";

    public static final Property<String> AD_URN = Property.of(AdProperty.class, String.class);
    public static final Property<String> AD_TYPE = Property.of(AdProperty.class, String.class);
    public static final Property<String> COMPANION_URN = Property.of(AdProperty.class, String.class);
    public static final Property<Urn> MONETIZABLE_TRACK_URN =  Property.of(AdProperty.class, Urn.class);
    public static final Property<Uri> CLICK_THROUGH_LINK = Property.of(AdProperty.class, Uri.class);
    public static final Property<Uri> ARTWORK = Property.of(AdProperty.class, Uri.class);
    public static final Property<String> MONETIZABLE_TRACK_TITLE =  Property.of(AdProperty.class, String.class);
    public static final Property<String> MONETIZABLE_TRACK_CREATOR =  Property.of(AdProperty.class, String.class);
    public static final Property<String> DEFAULT_TEXT_COLOR = Property.of(AdProperty.class, String.class);
    public static final Property<String> DEFAULT_BACKGROUND_COLOR = Property.of(AdProperty.class, String.class);
    public static final Property<String> PRESSED_TEXT_COLOR = Property.of(AdProperty.class, String.class);
    public static final Property<String> PRESSED_BACKGROUND_COLOR = Property.of(AdProperty.class, String.class);
    public static final Property<String> FOCUSED_TEXT_COLOR = Property.of(AdProperty.class, String.class);
    public static final Property<String> FOCUSED_BACKGROUND_COLOR = Property.of(AdProperty.class, String.class);
    public static final Property<List<String>> AD_IMPRESSION_URLS = Property.ofList(AdProperty.class, String.class);
    public static final Property<List<String>> AD_FINISH_URLS = Property.ofList(AdProperty.class, String.class);
    public static final Property<List<String>> AD_COMPANION_DISPLAY_IMPRESSION_URLS = Property.ofList(AdProperty.class, String.class);
    public static final Property<List<String>> AD_CLICKTHROUGH_URLS = Property.ofList(AdProperty.class, String.class);
    public static final Property<List<String>> AD_SKIP_URLS = Property.ofList(AdProperty.class, String.class);
}
