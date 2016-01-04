package com.soundcloud.android.profile;

import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.optional.Optional;

public interface PropertySetSourceHolder {
    Optional<PropertySetSource> getItem();
}
