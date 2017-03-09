package com.soundcloud.android.stream;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemCreator;
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
    private final TrackItemCreator trackItemCreator;

    @Inject
    public StreamEntityToItemTransformer(TrackRepository trackRepository, PlaylistRepository playlistRepository, TrackItemCreator trackItemCreator) {
        this.trackRepository = trackRepository;
        this.playlistRepository = playlistRepository;
        this.trackItemCreator = trackItemCreator;
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
                                              final TrackItem promotedTrackItem = trackItemCreator.trackItem(trackMap.get(urn), streamEntity);
                                              result.add(TrackStreamItem.create(promotedTrackItem, streamEntity.createdAt()));
                                          } else {
                                              final TrackItem trackItem = trackItemCreator.trackItem(trackMap.get(urn), streamEntity);
                                              result.add(TrackStreamItem.create(trackItem, streamEntity.createdAt()));
                                          }
                                      } else if (playlistMap.containsKey(urn)) {
                                          if (streamEntity.isPromoted()) {
                                              final PlaylistItem promotedPlaylistItem = PlaylistItem.from(playlistMap.get(urn), streamEntity);
                                              result.add(PlaylistStreamItem.create(promotedPlaylistItem, streamEntity.createdAt()));
                                          } else {
                                              final PlaylistItem playlistItem = PlaylistItem.from(playlistMap.get(urn), streamEntity);
                                              result.add(PlaylistStreamItem.create(playlistItem, streamEntity.createdAt()));
                                          }
                                      }
                                  }
                                  return result;
                              });
    }
}
