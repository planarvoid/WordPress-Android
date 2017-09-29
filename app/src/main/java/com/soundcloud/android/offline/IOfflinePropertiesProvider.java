package com.soundcloud.android.offline;

import io.reactivex.Observable;

public interface IOfflinePropertiesProvider {

    void subscribe();

    Observable<OfflineProperties> states();

    OfflineProperties latest();
}
