package com.soundcloud.android.playback;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.soundcloud.android.Actions;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.APIResponse;
import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.Token;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.functions.Func1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// TODO, move to playback package level
public class PlaybackOperations {

    private static final Predicate<ScModel> PLAYABLE_HOLDER_PREDICATE = new Predicate<ScModel>() {
        @Override
        public boolean apply(ScModel input) {
            return input instanceof PlayableHolder &&
                    ((PlayableHolder) input).getPlayable() instanceof Track;
        }
    };

    private static final String PARAM_CLIENT_ID = "client_id";

    private final ScModelManager modelManager;
    private final TrackStorage trackStorage;
    private final PlayQueueManager playQueueManager;
    private final AccountOperations accountOperations;
    private final HttpProperties httpProperties;
    private final FeatureFlags featureFlags;
    private final EventBus eventBus;
    private final RxHttpClient rxHttpClient;

    @Inject
    public PlaybackOperations(ScModelManager modelManager, TrackStorage trackStorage, PlayQueueManager playQueueManager,
                              AccountOperations accountOperations, HttpProperties httpProperties,RxHttpClient rxHttpClient,
                              FeatureFlags featureFlags, EventBus eventBus) {
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.playQueueManager = playQueueManager;
        this.accountOperations = accountOperations;
        this.httpProperties = httpProperties;
        this.featureFlags = featureFlags;
        this.eventBus = eventBus;
        this.rxHttpClient = rxHttpClient;
    }

    /**
     * Single play, the tracklist will be of length 1
     */
    public void playTrack(Context context, Track track, Screen screen) {
        playFromIdList(context, Lists.newArrayList(track.getId()), 0, track, new PlaySessionSource(screen));
    }

