#include <jni.h>
#include "main.h"

JNIEXPORT jint JNICALL
Java_com_xah_bsdiffs_BsdiffJNI_patch
        (JNIEnv *env, jobject obj, jstring oldFilePath, jstring newFilePath, jstring patchFilePath) {
    // 将Java的jstring转换为C字符串
    const char* oldFile = (*env)->GetStringUTFChars(env, oldFilePath, NULL);
    const char* newFile = (*env)->GetStringUTFChars(env, newFilePath, NULL);
    const char* patchFile = (*env)->GetStringUTFChars(env, patchFilePath, NULL);

    int result = patch(oldFile,newFile,patchFile);

    // 释放资源
    (*env)->ReleaseStringUTFChars(env, oldFilePath, oldFile);
    (*env)->ReleaseStringUTFChars(env, newFilePath, newFile);
    (*env)->ReleaseStringUTFChars(env, patchFilePath, patchFile);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_xah_bsdiffs_BsdiffJNI_merge
        (JNIEnv *env, jobject obj, jstring oldFilePath, jstring patchFilePath, jstring newFilePath) {
    // 将Java的jstring转换为C字符串
    const char* oldFile = (*env)->GetStringUTFChars(env, oldFilePath, NULL);
    const char* newFile = (*env)->GetStringUTFChars(env, newFilePath, NULL);
    const char* patchFile = (*env)->GetStringUTFChars(env, patchFilePath, NULL);

    int result = merge(oldFile,patchFile,newFile);

    // 释放资源
    (*env)->ReleaseStringUTFChars(env, oldFilePath, oldFile);
    (*env)->ReleaseStringUTFChars(env, newFilePath, newFile);
    (*env)->ReleaseStringUTFChars(env, patchFilePath, patchFile);

    return result;
}
