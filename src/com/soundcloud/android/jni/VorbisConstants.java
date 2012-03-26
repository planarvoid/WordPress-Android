package com.soundcloud.android.jni;

@SuppressWarnings("UnusedDeclaration")
public interface VorbisConstants {

    // vorbis/codec.h
    int OV_FALSE      =   -1;
    int OV_EOF        =   -2;
    int OV_HOLE       =   -3;
    int OV_EREAD      = -128;
    int OV_EFAULT     = -129;
    int OV_EIMPL      = -130;
    int OV_EINVAL     = -131;
    int OV_ENOTVORBIS = -132;
    int OV_EBADHEADER = -133;
    int OV_EVERSION   = -134;
    int OV_ENOTAUDIO  = -135;
    int OV_EBADPACKET = -136;
    int OV_EBADLINK   = -137;
    int OV_ENOSEEK    = -138;
}
