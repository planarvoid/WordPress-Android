package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AudioAdSource;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.java.collections.Iterables;

import android.net.Uri;

import javax.inject.Inject;

public class HlsStreamUrlBuilder {

    private static final String PARAM_CAN_SNIP = "can_snip";
    private static final String QUERY_PARAM_KEY_FORMAT = "format";
    private static final String HLS_OPUS_64_FORMAT = "hls_opus_64_url";
    private static final String HLS_MP3_128_FORMAT = "hls_mp3_128_url";

    private final AccountOperations accountOperations;
    private final SecureFileStorage secureFileStorage;
    private final ApiUrlBuilder urlBuilder;

    @Inject
    public HlsStreamUrlBuilder(AccountOperations accountOperations,
                               SecureFileStorage secureFileStorage,
                               ApiUrlBuilder urlBuilder) {
        this.accountOperations = accountOperations;
        this.secureFileStorage = secureFileStorage;
        this.urlBuilder = urlBuilder;
    }

    public String buildStreamUrl(PlaybackItem playbackItem) {
        checkState(accountOperations.isUserLoggedIn(), "SoundCloud User account does not exist");

        switch (playbackItem.getPlaybackType()) {
            case AUDIO_OFFLINE:
                return secureFileStorage.getFileUriForOfflineTrack(playbackItem.getUrn()).toString();
            case AUDIO_AD:
                return buildAudioAdUrl((AudioAdPlaybackItem) playbackItem);
            default:
                return buildRemoteUrl(playbackItem.getUrn(), playbackItem.getPlaybackType());
        }
    }

    public String buildStreamUrl(PreloadItem preloadItem) {
        return buildRemoteUrl(preloadItem.getUrn(), preloadItem.getPlaybackType());
    }

    private String buildRemoteUrl(Urn urn, PlaybackType playType) {
        if (playType == PlaybackType.AUDIO_SNIPPET) {
            return getApiUrlBuilder(urn, ApiEndpoints.HLS_SNIPPET_STREAM).build();
        } else {
            return getApiUrlBuilder(urn, ApiEndpoints.HLS_STREAM).withQueryParam(PARAM_CAN_SNIP, false).build();
        }
    }

    private ApiUrlBuilder getApiUrlBuilder(Urn trackUrn, ApiEndpoints endpoint) {
        ApiUrlBuilder builder = urlBuilder.from(endpoint, trackUrn);
        if (accountOperations.hasValidToken()) {
            builder.withQueryParam(ApiRequest.Param.OAUTH_TOKEN, accountOperations.getSoundCloudToken().getAccessToken());
        }
        return builder;
    }

    private String buildAudioAdUrl(AudioAdPlaybackItem adPlaybackItem) {
        final AudioAdSource source = Iterables.find(adPlaybackItem.getSources(), AudioAdSource::isHls);
        return source.requiresAuth() ? buildAdHlsUrlWithAuth(source) : source.url();
    }

    private String buildAdHlsUrlWithAuth(AudioAdSource source) {
        Uri.Builder builder = Uri.parse(source.url()).buildUpon();

        if (accountOperations.hasValidToken()) {
            builder.appendQueryParameter(ApiRequest.Param.OAUTH_TOKEN.toString(), accountOperations.getSoundCloudToken().getAccessToken());
        }

        return builder.build().toString();
    }
}
