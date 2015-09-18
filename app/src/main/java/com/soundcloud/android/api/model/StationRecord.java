package com.soundcloud.android.api.model;

import com.soundcloud.android.model.Urn;

import java.util.List;

public interface StationRecord {

    List<Urn> getTracks();

    Urn getUrn();

    String getType();

    String getTitle();

    String getPermalink();

    int getPreviousPosition();
}
