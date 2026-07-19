#!/bin/bash
# -------------------------------------------------------------------------
# Phase 3: Ultimate FFmpeg Build Script V4 (x86 Fix)
# Target: FFmpeg 8.0.1
# NDK: 29.0.14206865
# Fixes: Added '--disable-asm' for x86/x86_64 to fix relocation R_386_32 error
# -------------------------------------------------------------------------

set -e

# 1. SETUP VARIABLES
FFMPEG_VERSION="8.0.1"
NDK_VERSION="29.0.14206865"
MIN_SDK_VERSION=24
HOST_TAG="linux-x86_64"

WORKING_DIR="$(pwd)"
NDK_PATH="/home/noven/Android/Sdk/ndk/$NDK_VERSION"
TOOLCHAIN="$NDK_PATH/toolchains/llvm/prebuilt/$HOST_TAG"
SYSROOT="$TOOLCHAIN/sysroot"
OUTPUT_DIR="$WORKING_DIR/app/src/main/jniLibs"

# 2. CONFIGURATION FLAGS
COMMON_OPTIONS="
    --target-os=android \
    --enable-cross-compile \
    --enable-static \
    --disable-shared \
    --disable-asm \
    --disable-doc \
    --disable-programs \
    --disable-ffmpeg \
    --disable-ffplay \
    --disable-ffprobe \
    --disable-symver \
    --enable-small \
    --enable-jni \
    --enable-mediacodec \
    --enable-decoder=h264_mediacodec \
    --enable-decoder=hevc_mediacodec \
    --enable-gpl \
    --pkg-config=/bin/false \
"

FORMAT_FLAGS="
    --enable-demuxer=matroska \
    --enable-demuxer=mov \
    --enable-demuxer=flv \
    --enable-demuxer=mpegts \
    --enable-demuxer=mpegvideo \
    --enable-demuxer=avi \
    --enable-demuxer=asf \
    --enable-demuxer=vc1 \
    --enable-decoder=h264 \
    --enable-decoder=hevc \
    --enable-decoder=vp9 \
    --enable-decoder=mpeg2video \
    --enable-decoder=mpeg4 \
    --enable-decoder=msmpeg4v3 \
    --enable-decoder=aac \
    --enable-decoder=ac3 \
    --enable-decoder=eac3 \
    --enable-decoder=dca \
    --enable-decoder=mp3 \
    --enable-decoder=flac \
    --enable-decoder=vorbis \
    --enable-decoder=opus \
    --enable-decoder=truehd \
    --enable-decoder=wmv1 \
    --enable-decoder=wmv2 \
    --enable-decoder=wmv3 \
    --enable-decoder=vc1 \
    --enable-decoder=wmav1 \
    --enable-decoder=wmav2 \
"

PARSER_FLAGS="
    --enable-parser=mpegvideo \
    --enable-parser=mpeg4video \
    --enable-parser=h264 \
    --enable-parser=hevc \
    --enable-parser=aac \
    --enable-parser=ac3 \
    --enable-parser=dca \
    --enable-parser=mpegaudio \
    --enable-parser=vorbis \
    --enable-parser=opus \
    --enable-parser=truehd \
"

# 3. BUILD FUNCTION
build_arch() {
    ARCH=$1
    ABI=$2
    TARGET=$3

    echo ">>> Building for $ABI ($ARCH)..."

    PREFIX="$OUTPUT_DIR/$ABI"
    mkdir -p "$PREFIX"

    export CC="$TOOLCHAIN/bin/clang"
    export CXX="$TOOLCHAIN/bin/clang++"
    export AR="$TOOLCHAIN/bin/llvm-ar"
    export NM="$TOOLCHAIN/bin/llvm-nm"
    export RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
    export STRIP="$TOOLCHAIN/bin/llvm-strip"

    EXTRA_CONFIG=""

    TARGET_FLAGS="--target=${TARGET}${MIN_SDK_VERSION} --sysroot=${SYSROOT}"

    cd "ffmpeg-$FFMPEG_VERSION"
    make clean > /dev/null 2>&1 || true

    ./configure \
        --prefix="$PREFIX" \
        --arch="$ARCH" \
        --cross-prefix="$TOOLCHAIN/bin/llvm-" \
        --cc="$CC" \
        --cxx="$CXX" \
        --ar="$AR" \
        --nm="$NM" \
        --ranlib="$RANLIB" \
        --strip="$STRIP" \
        $COMMON_OPTIONS \
        $FORMAT_FLAGS \
        $PARSER_FLAGS \
        $EXTRA_CONFIG \
        --extra-cflags="$TARGET_FLAGS -O3 -fPIC -Wno-unused-command-line-argument" \
        --extra-ldflags="$TARGET_FLAGS" \
        || { echo "-----------------------------------------"; cat ffbuild/config.log | tail -n 50; echo "-----------------------------------------"; exit 1; }

    make -j$(nproc)
    make install

    echo ">>> Finished $ABI"
    cd ..
}

# 4. EXECUTION
if [ ! -d "ffmpeg-$FFMPEG_VERSION" ]; then
    echo "Downloading FFmpeg $FFMPEG_VERSION..."
    wget -q "https://ffmpeg.org/releases/ffmpeg-$FFMPEG_VERSION.tar.xz"
    tar -xf "ffmpeg-$FFMPEG_VERSION.tar.xz"
fi

build_arch "aarch64" "arm64-v8a" "aarch64-linux-android"
build_arch "arm" "armeabi-v7a" "armv7a-linux-androideabi"
build_arch "x86_64" "x86_64" "x86_64-linux-android"
build_arch "x86" "x86" "i686-linux-android"

echo "========================================================"
echo "ALL BUILDS SUCCESSFUL! (V4 - x86 ASM Disabled)"
echo "Output: $OUTPUT_DIR"
echo "========================================================"