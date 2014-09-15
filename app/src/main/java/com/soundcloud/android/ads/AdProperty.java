package com.soundcloud.android.ads;

import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.Property;

import android.net.Uri;

import java.util.List;

public class AdProperty {
    public static final Property<String> AD_URN = Property.of(AdProperty.class, String.class);
    public static final Property<TrackUrn> MONETIZABLE_TRACK_URN =  Property.of(AdProperty.class, TrackUrn.class);
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
    public static final Property<List<String>> AUDIO_AD_IMPRESSION_URLS = Property.of(AdProperty.class, (Class<List<String>>)(Class<?>) List.class);
    public static final Property<List<String>> AUDIO_AD_FINISH_URLS = Property.of(AdProperty.class, (Class<List<String>>)(Class<?>) List.class);
    public static final Property<List<String>> AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS = Property.of(AdProperty.class, (Class<List<String>>)(Class<?>) List.class);
    public static final Property<List<String>> AUDIO_AD_CLICKTHROUGH_URLS = Property.of(AdProperty.class, (Class<List<String>>) (Class<?>) List.class);
    public static final Property<List<String>> AUDIO_AD_SKIP_URLS = Property.of(AdProperty.class, (Class<List<String>>) (Class<?>) List.class);
    public static final Property<String> LEAVE_BEHIND_IMAGE_URL = Property.of(AdProperty.class, String.class);
    public static final Property<String> LEAVE_BEHIND_LINK_URL = Property.of(AdProperty.class, String.class);
}
