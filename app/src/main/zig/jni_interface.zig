const std = @import("std");

fn generateRandomInt(min: i32, max: i32) i32 {
    var m = min;
    var n = max;
    if (m > n) {
        const t = m;
        m = n;
        n = t;
    }
    if (m == n) return m;
    const rand = std.crypto.random;
    return m + @as(i32, @intCast(rand.uintLessThan(u32, @as(u32, @intCast(n - m + 1)))));
}

fn generateFileIndex(count: i32) i32 {
    if (count <= 0) return 0;
    return generateRandomInt(1, count);
}

fn stripPrefix(s: []const u8) []const u8 {
    var start: usize = 0;
    var len: usize = s.len;

    if (std.mem.indexOfScalar(u8, s, ',')) |comma_pos| {
        start = comma_pos + 1;
        len = s.len - start;
    }

    while (len > 0 and std.ascii.isWhitespace(s[start])) {
        start += 1;
        len -= 1;
    }

    while (len > 0 and std.ascii.isWhitespace(s[start + len - 1])) {
        len -= 1;
    }

    return s[start .. start + len];
}

fn decodeBase64(allocator: std.mem.Allocator, input: []const u8) ![]u8 {
    const clean = stripPrefix(input);
    if (clean.len == 0) return error.InvalidInput;

    var trimmed_len = clean.len;
    while (trimmed_len > 0 and clean[trimmed_len - 1] == '=') {
        trimmed_len -= 1;
    }

    const decoder = std.base64.standard.Decoder;
    const decoded_size = decoder.calcSizeForSlice(clean[0..trimmed_len]) catch return error.InvalidInput;

    const output = try allocator.alloc(u8, decoded_size);
    errdefer allocator.free(output);

    decoder.decode(output, clean[0..trimmed_len]) catch {
        allocator.free(output);
        return error.InvalidInput;
    };

    return output;
}

const JNIEnv = *opaque {};
const jobject = ?*anyopaque;
const jstring = ?*anyopaque;
const jint = i32;
const jbyte = i8;
const jbyteArray = ?*anyopaque;
const jsize = i32;

const JNINativeInterface = extern struct {
    reserved: [4]?*const anyopaque align(@alignOf(usize)),
    GetVersion: ?*const anyopaque,
    DefineClass: ?*const anyopaque,
    FindClass: ?*const anyopaque,
    FromReflectedMethod: ?*const anyopaque,
    FromReflectedField: ?*const anyopaque,
    ToReflectedMethod: ?*const anyopaque,
    GetSuperclass: ?*const anyopaque,
    IsAssignableFrom: ?*const anyopaque,
    ToReflectedField: ?*const anyopaque,
    Throw: ?*const anyopaque,
    ThrowNew: ?*const anyopaque,
    ExceptionOccurred: ?*const anyopaque,
    ExceptionDescribe: ?*const anyopaque,
    ExceptionClear: ?*const anyopaque,
    FatalError: ?*const anyopaque,
    PushLocalFrame: ?*const anyopaque,
    PopLocalFrame: ?*const anyopaque,
    _padding1: [8]?*const anyopaque,
    NewString: ?*const anyopaque,
    GetStringUTFChars: ?*const fn (*JNIEnv, jstring, ?*u8) callconv(.c) [*:0]const u8,
    ReleaseStringUTFChars: ?*const fn (*JNIEnv, jstring, [*:0]const u8) callconv(.c) void,
    GetStringUTFLength: ?*const anyopaque,
    GetStringUTFRegion: ?*const anyopaque,
    GetArrayLength: ?*const fn (*JNIEnv, jbyteArray) callconv(.c) jsize,
    _padding2: [4]?*const anyopaque,
    NewByteArray: ?*const fn (*JNIEnv, jsize) callconv(.c) jbyteArray,
    _padding3: [16]?*const anyopaque,
    SetByteArrayRegion: ?*const fn (*JNIEnv, jbyteArray, jsize, jsize, [*]const jbyte) callconv(.c) void,
};

export fn Java_com_folkbanner_utils_NativeRandomGenerator_nativeGenerateRandomIndex(env: *JNIEnv, thiz: jobject, count: jint) jint {
    _ = env;
    _ = thiz;
    return generateFileIndex(count);
}

export fn Java_com_folkbanner_utils_NativeRandomGenerator_nativeGenerateRandomInRange(env: *JNIEnv, thiz: jobject, min: jint, max: jint) jint {
    _ = env;
    _ = thiz;
    return generateRandomInt(min, max);
}

export fn Java_com_folkbanner_utils_NativeRandomGenerator_nativeDecodeBase64(env: *JNIEnv, thiz: jobject, input: jstring) jbyteArray {
    _ = thiz;

    if (input == null) return null;

    const env_ptr: *const JNINativeInterface = @ptrCast(@alignCast(env));
    const src = env_ptr.GetStringUTFChars.?(env, input, null);
    defer env_ptr.ReleaseStringUTFChars.?(env, input, src);

    const src_len = std.mem.len(src);
    if (src_len == 0) return null;

    var buf: [4096]u8 = undefined;
    var fba = std.heap.FixedBufferAllocator.init(&buf);

    const decoded = decodeBase64(fba.allocator(), src[0..src_len]) catch return null;

    const result = env_ptr.NewByteArray.?(env, @intCast(decoded.len));
    if (result) |arr| {
        env_ptr.SetByteArrayRegion.?(env, arr, 0, @intCast(decoded.len), @ptrCast(decoded.ptr));
    }

    return result;
}
