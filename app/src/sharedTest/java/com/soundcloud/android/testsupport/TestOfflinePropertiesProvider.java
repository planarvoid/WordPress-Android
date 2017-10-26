package com.soundcloud.android.testsupport;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.IOfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflineState;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import java.util.Collections;
import java.util.HashMap;

public class TestOfflinePropertiesProvider implements IOfflinePropertiesProvider {

    private final OfflineProperties properties;

    public TestOfflinePropertiesProvider() {
        this(new OfflineProperties(new HashMap<>(), OfflineState.NOT_OFFLINE));
    }

    public TestOfflinePropertiesProvider(OfflineProperties properties) {
        this.properties = properties;
    }

    @Override
    public void subscribe() {
        // no-op
    }

    public void setEntityState(Urn entity, OfflineState state) {
        properties.getOfflineEntitiesStates().put(entity, state);
    }

    @Override
    public Observable<OfflineProperties> states() {
        return BehaviorSubject.createDefault(properties);
    }

    @Override
    public OfflineProperties latest() {
        return properties;
    }

    public static TestOfflinePropertiesProvider fromOfflinePlaylist(Urn urn) {
        return new TestOfflinePropertiesProvider(new OfflineProperties(Collections.singletonMap(urn, OfflineState.DOWNLOADED), OfflineState.NOT_OFFLINE));
    }
}
