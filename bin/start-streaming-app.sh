#!/usr/bin/env bash

FWDIR="$(cd `dirname $0`/..; pwd)"
`$FWDIR/bin/spark-class com.asiainfo.ocdc.streaming.StreamingApp $@`