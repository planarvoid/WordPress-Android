package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class SearchEvent extends NewTrackingEvent {

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

    public abstract Optional<String> pageName();

    public abstract Optional<ClickName> clickName();

    public abstract Optional<Urn> clickObject();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<String> query();

    public abstract Optional<Integer> queryPosition();

    @Override
    public SearchEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_SearchEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    public static SearchEvent recentTagSearch() {
        return emptyBuilder(Kind.SEARCH_SUBMIT)
                .clickName(Optional.of(ClickName.ITEM_NAVIGATION))
                .build();
    }

    public static SearchEvent popularTagSearch() {
        return emptyBuilder(Kind.SEARCH_SUBMIT)
                .build();
    }

    public static SearchEvent searchStart(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return builderWithSearchQuery(Kind.SEARCH_SUBMIT, searchQuerySourceInfo, screen, ClickName.SEARCH)
                .build();
    }

    public static SearchEvent tapLocalSuggestionOnScreen(Screen screen, Urn itemUrn, String query, int clickPosition) {
        return emptyBuilder(Kind.SEARCH_LOCAL_SUGGESTION)
                .pageName(Optional.of(screen.get()))
                .clickName(Optional.of(ClickName.ITEM_NAVIGATION))
                .query(Optional.fromNullable(query))
                .clickObject(Optional.of(itemUrn))
                .queryPosition(Optional.of(clickPosition))
                .build();
    }

    public static SearchEvent tapTrackOnScreen(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return tapItemOnScreen(screen, searchQuerySourceInfo);
    }

    public static SearchEvent tapPlaylistOnScreen(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return tapItemOnScreen(screen, searchQuerySourceInfo);
    }

    public static SearchEvent tapPlaylistOnScreen(Screen screen) {
        return tapItemOnScreen(screen);
    }

    public static SearchEvent tapUserOnScreen(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return tapItemOnScreen(screen, searchQuerySourceInfo);
    }

    private static SearchEvent tapItemOnScreen(Screen screen,
                                               SearchQuerySourceInfo searchQuerySourceInfo) {
        return builderWithSearchQuery(Kind.SEARCH_RESULTS, searchQuerySourceInfo, screen, ClickName.ITEM_NAVIGATION)
                .build();
    }

    private static SearchEvent tapItemOnScreen(Screen screen) {
        return emptyBuilder(Kind.SEARCH_RESULTS)
                .clickName(Optional.of(ClickName.ITEM_NAVIGATION))
                .pageName(Optional.of(screen.get()))
                .build();
    }

    public static SearchEvent searchFormulationInit(Screen screen, String query) {
        return emptyBuilder(Kind.SEARCH_FORMULATION_INIT)
                .query(Optional.fromNullable(query))
                .pageName(Optional.of(screen.get()))
                .clickName(Optional.of(ClickName.FORMULATION_INIT))
                .build();
    }

    public static SearchEvent searchFormulationEnd(Screen screen,
                                                   String query,
                                                   Optional<Urn> queryUrn,
                                                   Optional<Integer> queryPosition) {
        return emptyBuilder(Kind.SEARCH_FORMULATION_END)
                .query(Optional.fromNullable(query))
                .pageName(Optional.of(screen.get()))
                .clickName(Optional.of(ClickName.FORMULATION_END))
                .queryUrn(queryUrn)
                .queryPosition(queryPosition)
                .build();
    }

    private static SearchEvent.Builder emptyBuilder(Kind kind) {

        return new AutoValue_SearchEvent.Builder().kind(kind)
                                                  .id(defaultId())
                                                  .timestamp(defaultTimestamp())
                                                  .referringEvent(Optional.absent())
                                                  .pageName(Optional.absent())
                                                  .clickName(Optional.absent())
                                                  .clickObject(Optional.absent())
                                                  .queryUrn(Optional.absent())
                                                  .query(Optional.absent())
                                                  .queryPosition(Optional.absent());
    }

    private static SearchEvent.Builder builderWithSearchQuery(Kind kind,
                                                              SearchQuerySourceInfo searchQuerySourceInfo,
                                                              Screen screen,
                                                              ClickName clickName) {

        final Urn clickUrn = searchQuerySourceInfo.getClickUrn();
        final Optional<Urn> optionalClickUrn = clickUrn != null && clickUrn != Urn.NOT_SET ? Optional.of(clickUrn) : Optional.absent();
        final int clickPosition = searchQuerySourceInfo.getClickPosition();
        final Optional<Integer> optionalClickPosition = clickPosition >= 0 ? Optional.of(clickPosition) : Optional.absent();
        return new AutoValue_SearchEvent.Builder().kind(kind)
                                                  .id(defaultId())
                                                  .timestamp(defaultTimestamp())
                                                  .referringEvent(Optional.absent())
                                                  .pageName(Optional.of(screen.get()))
                                                  .clickName(Optional.of(clickName))
                                                  .clickObject(optionalClickUrn)
                                                  .queryUrn(Optional.fromNullable(searchQuerySourceInfo.getQueryUrn()))
                                                  .query(Optional.absent())
                                                  .queryPosition(optionalClickPosition);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder kind(Kind kind);

        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder pageName(Optional<String> pageName);

        public abstract Builder clickName(Optional<ClickName> clickName);

        public abstract Builder clickObject(Optional<Urn> clickObject);

        public abstract Builder queryUrn(Optional<Urn> queryUrn);

        public abstract Builder query(Optional<String> query);

        public abstract Builder queryPosition(Optional<Integer> clickPosition);

        public abstract SearchEvent build();
    }
}
