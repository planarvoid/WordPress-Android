package com.soundcloud.android.activity.auth;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Env;
import org.jetbrains.annotations.NotNull;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

/**
 * Facebook SSO based login. Most of this code is taken from the
 * <a href="https://github.com/facebook/facebook-android-sdk/">Facebook Android SDK </a>
 */
public class FacebookSSO extends AbstractLoginActivity {
    private static final String TAG = FacebookSSO.class.getSimpleName();

    /* package */ static final String FB_PERMISSION_EXTRA = "scope";
    private static final String FB_CLIENT_ID_EXTRA = "client_id";
    private static final String TOKEN = "access_token";
    private static final String EXPIRES = "expires_in";
    private static final String SINGLE_SIGN_ON_DISABLED = "service_disabled";


    // permissions used by SoundCloud (also backend) - email is required for successful signup
    private static final String[] DEFAULT_PERMISSIONS = {
        "publish_actions",
        "offline_access",   /* this is going to be deprecated soon */
        "email",
        "user_birthday",
    };

    // intents coming from the Facebook app start with this string (action)
    private static final String COM_FACEBOOK_APPLICATION = "com.facebook.application.";
    public static final String ACCESS_DENIED = "access_denied";
    public static final String ACCESS_DENIED_EXCEPTION = "OAuthAccessDeniedException";


