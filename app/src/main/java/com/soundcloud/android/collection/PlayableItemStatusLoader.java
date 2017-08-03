package com.soundcloud.android.collection;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.collections.Iterables;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayableItemStatusLoader extends Command<Iterable<PlayableItem>, Iterable<PlayableItem>> {
    private final LoadLikedStatuses loadLikedStatuses;
    private final LoadRepostStatuses loadRepostStatuses;

    @Inject
    public PlayableItemStatusLoader(LoadLikedStatuses loadLikedStatuses,
                                    LoadRepostStatuses loadRepostStatuses) {
        this.loadLikedStatuses = loadLikedStatuses;
        this.loadRepostStatuses = loadRepostStatuses;
    }

    @Override
    public Iterable<PlayableItem> call(Iterable<PlayableItem> input) {
        final List<PlayableItem> result = new ArrayList<>();

        final Map<Urn, Boolean> likedStatuses = loadLikedStatuses.call(Iterables.transform(input, PlayableItem::getUrn));
        final Map<Urn, Boolean> repostStatus = loadRepostStatuses.call(Iterables.transform(input, PlayableItem::getUrn));

        for (PlayableItem playableItem : input) {
            final Urn itemUrn = playableItem.getUrn();
            boolean isLikedByCurrentUser = likedStatuses.containsKey(itemUrn) && likedStatuses.get(itemUrn);
            boolean isRepostedByCurrentUser = repostStatus.containsKey(itemUrn) && repostStatus.get(itemUrn);
            result.add(playableItem.updatedWithLikeAndRepostStatus(isLikedByCurrentUser, isRepostedByCurrentUser));
        }

        return result;
    }
}
