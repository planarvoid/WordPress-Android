package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class PlaybackProgressRepository {

    private Map<Urn, PlaybackProgress> storage = new ConcurrentHashMap<>();
    private final TrackRepository trackRepository;

    @Inject
    public PlaybackProgressRepository(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public void put(Urn urn, PlaybackProgress progress) {
        storage.put(urn, progress);
    }

    public void put(Urn urn, long position) {
        Optional<PlaybackProgress> playbackProgress = get(urn);
        if (playbackProgress.isPresent()) {
            put(urn, new PlaybackProgress(position, playbackProgress.get().getDuration(), urn));
        } else {
            trackRepository
                    .track(urn)
                    .map(track -> new PlaybackProgress(position, track.fullDuration(), urn))
                    .subscribe(playbackProgressToCache -> put(urn, playbackProgressToCache));
        }
    }

    public void remove(Urn urn) {
        storage.remove(urn);
    }

    public Optional<PlaybackProgress> get(Urn urn) {
        return Optional.fromNullable(storage.get(urn));
    }
}
