package com.soundcloud.android.playback;

import android.os.Build;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PlayerDeviceCompatibility {
    private static final String ONE_PLUS_CM = "bacon";
    private static final String LG_G3 = "g3";
    private static final String LG_OPTIMUS_G = "geehrc4g";
    private static final String LG_GPRO_LITE = "luv90ss";
    private static final String LG_L70 = "w5";
    private static final String LG_GVISTA = "b2l";
    private static final String LG_CONNECT_4G = "cayman";
    private static final String LG_VOLT = "x5";

    private static final Set<String> LG_DEVICES =
            new HashSet<>(Arrays.asList(LG_G3, LG_OPTIMUS_G, LG_GPRO_LITE, LG_L70, LG_GVISTA, LG_CONNECT_4G, LG_VOLT));

    private static final String MANUFACTURER_SAMSUNG = "samsung";

    public static boolean shouldForceMediaPlayer() {
        return (ONE_PLUS_CM.equalsIgnoreCase(Build.HARDWARE)
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) ||
                // All Samsung devices will be excluded until we completely fix the Exynos chip bug on skippy
                (MANUFACTURER_SAMSUNG.equalsIgnoreCase(Build.MANUFACTURER)) ||
                (MANUFACTURER_SAMSUNG.equalsIgnoreCase(Build.BRAND)) ||
                // These devices are known to have continuous play issues. Can be removed when fixed on skippy
                LG_DEVICES.contains(Build.DEVICE);
    }
}
