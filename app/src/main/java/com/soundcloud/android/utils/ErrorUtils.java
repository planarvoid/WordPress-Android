package com.soundcloud.android.utils;

import static com.soundcloud.android.playlists.PlaylistOperations.PlaylistMissingException;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.strings.Strings;
import io.fabric.sdk.android.Fabric;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.support.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public final class ErrorUtils {

    public static final String ERROR_CONTEXT_TAG = "error-context";

    private ErrorUtils() {
        // not to be instantiated.
    }

    public static void handleThrowable(Throwable t, CallsiteToken callsiteToken) {
        final StringWriter callsiteTrace = new StringWriter();
        callsiteToken.printStackTrace(new PrintWriter(callsiteTrace));
        handleThrowable(t, callsiteTrace.toString());
    }

    public static void handleThrowable(Throwable t, Class<?> context) {
        handleThrowable(t, context.getCanonicalName());
    }

    /**
     * Use this handler to provide default handling of Throwables in RxJava Observers.
     * <p/>
     * By default, RxJava wraps custom observers in a {@link rx.observers.SafeSubscriber}
     * which catches exceptions like NullPointerExceptions and forwards them to the
     * source observer. This is NOT what we want; we want a NPE to crash the app and be
     * reported as a crash into Crashlytics.
     * <p/>
     * This methods ensures that only checked exceptions make their way into an observer's
     * onError method, and also logs them silently into Crashlytics (unless they're blacklisted.)
     * <p/>
     * see https://github.com/Netflix/RxJava/issues/969
     *
     * @param t       the Exception or Error that was raised
     * @param context an extra message that can be attached to clarify the error context
     */
    public static synchronized void handleThrowable(Throwable t, String context) {
        Log.e(ERROR_CONTEXT_TAG, context);

        if (Fabric.isInitialized()) {
            Crashlytics.setString(ERROR_CONTEXT_TAG, context);
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (includeInReports(t)) {
            // don't rethrow checked exceptions
            handleSilentException(t);
        } else {
            t.printStackTrace();
        }
    }

    // This is aimed to be a temporary fix.
    //
    // TokenRetrievalException was recently added to track sing in/up issues in fabric.
    // It breaks the sign in/up logic since it is based on an exception flow control, that's why
    // this helper is needed.
    @Deprecated
    public static Throwable removeTokenRetrievalException(Exception exception) {
        if (exception instanceof TokenRetrievalException) {
            return exception.getCause();
        }
        return exception;
    }

    @VisibleForTesting
    static boolean includeInReports(Throwable t) {
        if (t instanceof SyncFailedException || isIOExceptionUnrelatedToParsing(t) || t instanceof PlaylistMissingException) {
            return false;
        }
        if (t instanceof ApiRequestException) {
            return ((ApiRequestException) t).loggable();
        }
        return true;
    }

    private static boolean isIOExceptionUnrelatedToParsing(Throwable t) {
        return IOException.class.isAssignableFrom(t.getClass()) && !JsonProcessingException.class.isAssignableFrom(t.getClass());
    }

    public static void handleSilentException(String message, Throwable e) {
        handleSilentException(e, "message", message);
    }

    public static void handleSilentException(Throwable e) {
        handleSilentException(e, null, null);
    }

    public static void handleSilentException(Throwable e, @NotNull Map<String, String> contextKeyValuePairs) {
        if (Fabric.isInitialized()) {
            Log.e(SoundCloudApplication.TAG, "Handling silent exception: " + e);
            for (Map.Entry<String, String> entry : contextKeyValuePairs.entrySet()) {
                Crashlytics.setString(entry.getKey(), entry.getValue());
            }
            Crashlytics.logException(e);
        }
    }

    public static synchronized void handleSilentException(Throwable e, @Nullable String contextKey, @Nullable String contextValue) {
        e.printStackTrace();
        if (Fabric.isInitialized()) {
            Log.e(SoundCloudApplication.TAG, "Handling silent exception: " + e);
            if (contextKey != null && contextValue != null) {
                Crashlytics.setString(contextKey, contextValue);
            }
            Crashlytics.logException(e);
        }
    }

    public static void log(int priority, String tag, String message) {
        if (Fabric.isInitialized()) {
            Crashlytics.log(priority, tag, message);
        } else {
            android.util.Log.println(priority, tag, message);
        }
    }

    public static void handleSilentExceptionWithLog(Throwable e, @Nullable String customLog) {
        if (Fabric.isInitialized()) {
            Log.e(SoundCloudApplication.TAG, "Handling silent exception: " + e);
            if (Strings.isNotBlank(customLog)) {
                BufferedReader rdr = new BufferedReader(new StringReader(customLog));
                try {
                    for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
                        Crashlytics.log(line);
                    }
                } catch (IOException ex) {
                    Log.e(SoundCloudApplication.TAG, "An IOException was caught", ex);
                } finally {
                    IOUtils.close(rdr);
                }
            }
            Crashlytics.logException(e);
        }
    }

    @VisibleForTesting
    public static boolean isCausedByOutOfMemory(Throwable uncaught) {
        Throwable throwable = uncaught;
        while (throwable != null) {
            if (throwable instanceof OutOfMemoryError) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    @Nullable
    public static Throwable findRootCause(@Nullable Throwable throwable) {
        if (throwable == null) {
            return null;
        } else {
            Throwable rootCause = throwable;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            return rootCause;
        }
    }

    public static EmptyView.Status emptyViewStatusFromError(Throwable error) {
        if (error instanceof ApiRequestException) {
            return (((ApiRequestException) error).isNetworkError() ? EmptyView.Status.CONNECTION_ERROR : EmptyView.Status.SERVER_ERROR);
        } else if (error instanceof SyncFailedException) {
            // default Sync Failures to connection for now as we can't tell the diff
            return EmptyView.Status.CONNECTION_ERROR;
        } else {
            return EmptyView.Status.ERROR;
        }
    }
}
