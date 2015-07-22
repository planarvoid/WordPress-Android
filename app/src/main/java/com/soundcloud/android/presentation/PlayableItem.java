package com.soundcloud.android.presentation;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

public abstract class PlayableItem implements ListItem {

    protected final PropertySet source;

    protected PlayableItem(PropertySet source) {
        this.source = source;
    }

    @Override
    public Urn getEntityUrn() {
        return source.get(PlayableProperty.URN);
    }

    @Override
    public PlayableItem update(PropertySet trackData) {
        this.source.update(trackData);
        return this;
    }

    public String getTitle() {
        return source.getOrElse(PlayableProperty.TITLE, ScTextUtils.EMPTY_STRING);
    }

    public String getCreatorName() {
        return source.getOrElse(PlayableProperty.CREATOR_NAME, ScTextUtils.EMPTY_STRING);
    }

    public Optional<String> getReposter() {
        return Optional.fromNullable(source.getOrElseNull(PlayableProperty.REPOSTER));
    }

    public boolean isPrivate() {
        return source.getOrElse(PlayableProperty.IS_PRIVATE, false);
    }

    public boolean isLiked() {
        return source.getOrElse(PlayableProperty.IS_LIKED, false);
    }
}
