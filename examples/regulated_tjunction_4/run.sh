#!/bin/sh

cat header.m reg.m footer.m > tmp.m
octave tmp.m

