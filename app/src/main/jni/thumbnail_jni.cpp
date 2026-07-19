#include "common_ffmpeg.h"

// --- VIDEO THUMBNAIL ---
extern "C" JNIEXPORT jboolean JNICALL
Java_com_noxtan_player_engine_NoxtanEngine_ffmpegGetVideoThumbnail(
        JNIEnv *env, jobject thiz,
        jstring videoPath,
        jobject bitmap) {

    const char *path = env->GetStringUTFChars(videoPath, nullptr);

    AVFormatContext *fmtCtx = nullptr;
    AVCodecContext *codecCtx = nullptr;
    struct SwsContext *sws_ctx = nullptr;
    AVPacket *packet = nullptr;
    AVFrame *frame = nullptr;
    AVFrame *rgbFrame = nullptr;
    uint8_t *buffer = nullptr;
    void *pixels = nullptr;
    AndroidBitmapInfo info;

    bool frameFinished = false;
    int videoStreamIdx = -1;
    int frameCount = 0;
    int rotation = 0;
    int v_width = 0, v_height = 0;
    int target_w = 0, target_h = 0;
    int crop_x = 0, crop_y = 0;
    int numBytes = 0;
    float video_aspect = 0.0f, bitmap_aspect = 0.0f;

    if (avformat_open_input(&fmtCtx, path, nullptr, nullptr) < 0) goto cleanup;
    if (avformat_find_stream_info(fmtCtx, nullptr) < 0) goto cleanup;

    for (int i = 0; i < (int)fmtCtx->nb_streams; i++) {
        if (fmtCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoStreamIdx = i;
            AVDictionaryEntry *tag = av_dict_get(fmtCtx->streams[i]->metadata, "rotate", nullptr, 0);
            if (tag) rotation = atoi(tag->value);
            break;
        }
    }
    if (videoStreamIdx == -1) goto cleanup;

    {
        AVCodecParameters *codecPar = fmtCtx->streams[videoStreamIdx]->codecpar;
        const AVCodec *codec = avcodec_find_decoder(codecPar->codec_id);
        if (!codec) goto cleanup;
        codecCtx = avcodec_alloc_context3(codec);
        avcodec_parameters_to_context(codecCtx, codecPar);
        if (avcodec_open2(codecCtx, codec, nullptr) < 0) goto cleanup;
    }

    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) goto cleanup;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) goto cleanup;

    v_width = codecCtx->width;
    v_height = codecCtx->height;
    if (rotation == 90 || rotation == 270) {
        v_width = codecCtx->height;
        v_height = codecCtx->width;
    }

    video_aspect = (float)v_width / (float)v_height;
    bitmap_aspect = (float)info.width / (float)info.height;

    if (video_aspect > bitmap_aspect) {
        target_h = (int)info.height;
        target_w = (int)((float)info.height * video_aspect);
        crop_x = (target_w - (int)info.width) / 2;
        crop_y = 0;
    } else {
        target_w = (int)info.width;
        target_h = (int)((float)info.width / video_aspect);
        crop_y = (target_h - (int)info.height) / 2;
        crop_x = 0;
    }

    sws_ctx = sws_getContext(
            codecCtx->width, codecCtx->height, codecCtx->pix_fmt,
            (rotation == 90 || rotation == 270) ? target_h : target_w,
            (rotation == 90 || rotation == 270) ? target_w : target_h,
            AV_PIX_FMT_RGBA, SWS_BILINEAR, nullptr, nullptr, nullptr
    );

    packet = av_packet_alloc();
    frame = av_frame_alloc();
    rgbFrame = av_frame_alloc();

    numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, (rotation == 90 || rotation == 270) ? target_h : target_w, (rotation == 90 || rotation == 270) ? target_w : target_h, 1);
    buffer = (uint8_t *) av_malloc((size_t)numBytes * sizeof(uint8_t));
    av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, buffer, AV_PIX_FMT_RGBA, (rotation == 90 || rotation == 270) ? target_h : target_w, (rotation == 90 || rotation == 270) ? target_w : target_h, 1);

    av_seek_frame(fmtCtx, videoStreamIdx, 5 * AV_TIME_BASE, AVSEEK_FLAG_BACKWARD);

    while (av_read_frame(fmtCtx, packet) >= 0) {
        if (packet->stream_index == videoStreamIdx) {
            if (avcodec_send_packet(codecCtx, packet) == 0) {
                if (avcodec_receive_frame(codecCtx, frame) == 0) {
                    sws_scale(sws_ctx, (const uint8_t *const *)frame->data, frame->linesize, 0, codecCtx->height, rgbFrame->data, rgbFrame->linesize);

                    uint32_t *src_pixels = (uint32_t *)rgbFrame->data[0];
                    uint32_t *dst_pixels = (uint32_t *)pixels;
                    int src_stride_pixels = (rotation == 90 || rotation == 270) ? target_h : target_w;

                    for (int y = 0; y < (int)info.height; y++) {
                        for (int x = 0; x < (int)info.width; x++) {
                            dst_pixels[y * (info.stride / 4) + x] = src_pixels[(y + crop_y) * src_stride_pixels + (x + crop_x)];
                        }
                    }

                    frameFinished = true;
                    break;
                }
            }
        }
        av_packet_unref(packet);
        if(frameCount++ > 500) break;
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    cleanup:
    if (buffer) av_free(buffer);
    if (rgbFrame) av_frame_free(&rgbFrame);
    if (frame) av_frame_free(&frame);
    if (packet) av_packet_free(&packet);
    if (sws_ctx) sws_freeContext(sws_ctx);
    if (codecCtx) avcodec_free_context(&codecCtx);
    if (fmtCtx) avformat_close_input(&fmtCtx);
    env->ReleaseStringUTFChars(videoPath, path);
    return frameFinished;
}