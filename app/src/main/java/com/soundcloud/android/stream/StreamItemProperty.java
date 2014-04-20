package com.soundcloud.android.stream;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Property;

import java.util.Date;

final class StreamItemProperty {
    static final Property<Urn> SOUND_URN = Property.of(Urn.class);
    static final Property<String> SOUND_TITLE = Property.of(String.class);
    static final Property<Date> CREATED_AT = Property.of(Date.class);
    static final Property<String> POSTER = Property.of(String.class);
    static final Property<Boolean> REPOST = Property.of(Boolean.class);
}
