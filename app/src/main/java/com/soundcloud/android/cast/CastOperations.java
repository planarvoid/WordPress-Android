package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CastOperations {

    public static final String TAG = "ChromeCast";

    private final VideoCastManager videoCastManager;

    @Inject
    public CastOperations(VideoCastManager videoCastManager) {
        this.videoCastManager = videoCastManager;
    }

    public RemotePlayQueue loadRemotePlayQueue() {
        final MediaInfo remoteMediaInformation;
        try {
            remoteMediaInformation = videoCastManager.getRemoteMediaInformation();
            return parseMediaInfoToRemotePlayQueue(remoteMediaInformation);
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to retrieve remote media information", e);
            return new RemotePlayQueue(Collections.<Urn>emptyList(), Urn.NOT_SET);
        }
    }

    private RemotePlayQueue parseMediaInfoToRemotePlayQueue(MediaInfo remoteMediaInformation) {
        try {
            if (remoteMediaInformation != null){
                final JSONObject customData = remoteMediaInformation.getCustomData();
                if (customData != null){
                    return new RemotePlayQueue(convertRemoteDataToTrackList(customData), getRemoteUrn(remoteMediaInformation));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Unable to retrieve remote play queue", e);
        }
        return new RemotePlayQueue(new ArrayList<Urn>(), Urn.NOT_SET);
    }

    private List<Urn> convertRemoteDataToTrackList(JSONObject customData) throws JSONException {
        final JSONArray jsonArray = (JSONArray) customData.get(CastPlayer.KEY_PLAY_QUEUE);
        List<Urn> remoteTracks = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++){
            remoteTracks.add(new Urn(jsonArray.getString(i)));
        }
        return remoteTracks;
    }

    private Urn getRemoteUrn(MediaInfo mediaInfo) {
        return CastPlayer.getUrnFromMediaMetadata(mediaInfo);
    }

}
