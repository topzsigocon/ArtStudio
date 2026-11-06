#include <jni.h>
#include <vector>
#include <cmath>
#include <android/bitmap.h>

struct Point {
    float x, y, pressure;
};

// Smooth interpolation for native touch drawing
extern "C" JNIEXPORT void JNICALL
Java_com_artstudio_pro_core_graphics_NativeBrushEngine_drawStrokeNative(
        JNIEnv* env,
        jobject thiz,
        jobject bitmap,
        jfloatArray pointsArray,
        jint color,
        jfloat brushSize,
        jfloat hardness
) {
    AndroidBitmapInfo info;
    void* pixels;
    
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) return;

    jsize len = env->GetArrayLength(pointsArray);
    jfloat* pts = env->GetFloatArrayElements(pointsArray, NULL);

    // Rasterize smooth stroke directly into bitmap pixel array
    uint32_t* pixelPtr = (uint32_t*)pixels;
    for (int i = 0; i < len; i += 3) {
        float px = pts[i];
        float py = pts[i+1];
        float p  = pts[i+2]; // Stylus Pressure
        
        float currentRadius = (brushSize * p) / 2.0f;
        int minX = std::max(0, (int)(px - currentRadius));
        int maxX = std::min((int)info.width - 1, (int)(px + currentRadius));
        int minY = std::max(0, (int)(py - currentRadius));
        int maxY = std::min((int)info.height - 1, (int)(py + currentRadius));

        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                float dist = std::hypot(x - px, y - py);
                if (dist <= currentRadius) {
                    // Apply Pixel Color Buffer Modification
                    pixelPtr[y * info.width + x] = color;
                }
            }
        }
    }

    env->ReleaseFloatArrayElements(pointsArray, pts, 0);
    AndroidBitmap_unlockPixels(env, bitmap);
}
