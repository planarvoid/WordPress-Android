package com.soundcloud.android.audio.managers;

import android.content.Context;
import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class AudioManagerFactory {
    private AudioManagerFactory() {
    }

    public static IAudioManager createAudioManager(Context context) {
        IAudioManager manager = null;

        try {
            Class klass = Class.forName("com.soundcloud.android.audio.managers.FroyoAudioManager");
            Constructor ctor = klass.getConstructor(Context.class);
            manager = (IAudioManager) ctor.newInstance(context);
        } catch (ClassNotFoundException ignored) {
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException ignored) {
        }
        // fallback
        if (manager == null) {
            manager = new FallbackAudioManager(context);
        }
        return manager;
    }

    public static IRemoteAudioManager createRemoteAudioManager(Context context) {
        IRemoteAudioManager manager = null;

        try {
            final String name = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ?
                    "com.soundcloud.android.audio.managers.ICSRemoteAudioManager" :
                    "com.soundcloud.android.audio.managers.FroyoRemoteAudioManager";

            Class klass = Class.forName(name);
            Constructor ctor = klass.getConstructor(Context.class);
            manager = (IRemoteAudioManager) ctor.newInstance(context);
        } catch (ClassNotFoundException ignored) {
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException ignored) {
        }
        // fallback
        if (manager == null) {
            manager = new FallbackAudioManager(context);
        }
        return manager;
    }
}
