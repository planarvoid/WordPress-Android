package com.soundcloud.android.utils;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.track.PlaylistDetailActivity;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public final class PlayUtils {

    private Context mContext;
    private ScModelManager mModelManager;

    public PlayUtils(Context context) {
        this(context, SoundCloudApplication.MODEL_MANAGER);
    }

    public PlayUtils(Context context, ScModelManager modelManager) {
        mContext = context;
        mModelManager = modelManager;
    }

    public void playTrack(PlayInfo playInfo) {
        final Intent playIntent = getPlayIntent(playInfo);
        mContext.startActivity(playIntent);
    }

    public Intent getPlayIntent(PlayInfo info) {
        return getPlayIntent(info, CloudPlaybackService.getCurrentTrackId() != info.initialTrack.getId());
    }

    public Intent getPlayIntent(PlayInfo info, boolean changingTracks) {
        Intent intent = new Intent(Actions.PLAY).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        if (changingTracks) {
            configureIntentViaPlayInfo(info, intent);
        }
        return intent;
    }

    public void playFromAdapter(List<? extends ScModel> data, int position, Uri streamUri) {
        if (position >= data.size() || !(data.get(position) instanceof PlayableHolder)) {
            throw new AssertionError("Invalid item " + position + ", must be a playable");
        }

        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
        if (playable instanceof Track) {

            List<Track> tracks = new ArrayList<Track>(data.size());
            // Required for mixed adapters (e.g. mix of users and tracks, we only want tracks)
            int adjustedPosition = position;
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i) instanceof PlayableHolder && ((PlayableHolder) data.get(i)).getPlayable() instanceof Track) {
                    tracks.add((Track) ((PlayableHolder) data.get(i)).getPlayable());
                } else if (i < position) {
                    adjustedPosition--;
                }
            }

            PlayInfo info = PlayInfo.fromUriWithInitialTracklist(streamUri, adjustedPosition, tracks.get(adjustedPosition), tracks);
            mContext.startActivity(getPlayIntent(info));

        } else if (playable instanceof Playlist) {
            PlaylistDetailActivity.start(mContext, (Playlist) playable, mModelManager);
        } else {
            throw new AssertionError("Unexpected playable type");
        }
    }

    public static Track getTrackFromIntent(Intent intent){
        if (intent.getBooleanExtra(CloudPlaybackService.PlayExtras.playFromXferList,false)){
            final int position = intent.getIntExtra(CloudPlaybackService.PlayExtras.playPosition,-1);
            final List<Track> list = CloudPlaybackService.playlistXfer;
            if (list != null && position > -1 && position < list.size() && list.get(position).getPlayable() instanceof Track){
                return (Track) list.get(position).getPlayable();
            }
        } else if (intent.getLongExtra(CloudPlaybackService.PlayExtras.trackId,-1l) > 0) {
            return SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra(CloudPlaybackService.PlayExtras.trackId,-1l));
        } else if (intent.getParcelableExtra(Track.EXTRA) != null) {
            return intent.getParcelableExtra(Track.EXTRA);
        }
        return null;
    }

    private void configureIntentViaPlayInfo(PlayInfo info, Intent intent) {
        intent.putExtra(CloudPlaybackService.PlayExtras.fetchRelated, info.fetchRelated);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, info.sourceInfo);
        intent.putExtra(CloudPlaybackService.PlayExtras.track, info.initialTrack);
        CloudPlaybackService.playlistXfer = info.iniitalTracklist;

        if (info.uri != null) {
            mModelManager.cache(info.initialTrack);
            intent.putExtra(CloudPlaybackService.PlayExtras.trackId, info.initialTrack.getId())
                    .putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                    .setData(info.uri);

        } else if (info.iniitalTracklist.size() > 1) {
            intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                    .putExtra(CloudPlaybackService.PlayExtras.playFromXferList, true);
        }
    }
}
