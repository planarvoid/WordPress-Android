/* Copyright (C) 2010  Andor Polgar */
#include <libwav/libwav.h>

int writeWavHeader(FILE *fp,
    unsigned int   num_samples,
    unsigned short num_channels,
    unsigned int   sample_rate,
    unsigned short bits_per_sample) {

    unsigned int fmt_subchunk_size;
    unsigned int data_subchunk_size;
    unsigned int chunk_size;

    unsigned short audio_format;
    unsigned int   byte_rate;
    unsigned short block_align;

    if ((bits_per_sample % 8) != 0) {
        return 0;
    }

    fmt_subchunk_size = 16;
    data_subchunk_size = num_samples * num_channels * (bits_per_sample / 8);
    chunk_size = 4 + (8 + fmt_subchunk_size) + (8 + data_subchunk_size);

    audio_format = 1;
    byte_rate = sample_rate * num_channels * (bits_per_sample / 8);
    block_align = num_channels * (bits_per_sample / 8);

    /* WAVE file header */
    fwrite("RIFF",              1, 4, fp);
    fwrite(&chunk_size,         4, 1, fp);
    fwrite("WAVE",              1, 4, fp);

    /* fmt subchunk */
    fwrite("fmt ",              1, 4, fp);
    fwrite(&fmt_subchunk_size,  4, 1, fp);
    fwrite(&audio_format,       2, 1, fp);
    fwrite(&num_channels,       2, 1, fp);
    fwrite(&sample_rate,        4, 1, fp);
    fwrite(&byte_rate,          4, 1, fp);
    fwrite(&block_align,        2, 1, fp);
    fwrite(&bits_per_sample,    2, 1, fp);

    /* data subchunk */
    fwrite("data",              1, 4, fp);
    fwrite(&data_subchunk_size, 4, 1, fp);

    return 1;
}