package com.soundcloud.android.service.playback;

import android.content.Context;
import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class AudioManagerFactory {
    private AudioManagerFactory() {}

    static IAudioManager createAudioManager(Context context) {
        IAudioManager manager = null;
        if (Build.VERSION.SDK_INT >= 8) {
            try {
                Class klass = Class.forName("com.soundcloud.android.service.playback.FroyoAudioManager");
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
