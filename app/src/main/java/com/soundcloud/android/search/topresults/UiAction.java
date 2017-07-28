package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;

import java.util.List;

abstract class UiAction {

    public abstract Optional<ClickParams> clickParams();

    @AutoValue
    abstract static class TrackClick extends UiAction {
        abstract Urn trackUrn();

        public abstract TrackSourceInfo trackSourceInfo();

        public abstract List<Urn> allTracks();

        public abstract int trackPosition();

        static TrackClick create(ClickParams clickParams, Urn trackUrn, TrackSourceInfo trackSourceInfo, List<Urn> allTracks, int position) {
            return new AutoValue_UiAction_TrackClick(Optional.of(clickParams), trackUrn, trackSourceInfo, allTracks, position);
        }
    }

    @AutoValue
    abstract static class PlaylistClick extends UiAction {

        static PlaylistClick create(ClickParams clickParams) {
            return new AutoValue_UiAction_PlaylistClick(Optional.of(clickParams));
        }
    }

    @AutoValue
    abstract static class UserClick extends UiAction {

        static UserClick create(ClickParams clickParams) {
            return new AutoValue_UiAction_UserClick(Optional.of(clickParams));
        }
    }

    @AutoValue
    abstract static class ViewAllClick extends UiAction {
        abstract TopResultsBucketViewModel.Kind bucketKind();

        static ViewAllClick create(ClickParams clickParams, TopResultsBucketViewModel.Kind bucketKind) {
            return new AutoValue_UiAction_ViewAllClick(Optional.of(clickParams), bucketKind);
        }
    }

    @AutoValue
    abstract static class HelpClick extends UiAction {
        static HelpClick create() {
            return new AutoValue_UiAction_HelpClick(Optional.absent());
        }
    }

    @AutoValue
    abstract static class Search extends UiAction {
        abstract SearchParams searchParams();

        static Search create(SearchParams searchParams) {
            return new AutoValue_UiAction_Search(Optional.absent(), searchParams);
        }
    }

    @AutoValue
    abstract static class Refresh extends UiAction {
        abstract SearchParams searchParams();

        static Refresh create(SearchParams searchParams) {
            return new AutoValue_UiAction_Refresh(Optional.absent(), searchParams);
        }
    }

    @AutoValue
    abstract static class Enter extends UiAction {
        abstract long timestamp();
        abstract String searchQuery();

        static Enter create(long timestamp, String searchQuery) {
            return new AutoValue_UiAction_Enter(Optional.absent(), timestamp, searchQuery);
        }
    }
}
