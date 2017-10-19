package com.soundcloud.android.stream;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class StreamEntityToItemTransformer implements Function<List<StreamEntity>, Single<List<StreamItem>>> {
    private final TrackRepository trackRepository;
    private final PlaylistRepository playlistRepository;
    private final EntityItemCreator entityItemCreator;

    @Inject
    public StreamEntityToItemTransformer(TrackRepository trackRepository,
                                         PlaylistRepository playlistRepository,
                                         EntityItemCreator entityItemCreator) {
        this.trackRepository = trackRepository;
        this.playlistRepository = playlistRepository;
        this.entityItemCreator = entityItemCreator;
    }

    @Override
    public Single<List<StreamItem>> apply(List<StreamEntity> streamEntities) {
        final List<StreamEntity> trackList = newArrayList(filter(streamEntities, entity -> entity.urn().isTrack()));
        final List<StreamEntity> playlistList = newArrayList(filter(streamEntities, entity -> entity.urn().isPlaylist()));
        return Single.zip(trackRepository.fromUrns(Lists.transform(trackList, StreamEntity::urn)),
                          playlistRepository.withUrns(Lists.transform(playlistList, StreamEntity::urn)),
                          (trackMap, playlistMap) -> {
                              final List<StreamItem> result = new ArrayList<>(streamEntities.size());
                              for (StreamEntity streamEntity : streamEntities) {
                                  final Urn urn = streamEntity.urn();
                                  final Optional<String> avatarUrlTemplate = streamEntity.avatarUrlTemplate();
                                  if (trackMap.containsKey(urn)) {
                                      final TrackItem promotedTrackItem = entityItemCreator.trackItem(trackMap.get(urn), streamEntity);
                                      result.add(TrackStreamItem.Companion.create(promotedTrackItem, streamEntity.createdAt(), avatarUrlTemplate));
                                  } else if (playlistMap.containsKey(urn)) {
                                      final PlaylistItem playlistItem = entityItemCreator.playlistItem(playlistMap.get(urn), streamEntity);
                                      result.add(new PlaylistStreamItem(playlistItem, playlistItem.isPromoted(), streamEntity.createdAt(), avatarUrlTemplate));
                                  }
                              }
                              return result;
                          });
    }
}
