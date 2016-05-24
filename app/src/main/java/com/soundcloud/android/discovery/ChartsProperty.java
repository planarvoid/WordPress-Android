package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

class ChartsProperty {
    static final Property<Long> LOCAL_ID = Property.of(ChartsProperty.class, Long.class);
    static final Property<String> TITLE = Property.of(ChartsProperty.class, String.class);
    static final Property<String> PAGE = Property.of(ChartsProperty.class, String.class);
    static final Property<ChartType> CHART_TYPE = Property.of(ChartsProperty.class, ChartType.class);
    static final Property<ChartCategory> CHART_CATEGORY = Property.of(ChartsProperty.class, ChartCategory.class);
    static final Property<Optional<Urn>> GENRE = Property.ofOptional(ChartsProperty.class, Urn.class);
}
