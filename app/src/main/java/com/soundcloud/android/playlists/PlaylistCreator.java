package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;
import rx.Observable;

// This will only stick around until we can get rid of the legacy impl in LegacyPlaylistOps
interface PlaylistCreator<ModelT> {

    Observable<ModelT> createNewPlaylist(String title, boolean isPrivate, Urn firstTrackUrn);

}
