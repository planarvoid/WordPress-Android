package com.soundcloud.android.playback;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class PlayableWithReposter {
    public static PlayableWithReposter create(Urn urn, Optional<Urn> reposterUrn) {
        return new AutoValue_PlayableWithReposter(urn, reposterUrn);
    }

    public static PlayableWithReposter from(Urn urn) {
        return new AutoValue_PlayableWithReposter(urn, Optional.absent());
    }

    public static PlayableWithReposter from(PlayableItem item) {
        return new AutoValue_PlayableWithReposter(item.getUrn(), item.reposterUrn());
    }

    public abstract Urn getUrn();

    public abstract Optional<Urn> getReposterUrn();
}
