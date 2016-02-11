package com.soundcloud.android.playback;

import android.annotation.TargetApi;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;

import com.soundcloud.android.ads.VideoSource;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static android.media.CamcorderProfile.QUALITY_1080P;
import static android.media.CamcorderProfile.QUALITY_480P;
import static android.media.CamcorderProfile.QUALITY_720P;

public class VideoSourceProvider {

    static final int RESOLUTION_HEIGHT_PX_1080P = 1080;
    static final int RESOLUTION_HEIGHT_PX_720P = 720;
    static final int RESOLUTION_HEIGHT_PX_480P = 480;
    static final int RESOLUTION_HEIGHT_PX_360P = 360;

    static final String MP4_TYPE = "video/mp4"; // We only support H.264 AVC at the moment
    private static final List<String> SUPPORTED_FORMATS = Collections.singletonList(MP4_TYPE);
    private static final Predicate<VideoSource> UNSUPPORTED_FORMAT_PREDICATE = new Predicate<VideoSource>() {
        @Override
        public boolean apply(VideoSource source) {
            return !SUPPORTED_FORMATS.contains(source.getCodec());
        }
    };

    private final ApplicationProperties applicationProperties;
    private final DeviceHelper deviceHelper;
    private final NetworkConnectionHelper networkConnectionHelper;

    @Inject
    public VideoSourceProvider(ApplicationProperties applicationProperties,
                               DeviceHelper deviceHelper,
                               NetworkConnectionHelper networkConnectionHelper) {
        this.applicationProperties = applicationProperties;
        this.deviceHelper = deviceHelper;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    public VideoSource selectOptimalSource(VideoPlaybackItem videoPlaybackItem) {
        final List<VideoSource> sources = new ArrayList<>(videoPlaybackItem.getSources());
        Collections.sort(sources, VideoSource.BITRATE_COMPARATOR);

        final List<VideoSource> supportedFormatSources = filterList(sources, UNSUPPORTED_FORMAT_PREDICATE);
        if (!supportedFormatSources.isEmpty()) {
            final int maxDeviceResolution = maxResolutionForDevice();
            final List<VideoSource> supportedResolutionSources = filterList(supportedFormatSources, new MaxResolutionPredicate(maxDeviceResolution));
            return supportedResolutionSources.isEmpty() ? supportedFormatSources.get(0) : selectSuitableBitrate(supportedResolutionSources);
        } else {
            throw new IllegalArgumentException("VideoPlaybackItem has no supported video source formats");
        }
    }

    private VideoSource selectSuitableBitrate(List<VideoSource> sources) {
        final int maxNetworkResolution = maxResolutionForConnection(networkConnectionHelper.getCurrentConnectionType());
        final List<VideoSource> suitableBitrateSources = filterList(sources, new MaxResolutionPredicate(maxNetworkResolution));

        if (suitableBitrateSources.isEmpty()) {
            // Fallback to lowest of available bit rates
            return sources.get(0);
        } else {
            return Iterables.getLast(suitableBitrateSources);
        }
    }

    private List<VideoSource> filterList(List<VideoSource> sources, Predicate predicate) {
        final List<VideoSource> sourceListCopy = new ArrayList<>(sources);
        Iterables.removeIf(sourceListCopy, predicate);
        return sourceListCopy;
    }

    // A device's available video recording profiles can be used as a proxy for media playback capabilities
    // as per http://developer.android.com/guide/appendix/media-formats.html
    private int maxResolutionForDevice() {
        if (deviceHelper.hasCamcorderProfile(QUALITY_1080P)) {
            return RESOLUTION_HEIGHT_PX_1080P;
        } else if (deviceHelper.hasCamcorderProfile(QUALITY_720P)) {
            return RESOLUTION_HEIGHT_PX_720P;
        } else if (deviceHelper.hasCamcorderProfile(QUALITY_480P)) {
            return RESOLUTION_HEIGHT_PX_480P;
        } else if (applicationProperties.canAccessVideoCodecCapabilities()) {
            return maxResolutionForDeviceFromCodec();
        } else {
            return RESOLUTION_HEIGHT_PX_360P;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private int maxResolutionForDeviceFromCodec() {
        final MediaCodecInfo.VideoCapabilities videoCapabilities = VideoDecoderCodecCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);
        if (videoCapabilities != null) {
            final int maxHeightSupported = videoCapabilities.getSupportedHeights().getUpper();
            if (maxHeightSupported >= RESOLUTION_HEIGHT_PX_1080P) {
                return RESOLUTION_HEIGHT_PX_1080P;
            } else if (maxHeightSupported >= RESOLUTION_HEIGHT_PX_720P) {
                return RESOLUTION_HEIGHT_PX_720P;
            } else if (maxHeightSupported >= RESOLUTION_HEIGHT_PX_480P) {
                return RESOLUTION_HEIGHT_PX_480P;
            }
        }
        return RESOLUTION_HEIGHT_PX_360P;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static MediaCodecInfo.VideoCapabilities VideoDecoderCodecCapabilitiesForType(String mimeType) {
        final MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo codecInfo : mediaCodecList.getCodecInfos()) {
            if (!codecInfo.isEncoder()) {
                String[] types = codecInfo.getSupportedTypes();
                for (String type : types) {
                    if (type.equals(mimeType)) {
                        return codecInfo.getCapabilitiesForType(mimeType).getVideoCapabilities();
                    }
                }
            }
        }
        return null;
    }

    private int maxResolutionForConnection(ConnectionType connectionType) {
            switch (connectionType) {
                case WIFI:
                    return RESOLUTION_HEIGHT_PX_1080P;
                case FOUR_G:
                    return RESOLUTION_HEIGHT_PX_720P;
                case THREE_G:
                    return RESOLUTION_HEIGHT_PX_480P;
                case TWO_G:
                case OFFLINE:
                case UNKNOWN:
                default:
                    return RESOLUTION_HEIGHT_PX_360P;
            }
    }

    static class MaxResolutionPredicate implements Predicate<VideoSource> {

        private final int maxResolution;

        MaxResolutionPredicate(int maxResolution) {
            this.maxResolution = maxResolution;
        }

        @Override
        public boolean apply(VideoSource source) {
            return source.getHeight() > maxResolution;
        }
    }

}
