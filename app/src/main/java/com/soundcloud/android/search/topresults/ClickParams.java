package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class ClickParams {

    abstract Urn urn();

    abstract String searchQuery();

    abstract Optional<Urn> queryUrn();

    abstract int position();

    abstract Module module();

    abstract Screen screen();

    abstract SearchEvent.ClickSource clickSource();

    static ClickParams create(Urn itemUrn, String searchQuery, Optional<Urn> queryUrn, int position, Module module, Screen screen, SearchEvent.ClickSource clickSource) {
        return new AutoValue_ClickParams(itemUrn, searchQuery, queryUrn, position, module, screen, clickSource);
    }

    SearchQuerySourceInfo searchQuerySourceInfo() {
        return new SearchQuerySourceInfo(queryUrn().or(Urn.NOT_SET), position(), urn(), searchQuery());
    }

    UIEvent uiEvent() {
        return UIEvent.fromNavigation(urn(), getEventContextMetadata());
    }

    private EventContextMetadata getEventContextMetadata() {
        return EventContextMetadata.builder().pageName(clickSource().key).module(module()).build();
    }
}
