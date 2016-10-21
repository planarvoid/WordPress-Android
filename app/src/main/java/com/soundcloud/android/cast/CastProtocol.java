package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.PropertySet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.net.Uri;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CastProtocol {

    public static final String TAG = "GoogleCast";

    private static final String KEY_URN = "urn";
    private static final String KEY_PLAY_QUEUE = "play_queue";

    private final ImageOperations imageOperations;
    private final Resources resources;

    @Inject
    CastProtocol(ImageOperations imageOperations, Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    JSONObject createPlayQueueJSON(List<Urn> urns) {
        JSONObject playQueue = new JSONObject();
        try {
            playQueue.put(KEY_PLAY_QUEUE, new JSONArray(Urns.toString(urns)));
        } catch (JSONException e) {
            Log.e(TAG, "Unable to build play queue JSON object", e);
        }
        return playQueue;
    }

    MediaInfo createMediaInfo(PropertySet track) {
        final Urn trackUrn = track.get(TrackProperty.URN);
        final String artworkUrl = imageOperations.getUrlForLargestImage(resources, trackUrn);
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, track.get(TrackProperty.TITLE));
        mediaMetadata.putString(MediaMetadata.KEY_ARTIST, track.get(TrackProperty.CREATOR_NAME));
        mediaMetadata.putString(KEY_URN, trackUrn.toString());

        if (artworkUrl != null) {
            mediaMetadata.addImage(new WebImage(Uri.parse(artworkUrl)));
        }

        return new MediaInfo.Builder(trackUrn.toString())
                .setContentType("audio/mpeg")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
    }

    List<Urn> convertRemoteDataToTrackList(JSONObject customData) throws JSONException {
        final JSONArray jsonArray = (JSONArray) customData.get(KEY_PLAY_QUEUE);
        List<Urn> remoteTracks = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            remoteTracks.add(new Urn(jsonArray.getString(i)));
        }
        return remoteTracks;
    }

    Urn getRemoteCurrentTrackUrn(MediaInfo mediaInfo) {
        return mediaInfo == null ? Urn.NOT_SET : new Urn(mediaInfo.getMetadata().getString(KEY_URN));
    }

}