    @Override
    protected void build() {
        // no UI
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent auth = getAuthIntent(this, DEFAULT_PERMISSIONS);
        if (validateAppSignatureForIntent(auth)) {
            startActivityForResult(auth, 0);
        } else {
            setResult(RESULT_OK,
                    new Intent().putExtra("error", "fb app not installed or sig invalid"));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            try {
                final Token token = getTokenFromIntent(data);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "got token: "+token);
                }

                token.store(this);

                loginExtensionGrantype(CloudAPI.FACEBOOK_GRANT_TYPE+token.accessToken);
            } catch (SSOException e) {
                Log.w(TAG, "error getting Facebook token", e);

                Intent result = new Intent();
                result.putExtra("error", e.getMessage());
                result.putExtra("canceled", e instanceof SSOCanceledException);
                setResult(RESULT_OK, result);
                finish();
            }
        } else {
            finish();
        }
    }

    protected boolean validateAppSignatureForIntent(Intent intent) {
        return validateActivityIntent(this, intent);
    }

    public static boolean isFacebookView(Context context, Intent intent) {
        //noinspection SimplifiableIfStatement
        if (intent == null || intent.getAction() == null ||
                !intent.getAction().startsWith(COM_FACEBOOK_APPLICATION)) {
            return false;
        } else {
            return intent.getAction().equals(COM_FACEBOOK_APPLICATION + getFacebookAppId(SoundCloudApplication.instance));
        }
    }

    static Intent getAuthIntent(Context context, String... permissions) {
        final String applicationId = getFacebookAppId(SoundCloudApplication.instance);
        Intent intent = new Intent();
        intent.setClassName("com.facebook.katana", "com.facebook.katana.ProxyAuth");
        intent.putExtra(FB_CLIENT_ID_EXTRA, applicationId);
        if (permissions.length > 0) {
            intent.putExtra(FB_PERMISSION_EXTRA, TextUtils.join(",", permissions));
        }
        return intent;
    }

    private static class SSOException extends Exception {
        public SSOException(String s) {
            super(s);
        }
    }

    private static class SSOCanceledException extends SSOException {
        public SSOCanceledException() {
            super("Login canceled by user");
        }
    }

    private static String getFacebookAppId(AndroidCloudAPI api) {
        return api.getContext().getString(
               api.getEnv() == Env.LIVE ? R.string.production_facebook_app_id : R.string.sandbox_facebook_app_id);
    }


    private static boolean validateAppSignatureForPackage(Context context,
                                                   String packageName) {

        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        for (Signature signature : packageInfo.signatures) {
            if (signature.toCharsString().equals(FB_APP_SIGNATURE)) {
                return true;
            }
        }
        return false;
    }


    private static Token getTokenFromIntent(Intent data) throws SSOException {
        // Check OAuth 2.0/2.10 error code.
        String error = data.getStringExtra("error");
        if (error == null) {
            error = data.getStringExtra("error_type");
        }

        if (error != null) { // A Facebook error occurred.
            if (error.equals(SINGLE_SIGN_ON_DISABLED)) {

                throw new SSOException("SSO disabled");
            } else if (error.equals(ACCESS_DENIED)
                    || error.equals(ACCESS_DENIED_EXCEPTION)) {
                throw new SSOCanceledException();
            } else {
                String description = data.getStringExtra("error_description");
                if (description != null) {
                    error = error + ":" + description;
                }
                throw new SSOException("Login failed:" + error);
            }
        } else {   // No errors.
            String token = data.getStringExtra(TOKEN);
            String expiresIn = data.getStringExtra(EXPIRES);

            final long expires = !"0".equals(expiresIn) ?
                    System.currentTimeMillis() + Long.parseLong(expiresIn) * 1000L : 0;

            if (token != null && (expires == 0 || System.currentTimeMillis() < expires)) {
                return new Token(token, expires);
            } else {
                throw new SSOException("session is not valid");
            }
        }
    }

    public static boolean extendAccessTokenIfNeeded(Context context) {
        if (!isSupported(context)) return false;

        Token token = Token.load(context);
        return token.isFresh() || extendAccessToken(token, context);
    }

    private static boolean extendAccessToken(final Token token, final Context context) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "extendAccessToken("+token+")");
        if (token.accessToken == null) return false;

        Intent intent = new Intent();
        intent.setClassName("com.facebook.katana",
                "com.facebook.katana.platform.TokenRefreshService");

        return validateServiceIntent(context, intent) &&
               context.bindService(intent, new ServiceConnection() {

            private final Messenger messenger = new Messenger(new Handler() {
                @Override public void handleMessage(Message msg) {
                    String aToken = msg.getData().getString(TOKEN);
                    long expiresAt = msg.getData().getLong(EXPIRES) * 1000L;

                    if (aToken != null) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "token refresh via service: "+token);
                        }

                        new Token(aToken, expiresAt).store(context);
                    } else {
                        Log.w(TAG, "token is null");
                    }
                }
            });
            private Messenger sender;

            @Override public void onServiceConnected(ComponentName name, IBinder service) {
                sender = new Messenger(service);
                Bundle requestData = new Bundle();
                requestData.putString(TOKEN, token.accessToken);
                Message request = Message.obtain();
                request.setData(requestData);
                request.replyTo = messenger;
                try {
                    sender.send(request);
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }

            @Override public void onServiceDisconnected(ComponentName name) {
                context.unbindService(this);
            }
        }, Context.BIND_AUTO_CREATE);
    }

    public static boolean isSupported(Context context) {
        return FacebookSSO.validateActivityIntent(context, FacebookSSO.getAuthIntent(context));
    }

    private static boolean validateServiceIntent(Context context, Intent intent) {
        ResolveInfo resolveInfo =
                context.getPackageManager().resolveService(intent, 0);
        return resolveInfo != null &&
                validateAppSignatureForPackage(context, resolveInfo.serviceInfo.packageName);
    }

    private static boolean validateActivityIntent(Context context, Intent intent) {
        ResolveInfo resolveInfo =
                context.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null
                && validateAppSignatureForPackage(context, resolveInfo.activityInfo.packageName);

    }

    static class Token {
        private static final String PREF_KEY    = "facebook-session";
        private static final String TOKEN_KEY   = "token";
        private static final String EXPIRES_KEY = "expires";
        private static final String LAST_REFRESH_KEY = "lastRefresh";

        private static final long REFRESH_TOKEN_BARRIER = 24L * 60L * 60L * 1000L;

        final String accessToken;
        final long expires;
        long lastRefresh = System.currentTimeMillis();

        Token(String accessToken, long expires) {
            this.accessToken = accessToken;
            this.expires = expires;
        }

        public boolean store(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
            return prefs.edit()
                    .putString(TOKEN_KEY, accessToken)
                    .putLong(EXPIRES_KEY, expires)
                    .putLong(LAST_REFRESH_KEY, lastRefresh)
                    .commit();
        }

        public static @NotNull Token load(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
            Token token = new Token(prefs.getString(TOKEN_KEY, null), prefs.getLong(EXPIRES_KEY, 0));
            token.lastRefresh = prefs.getLong(LAST_REFRESH_KEY, 0);
            return token;
        }

        public boolean isFresh() {
            return accessToken != null &&
                (System.currentTimeMillis() - lastRefresh <= REFRESH_TOKEN_BARRIER);
        }

        @Override
        public String toString() {
            // NB: for security reasons make sure not to log the full access token here
            return "Token{" +
                    "accessToken='" +
                        (accessToken != null ?
                         accessToken.substring(0, Math.min(accessToken.length(), 10)) + "..." : null)  +
                    ", expires=" + expires +
                    ", lastRefresh=" + lastRefresh +
                    '}';
        }
    }

    public static final String FB_APP_SIGNATURE =
            "30820268308201d102044a9c4610300d06092a864886f70d0101040500307a310"
                    + "b3009060355040613025553310b30090603550408130243413112301006035504"
                    + "07130950616c6f20416c746f31183016060355040a130f46616365626f6f6b204"
                    + "d6f62696c653111300f060355040b130846616365626f6f6b311d301b06035504"
                    + "03131446616365626f6f6b20436f72706f726174696f6e3020170d30393038333"
                    + "13231353231365a180f32303530303932353231353231365a307a310b30090603"
                    + "55040613025553310b30090603550408130243413112301006035504071309506"
                    + "16c6f20416c746f31183016060355040a130f46616365626f6f6b204d6f62696c"
                    + "653111300f060355040b130846616365626f6f6b311d301b06035504031314466"
                    + "16365626f6f6b20436f72706f726174696f6e30819f300d06092a864886f70d01"
                    + "0101050003818d0030818902818100c207d51df8eb8c97d93ba0c8c1002c928fa"
                    + "b00dc1b42fca5e66e99cc3023ed2d214d822bc59e8e35ddcf5f44c7ae8ade50d7"
                    + "e0c434f500e6c131f4a2834f987fc46406115de2018ebbb0d5a3c261bd97581cc"
                    + "fef76afc7135a6d59e8855ecd7eacc8f8737e794c60a761c536b72b11fac8e603"
                    + "f5da1a2d54aa103b8a13c0dbc10203010001300d06092a864886f70d010104050"
                    + "0038181005ee9be8bcbb250648d3b741290a82a1c9dc2e76a0af2f2228f1d9f9c"
                    + "4007529c446a70175c5a900d5141812866db46be6559e2141616483998211f4a6"
                    + "73149fb2232a10d247663b26a9031e15f84bc1c74d141ff98a02d76f85b2c8ab2"
                    + "571b6469b232d8e768a7f7ca04f7abe4a775615916c07940656b58717457b42bd"
                    + "928a2";
}
