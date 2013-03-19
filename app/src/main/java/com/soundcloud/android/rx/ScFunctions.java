package com.soundcloud.android.rx;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import rx.Observable;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

import java.util.List;

public class ScFunctions {

    public static AlwaysTrue alwaysTrue() {
        return AlwaysTrue.INSTANCE;
    }

    private enum AlwaysTrue implements Func0<Boolean> {
        INSTANCE;

        @Override
        public Boolean call() {
            return true;
        }
    }

    /**
     * Turns an <pre>Observable<Playlist></pre> into an <pre>Observable<List<Track>></pre> by mapping the playlist to its tracks.
     */
    public static final Func1<Observable<Playlist>, Observable<List<Track>>> PLAYLIST_OBS_TO_TRACKS_OBS =
            new Func1<Observable<Playlist>, Observable<List<Track>>>() {
                @Override
                public Observable<List<Track>> call(Observable<Playlist> observable) {
                    // do not attempt to map no-op observables, this may screw up filtering later
                    if (ScObservables.EMPTY == observable) {
                        return ScObservables.EMPTY;
                    }
                    return observable.map(PLAYLIST_TO_TRACKS);
                }
            };

    /**
     * Takes a playlist and returns its tracks.
     */
    public static final Func1<Playlist, List<Track>> PLAYLIST_TO_TRACKS =
            new Func1<Playlist, List<Track>>() {
                @Override
                public List<Track> call(Playlist playlist) {
                    return playlist.tracks;
                }
            };
    /**
     * Takes a playlist and a list of tracks, sets these tracks on the playlist, and returns the playlist
     */
    public static final Func2<Playlist, List<Track>, Playlist> FOLD_TRACKS_INTO_PLAYLIST =
            new Func2<Playlist, List<Track>, Playlist>() {
                @Override
                public Playlist call(Playlist playlist, List<Track> tracks) {
                    playlist.tracks = tracks;
                    return playlist;
                }
            };
}
