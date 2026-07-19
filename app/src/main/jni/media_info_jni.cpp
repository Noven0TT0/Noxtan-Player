#include "common_ffmpeg.h"

// --- 4. VIDEO DURATION ---
extern "C" JNIEXPORT jlong JNICALL
Java_com_noxtan_player_engine_NoxtanEngine_ffmpegGetDuration(
        JNIEnv *env, jobject thiz,
        jstring path) {

    const char *filePath = env->GetStringUTFChars(path, nullptr);
    int64_t duration = 0;

    AVFormatContext *fmtCtx = avformat_alloc_context();
    if (avformat_open_input(&fmtCtx, filePath, nullptr, nullptr) >= 0) {
        if (avformat_find_stream_info(fmtCtx, nullptr) >= 0) {
            duration = fmtCtx->duration;
        }
        avformat_close_input(&fmtCtx);
    }

    env->ReleaseStringUTFChars(path, filePath);

    if (duration != AV_NOPTS_VALUE) {
        return (jlong) (duration / 1000);
    }
    return 0;
}

// --- 5. GET ROTATION ---
extern "C" JNIEXPORT jint JNICALL
Java_com_noxtan_player_engine_NoxtanEngine_ffmpegGetRotation(
        JNIEnv *env, jobject thiz,
        jstring path) {

    const char *filePath = env->GetStringUTFChars(path, nullptr);
    int rotation = 0;

    AVFormatContext *fmtCtx = avformat_alloc_context();
    if (avformat_open_input(&fmtCtx, filePath, nullptr, nullptr) >= 0) {
        if (avformat_find_stream_info(fmtCtx, nullptr) >= 0) {

            for (int i = 0; i < fmtCtx->nb_streams; i++) {
                if (fmtCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                    AVStream *stream = fmtCtx->streams[i];

                    AVDictionaryEntry *rotate_tag = av_dict_get(stream->metadata, "rotate", NULL, 0);
                    if (rotate_tag) {
                        rotation = atoi(rotate_tag->value);
                    }
                    break;
                }
            }
        }
        avformat_close_input(&fmtCtx);
    }

    env->ReleaseStringUTFChars(path, filePath);

    while (rotation < 0) rotation += 360;
    while (rotation >= 360) rotation -= 360;

    return rotation;
}

// --- 6. GET SUBTITLE TIMESTAMPS ---
extern "C" JNIEXPORT jlongArray JNICALL
Java_com_noxtan_player_engine_NoxtanEngine_ffmpegGetSubtitleTimestamps(
        JNIEnv *env, jobject thiz,
        jstring path) {

    const char *filePath = env->GetStringUTFChars(path, nullptr);
    std::vector<int64_t> timestamps;

    AVFormatContext *fmtCtx = avformat_alloc_context();
    if (avformat_open_input(&fmtCtx, filePath, nullptr, nullptr) >= 0) {
        if (avformat_find_stream_info(fmtCtx, nullptr) >= 0) {

            int subtitleStreamIdx = -1;
            for (int i = 0; i < fmtCtx->nb_streams; i++) {
                if (fmtCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_SUBTITLE) {
                    subtitleStreamIdx = i;
                    break;
                }
            }

            if (subtitleStreamIdx != -1) {
                AVPacket *packet = av_packet_alloc();
                while (av_read_frame(fmtCtx, packet) >= 0) {
                    if (packet->stream_index == subtitleStreamIdx) {
                        if (packet->pts != AV_NOPTS_VALUE) {
                            int64_t pts = packet->pts;
                            AVRational time_base = fmtCtx->streams[subtitleStreamIdx]->time_base;
                            int64_t timeMs = av_rescale_q(pts, time_base, {1, 1000});
                            timestamps.push_back(timeMs);
                        }
                    }
                    av_packet_unref(packet);
                }
                av_packet_free(&packet);
            }
        }
        avformat_close_input(&fmtCtx);
    }

    env->ReleaseStringUTFChars(path, filePath);

    jlongArray result = env->NewLongArray(timestamps.size());
    if (timestamps.size() > 0) {
        env->SetLongArrayRegion(result, 0, timestamps.size(), (const jlong *)timestamps.data());
    }
    return result;
}

// --- 7. GET FFMPEG VERSION ---
extern "C" JNIEXPORT jstring JNICALL
Java_com_noxtan_player_engine_NoxtanEngine_getFFmpegVersion(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(av_version_info());
}

// --- 8. GET VIDEO RESOLUTION ---
extern "C" JNIEXPORT jstring JNICALL
Java_com_noxtan_player_engine_NoxtanEngine_ffmpegGetResolution(
        JNIEnv *env, jobject thiz,
        jstring path) {

    const char *filePath = env->GetStringUTFChars(path, nullptr);
    int width = 0;
    int height = 0;

    AVFormatContext *fmtCtx = avformat_alloc_context();
    if (avformat_open_input(&fmtCtx, filePath, nullptr, nullptr) >= 0) {
        if (avformat_find_stream_info(fmtCtx, nullptr) >= 0) {
            for (int i = 0; i < fmtCtx->nb_streams; i++) {
                if (fmtCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                    width = fmtCtx->streams[i]->codecpar->width;
                    height = fmtCtx->streams[i]->codecpar->height;
                    break;
                }
            }
        }
        avformat_close_input(&fmtCtx);
    }

    env->ReleaseStringUTFChars(path, filePath);

    if (width > 0 && height > 0) {
        char resStr[32];
        snprintf(resStr, sizeof(resStr), "%dx%d", width, height);
        return env->NewStringUTF(resStr);
    }

    return env->NewStringUTF("0x0");
}