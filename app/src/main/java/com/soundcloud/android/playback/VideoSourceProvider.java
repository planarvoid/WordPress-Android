package com.soundcloud.android.playback;

import com.soundcloud.android.ads.VideoSource;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static android.media.CamcorderProfile.QUALITY_1080P;
import static android.media.CamcorderProfile.QUALITY_480P;
import static android.media.CamcorderProfile.QUALITY_720P;
import static com.soundcloud.android.playback.PlaybackConstants.MAX_BITRATE_KBPS_2G;
import static com.soundcloud.android.playback.PlaybackConstants.MAX_BITRATE_KPBS_3G;
import static com.soundcloud.android.playback.PlaybackConstants.MAX_BITRATE_KBPS_4G;
import static com.soundcloud.android.playback.PlaybackConstants.MAX_BITRATE_KBPS_WIFI;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_1080P;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_360P;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_480P;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_720P;

public class VideoSourceProvider {

    private static final List<String> SUPPORTED_FORMATS = Collections.singletonList(PlaybackConstants.MIME_TYPE_MP4);
    private static final Predicate<VideoSource> SUPPORTED_FORMAT_PREDICATE = new Predicate<VideoSource>() {
        @Override
        public boolean apply(VideoSource source) {
            return SUPPORTED_FORMATS.contains(source.getType());
        }
    };

    private final ApplicationProperties applicationProperties;
    private final DeviceHelper deviceHelper;
    private final MediaCodecInfoProvider mediaCodecInfoProvider;
    private final NetworkConnectionHelper networkConnectionHelper;

    @Inject
    public VideoSourceProvider(ApplicationProperties applicationProperties,
                               DeviceHelper deviceHelper,
                               MediaCodecInfoProvider mediaCodecInfoProvider,
                               NetworkConnectionHelper networkConnectionHelper) {
        this.applicationProperties = applicationProperties;
        this.deviceHelper = deviceHelper;
        this.mediaCodecInfoProvider = mediaCodecInfoProvider;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    public VideoSource selectOptimalSource(VideoAdPlaybackItem videoPlaybackItem) {
        final List<VideoSource> sources = new ArrayList<>(videoPlaybackItem.getSources());
        Collections.sort(sources, VideoSource.BITRATE_COMPARATOR);

        final Collection<VideoSource> supportedFormatSources = MoreCollections.filter(sources,
                                                                                      SUPPORTED_FORMAT_PREDICATE);
        if (!supportedFormatSources.isEmpty()) {
            final Collection<VideoSource> supportedResolutionSources = MoreCollections.filter(supportedFormatSources,
                                                                                              new SupportedResolutionPredicate(
                                                                                                      maxResolutionForDevice()));
            if (supportedResolutionSources.isEmpty()) {
                return Iterables.getFirst(supportedFormatSources, null);
            } else {
                return selectSuitableBitrate(supportedResolutionSources);
            }
        } else {
            throw new IllegalArgumentException("VideoAdPlaybackItem has no supported video source formats");
        }
    }

    private VideoSource selectSuitableBitrate(Collection<VideoSource> sources) {
        final int maxNetworkBitrate = maxBitrateForConnection(networkConnectionHelper.getCurrentConnectionType());
        final Collection<VideoSource> suitableBitrateSources = MoreCollections.filter(sources,
                                                                                      new SuitableBitratePredicate(
                                                                                              maxNetworkBitrate));

        if (suitableBitrateSources.isEmpty()) { // Fallback to lowest of available bit rates
            return Iterables.getFirst(sources, null);
        } else {
            return Iterables.getLast(suitableBitrateSources);
        }
    }

    // A device's available video recording profiles can be used as a proxy for media playback capabilities
    // as per http://developer.android.com/guide/appendix/media-formats.html
    private int maxResolutionForDevice() {
        if (deviceHelper.hasCamcorderProfile(QUALITY_1080P)) {
            return RESOLUTION_PX_1080P;
        } else if (deviceHelper.hasCamcorderProfile(QUALITY_720P)) {
            return RESOLUTION_PX_720P;
        } else if (deviceHelper.hasCamcorderProfile(QUALITY_480P)) {
            return RESOLUTION_PX_480P;
        } else if (applicationProperties.canAccessCodecInformation()) {
            return mediaCodecInfoProvider.maxResolutionSupportForAvcOnDevice();
        } else {
            return RESOLUTION_PX_360P;
        }
    }

    private int maxBitrateForConnection(ConnectionType connectionType) {
        switch (connectionType) {
            case WIFI:
                return MAX_BITRATE_KBPS_WIFI;
            case FOUR_G:
                return MAX_BITRATE_KBPS_4G;
            case THREE_G:
                return MAX_BITRATE_KPBS_3G;
            case TWO_G:
            case OFFLINE:
            case UNKNOWN:
            default:
                return MAX_BITRATE_KBPS_2G;
        }
    }

    private static class SupportedResolutionPredicate implements Predicate<VideoSource> {

        private final int maxResolution;

        SupportedResolutionPredicate(int maxResolution) {
            this.maxResolution = maxResolution;
        }

        @Override
        public boolean apply(VideoSource source) {
            // Resolution refers to the smaller dimension. (e.g. 1080x1920 & 1920x1080 -> 1080p)
            return Math.min(source.getHeight(), source.getWidth()) <= maxResolution;
        }
    }

    private static class SuitableBitratePredicate implements Predicate<VideoSource> {

        private final int maxBitrateKbps;

        SuitableBitratePredicate(int maxBitrateKbps) {
            this.maxBitrateKbps = maxBitrateKbps;
        }

        @Override
        public boolean apply(VideoSource source) {
            return source.getBitRateKbps() <= maxBitrateKbps;
        }
    }
}