    /**
     * Created by anything played from the {@link com.soundcloud.android.explore.ExploreFragment} section.
     */
    public void playExploreTrack(Context context, Track track, String exploreTag, String screenTag) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screenTag);
        playSessionSource.setExploreVersion(exploreTag);
        playTrack(context, track, playSessionSource, true);
    }

    public String buildHLSUrlForTrack(Track track) {
        checkState(accountOperations.isUserLoggedIn(), "SoundCloud User account does not exist");
        Token token = accountOperations.getSoundCloudToken();
        return Uri.parse(httpProperties.getPrivateApiHostWithHttpScheme() + APIEndpoints.HLS_STREAM.unencodedPath(track.getUrn()))
                .buildUpon()
                .appendQueryParameter(HttpProperties.Parameter.OAUTH_PARAMETER.toString(), token.access)
                .build().toString();
    }

    private void playTrack(Context context, Track track, PlaySessionSource playSessionSource, boolean loadRelated) {
        playFromIdList(context, Lists.newArrayList(track.getId()), 0, track, playSessionSource, loadRelated);
    }

    /**
     * From a uri with an initial track to show while loading the full playlist from the DB.
     * Used in {@link com.soundcloud.android.playlists.PlaylistFragment}
     */
    public void playPlaylistFromPosition(Context context, Playlist playlist, int startPosition, Track initialTrack, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen.get());
        playSessionSource.setPlaylist(playlist);
        playFromUri(context, playlist.toUri(), startPosition, initialTrack, playSessionSource);
    }

    public void playPlaylist(Context context, Playlist playlist, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen.get());
        playSessionSource.setPlaylist(playlist);
        final List<Long> trackIds = Lists.transform(playlist.getTracks(), new Function<Track, Long>() {
            @Override
            public Long apply(Track track) {
                return track.getId();
            }
        });
        startPlaySession(context, trackIds, 0, playSessionSource);
    }

    @Deprecated
    public void playFromAdapter(Context context, List<? extends ScModel> data, int position, Uri uri, Screen screen) {
        if (position >= data.size() || !(data.get(position) instanceof PlayableHolder)) {
            throw new AssertionError("Invalid item " + position + ", must be a playable");
        }

        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
        if (playable instanceof Track) {

            final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
            final int adjustedPosition = Collections2.filter(data.subList(0, position), PLAYABLE_HOLDER_PREDICATE).size();

            if (uri != null){
                playFromUri(context, uri, position, (Track) playable, playSessionSource);
            } else {
                playFromIdList(context, getPlayableIdsFromModels(data), adjustedPosition, (Track) playable, playSessionSource);
            }

        } else if (playable instanceof Playlist) {
            PlaylistDetailActivity.start(context, (Playlist) playable, modelManager, screen);
        } else {
            throw new AssertionError("Unexpected playable type");
        }
    }

    public void startPlaybackWithRecommendations(final Context context, Track track, Screen screen) {
        modelManager.cache(track);
        startPlaybackWithRecommendations(context, track.getId(), screen);
    }

    public void startPlaybackWithRecommendations(final Context context, long id, Screen screen) {
        startPlaySession(context, Lists.newArrayList(id), 0, new PlaySessionSource(screen), true);
    }

    public void togglePlayback(final Context context) {
        context.startService(new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
    }

    public void playCurrent(final Context context) {
        context.startService(new Intent(PlaybackService.Actions.PLAY_CURRENT));
    }

    public void setPlayQueuePosition(int position) {
        playQueueManager.setPosition(position);
    }

    public void previousTrack() {
        playQueueManager.previousTrack();
    }

    public void nextTrack() {
        playQueueManager.nextTrack();
    }

    public void playFromIdListShuffled(final Context context, List<Long> ids, Screen screen) {
        List<Long> shuffled = Lists.newArrayList(ids);
        Collections.shuffle(shuffled);
        startPlaySession(context, shuffled, 0, new PlaySessionSource(screen));
        gotoPlayer(context, shuffled.get(0));
    }

    private ArrayList<Long> getPlayableIdsFromModels(List<? extends ScModel> data) {
        final Iterable<? extends ScModel> playables = Iterables.filter(data, PLAYABLE_HOLDER_PREDICATE);
        Iterable<Long> trackIds = Iterables.transform(playables, new Function<ScModel, Long>() {
            @Override
            public Long apply(ScModel input) {
                return ((PlayableHolder) input).getPlayable().getId();
            }
        });
        return Lists.newArrayList(trackIds);
    }

    private void playFromUri(final Context context, Uri uri, final int startPosition, final Track initialTrack,
                             final PlaySessionSource playSessionSource) {
        showPlayer(context, initialTrack);

        if (shouldChangePlayQueue(initialTrack, playSessionSource)) {
            trackStorage.getTrackIdsForUriAsync(uri).subscribe(new DefaultSubscriber<List<Long>>() {
                @Override
                public void onNext(List<Long> idList) {

                    final int updatedPosition = correctStartPositionAndDeduplicateList(idList, startPosition, initialTrack);
                    PlayQueue playQueue = PlayQueue.fromIdList(idList, updatedPosition, playSessionSource);
                    playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
                    playCurrent(context);
                }
            });
        }
    }

    private void playFromIdList(Context context, List<Long> idList, int startPosition, Track initialTrack,
                                PlaySessionSource playSessionSource) {
        playFromIdList(context, idList, startPosition, initialTrack, playSessionSource, false);
    }

    private void playFromIdList(Context context, List<Long> idList, int startPosition, Track initialTrack,
                                PlaySessionSource playSessionSource, boolean loadRelated) {
        showPlayer(context, initialTrack);

        if (shouldChangePlayQueue(initialTrack, playSessionSource)) {
            final int adjustedPosition = getDeduplicatedIdList(idList, startPosition);
            startPlaySession(context, idList, adjustedPosition, playSessionSource, loadRelated);
        }
    }

    private void showPlayer(Context context, Track initialTrack) {
        modelManager.cache(initialTrack);
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forPlayTriggered());
        } else {
            gotoPlayer(context, initialTrack.getId());
        }
    }

    private void gotoPlayer(Context context, long initialTrackId) {
        Intent playerActivityIntent = new Intent(Actions.PLAYER)
                .putExtra(Track.EXTRA_ID, initialTrackId)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(playerActivityIntent);
    }

    private boolean shouldChangePlayQueue(Track track, PlaySessionSource playSessionSource){
        return playQueueManager.getCurrentTrackId() != track.getId() ||
                !playSessionSource.getOriginScreen().equals(playQueueManager.getScreenTag()) ||
                !playQueueManager.isCurrentPlaylist(playSessionSource.getPlaylistId());
    }

    private void startPlaySession(Context context, final List<Long> trackList, int startPosition,
                                PlaySessionSource playSessionSource) {
        startPlaySession(context, trackList, startPosition, playSessionSource, false);
    }

    private void startPlaySession(Context context, final List<Long> trackList, int startPosition,
                                    PlaySessionSource playSessionSource, boolean loadRecommended) {

        PlayQueue playQueue = PlayQueue.fromIdList(trackList, startPosition, playSessionSource);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        playCurrent(context);

        if (loadRecommended){
            playQueueManager.fetchRelatedTracks(playQueue.getCurrentTrackId());
        }
    }

    private int correctStartPositionAndDeduplicateList(List<Long> idList, int startPosition, final Track initialTrack) {
        final int updatedPosition;
        if (startPosition < idList.size() &&
                idList.get(startPosition) == initialTrack.getId()) {
            updatedPosition = startPosition;
        } else {
            updatedPosition = Iterables.indexOf(idList, new Predicate<Long>() {
                @Override
                public boolean apply(Long input) {
                    return input == initialTrack.getId();
                }
            });
        }

        return getDeduplicatedIdList(idList, updatedPosition);
    }

    /**
     * Remove duplicates from playqueue, preserving the ordering with regards to the item they clicked on
     * Returns the new startPosition
     */
    private int getDeduplicatedIdList(List<Long> idList, int startPosition){
        final Set <Long> seenIds = Sets.newHashSetWithExpectedSize(idList.size());
        final long playedId = idList.get(startPosition);

        int i = 0;
        Iterator<Long> iter = idList.iterator();
        while (iter.hasNext()) {
            final long val = iter.next().longValue();
            if (i != startPosition && (seenIds.contains(val) || val == playedId)) {
                iter.remove();
                if (i < startPosition) startPosition--;
            } else {
                seenIds.add(val);
                i++;
            }
        }
        return startPosition;
    }

    public enum AppendState {
        IDLE, LOADING, ERROR, EMPTY;
    }

    public @Nullable Intent getServiceBasedUpIntent() {
        final String originScreen = playQueueManager.getScreenTag();
        if (ScTextUtils.isBlank(originScreen)){
            return null; // might have come from widget and the play queue is empty
        }

        if (playQueueManager.isPlaylist()) {
            return getUpDestinationFromPlaylist(playQueueManager.getPlaylistId(), originScreen);
        } else {
            return Screen.getUpDestinationFromScreenTag(originScreen);
        }
    }

    private Intent getUpDestinationFromPlaylist(long playlistId, String originScreen) {
        final Screen screen = Screen.fromScreenTag(originScreen);
        final Intent upIntent = PlaylistDetailActivity.getIntent(Urn.forPlaylist(playlistId), screen);
        upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return upIntent;
    }

    public Observable<TrackUrn> logPlay(final TrackUrn urn){
        final APIRequest apiRequest = buildRequestForLoggingPlay(urn);
        return rxHttpClient.fetchResponse(apiRequest).map(new Func1<APIResponse, TrackUrn>() {
            @Override
            public TrackUrn call(APIResponse apiResponse) {
                return urn;
            }
        });
    }

    private APIRequest buildRequestForLoggingPlay(final TrackUrn trackUrn) {
        final String endpoint = String.format(APIEndpoints.LOG_PLAY.path(), trackUrn.toEncodedString());
        SoundCloudAPIRequest.RequestBuilder builder = SoundCloudAPIRequest.RequestBuilder.post(endpoint);
        builder.addQueryParameters(PARAM_CLIENT_ID, httpProperties.getClientId());
        return builder.forPrivateAPI(1).build();
    }

}
