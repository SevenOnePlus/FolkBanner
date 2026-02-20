#include <stdlib.h>
#include <time.h>
#include <stdint.h>
#include <string.h>

static volatile int initialized = 0;

static inline void ensure_initialized(void) {
    if (!initialized) {
        srand((unsigned int)time(NULL));
        __sync_synchronize();
        initialized = 1;
    }
}

int generate_random_int(int min, int max) {
    ensure_initialized();
    if (min > max) {
        int temp = min;
        min = max;
        max = temp;
    }
    if (min == max) return min;
    return min + (rand() % (max - min + 1));
}

int generate_file_index(int count) {
    if (count <= 0) return 0;
    return generate_random_int(1, count);
}

static const unsigned char base64_decode_table[256] = {
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

size_t base64_decode(const char* input, size_t input_len, unsigned char* output) {
    if (!input || !output || input_len == 0) return 0;
    
    const char* src = input;
    unsigned char* dst = output;
    size_t decoded_len = 0;
    
    while (input_len > 0 && src[input_len - 1] == '=') {
        input_len--;
    }
    
    while (input_len >= 4) {
        uint32_t val = 0;
        for (int i = 0; i < 4; i++) {
            unsigned char c = (unsigned char)src[i];
            if (c >= 256 || base64_decode_table[c] == 255) {
                return 0;
            }
            val = (val << 6) | base64_decode_table[c];
        }
        
        *dst++ = (val >> 16) & 0xFF;
        *dst++ = (val >> 8) & 0xFF;
        *dst++ = val & 0xFF;
        
        src += 4;
        input_len -= 4;
        decoded_len += 3;
    }
    
    if (input_len >= 2) {
        uint32_t val = 0;
        int pad = 0;
        
        for (size_t i = 0; i < input_len; i++) {
            unsigned char c = (unsigned char)src[i];
            if (c >= 256 || base64_decode_table[c] == 255) {
                return 0;
            }
            val = (val << 6) | base64_decode_table[c];
        }
        
        for (size_t i = input_len; i < 4; i++) {
            val <<= 6;
            pad++;
        }
        
        *dst++ = (val >> 16) & 0xFF;
        decoded_len++;
        
        if (pad < 2 && input_len >= 3) {
            *dst++ = (val >> 8) & 0xFF;
            decoded_len++;
        }
    }
    
    return decoded_len;
}

const char* strip_base64_prefix(const char* input, size_t* out_len) {
    if (!input || !out_len) return NULL;
    
    const char* data_prefix = "data:";
    const char* base64_marker = ";base64,";
    
    size_t input_len = strlen(input);
    const char* start = input;
    
    if (strncmp(input, data_prefix, 5) == 0) {
        const char* comma = strchr(input, ',');
        if (comma) {
            start = comma + 1;
            input_len -= (start - input);
        }
    }
    
    while (input_len > 0 && (*start == ' ' || *start == '\n' || *start == '\r' || *start == '\t')) {
        start++;
        input_len--;
    }
    
    while (input_len > 0 && (start[input_len - 1] == ' ' || start[input_len - 1] == '\n' || 
           start[input_len - 1] == '\r' || start[input_len - 1] == '\t')) {
        input_len--;
    }
    
    *out_len = input_len;
    return start;
}
