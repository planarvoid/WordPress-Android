package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class SearchEvent extends TrackingEvent {
    public static final String EVENT_NAME = "click";

    public enum ClickName {
        ITEM_NAVIGATION("item_navigation"),
        FORMULATION_INIT("search_formulation_init"),
        FORMULATION_END("search_formulation_end"),
        SEARCH("search");

        ClickName(String key) {
            this.key = key;
        }

        public final String key;
    }

    public enum ClickSource {
        TOP_RESULTS_BUCKET("search:top_results"),
        GO_TRACKS_BUCKET("search:high_tier"),
        TRACKS_BUCKET("search:tracks"),
        PLAYLISTS_BUCKET("search:playlists"),
        ALBUMS_BUCKET("search:albums"),
        PEOPLE_BUCKET("search:people"),
        AUTOCOMPLETE("search-autocomplete");

        ClickSource(String key) {
            this.key = key;
        }

        public final String key;
    }

    public enum Kind {
        SUBMIT
    }

    public abstract Optional<String> pageName();

    public abstract Optional<ClickName> clickName();

    public abstract Optional<Urn> clickObject();

    public abstract Optional<ClickSource> clickSource();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<String> query();

    public abstract Optional<Integer> queryPosition();

    public abstract Optional<Kind> kind();

    @Override
    public SearchEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_SearchEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    public static SearchEvent recentTagSearch() {
        return emptyBuilder().kind(Optional.of(Kind.SUBMIT)).clickName(Optional.of(ClickName.ITEM_NAVIGATION)).build();
    }

    public static SearchEvent popularTagSearch() {
        return emptyBuilder().kind(Optional.of(Kind.SUBMIT)).build();
    }

    public static SearchEvent searchStart(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return builderWithSearchQuery(searchQuerySourceInfo, screen, ClickName.SEARCH).kind(Optional.of(Kind.SUBMIT)).build();
    }

    public static SearchEvent tapLocalSuggestionOnScreen(Screen screen, Urn itemUrn, String query, int clickPosition) {
        return emptyBuilder()
                .pageName(Optional.of(screen.get()))
                .clickName(Optional.of(ClickName.ITEM_NAVIGATION))
                .query(Optional.fromNullable(query))
                .clickObject(Optional.of(itemUrn))
                .queryPosition(Optional.of(clickPosition))
                .clickSource(Optional.of(ClickSource.AUTOCOMPLETE))
                .build();
    }

    public static SearchEvent tapPlaylistOnScreen(Screen screen) {
        return tapItemOnScreen(screen);
    }

    public static SearchEvent tapItemOnScreen(Screen screen,
                                               SearchQuerySourceInfo searchQuerySourceInfo,
                                               ClickSource source) {
        return tapItemOnScreen(screen, searchQuerySourceInfo, Optional.of(source));
    }

    public static SearchEvent tapItemOnScreen(Screen screen,
                                               SearchQuerySourceInfo searchQuerySourceInfo) {
        return tapItemOnScreen(screen, searchQuerySourceInfo, Optional.absent());
    }

    private static SearchEvent tapItemOnScreen(Screen screen,
                                               SearchQuerySourceInfo searchQuerySourceInfo,
                                               Optional<ClickSource> source) {
        return builderWithSearchQuery(searchQuerySourceInfo, screen, ClickName.ITEM_NAVIGATION).clickSource(source).build();
    }

    private static SearchEvent tapItemOnScreen(Screen screen) {
        return emptyBuilder()
                .clickName(Optional.of(ClickName.ITEM_NAVIGATION))
                .pageName(Optional.of(screen.get()))
                .build();
    }

    public static SearchEvent searchFormulationInit(Screen screen, String query) {
        return emptyBuilder()
                .query(Optional.fromNullable(query))
                .pageName(Optional.of(screen.get()))
                .clickName(Optional.of(ClickName.FORMULATION_INIT))
                .build();
    }

    public static SearchEvent searchFormulationEnd(Screen screen,
                                                   String query,
                                                   Optional<Urn> queryUrn,
                                                   Optional<Integer> queryPosition) {
        return emptyBuilder()
                .query(Optional.fromNullable(query))
                .pageName(Optional.of(screen.get()))
                .clickName(Optional.of(ClickName.FORMULATION_END))
                .queryUrn(queryUrn)
                .queryPosition(queryPosition)
                .build();
    }

    private static SearchEvent.Builder emptyBuilder() {

        return new AutoValue_SearchEvent.Builder().kind(Optional.absent())
                                                  .id(defaultId())
                                                  .timestamp(defaultTimestamp())
                                                  .referringEvent(Optional.absent())
                                                  .pageName(Optional.absent())
                                                  .clickName(Optional.absent())
                                                  .clickObject(Optional.absent())
                                                  .clickSource(Optional.absent())
                                                  .queryUrn(Optional.absent())
                                                  .query(Optional.absent())
                                                  .queryPosition(Optional.absent());
    }

    private static SearchEvent.Builder builderWithSearchQuery(SearchQuerySourceInfo searchQuerySourceInfo, Screen screen,
                                                              ClickName clickName) {

        final Urn clickUrn = searchQuerySourceInfo.getClickUrn();
        final Optional<Urn> optionalClickUrn = clickUrn != null && clickUrn != Urn.NOT_SET ? Optional.of(clickUrn) : Optional.absent();
        final int clickPosition = searchQuerySourceInfo.getClickPosition();
        final Optional<Integer> optionalClickPosition = clickPosition >= 0 ? Optional.of(clickPosition) : Optional.absent();
        return new AutoValue_SearchEvent.Builder().kind(Optional.absent())
                                                  .id(defaultId())
                                                  .timestamp(defaultTimestamp())
                                                  .referringEvent(Optional.absent())
                                                  .pageName(Optional.of(screen.get()))
                                                  .clickName(Optional.of(clickName))
                                                  .clickObject(optionalClickUrn)
                                                  .clickSource(Optional.absent())
                                                  .queryUrn(Optional.fromNullable(searchQuerySourceInfo.getQueryUrn()))
                                                  .query(Optional.fromNullable(searchQuerySourceInfo.getQueryString()))
                                                  .queryPosition(optionalClickPosition);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder pageName(Optional<String> pageName);

        public abstract Builder clickName(Optional<ClickName> clickName);

        public abstract Builder clickObject(Optional<Urn> clickObject);

        public abstract Builder clickSource(Optional<ClickSource> clickSource);

        public abstract Builder queryUrn(Optional<Urn> queryUrn);

        public abstract Builder query(Optional<String> query);

        public abstract Builder queryPosition(Optional<Integer> clickPosition);

        public abstract Builder kind(Optional<Kind> kind);

        public abstract SearchEvent build();
    }
}
