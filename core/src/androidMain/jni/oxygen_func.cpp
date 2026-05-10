#include "oxygen_func.hpp"
#include "jni.h"
extern "C" {
JNIEXPORT void JNICALL Java_oxygen_bridge_FuncUtils_callVoid(JNIEnv *env,
                                                             jclass thiz,
                                                             jlong ptr) {
  invokeCallback(ptr);
}

JNIEXPORT void JNICALL Java_oxygen_bridge_FuncUtils_callStrCons(JNIEnv *env,
                                                                jclass thiz,
                                                                jstring text,
                                                                jlong ptr) {

  const char *nativeText = env->GetStringUTFChars(text, nullptr);
  invokeCallback(ptr, nativeText);
  if (nativeText != nullptr) {
    env->ReleaseStringUTFChars(text, nativeText);
  }
}

JNIEXPORT void JNICALL Java_oxygen_bridge_FuncUtils_callStrCons2(JNIEnv *env,
                                                                jclass thiz,
                                                                jstring str1,
                                                                jstring str2,
                                                                jlong ptr) {

  const char *s1 = env->GetStringUTFChars(str1, nullptr);
  const char *s2 = env->GetStringUTFChars(str2, nullptr);
  invokeCallback(ptr, s1, s2);
  if (s1 != nullptr) {
    env->ReleaseStringUTFChars(str1, s1);
  }
  if (s2 != nullptr) {
    env->ReleaseStringUTFChars(str2, s2);
  }
}

JNIEXPORT void JNICALL Java_oxygen_bridge_FuncUtils_release(JNIEnv *env,
                                                            jclass thiz,
                                                            jlong ptr) {
  releaseCallback(ptr);
}
}
