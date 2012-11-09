package com.soundcloud.android.audio.managers;

import android.content.Context;
import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class AudioManagerFactory {
    private AudioManagerFactory() {
    }

    public static IAudioManager createAudioManager(Context context) {
        return new FroyoAudioManager(context);
    }

    public static IRemoteAudioManager createRemoteAudioManager(Context context) {
        IRemoteAudioManager manager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                Class klass = Class.forName("com.soundcloud.android.audio.managers.ICSRemoteAudioManager");
                Constructor ctor = klass.getConstructor(Context.class);
                manager = (IRemoteAudioManager) ctor.newInstance(context);
            } catch (ClassNotFoundException ignored) {
            } catch (InstantiationException ignored) {
            } catch (IllegalAccessException ignored) {
            } catch (NoSuchMethodException ignored) {
            } catch (InvocationTargetException ignored) {
            }
        }
        if (manager == null) {
            manager = new FroyoRemoteAudioManager(context);
        }
        return manager;
    }
}
