package com.soundcloud.android.model;

import com.soundcloud.propeller.PropertySet;

/**
 * Resource that supports mapping to PropertySet
 */
public interface PropertySetSource {
    public PropertySet toPropertySet();
}
