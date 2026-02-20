#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

static volatile int initialized = 0;

static inline void ensure_initialized(void) {
    if (!initialized) {
        srand((unsigned int)time(NULL));
        __sync_synchronize();
        initialized = 1;
    }
}

static inline int generate_random_int(int min, int max) {
    if (min > max) { int t = min; min = max; max = t; }
    if (min == max) return min;
    ensure_initialized();
    return min + (rand() % (max - min + 1));
}

static inline int generate_file_index(int count) {
    return count <= 0 ? 0 : generate_random_int(1, count);
}

static const unsigned char dec_table[256] = {
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255, 62,255,255,255, 63,
     52, 53, 54, 55, 56, 57, 58, 59, 60, 61,255,255,255,  0,255,255,
    255,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
     15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,255,255,255,255,255,
    255, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
     41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
    255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255
};

static const char* strip_prefix(const char* s, size_t* len) {
    if (!s || !len) return NULL;
    *len = strlen(s);
    const char* start = s;
    const char* comma = strchr(s, ',');
    if (comma) {
        start = comma + 1;
        *len -= (size_t)(start - s);
    }
    while (*len > 0 && (*start == ' ' || *start == '\n' || *start == '\r' || *start == '\t')) {
        start++; (*len)--;
    }
    while (*len > 0 && (start[*len - 1] == ' ' || start[*len - 1] == '\n' || start[*len - 1] == '\r' || start[*len - 1] == '\t')) {
        (*len)--;
    }
    return start;
}

JNIEXPORT jint JNICALL
Java_com_folkbanner_utils_NativeRandomGenerator_nativeGenerateRandomIndex(
    JNIEnv* env, jobject thiz, jint count) {
    (void)env; (void)thiz;
    return generate_file_index((int)count);
}

JNIEXPORT jint JNICALL
Java_com_folkbanner_utils_NativeRandomGenerator_nativeGenerateRandomInRange(
    JNIEnv* env, jobject thiz, jint min, jint max) {
    (void)env; (void)thiz;
    return generate_random_int((int)min, (int)max);
}

JNIEXPORT jbyteArray JNICALL
Java_com_folkbanner_utils_NativeRandomGenerator_nativeDecodeBase64(
    JNIEnv* env, jobject thiz, jstring input) {
    (void)thiz;
    if (!input) return NULL;
    
    const char* src = (*env)->GetStringUTFChars(env, input, NULL);
    if (!src) return NULL;
    
    size_t len = 0;
    const char* clean = strip_prefix(src, &len);
    if (!clean || len == 0) {
        (*env)->ReleaseStringUTFChars(env, input, src);
        return NULL;
    }
    
    while (len > 0 && clean[len-1] == '=') len--;
    
    size_t out_len = (len * 3) / 4;
    unsigned char* out = (unsigned char*)malloc(out_len + 1);
    if (!out) {
        (*env)->ReleaseStringUTFChars(env, input, src);
        return NULL;
    }
    
    size_t j = 0;
    for (size_t i = 0; i < len; ) {
        uint32_t v = 0;
        int n = 0;
        for (int k = 0; k < 4 && i < len; k++, i++) {
            unsigned char c = (unsigned char)clean[i];
            if (dec_table[c] == 255) continue;
            v = (v << 6) | dec_table[c];
            n++;
        }
        for (int k = n; k < 4; k++) v <<= 6;
        if (n >= 2) out[j++] = (v >> 16) & 0xFF;
        if (n >= 3) out[j++] = (v >> 8) & 0xFF;
        if (n >= 4) out[j++] = v & 0xFF;
    }
    
    jbyteArray result = (*env)->NewByteArray(env, (jsize)j);
    if (result) (*env)->SetByteArrayRegion(env, result, 0, (jsize)j, (jbyte*)out);
    
    free(out);
    (*env)->ReleaseStringUTFChars(env, input, src);
    return result;
}
