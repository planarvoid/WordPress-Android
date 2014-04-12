package com.soundcloud.android.stream;

import com.soundcloud.android.storage.Property;

final class StreamItemProperty {
    static final Property<String> SOUND_URN = Property.of(String.class);
    static final Property<String> SOUND_TITLE = Property.of(String.class);
    static final Property<Boolean> REPOSTED = Property.of(Boolean.class);
}
