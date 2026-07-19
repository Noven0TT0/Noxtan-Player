#ifndef COMMON_FFMPEG_H
#define COMMON_FFMPEG_H

#pragma once

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <android/bitmap.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
#include <libavutil/channel_layout.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
#include <libavutil/display.h>
#include <libavutil/dict.h>
#include <libavutil/mem.h>
#include <stdlib.h>
}

#define LOG_TAG "NoxtanEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#endif // COMMON_FFMPEG_H