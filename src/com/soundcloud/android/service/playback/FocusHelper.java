package com.soundcloud.android.service.playback;


import com.soundcloud.android.service.RemoteControlReceiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class FocusHelper {
    public interface MusicFocusable {
        public void focusGained();
        public void focusLost(boolean isTransient, boolean canDuck);
    }

    private static final String TAG = CloudPlaybackService.TAG;

    private AudioManager mAudioManager;
    private Object mAudioFocusChangeListener;
    private MusicFocusable mMusicFocusable;
    private boolean mAudioFocusLost = false;

    private static Class<? extends BroadcastReceiver> RECEIVER = RemoteControlReceiver.class;


    // Backwards compatibility code (methods available as of SDK Level 8)
    static {
        initializeRemoteControlRegistrationMethods();
        initializeStaticCompat();
    }

    public FocusHelper(Context context, MusicFocusable musicFocusable) {
        if (sClassOnAudioFocusChangeListener != null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            mMusicFocusable = musicFocusable;
            createAudioFocusChangeListener();
        }
    }


    // http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
    private static void initializeRemoteControlRegistrationMethods() {
        try {
            if (sRegisterMediaButtonEventReceiver == null) {
                sRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "registerMediaButtonEventReceiver",
                        new Class[]{ComponentName.class});
            }
            if (sUnregisterMediaButtonEventReceiver == null) {
                sUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "unregisterMediaButtonEventReceiver",
                        new Class[]{ComponentName.class});
            }
        } catch (NoSuchMethodException ignored) {
            // Android < 2.2
        }
    }

    public static void registerHeadphoneRemoteControl(Context context) {
        if (sRegisterMediaButtonEventReceiver == null) return;
        try {
            sRegisterMediaButtonEventReceiver.invoke(
                    context.getSystemService(Context.AUDIO_SERVICE),
                    new ComponentName(context, RECEIVER));
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected", ie);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static void unregisterRemoteControl(Context context) {
        if (sUnregisterMediaButtonEventReceiver == null) return;
        try {
            sUnregisterMediaButtonEventReceiver.invoke(
                    context.getSystemService(Context.AUDIO_SERVICE),
                    new ComponentName(context, RECEIVER));
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected", ie);
        }
    }


    public boolean isSupported() {
         return (sClassOnAudioFocusChangeListener != null);
     }

     public int requestMusicFocus() {
         final int ret = requestAudioFocusCompat(mAudioManager, mAudioFocusChangeListener,
                 AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

         if (Log.isLoggable(CloudPlaybackService.TAG, Log.DEBUG)) {
             Log.d(TAG, "requestMusicFocus() => "+ret);
         }

         if (mMusicFocusable instanceof Context) {
            registerHeadphoneRemoteControl((Context) mMusicFocusable);
         }
         return ret;
     }

     public int abandonMusicFocus() {
         final int ret = abandonAudioFocusCompat(mAudioManager, mAudioFocusChangeListener);
         if (Log.isLoggable(CloudPlaybackService.TAG, Log.DEBUG)) {
             Log.d(TAG, "abandonMusicFocus() => "+ret);
         }

         if (mMusicFocusable instanceof Context) {
            unregisterRemoteControl((Context) mMusicFocusable);
         }
         return ret;
     }

     public void createAudioFocusChangeListener() {
         if (sClassOnAudioFocusChangeListener == null) return;

         mAudioFocusChangeListener = Proxy.newProxyInstance(AudioManager.class.getClassLoader(),
                 new Class[]{sClassOnAudioFocusChangeListener},
                 new InvocationHandler() {
                     public Object invoke(Object proxy, Method method, Object[] args) {
                         if (!method.getName().equals("onAudioFocusChange"))
                             return null;

                         if (Log.isLoggable(TAG, Log.DEBUG)) {
                             Log.d(TAG, "onAudioFocusChange("+ Arrays.asList(args)+")");
                         }

                         int focusChange = (Integer) args[0];
                         if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                             if (mAudioFocusLost) {
                                 mMusicFocusable.focusGained();
                                 mAudioFocusLost = false;
                             }
                         } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                             mAudioFocusLost = true;
                             mMusicFocusable.focusLost(false, false);
                         } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                             mAudioFocusLost = true;
                             mMusicFocusable.focusLost(true, false);
                         } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                             mAudioFocusLost = true;
                             mMusicFocusable.focusLost(true, true);
                         }
                         return null;
                     }
                 });
     }


    @SuppressWarnings("rawtypes")
    static Class sClassOnAudioFocusChangeListener;
    static Method sMethodRequestAudioFocus;
    static Method sMethodAbandonAudioFocus;
    static Method sRegisterMediaButtonEventReceiver;
    static Method sUnregisterMediaButtonEventReceiver;

    private static void initializeStaticCompat() {
        try {
            sClassOnAudioFocusChangeListener = Class.forName("android.media.AudioManager$OnAudioFocusChangeListener");
            sMethodRequestAudioFocus = AudioManager.class.getMethod(
                    "requestAudioFocus",
                    new Class[]{sClassOnAudioFocusChangeListener, int.class, int.class});
            sMethodAbandonAudioFocus = AudioManager.class.getMethod(
                    "abandonAudioFocus",
                    new Class[]{sClassOnAudioFocusChangeListener});
        } catch (ClassNotFoundException e) {
            // Silently fail when running on an OS before SDK level 8.
        } catch (NoSuchMethodException e) {
            // Silently fail when running on an OS before SDK level 8.
        } catch (IllegalArgumentException e) {
            // Silently fail when running on an OS before SDK level 8.
        } catch (SecurityException e) {
            // Silently fail when running on an OS before SDK level 8.
        }
    }

    private static int requestAudioFocusCompat(AudioManager audioManager,
                                                Object focusChangeListener, int stream, int durationHint) {
        if (sMethodRequestAudioFocus == null)
            return AudioManager.AUDIOFOCUS_REQUEST_FAILED;

        try {
            Object[] args = new Object[3];
            args[0] = focusChangeListener;
            args[1] = stream;
            args[2] = durationHint;
            Object ret = sMethodRequestAudioFocus.invoke(audioManager, args);
            if (ret instanceof Integer) {
                return (Integer) ret;
            } else {
                return AudioManager.AUDIOFOCUS_REQUEST_FAILED;
            }
        } catch (InvocationTargetException e) {
            // Unpack original exception when possible
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                // Unexpected checked exception; wrap and re-throw
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException invoking requestAudioFocus.");
            return AudioManager.AUDIOFOCUS_REQUEST_FAILED;
        }
    }

    private static int abandonAudioFocusCompat(AudioManager audioManager,
                                                Object focusChangeListener) {
        if (sMethodAbandonAudioFocus == null)
            return AudioManager.AUDIOFOCUS_REQUEST_FAILED;

        try {
            Object ret = sMethodAbandonAudioFocus.invoke(audioManager, focusChangeListener);
            if (ret instanceof Integer) {
                return (Integer) ret;
            } else {
                return AudioManager.AUDIOFOCUS_REQUEST_FAILED;
            }
        } catch (InvocationTargetException e) {
            // Unpack original exception when possible
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                // Unexpected checked exception; wrap and re-throw
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException invoking abandonAudioFocus.");
            return AudioManager.AUDIOFOCUS_REQUEST_FAILED;
        }
    }
}
