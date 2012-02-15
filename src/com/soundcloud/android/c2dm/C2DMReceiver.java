package com.soundcloud.android.c2dm;

import android.net.Uri;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.android.utils.CloudUtils;

import android.accounts.Account;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class C2DMReceiver extends BroadcastReceiver {
    public static final String TAG = C2DMReceiver.class.getSimpleName();

    public static final String PREF_REG_ID          = "c2dm.reg_id";
    public static final String PREF_DEVICE_URL      = "c2dm.device_url";
    public static final String PREF_REG_LAST_TRY    = "c2dm.last_try";
    public static final String PREF_REG_TO_DELETE   = "c2dm.to_delete";

    public static final String SENDER            = "android-c2dm@soundcloud.com";

    public static final String ACTION_REGISTER   = "com.google.android.c2dm.intent.REGISTER";
    public static final String ACTION_UNREGISTER = "com.google.android.c2dm.intent.UNREGISTER";

    public static final String ACTION_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";
    public static final String ACTION_RECEIVE      = "com.google.android.c2dm.intent.RECEIVE";


    public static final String C2DM_EXTRA_UNREGISTERED = "unregistered";
    public static final String C2DM_EXTRA_ERROR        = "error";
    public static final String C2DM_EXTRA_REG_ID       = "registration_id";

    // actual extras sent via push notifications
    public static final String SC_EXTRA_EVENT_TYPE     = "event_type";
    @SuppressWarnings("UnusedDeclaration")
    public static final String SC_URI                  = "uri";

    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onReceive(" + intent + ")");

        if (mWakeLock == null) {
            mWakeLock = makeLock(context);
        }

        mWakeLock.acquire();
        try {
            if (intent.getAction().equals(ACTION_REGISTRATION)) {
                final String error = intent.getStringExtra(C2DM_EXTRA_ERROR);
                if (error != null) {
                    onRegistrationError(context, intent, error);
                } else if (intent.hasExtra(C2DM_EXTRA_UNREGISTERED)) {
                    onUnregister(context, intent);
                } else {
                    onRegister(context, intent);
                }
            } else if (intent.getAction().equals(ACTION_RECEIVE)) {
                // actual c2dm message
                onReceiveMessage(context, intent);
            }
        } finally {
            mWakeLock.release();
        }
    }

    public static synchronized void register(Context context, User user) {
        if (!isEnabled()) return;
        final PowerManager.WakeLock lock = makeLock(context);
        final String regId = getRegistrationData(context, PREF_REG_ID);

        if (regId == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "registering " + user + " for c2dm");
            setRegistrationData(context, PREF_REG_LAST_TRY,
                    String.valueOf(System.currentTimeMillis()));

            Intent reg = new Intent(ACTION_REGISTER)
                    .putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0))
                    .putExtra("sender", SENDER);

            context.startService(reg);
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "device is already registered with " + regId);

            final String devUrl = getRegistrationData(context, PREF_DEVICE_URL);
            // make sure there is a server-side device registered
            if (devUrl == null) {
                sendRegId(context, regId, lock);
            } else {
                // would be good to have a way to make sure the devUrl is still valid -
                // however me/devices/id only supports POST/DELETE at the moment.
            }
        }
        // delete old device ids
        processDeletionQueue(context, lock);
    }

    public static synchronized void unregister(Context context) {
        if (!isEnabled()) return;

        clearRegistrationData(context);

        Intent unreg = new Intent(ACTION_UNREGISTER)
                .putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        context.startService(unreg);
    }


    /** callback when device successfully registered */
    private void onRegister(final Context context, Intent intent) {
        final String regId = intent.getStringExtra(C2DM_EXTRA_REG_ID);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onRegister(id=" + regId+")");

        if (regId != null) {
            // save the reg_id
            setRegistrationData(context, PREF_REG_ID, regId);
            sendRegId(context, regId, mWakeLock);
        } else {
            Log.w(TAG, "received registration intent without id");
        }
    }

    private static AsyncTask<String, Void, String> sendRegId(final Context context, String regId,
                                                             PowerManager.WakeLock lock) {

        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "sendRegId("+regId+")");
        return new SendRegIdTask((AndroidCloudAPI) context.getApplicationContext(), lock) {
            @Override
            protected void onPostExecute(String url) {
                if (url != null) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "device registered as " + url);
                    setRegistrationData(context, PREF_DEVICE_URL, url);
                } else {
                    Log.w(TAG, "device registration failed");

                    // registering failed, need to retry later
                    // mark the current device as unregistered by removing the url key
                    setRegistrationData(context, PREF_DEVICE_URL, null);
                }
            }
        }.execute(regId, CloudUtils.getPackagename(context), CloudUtils.getUniqueDeviceID(context));
    }

    /** callback when device is unregistered */
    private void onUnregister(final Context context, Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onUnregister(" + intent + ")");

        // clear local data
        setRegistrationData(context, PREF_DEVICE_URL, null);
        setRegistrationData(context, PREF_REG_ID, null);

        // clear remote state
        processDeletionQueue(context, mWakeLock);
    }


    @SuppressWarnings("UnusedParameters")
    private void onRegistrationError(Context context, Intent intent, String error) {
        Log.w(TAG, "error registering with C2DM: " + error);

        switch (RegError.fromString(error)) {
            case SERVICE_NOT_AVAILABLE:
                // back-off and retry later
                break;
            case ACCOUNT_MISSING:
                // user should add google account
                break;
            case TOO_MANY_REGISTRATIONS:
                break;
            case AUTHENTICATION_FAILED:
                // wrong username/pw
                break;
            case UNKNOWN_ERROR:
                Log.w(TAG, "unknown error: " +error);
               break;

            case INVALID_SENDER:
            case PHONE_REGISTRATION_ERROR:
            default:
                Log.w(TAG, "registration error: " +error);
        }
    }

    private void onReceiveMessage(Context context, Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onReceiveMessage(" + intent + ")");
        final Account account = SoundCloudApplication.fromContext(context).getAccount();

        if (account == null) {
            Log.w(TAG, "push event received but no account registered - ignoring");
        } else {
            final PushEvent event = PushEvent.fromIntent(intent);
            switch (event) {
                case LIKE:
                case COMMENT:
                case FOLLOWER:
                    Bundle extras = new Bundle();
                    extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, event.type);
                    if (intent.getExtras().containsKey(C2DMReceiver.SC_URI)){
                        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT_URI, intent.getExtras().getString(C2DMReceiver.SC_URI));
                    }
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "requesting sync (event="+event+")");
                    ContentResolver.requestSync(account, ScContentProvider.AUTHORITY, extras);
                    break;
                default:
                    // other types not handled yet
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "unhandled event "+event);
            }
        }
    }

    /* package */ static String getRegistrationData(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                       .getString(key, null);
    }

    /* package */ static boolean setRegistrationData(Context context, String key, String value) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        if (value == null) e.remove(key); else e.putString(key, value);
        return e.commit();
    }

    private static synchronized boolean queueForDeletion(Context context, final String url) {
        if (url != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "queued " + url + " for later deletion");
            }
            String _urls = getRegistrationData(context, PREF_REG_TO_DELETE);
            if (_urls != null) {
                return !_urls.contains(url) && setRegistrationData(context, PREF_REG_TO_DELETE, _urls + "," + url);
            } else {
                return setRegistrationData(context, PREF_REG_TO_DELETE, url);
            }
        } else return false;
    }

    private static synchronized boolean removeFromDeletionQueue(Context context, final String url) {
        final String _urls = getRegistrationData(context, PREF_REG_TO_DELETE);
        if (_urls != null && _urls.contains(url)) {
            String[] urls = _urls.split(",");
            if (urls.length == 1) {
                return setRegistrationData(context, PREF_REG_TO_DELETE, null);
            } else {
                List<String> newUrls = new ArrayList<String>(_urls.length()-1);
                for (String u : urls) if (!u.equals(url)) newUrls.add(u);
                return setRegistrationData(context, PREF_REG_TO_DELETE, TextUtils.join(",", newUrls));
            }
        } else return false;
    }

    /* package */ static synchronized boolean processDeletionQueue(Context context, PowerManager.WakeLock lock) {
        if (isConnected(context)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "processDeletionQueue()");
            String _urls = getRegistrationData(context, PREF_REG_TO_DELETE);
            if (_urls != null) {
                for (String url : _urls.split(",")) {
                    deleteDevice(context, lock, url);
                }
                return true;
            } else {
                return false;
            }
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "not connectected, skipping deletion queue process");
            return false;
        }
    }

    private static void deleteDevice(final Context context, PowerManager.WakeLock lock, final String url) {
        if (url != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "deleting " + url);

            new DeleteRegIdTask((AndroidCloudAPI) context.getApplicationContext(), lock) {
                @Override protected void onPostExecute(Boolean success) {
                    super.onPostExecute(success);
                    if (success) {
                        removeFromDeletionQueue(context, url);
                    }
                }
            }.execute(url);
        }
    }

    private static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    private static void clearRegistrationData(Context context) {
        // mark current device_url to be deleted
        queueForDeletion(context, getRegistrationData(context, PREF_DEVICE_URL));

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(PREF_REG_ID)
                .remove(PREF_REG_LAST_TRY)
                .remove(PREF_DEVICE_URL)
                .commit();
    }

    private static boolean isEnabled() {
        return Build.VERSION.SDK_INT >= 8;
    }


    private static PowerManager.WakeLock makeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C2DMReceiver.class.getSimpleName());
    }
}
