package com.soundcloud.android.view.adapters;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.tracks.TrackUrn;

import javax.annotation.Nullable;
import java.util.List;

public class AdaptersUtils {
    private static final Predicate<ScModel> PLAYABLE_HOLDER_PREDICATE = new Predicate<ScModel>() {
        @Override
        public boolean apply(ScModel input) {
            return input instanceof PlayableHolder &&
                    ((PlayableHolder) input).getPlayable() instanceof PublicApiTrack;
        }
    };

    public static List<TrackUrn> toTrackUrn(List<? extends PlayableHolder> filter) {
        return Lists.transform(filter, new Function<PlayableHolder, TrackUrn>() {
            @Override
            public TrackUrn apply(@Nullable PlayableHolder input) {
                return ((PublicApiTrack) input.getPlayable()).getUrn();
            }
        });
    }

    public static List<? extends PlayableHolder> filterPlayables(List<? extends ScModel> data) {
        return Lists.newArrayList((Iterable<? extends PlayableHolder>) Iterables.filter(data, PLAYABLE_HOLDER_PREDICATE));
    }
}
