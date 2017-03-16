package com.soundcloud.android.playback;

import android.annotation.TargetApi;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.os.Build;

import com.soundcloud.android.Consts;

import java.util.Arrays;

import javax.inject.Inject;

import static com.soundcloud.android.playback.PlaybackConstants.MIME_TYPE_AVC;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_1080P;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_360P;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_480P;
import static com.soundcloud.android.playback.PlaybackConstants.RESOLUTION_PX_720P;

class MediaCodecInfoProvider {

    @Inject
    public MediaCodecInfoProvider() {
        // for injection
    }

    public int maxResolutionSupportForAvcOnDevice() {
        final CodecCapabilities codecCapabilities = getDecoderCodecCapabilitiesForMimeType(MIME_TYPE_AVC);
        int highestSupportedAVCLevel = Consts.NOT_SET;

        if (codecCapabilities != null) {
            for (CodecProfileLevel profileLevel : codecCapabilities.profileLevels) {
                if (profileLevel.level > highestSupportedAVCLevel) {
                    highestSupportedAVCLevel = profileLevel.level;
                }
            }
        }
        return resolutionForAvcLevel(highestSupportedAVCLevel);
    }

    private int resolutionForAvcLevel(int level) {
        // (Codec level -> max resolution @ 30FPS) mapping from https://en.wikipedia.org/wiki/H.264/MPEG-4_AVC#Levels
        switch (level) {
            case Consts.NOT_SET: // Default to 360P for AVC levels 1 to 2 even though it isn't supported.
            case CodecProfileLevel.AVCLevel1:
            case CodecProfileLevel.AVCLevel11:
            case CodecProfileLevel.AVCLevel12:
            case CodecProfileLevel.AVCLevel13:
            case CodecProfileLevel.AVCLevel1b:
            case CodecProfileLevel.AVCLevel2:
            case CodecProfileLevel.AVCLevel21:
            case CodecProfileLevel.AVCLevel22:
                return RESOLUTION_PX_360P;
            case CodecProfileLevel.AVCLevel3:
                return RESOLUTION_PX_480P;
            case CodecProfileLevel.AVCLevel31:
            case CodecProfileLevel.AVCLevel32:
                return RESOLUTION_PX_720P;
            case CodecProfileLevel.AVCLevel4:
            default:
                return RESOLUTION_PX_1080P;
        }
    }

    private CodecCapabilities getDecoderCodecCapabilitiesForMimeType(String mimeType) {
        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder() && Arrays.asList(codecInfo.getSupportedTypes()).contains(mimeType)) {
                return codecInfo.getCapabilitiesForType(mimeType);
            }
        }
        return null;
    }

}
