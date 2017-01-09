package com.soundcloud.android.cast;

import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.Urns;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.TimeInterval;

import android.content.res.Resources;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LegacyCastOperations implements CastOperations {

    public static final String TAG = "LegacyGoogleCast";

    private static final String KEY_URN = "urn";
    private static final String KEY_PLAY_QUEUE = "play_queue";

    private static final Func1<TrackItem, Boolean> FILTER_PRIVATE_TRACKS = track -> !track.isPrivate();
    private static final Func1<TrackItem, Urn> TO_URNS = track -> track.getUrn();

    private final VideoCastManager videoCastManager;
    private final TrackRepository trackRepository;
    private final PolicyOperations policyOperations;
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final Scheduler progressPullIntervalScheduler;

    private final Func1<Urn, Observable<TrackItem>> loadTracks = new Func1<Urn, Observable<TrackItem>>() {
        @Override
        public Observable<TrackItem> call(Urn urn) {
            return trackRepository.track(urn);
        }
    };

    @Inject
    public LegacyCastOperations(VideoCastManager videoCastManager,
                                TrackRepository trackRepository,
                                PolicyOperations policyOperations,
                                ImageOperations imageOperations,
                                Resources resources,
                                @Named(LOW_PRIORITY) Scheduler progressPullIntervalScheduler) {
        this.videoCastManager = videoCastManager;
        this.trackRepository = trackRepository;
        this.policyOperations = policyOperations;
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.progressPullIntervalScheduler = progressPullIntervalScheduler;
    }

    public Observable<LocalPlayQueue> loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(final Urn currentTrackUrn,
                                                                                           List<Urn> unfilteredLocalPlayQueueTracks) {
        return filterMonetizableAndPrivateTracks(unfilteredLocalPlayQueueTracks).toList()
                                                                                .flatMap(new Func1<List<Urn>, Observable<LocalPlayQueue>>() {
                                                                                    @Override
                                                                                    public Observable<LocalPlayQueue> call(
                                                                                            List<Urn> filteredLocalPlayQueueTracks) {
                                                                                        if (filteredLocalPlayQueueTracks
                                                                                                .isEmpty()) {
                                                                                            return Observable.just(
                                                                                                    LocalPlayQueue.empty());
                                                                                        } else if (filteredLocalPlayQueueTracks
                                                                                                .contains(
                                                                                                        currentTrackUrn)) {
                                                                                            return loadLocalPlayQueue(
                                                                                                    currentTrackUrn,
                                                                                                    filteredLocalPlayQueueTracks);
                                                                                        } else {
                                                                                            return loadLocalPlayQueue(
                                                                                                    filteredLocalPlayQueueTracks
                                                                                                            .get(0),
                                                                                                    filteredLocalPlayQueueTracks);
                                                                                        }
                                                                                    }
                                                                                });
    }

    public Observable<LocalPlayQueue> loadLocalPlayQueue(Urn currentTrackUrn, List<Urn> filteredLocalPlayQueueTracks) {
        return Observable.zip(trackRepository.track(currentTrackUrn),
                              Observable.from(filteredLocalPlayQueueTracks).toList(),
                              (track, filteredLocalPlayQueueTracks1) -> new LocalPlayQueue(
                                      createPlayQueueJSON(filteredLocalPlayQueueTracks1),
                                      filteredLocalPlayQueueTracks1,
                                      createMediaInfo(track),
                                      track.getUrn()));
    }

    private Observable<Urn> filterMonetizableAndPrivateTracks(List<Urn> unfilteredLocalPlayQueueTracks) {
        return policyOperations.filterMonetizableTracks(unfilteredLocalPlayQueueTracks)
                               .flatMap(RxUtils.<Urn>iterableToObservable())
                               .flatMap(loadTracks)
                               .filter(FILTER_PRIVATE_TRACKS)
                               .map(TO_URNS);
    }

    private JSONObject createPlayQueueJSON(List<Urn> urns) {
        JSONObject playQueue = new JSONObject();
        try {
            playQueue.put(KEY_PLAY_QUEUE, new JSONArray(Urns.toString(urns)));
        } catch (JSONException e) {
            Log.e(TAG, "Unable to build play queue JSON object", e);
        }
        return playQueue;
    }

    private MediaInfo createMediaInfo(TrackItem track) {
        final Urn trackUrn = track.getUrn();
        final String artworkUrl = imageOperations.getUrlForLargestImage(resources, trackUrn);
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, track.getTitle());
        mediaMetadata.putString(MediaMetadata.KEY_ARTIST, track.getCreatorName());
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

    public RemotePlayQueue loadRemotePlayQueue() {
        final MediaInfo remoteMediaInformation;
        try {
            remoteMediaInformation = videoCastManager.getRemoteMediaInformation();
            return parseMediaInfoToRemotePlayQueue(remoteMediaInformation);
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to retrieve remote media information", e);
            return RemotePlayQueue.create(Collections.<Urn>emptyList(), Urn.NOT_SET);
        }
    }

    private RemotePlayQueue parseMediaInfoToRemotePlayQueue(MediaInfo remoteMediaInformation) {
        try {
            if (remoteMediaInformation != null) {
                final JSONObject customData = remoteMediaInformation.getCustomData();
                if (customData != null) {
                    return RemotePlayQueue.create(convertRemoteDataToTrackList(customData),
                                               getCurrentTrackUrn(remoteMediaInformation));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Unable to retrieve remote play queue", e);
        }
        return RemotePlayQueue.create(new ArrayList<>(), Urn.NOT_SET);
    }

    private List<Urn> convertRemoteDataToTrackList(JSONObject customData) throws JSONException {
        final JSONArray jsonArray = (JSONArray) customData.get(KEY_PLAY_QUEUE);
        List<Urn> remoteTracks = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            remoteTracks.add(new Urn(jsonArray.getString(i)));
        }
        return remoteTracks;
    }

    public Urn getRemoteCurrentTrackUrn() {
        try {
            return getCurrentTrackUrn(videoCastManager.getRemoteMediaInformation());
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to get remote media information", e);
            return Urn.NOT_SET;
        }
    }

    private Urn getCurrentTrackUrn(MediaInfo mediaInfo) {
        return mediaInfo == null ? Urn.NOT_SET : new Urn(mediaInfo.getMetadata().getString(KEY_URN));
    }

    @Override
    public Observable<TimeInterval<Long>> intervalForProgressPull() {
        return Observable.interval(PlaybackConstants.PROGRESS_DELAY_MS,
                                   TimeUnit.MILLISECONDS,
                                   progressPullIntervalScheduler).timeInterval();
    }

}
