package com.soundcloud.android.testsupport;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.IOfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflineState;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import java.util.Collections;

public class TestOfflinePropertiesProvider implements IOfflinePropertiesProvider {

    private final OfflineProperties properties;

    public TestOfflinePropertiesProvider() {
        this(new OfflineProperties());
    }

    public TestOfflinePropertiesProvider(OfflineProperties properties) {
        this.properties = properties;
    }

    @Override
    public void subscribe() {
        // no-op
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
