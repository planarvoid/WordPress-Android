#!/bin/sh

say '1 2 3' -o 123.aifc
say '4 5 6' -o 456.aifc
say '7 8 9' -o 789.aifc

sox 123.aifc 123.wav rate 44100
sox 456.aifc 456.wav rate 44100
sox 789.aifc 789.wav rate 44100

oggenc 123.wav -o 123.ogg
oggenc 456.wav -o 456.ogg
oggenc 789.wav -o 789.ogg

cat 123.ogg 456.ogg 789.ogg > 123456789.ogg

rm 123.* 456.* 789.*
