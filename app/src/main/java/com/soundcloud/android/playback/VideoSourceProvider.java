package com.soundcloud.android.playback;

import static android.media.CamcorderProfile.QUALITY_1080P;
import static android.media.CamcorderProfile.QUALITY_480P;
import static android.media.CamcorderProfile.QUALITY_720P;
import static com.soundcloud.android.playback.PlaybackConstants.MAX_BITRATE_KBPS_2G;
import static com.soundcloud.android.playback.PlaybackConstants.MAX_BITRATE_KBPS_4G;
import static com.soundcloud.android.playback.PlaybackConstants.MAX_BITRATE_KBPS_WIFI;
import static com.soundcloud.android.playback.PlaybackConstants.MAX_BITRATE_KPBS_3G;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_1080P;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_480P;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_720P;

import com.soundcloud.android.ads.VideoAdSource;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;

@Singleton
public class VideoSourceProvider {

    private final DeviceHelper deviceHelper;
    private final MediaCodecInfoProvider mediaCodecInfoProvider;
    private final NetworkConnectionHelper networkConnectionHelper;
    private final ApplicationProperties applicationProperties;

    private Optional<VideoAdSource> currentSource = Optional.absent();

    @Inject
    public VideoSourceProvider(DeviceHelper deviceHelper,
                               MediaCodecInfoProvider mediaCodecInfoProvider,
                               NetworkConnectionHelper networkConnectionHelper, ApplicationProperties applicationProperties) {
        this.deviceHelper = deviceHelper;
        this.mediaCodecInfoProvider = mediaCodecInfoProvider;
        this.networkConnectionHelper = networkConnectionHelper;
        this.applicationProperties = applicationProperties;
    }

    public Optional<VideoAdSource> getCurrentSource() {
        return currentSource;
    }

    public VideoAdSource selectOptimalSource(VideoAdPlaybackItem videoPlaybackItem) {
        final List<VideoAdSource> sources = videoPlaybackItem.getSortedSources();

        final Collection<VideoAdSource> hlsSources = MoreCollections.filter(sources, VideoAdSource::isHLS);
        final Collection<VideoAdSource> mp4Sources = MoreCollections.filter(sources, VideoAdSource::isMP4);

        final Optional<VideoAdSource> optimalHLS = Optional.fromNullable(Iterables.getFirst(hlsSources, null));
        final Optional<VideoAdSource> optimalMP4 = Optional.fromNullable(getOptimalMp4(mp4Sources));

        final boolean shouldReturnHLS = applicationProperties.canMediaPlayerSupportVideoHLS() && optimalHLS.isPresent();
        currentSource = shouldReturnHLS ? optimalHLS : optimalMP4;

        if (currentSource.isPresent()) {
            return currentSource.get();
        } else {
            throw new IllegalArgumentException("VideoAdPlaybackItem has no supported video source formats");
        }
    }

    private VideoAdSource getOptimalMp4(Collection<VideoAdSource> sources) {
        final Collection<VideoAdSource> supportedResolutions = getSupportedResolutions(sources);
        return supportedResolutions.isEmpty() ? Iterables.getFirst(sources, null)
                                              : getBestBitrate(supportedResolutions);
    }

    private Collection<VideoAdSource> getSupportedResolutions(Collection<VideoAdSource> sources) {
        final Predicate<VideoAdSource> resolutionPredicate = source -> {
            // Resolution refers to the smaller dimension. (e.g. 1080x1920 & 1920x1080 -> 1080p)
            final int resolution = Math.min(source.getHeight(), source.getWidth());
            return resolution <= maxResolutionForDevice();
        };
        return MoreCollections.filter(sources, resolutionPredicate);
    }

    private VideoAdSource getBestBitrate(Collection<VideoAdSource> sources) {
        final Predicate<VideoAdSource> bitratePredicate = source -> source.getBitRateKbps() <= maxBitrateForConnection();
        final Collection<VideoAdSource> validSources = MoreCollections.filter(sources, bitratePredicate);
        // Fallback to lowest of available bit rates
        return validSources.isEmpty() ? Iterables.getFirst(sources, null)
                                      : Iterables.getLast(validSources);
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
        } else return mediaCodecInfoProvider.maxResolutionSupportForAvcOnDevice();
    }

    private int maxBitrateForConnection() {
        switch (networkConnectionHelper.getCurrentConnectionType()) {
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
}
