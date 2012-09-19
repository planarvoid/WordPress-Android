/* Copyright (C) 2010 Andor Polgar */

#ifndef __LIBWAV_H__
#define __LIBWAV_H__

#include <stdio.h>

int writeWavHeader(FILE *fp,
             unsigned int num_samples,
             unsigned short num_channels,
             unsigned int sample_rate,
             unsigned short bits_per_sample);

#endif /* __LIBWAV__ */
