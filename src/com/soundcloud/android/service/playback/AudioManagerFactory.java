package com.soundcloud.android.service.playback;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class AudioManagerFactory {
    private AudioManagerFactory() {
    }

    public static IAudioManager createAudioManager(Context context) {
        IAudioManager manager = null;

        final int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= Build.VERSION_CODES.FROYO) {
            try {
                final String name = sdkInt >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ?
                        "com.soundcloud.android.service.playback.ICSAudioManager" :
                        "com.soundcloud.android.service.playback.FroyoAudioManager";

                Class klass = Class.forName(name);
                Constructor ctor = klass.getConstructor(Context.class);
                manager = (IAudioManager) ctor.newInstance(context);
            } catch (ClassNotFoundException ignored) {
            } catch (InstantiationException ignored) {
            } catch (IllegalAccessException ignored) {
            } catch (NoSuchMethodException ignored) {
            } catch (InvocationTargetException ignored) {
            }
        }
        // fallback
        if (manager == null) {
            manager = new FallbackAudioManager();
        }
        return manager;
    }
}
