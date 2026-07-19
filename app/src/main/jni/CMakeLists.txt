cmake_minimum_required(VERSION 4.1.2)
project("noxtan-ultimate-engine")

set(ffmpeg_lib_dir ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
set(ffmpeg_include_dir ${CMAKE_SOURCE_DIR}/../jniLibs/include)
include_directories(${ffmpeg_include_dir})

# Import FFmpeg as STATIC libraries (ပြဿနာကင်းစင်စေရန် ၅ ခုလုံးကို ထည့်သွင်းထားပါသည်)
add_library(avcodec STATIC IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${ffmpeg_lib_dir}/lib/libavcodec.a)

add_library(avformat STATIC IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${ffmpeg_lib_dir}/lib/libavformat.a)

add_library(avutil STATIC IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${ffmpeg_lib_dir}/lib/libavutil.a)

add_library(swresample STATIC IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${ffmpeg_lib_dir}/lib/libswresample.a)

add_library(swscale STATIC IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${ffmpeg_lib_dir}/lib/libswscale.a)

# Build noxtanplayer_engine
add_library(noxtanplayer_engine SHARED
        thumbnail_jni.cpp
        media_info_jni.cpp
)

# android system libraries များနှင့် zlib, mediandk တို့ကို စုံလင်စွာ ချိတ်ဆက်ထားပါသည်
target_link_libraries(noxtanplayer_engine avcodec avformat avutil swresample swscale log jnigraphics android z mediandk)