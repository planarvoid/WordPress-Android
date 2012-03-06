package com.soundcloud.android.service.playback;


import com.soundcloud.android.model.Track;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class AudioManagerHelper {

    public final static int PLAYSTATE_STOPPED            = 1;
    public final static int PLAYSTATE_PAUSED             = 2;
    public final static int PLAYSTATE_PLAYING            = 3;

    public final static int FLAG_KEY_MEDIA_PREVIOUS = 1 << 0;
    public final static int FLAG_KEY_MEDIA_REWIND = 1 << 1;
    public final static int FLAG_KEY_MEDIA_PLAY = 1 << 2;
    public final static int FLAG_KEY_MEDIA_PLAY_PAUSE = 1 << 3;
    public final static int FLAG_KEY_MEDIA_PAUSE = 1 << 4;
    public final static int FLAG_KEY_MEDIA_STOP = 1 << 5;
    public final static int FLAG_KEY_MEDIA_FAST_FORWARD = 1 << 6;

    public final static int FLAG_KEY_MEDIA_NEXT = 1 << 7;

     public final static int BITMAP_KEY_ARTWORK = 100;

    public interface MusicFocusable {
        public void focusGained();
        public void focusLost(boolean isTransient, boolean canDuck);
    }

    private static final String TAG = CloudPlaybackService.TAG;

    private AudioManager mAudioManager;
    private Object mAudioFocusChangeListener, mRemoteControlClient;
    private MusicFocusable mMusicFocusable;
    private boolean mAudioFocusLost = false;
    private Track mCurrentTrack;

    public static Class<? extends BroadcastReceiver> RECEIVER = RemoteControlReceiver.class;

    @SuppressWarnings("rawtypes")
    static Class sClassOnAudioFocusChangeListener;
    static Method sMethodRequestAudioFocus;
    static Method sMethodAbandonAudioFocus;
    static Method sRegisterMediaButtonEventReceiver;
    static Method sUnregisterMediaButtonEventReceiver;
    static Method sRegisterRemoteControlClient;
    static Method sUnregisterRemoteControlClient;

    @SuppressWarnings("rawtypes")
    static Class sClassRemoteControlClient;
    static Method sMethodSetPlaybackState;
    static Method sMethodSetTransportControlFlags;

    static Class sClassRemoteControlClientMetadataEditor;
    static Method sMethodRemoteControlClientMetadataEditorPutString;
    static Method sMethodRemoteControlClientMetadataEditorPutBitmap;
    static Method sMethodRemoteControlClientMetadataEditorPutLong;
    static Method sMethodRemoteControlClientEditMetaData;
    static Method sMethodRemoteControlClientMetadataEditorApply;

    // Backwards compatibility code (methods available as of SDK Level 8)
    static {
        initializeStaticCompat();
        initializeRemoteControlRegistrationMethods();
    }

    public AudioManagerHelper(Context context, MusicFocusable musicFocusable) {
        if (sClassOnAudioFocusChangeListener != null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            mMusicFocusable = musicFocusable;
            mAudioFocusChangeListener = createAudioFocusChangeListener();
        }
    }



    public void setPlaybackState(boolean isPlaying) {
        setPlaybackStateCompat(mRemoteControlClient, isPlaying ? PLAYSTATE_PLAYING : PLAYSTATE_PAUSED);
    }


    public void getRemoteControlClient() {
        if (mRemoteControlClient == null) {
            mRemoteControlClient = registerRemoteControlClient(((Context) mMusicFocusable).getApplicationContext());
            if (mRemoteControlClient != null) {
                int flags = FLAG_KEY_MEDIA_PREVIOUS
                        | FLAG_KEY_MEDIA_NEXT
                        | FLAG_KEY_MEDIA_PLAY
                        | FLAG_KEY_MEDIA_PAUSE
                        | FLAG_KEY_MEDIA_PLAY_PAUSE
                        | FLAG_KEY_MEDIA_STOP;
                setTransportControlFlagsCompat(mRemoteControlClient, flags);
            }
        }
    }

    protected void applyRemoteMetadata(final Context context, final Track track) {
        applyRemoteMetadata(context,track,null);
    }

    protected void applyRemoteMetadata(final Context context, final Track track, final Bitmap bitmap) {
        mCurrentTrack = track;
        if (mRemoteControlClient == null) getRemoteControlClient();
        setRemoteMetadataCompat(mRemoteControlClient,track, bitmap);
    }

    public boolean isSupported() {
        return (sClassOnAudioFocusChangeListener != null);
    }

    public int requestMusicFocus() {
         final int ret = requestAudioFocusCompat(mAudioManager, mAudioFocusChangeListener,
                 AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (mRemoteControlClient == null) getRemoteControlClient();

         if (Log.isLoggable(CloudPlaybackService.TAG, Log.DEBUG)) {
             Log.d(TAG, "requestMusicFocus() => " + ret);
         }
         return ret;
     }

    public int abandonMusicFocus(boolean isTemporary) {
         final int ret = abandonAudioFocusCompat(mAudioManager, mAudioFocusChangeListener);
         if (Log.isLoggable(CloudPlaybackService.TAG, Log.DEBUG)) {
             Log.d(TAG, "abandonMusicFocus() => "+ret);
         }

         // only unregister headphone control on stop, not on pause
         if (!isTemporary && mMusicFocusable instanceof Context) {
            unregisterRemoteControl(((Context) mMusicFocusable).getApplicationContext(), mRemoteControlClient);
         }
         return ret;
     }



    public Object createAudioFocusChangeListener() {
        if (sClassOnAudioFocusChangeListener == null) return null;
        return Proxy.newProxyInstance(AudioManager.class.getClassLoader(),
            new Class[]{ sClassOnAudioFocusChangeListener },
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) {

                    if (!method.getName().equals("onAudioFocusChange"))
                        return null;

                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onAudioFocusChange("+ Arrays.toString(args)+")");
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


    // http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
    private static void initializeRemoteControlRegistrationMethods() {
        try {
            if (sRegisterMediaButtonEventReceiver == null) {
                sRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "registerMediaButtonEventReceiver",
                        new Class[]{ComponentName.class});
            }

            if (sRegisterRemoteControlClient == null && sClassRemoteControlClient != null) {
                sRegisterRemoteControlClient = AudioManager.class.getMethod(
                        "registerRemoteControlClient",
                        new Class[]{sClassRemoteControlClient});
            }

            if (sUnregisterMediaButtonEventReceiver == null) {
                sUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "unregisterMediaButtonEventReceiver",
                        new Class[]{ComponentName.class});
            }

             if (sUnregisterRemoteControlClient == null) {
                sUnregisterRemoteControlClient = AudioManager.class.getMethod(
                        "unregisterRemoteControlClient",
                        new Class[]{ComponentName.class});
            }
        } catch (NoSuchMethodException ignored) {
            // Android < 2.2
        }
    }



    public static Object registerRemoteControlClient(Context context) {
        if (sRegisterMediaButtonEventReceiver == null) return null;
        try {
            ComponentName rec = new ComponentName(context, RECEIVER);
            sRegisterMediaButtonEventReceiver.invoke(
                    context.getSystemService(Context.AUDIO_SERVICE), rec);

            if (sClassRemoteControlClient == null || sRegisterRemoteControlClient == null) return null;

            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setComponent(rec);

            PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context,
                         0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Object client = createRemoteControlClient(mediaPendingIntent);
            sRegisterRemoteControlClient.invoke(context.getSystemService(Context.AUDIO_SERVICE), client);

            return client;

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
        return null;
    }

    private static Object createRemoteControlClient(PendingIntent pendingIntent) {
        if (sClassRemoteControlClient == null) return null;
        try {
            Constructor<?> c = sClassRemoteControlClient.getDeclaredConstructor(PendingIntent.class);
            c.setAccessible(true);
            return c.newInstance(new Object[]{pendingIntent});

        } catch (NoSuchMethodException e) {
            Log.e(TAG, "unexpected", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "unexpected", e);
        } catch (InstantiationException e) {
            Log.e(TAG, "unexpected", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "unexpected", e);
        }
        return null;
    }

    public static void unregisterRemoteControl(Context context, Object remoteControlClient) {
        if (sUnregisterMediaButtonEventReceiver == null || remoteControlClient == null) return;
        try {
            sUnregisterMediaButtonEventReceiver.invoke(
                    context.getSystemService(Context.AUDIO_SERVICE),
                    new ComponentName(context, RECEIVER));
            if (remoteControlClient != null){
                sUnregisterRemoteControlClient.invoke(
                        context.getSystemService(Context.AUDIO_SERVICE),
                        remoteControlClient);
            }
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

    private static void initializeStaticCompat() {
        try {
            sClassOnAudioFocusChangeListener = Class.forName("android.media.AudioManager$OnAudioFocusChangeListener");
            sMethodRequestAudioFocus = AudioManager.class.getMethod(
                    "requestAudioFocus",
                    new Class[]{sClassOnAudioFocusChangeListener, int.class, int.class});
            sMethodAbandonAudioFocus = AudioManager.class.getMethod(
                    "abandonAudioFocus",
                    new Class[]{sClassOnAudioFocusChangeListener});

            sClassRemoteControlClient = Class.forName("android.media.RemoteControlClient");
            sMethodSetPlaybackState = sClassRemoteControlClient.getMethod("setPlaybackState",new Class[]{Integer.TYPE});
            sMethodSetTransportControlFlags = sClassRemoteControlClient.getMethod("setTransportControlFlags",new Class[]{Integer.TYPE});

            sClassRemoteControlClientMetadataEditor = Class.forName("android.media.RemoteControlClient$MetadataEditor");

            sMethodRemoteControlClientEditMetaData = sClassRemoteControlClient.getMethod("editMetadata",new Class[]{Boolean.TYPE});
            sMethodRemoteControlClientMetadataEditorPutString = sClassRemoteControlClientMetadataEditor.getMethod("putString",new Class[]{Integer.TYPE, String.class});
            sMethodRemoteControlClientMetadataEditorPutLong = sClassRemoteControlClientMetadataEditor.getMethod("putLong",new Class[]{Integer.TYPE, Long.TYPE});
            sMethodRemoteControlClientMetadataEditorPutBitmap = sClassRemoteControlClientMetadataEditor.getMethod("putBitmap",new Class[]{Integer.TYPE, Bitmap.class});
            sMethodRemoteControlClientMetadataEditorApply = sClassRemoteControlClientMetadataEditor.getMethod("apply");

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
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED;

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
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private static int abandonAudioFocusCompat(AudioManager audioManager,
                                                Object focusChangeListener) {
        if (sMethodAbandonAudioFocus == null)
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED;

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
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private static void setPlaybackStateCompat(Object remoteControlClient, int i) {
        if (sMethodSetPlaybackState == null) return;

        try {
            Object ret = sMethodSetPlaybackState.invoke(remoteControlClient, i);
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
            Log.e(TAG, "IllegalAccessException invoking setPlaybackState.");
        }
        return;
    }

    private static void setTransportControlFlagsCompat(Object remoteControlClient, int flags) {
        if (sMethodSetTransportControlFlags == null) return;

        try {
            sMethodSetTransportControlFlags.invoke(remoteControlClient, flags);
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
            Log.e(TAG, "IllegalAccessException invoking setPlaybackState.");
        }
    }

    private static void setRemoteMetadataCompat(Object remoteControlClient, Track track, Bitmap bitmap) {

        if (remoteControlClient == null
                || sClassRemoteControlClientMetadataEditor == null
                || sMethodRemoteControlClientEditMetaData == null
                || sMethodRemoteControlClientMetadataEditorApply == null) return;


        try {
            Object editor = sMethodRemoteControlClientEditMetaData.invoke(remoteControlClient,true);

            if (sMethodRemoteControlClientMetadataEditorPutString != null){
                sMethodRemoteControlClientMetadataEditorPutString.invoke(editor,MediaMetadataRetriever.METADATA_KEY_TITLE, track.title);
                sMethodRemoteControlClientMetadataEditorPutString.invoke(editor, MediaMetadataRetriever.METADATA_KEY_ARTIST, track.user.username);
            }

            if (sMethodRemoteControlClientMetadataEditorPutLong != null){
                sMethodRemoteControlClientMetadataEditorPutLong.invoke(editor, MediaMetadataRetriever.METADATA_KEY_DURATION, track.duration);
            }

            if (sMethodRemoteControlClientMetadataEditorPutBitmap != null){
                sMethodRemoteControlClientMetadataEditorPutBitmap.invoke(editor, BITMAP_KEY_ARTWORK, bitmap);
            }

            sMethodRemoteControlClientMetadataEditorApply.invoke(editor);

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
            Log.e(TAG, "IllegalAccessException invoking setPlaybackState.");
        }
    }
}
