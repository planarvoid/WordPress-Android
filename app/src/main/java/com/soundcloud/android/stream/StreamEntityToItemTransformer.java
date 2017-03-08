package com.soundcloud.android.stream;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class StreamEntityToItemTransformer implements Func1<List<StreamEntity>, Observable<List<StreamItem>>> {
    private final TrackRepository trackRepository;
    private final PlaylistRepository playlistRepository;

    @Inject
    public StreamEntityToItemTransformer(TrackRepository trackRepository, PlaylistRepository playlistRepository) {
        this.trackRepository = trackRepository;
        this.playlistRepository = playlistRepository;
    }

    @Override
    public Observable<List<StreamItem>> call(List<StreamEntity> streamEntities) {
        final List<StreamEntity> trackList = newArrayList(filter(streamEntities, entity -> entity.urn().isTrack()));
        final List<StreamEntity> playlistList = newArrayList(filter(streamEntities, entity -> entity.urn().isPlaylist()));
        return Observable.zip(trackRepository.fromUrns(Lists.transform(trackList, StreamEntity::urn)),
                              playlistRepository.withUrns(Lists.transform(playlistList, StreamEntity::urn)),
                              (trackMap, playlistMap) -> {
                                  final List<StreamItem> result = new ArrayList<>(streamEntities.size());
                                  for (StreamEntity streamEntity : streamEntities) {
                                      final Urn urn = streamEntity.urn();
                                      if (trackMap.containsKey(urn)) {
                                          if (streamEntity.isPromoted()) {
                                              final TrackItem promotedTrackItem = TrackItem.from(trackMap.get(urn), streamEntity, streamEntity.promotedProperties().get());
                                              result.add(TrackStreamItem.create(promotedTrackItem, streamEntity.createdAt()));
                                          } else {
                                              final TrackItem trackItem = TrackItem.fromTrackAndStreamEntity(trackMap.get(urn), streamEntity);
                                              result.add(TrackStreamItem.create(trackItem, streamEntity.createdAt()));
                                          }
                                      } else if (playlistMap.containsKey(urn)) {
                                          if (streamEntity.isPromoted()) {
                                              final PromotedPlaylistItem promotedPlaylistItem = PromotedPlaylistItem.from(playlistMap.get(urn), streamEntity, streamEntity.promotedProperties().get());
                                              result.add(PlaylistStreamItem.createForPromoted(promotedPlaylistItem, streamEntity.createdAt()));
                                          } else {
                                              final PlaylistItem playlistItem = PlaylistItem.fromPlaylistAndStreamEntity(playlistMap.get(urn), streamEntity);
                                              result.add(PlaylistStreamItem.create(playlistItem, streamEntity.createdAt()));
                                          }
                                      }
                                  }
                                  return result;
                              });
    }
}
