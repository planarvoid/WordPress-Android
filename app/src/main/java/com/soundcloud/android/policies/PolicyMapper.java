package com.soundcloud.android.policies;

import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public class PolicyMapper extends RxResultMapper<PropertySet> {
    @Override
    public PropertySet map(CursorReader reader) {
        final PropertySet propertySet = PropertySet.create(4);
        if (reader.isNotNull(Tables.TrackPolicies.BLOCKED)) {
            propertySet.put(TrackProperty.BLOCKED, reader.getBoolean(Tables.TrackPolicies.BLOCKED));
        }
        if (reader.isNotNull(Tables.TrackPolicies.SNIPPED)) {
            propertySet.put(TrackProperty.SNIPPED, reader.getBoolean(Tables.TrackPolicies.SNIPPED));
        }
        if (reader.isNotNull(Tables.TrackPolicies.SUB_HIGH_TIER)) {
            propertySet.put(TrackProperty.SUB_HIGH_TIER, reader.getBoolean(Tables.TrackPolicies.SUB_HIGH_TIER));
        }
        if (reader.isNotNull(Tables.TrackPolicies.SUB_MID_TIER)) {
            propertySet.put(TrackProperty.SUB_MID_TIER, reader.getBoolean(Tables.TrackPolicies.SUB_MID_TIER));
        }
        return propertySet;
    }
}
