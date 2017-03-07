package com.soundcloud.android.sync.posts;

import com.soundcloud.android.model.Urn;

import java.util.Comparator;
import java.util.Date;

public interface PostRecord {

    Comparator<PostRecord> COMPARATOR = (lhs, rhs) -> {
        int result = lhs.getTargetUrn().compareTo(rhs.getTargetUrn());
        if (result == 0) {
            result = Boolean.valueOf(lhs.isRepost()).compareTo(rhs.isRepost());
        }
        return result == 0 ? lhs.getCreatedAt().compareTo(rhs.getCreatedAt()) : result;
    };

    Urn getTargetUrn();

    Date getCreatedAt();

    boolean isRepost();
}
