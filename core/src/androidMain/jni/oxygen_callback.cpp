#include "log.hpp"
#include "oxygen.h"

#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <jni.h>
#include <string>

std::string extractString(JNIEnv *env1, jstring jstr) {
  const char *chars = env1->GetStringUTFChars(jstr, nullptr);
  std::string result(chars);
  env1->ReleaseStringUTFChars(jstr, chars);
  return result;
}
jstring createString(JNIEnv *env2, const std::string &str) {
  return env2->NewStringUTF(str.c_str());
}
jstring conveyStr(JNIEnv *env1, JNIEnv *env2, jstring jstr) {
  return createString(env2, extractString(env1, jstr));
}
JNIEnv *getEnv(JavaVM *vm) {
  JNIEnv *env = nullptr;
  jint attached = vm->GetEnv((void **)&env, JNI_VERSION_1_2);
  if (attached == JNI_EDETACHED) {
    attached = vm->AttachCurrentThread(&env, nullptr);
    if (attached != JNI_OK || env == nullptr) {
      LOGE("Oxygen", "Failed to attach thread to VM {}", (void *)env);
    }
  }
  return env;
}
extern "C" {
JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onWindowFocusChanged(
    JNIEnv *env, jobject thiz, jboolean value) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onWindowFocusChangedID,
                       value);
}
JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onPause(JNIEnv *env,
                                                               jobject thiz) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onPauseID);
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onResume(JNIEnv *env,
                                                                jobject thiz) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onResumeID);
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onDestroy(JNIEnv *env,
                                                                 jobject thiz) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onDestroyID);
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onConfigurationChanged(
    JNIEnv *env, jobject thiz, jstring config) {
  if (!oxygen->callback_init)
    return;
  auto envJ = getEnv(oxygen->jvm);
  envJ->CallVoidMethod(oxygen->object_callback,
                       oxygen->onConfigurationChangedID,
                       conveyStr(env, envJ, config));
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onActivityResult(
    JNIEnv *env, jobject thiz, jint requestCode, jint resultCode,
    jstring data) {
  if (!oxygen->callback_init)
    return;
  auto envJ = getEnv(oxygen->jvm);
  envJ->CallVoidMethod(oxygen->object_callback, oxygen->onActivityResultID,
                       requestCode, resultCode, conveyStr(env, envJ, data));
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onSurfaceCreated(
    JNIEnv *env, jobject thiz, jobject surface) {
  if (!oxygen->callback_init)
    return;
  auto sur = ANativeWindow_fromSurface(env, surface);
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onSurfaceCreatedID, (jlong)(intptr_t)sur);
  ANativeWindow_release(sur);
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onSurfaceChanged(
    JNIEnv *env, jobject thiz, jobject surface, jint width, jint height) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onSurfaceChangedID, width, height);

}

JNIEXPORT void JNICALL
Java_oxygen_bridge_OxygenBridge_onSurfaceDestroyed(JNIEnv *env, jobject thiz) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onSurfaceDestroyedID);

}

//

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_setCallback(
    JNIEnv *env, jclass thiz, jobject callback) {
  oxygen->object_callback = env->NewGlobalRef(callback);
  oxygen->callback_init = true;
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_logLauncher(JNIEnv *env,
                                                                  jclass clazz,
                                                                  jstring msg) {
  auto envA = getEnv(oxygen->android_jvm);
  envA->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->logID,
                       conveyStr(env, envA, msg));
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_logOS(JNIEnv *env,
                                                            jclass clazz,
                                                            jstring msg) {
  const char *msgStr = env->GetStringUTFChars(msg, nullptr);
  __android_log_print(ANDROID_LOG_INFO, "Oxygen", "%s", msgStr);
  env->ReleaseStringUTFChars(msg, msgStr);
}

JNIEXPORT jboolean JNICALL
Java_oxygen_api_LauncherBridge_isAndroid(JNIEnv *env, jclass clazz) {
  // TODO IOS?
  return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_oxygen_api_LauncherBridge_getVersion(JNIEnv *env,
                                                                 jclass clazz) {
  jclass buildVersionClass =
      getEnv(oxygen->android_jvm)->FindClass("android/os/Build$VERSION");
  jfieldID sdkIntField =
      getEnv(oxygen->android_jvm)
          ->GetStaticFieldID(buildVersionClass, "SDK_INT", "I");
  return getEnv(oxygen->android_jvm)
      ->GetStaticIntField(buildVersionClass, sdkIntField);
}

JNIEXPORT jlong JNICALL
Java_oxygen_api_LauncherBridge_getNativeHeap(JNIEnv *env, jclass clazz) {
  jclass debugClass =
      getEnv(oxygen->android_jvm)->FindClass("android/os/Debug");
  jmethodID getNativeHeapAllocatedSize =
      getEnv(oxygen->android_jvm)
          ->GetStaticMethodID(debugClass, "getNativeHeapAllocatedSize", "()J");
  return getEnv(oxygen->android_jvm)
      ->CallStaticLongMethod(debugClass, getNativeHeapAllocatedSize);
}

JNIEXPORT jboolean JNICALL Java_oxygen_api_LauncherBridge_openURI(JNIEnv *env,
                                                                  jclass clazz,
                                                                  jstring uri) {
  auto envA = getEnv(oxygen->android_jvm);
  return envA->CallBooleanMethod(oxygen->object_OxygenBridge, oxygen->openURIID,
                                 conveyStr(env, envA, uri));
}

JNIEXPORT jboolean JNICALL Java_oxygen_api_LauncherBridge_openFolder(
    JNIEnv *env, jclass clazz, jstring path) {
  auto envA = getEnv(oxygen->android_jvm);
  return envA->CallBooleanMethod(oxygen->object_OxygenBridge,
                                 oxygen->openFolderID,
                                 conveyStr(env, envA, path));
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_setClipboardText(
    JNIEnv *env, jclass clazz, jstring text) {
  auto envA = getEnv(oxygen->android_jvm);
  envA->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->setClipboardTextID,
                       conveyStr(env, envA, text));
}

JNIEXPORT jstring JNICALL
Java_oxygen_api_LauncherBridge_getClipboardText(JNIEnv *env, jclass clazz) {
  auto envA = getEnv(oxygen->android_jvm);
  return conveyStr(envA, env,
                   (jstring)envA->CallObjectMethod(oxygen->object_OxygenBridge,
                                                   oxygen->getClipboardTextID));
}

JNIEXPORT void JNICALL
Java_oxygen_api_LauncherBridge_createsurface(JNIEnv *env, jclass clazz) {
  getEnv(oxygen->android_jvm)
      ->CallObjectMethod(oxygen->object_OxygenBridge, oxygen->createsurfaceID);
}
}
