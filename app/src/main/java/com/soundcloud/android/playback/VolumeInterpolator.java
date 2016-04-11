package com.soundcloud.android.playback;

// Note: This is a simplified version of AccelerateInterpolator,
// DecelerateInterpolator and AccelerateDecelerateInterpolator
// without native code implementations

enum VolumeInterpolator {
    LINEAR, ACCELERATE, ACCELERATE_DECELERATE, DECELERATE;

    float interpolate(float input) {
        switch (this) {
            case ACCELERATE:
                return input * input;
            case DECELERATE:
                return (1f - (1f - input) * (1f - input));
            case ACCELERATE_DECELERATE:
                return (float) (Math.cos((input + 1f) * Math.PI) / 2f) + .5f;
            case LINEAR:
            default:
                return input;
        }
    }

    float range(float input, float start, float end) {
        float fraction = interpolate(input);

        if (start >= end) {
            return ((1 - fraction) * (start - end)) + end;
        } else {
            return (fraction * (end - start)) + start;
        }
    }

}
