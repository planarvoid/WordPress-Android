package com.soundcloud.android.service.playback;

public class Errors {

    // include/media/stagefright/MediaErrors.h
    static final int STAGEFRIGHT_ERROR_IO = -1004;
    static final int STAGEFRIGHT_ERROR_CONNECTION_LOST = -1005;

    // external/opencore/pvmi/pvmf/include/pvmf_return_codes.h
    // Return code for general failure
    static final int OPENCORE_PVMFFailure = -1;
    // Error due to request timing out
    static final int OPENCORE_PVMFErrTimeout = -11;

    // Custom error for lack of MP error reporting on buffer run out
    static final int STAGEFRIGHT_ERROR_BUFFER_EMPTY = 99;

}
