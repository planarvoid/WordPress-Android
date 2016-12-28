package com.soundcloud.android.model;

import com.soundcloud.android.sync.posts.PostRecord;
import com.soundcloud.java.collections.Property;

import java.util.Comparator;
import java.util.Date;

public class PostProperty {
    public static final Property<Urn> TARGET_URN = EntityProperty.URN;
    public static final Property<Date> CREATED_AT = Property.of(PostProperty.class, Date.class);
    public static final Property<Boolean> IS_REPOST = Property.of(PostProperty.class, Boolean.class);
    public static final Property<String> REPOSTER = Property.of(PostProperty.class, String.class);
    public static final Property<Urn> REPOSTER_URN = Property.of(PostProperty.class, Urn.class);

    public static final Comparator<PostRecord> COMPARATOR = new Comparator<PostRecord>() {
        @Override
        public int compare(PostRecord lhs, PostRecord rhs) {
            int result = lhs.getTargetUrn().compareTo(rhs.getTargetUrn());
            if (result == 0) {
                result = Boolean.valueOf(lhs.isRepost()).compareTo(rhs.isRepost());
            }
            return result == 0 ? lhs.getCreatedAt().compareTo(rhs.getCreatedAt()) : result;
        }
    };
}

