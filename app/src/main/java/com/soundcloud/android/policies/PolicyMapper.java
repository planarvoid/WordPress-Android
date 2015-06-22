package com.soundcloud.android.policies;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.RxResultMapper;

public class PolicyMapper extends RxResultMapper<PropertySet> {
    @Override
    public PropertySet map(CursorReader reader) {
        final PropertySet propertySet = PropertySet.create(1);
        if (reader.isNotNull(TableColumns.TrackPolicies.SUB_MID_TIER)){
            propertySet.put(TrackProperty.SUB_MID_TIER, reader.getBoolean(TableColumns.TrackPolicies.SUB_MID_TIER));
        }
        return propertySet;
    }
}
