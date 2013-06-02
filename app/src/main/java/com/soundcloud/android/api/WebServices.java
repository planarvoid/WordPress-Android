package com.soundcloud.android.api;

import static com.soundcloud.android.api.WebServices.APIRequestException.APIErrorReason.BAD_RESPONSE;
import static com.soundcloud.android.api.WebServices.APIRequestException.APIErrorReason.IO_ERROR;
import static com.soundcloud.android.api.WebServices.APIRequestException.APIErrorReason.TOKEN_AUTH_ERROR;
import static com.soundcloud.android.api.WebServices.APIRequestException.APIErrorReason.UNKNOWN_ERROR;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;

import android.content.Context;

import java.io.IOException;

public class WebServices {

    /**
     * TODO Eventually get rid of this and replace directly with {@link com.soundcloud.api.ApiWrapper}
     */
    private final AndroidCloudAPI androidCloudAPI;

    protected WebServices(Context context){
        this(Wrapper.getInstance(context));
    }

    @VisibleForTesting
    protected WebServices(AndroidCloudAPI androidCloudAPI){
        this.androidCloudAPI = androidCloudAPI;
    }

    public APIResponse get(WebServiceEndPoint endpoint) {
        APIResponse response;
        try {
            response = new APIResponse(androidCloudAPI.get(Request.to(endpoint.path())));
        } catch (CloudAPI.InvalidTokenException e) {
            throw new APIRequestException(TOKEN_AUTH_ERROR, e);
        } catch (IOException e){
            throw new APIRequestException(IO_ERROR, e);
        } catch(Exception e){
            throw new APIRequestException(UNKNOWN_ERROR, e);
        }

        if(response.isNotSuccess()){
            throw new APIRequestException(BAD_RESPONSE);
        }

        return response;

    }

    protected static class APIRequestException extends RuntimeException{
        protected enum APIErrorReason{
            TOKEN_AUTH_ERROR,
            IO_ERROR,
            BAD_RESPONSE,
            UNKNOWN_ERROR
        }

        APIRequestException(APIErrorReason errorReason, Exception e) {
            super(e);
            this.errorReason = errorReason;
        }

        APIRequestException(APIErrorReason errorReason) {
            this.errorReason = errorReason;
        }

        private APIErrorReason errorReason;

        public APIErrorReason reason(){
            return errorReason;
        }
    }
}
