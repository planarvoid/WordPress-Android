package com.soundcloud.android.profile;

import com.soundcloud.android.collection.PlayableItemStatusLoader;
import com.soundcloud.android.commands.Command;

import javax.inject.Inject;

public class SpotlightItemStatusLoader extends Command<UserProfile, UserProfile> {

    private final PlayableItemStatusLoader playableItemStatusLoader;

    @Inject
    public SpotlightItemStatusLoader(PlayableItemStatusLoader playableItemStatusLoader) {
        this.playableItemStatusLoader = playableItemStatusLoader;
    }

    @Override
    public UserProfile call(UserProfile input) {
        playableItemStatusLoader.call(input.getSpotlight().getCollection());
        return input;
    }
}
