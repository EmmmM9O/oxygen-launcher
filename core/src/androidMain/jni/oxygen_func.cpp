#include "oxygen_func.hpp"
#include "jni.h"
extern "C" {
JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_CallVoid(JNIEnv *env,
                                                                jobject thiz,
                                                                jlong ptr) {
  invokeCallbackOnce(ptr);
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_CallStrCons(JNIEnv *env,
                                                                   jobject thiz,
                                                                   jstring text,
                                                                   jlong ptr) {

  const char *nativeText = env->GetStringUTFChars(text, nullptr);
  invokeCallbackOnce(ptr, nativeText);
  if (nativeText != nullptr) {
    env->ReleaseStringUTFChars(text, nativeText);
  }
}
}
