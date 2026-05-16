#include "log.hpp"
#include "object_manager.hpp"
#include "oxygen.h"
#include "oxygen_func.hpp"

#include <android/input.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <jni.h>
#include <string>

std::string extractString(JNIEnv *env1, jstring jstr) {
  if (jstr == nullptr) {
    return std::string();
  }
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
#define DEFINE_CONVEY_ARRAY(ElementType, jArrayType, GetElements,              \
                            ReleaseElements, NewArray, SetRegion)              \
  jArrayType convey##jArrayType(JNIEnv *envSrc, JNIEnv *envDst,                \
                                jArrayType srcArray) {                         \
    if (srcArray == NULL)                                                      \
      return NULL;                                                             \
    jsize length = envSrc->GetArrayLength(srcArray);                           \
    ElementType *srcElements = envSrc->GetElements(srcArray, NULL);            \
    if (srcElements == NULL)                                                   \
      return NULL;                                                             \
    jArrayType dstArray = envDst->NewArray(length);                            \
    if (dstArray == NULL) {                                                    \
      envSrc->ReleaseElements(srcArray, srcElements, JNI_ABORT);               \
      return NULL;                                                             \
    }                                                                          \
    envDst->SetRegion(dstArray, 0, length, srcElements);                       \
    envSrc->ReleaseElements(srcArray, srcElements, JNI_ABORT);                 \
    return dstArray;                                                           \
  }

DEFINE_CONVEY_ARRAY(jint, jintArray, GetIntArrayElements,
                    ReleaseIntArrayElements, NewIntArray, SetIntArrayRegion)
DEFINE_CONVEY_ARRAY(jfloat, jfloatArray, GetFloatArrayElements,
                    ReleaseFloatArrayElements, NewFloatArray,
                    SetFloatArrayRegion)
DEFINE_CONVEY_ARRAY(jlong, jlongArray, GetLongArrayElements,
                    ReleaseLongArrayElements, NewLongArray, SetLongArrayRegion)

std::vector<std::string> extractStringArray(JNIEnv *env,
                                            jobjectArray javaArray) {
  std::vector<std::string> result;
  if (javaArray == nullptr)
    return result;

  jsize len = env->GetArrayLength(javaArray);
  result.reserve(len);

  for (jsize i = 0; i < len; i++) {
    jstring str = (jstring)env->GetObjectArrayElement(javaArray, i);
    const char *chars = env->GetStringUTFChars(str, nullptr);
    result.emplace_back(chars);
    env->ReleaseStringUTFChars(str, chars);
    env->DeleteLocalRef(str);
  }
  return result;
}
jobjectArray createStringArray(JNIEnv *env,
                               const std::vector<std::string> &vec) {
  if (vec.empty())
    return nullptr;

  jclass stringClass = env->FindClass("java/lang/String");
  jobjectArray result =
      env->NewObjectArray(static_cast<jsize>(vec.size()), stringClass, nullptr);

  for (size_t i = 0; i < vec.size(); i++) {
    jstring str = env->NewStringUTF(vec[i].c_str());
    env->SetObjectArrayElement(result, static_cast<jsize>(i), str);
    env->DeleteLocalRef(str);
  }
  return result;
}
jobjectArray conveyStrArray(JNIEnv *env1, JNIEnv *env2, jobjectArray jarr) {
  return createStringArray(env2, extractStringArray(env1, jarr));
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
JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_loop(JNIEnv *env,
                                                            jobject thiz) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)->CallVoidMethod(oxygen->object_callback, oxygen->loopID);
}
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

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onExit(JNIEnv *env,
                                                              jobject thiz) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onExitID);
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
      ->CallVoidMethod(oxygen->object_callback, oxygen->onSurfaceCreatedID,
                       (jlong)(intptr_t)sur);
  ANativeWindow_release(sur);
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_onSurfaceChanged(
    JNIEnv *env, jobject thiz, jobject surface, jint width, jint height) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onSurfaceChangedID,
                       width, height);
}

JNIEXPORT void JNICALL
Java_oxygen_bridge_OxygenBridge_onSurfaceDestroyed(JNIEnv *env, jobject thiz) {
  if (!oxygen->callback_init)
    return;
  getEnv(oxygen->jvm)
      ->CallVoidMethod(oxygen->object_callback, oxygen->onSurfaceDestroyedID);
}

JNIEXPORT jboolean JNICALL Java_oxygen_bridge_OxygenBridge_handleTouch(
    JNIEnv *env, jobject thiz, jintArray intData, jfloatArray floatData) {
  if (!oxygen->callback_init)
    return true;
  auto envJ = getEnv(oxygen->jvm);
  return envJ->CallBooleanMethod(oxygen->object_callback, oxygen->handleTouchID,
                                 conveyjintArray(env, envJ, intData),
                                 conveyjfloatArray(env, envJ, floatData));
}

JNIEXPORT jboolean JNICALL Java_oxygen_bridge_OxygenBridge_handleGenericMotion(
    JNIEnv *env, jobject thiz, jintArray intData, jfloatArray floatData) {
  if (!oxygen->callback_init)
    return true;
  auto envJ = getEnv(oxygen->jvm);
  return envJ->CallBooleanMethod(oxygen->object_callback, oxygen->handleTouchID,
                                 conveyjintArray(env, envJ, intData),
                                 conveyjfloatArray(env, envJ, floatData));
}

JNIEXPORT jboolean JNICALL Java_oxygen_bridge_OxygenBridge_handleKey(
    JNIEnv *env, jobject thiz, jint keyCode, jintArray intData, jstring str) {
  if (!oxygen->callback_init)
    return true;
  auto envJ = getEnv(oxygen->jvm);
  return envJ->CallBooleanMethod(oxygen->object_callback, oxygen->handleKeyID,
                                 keyCode, conveyjintArray(env, envJ, intData),
                                 conveyStr(env, envJ, str));
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
                                                            jint prio,
                                                            jstring msg) {
  const char *msgStr = env->GetStringUTFChars(msg, nullptr);
  __android_log_write(prio, "Oxygen", msgStr);
  env->ReleaseStringUTFChars(msg, msgStr);
}

JNIEXPORT jboolean JNICALL
Java_oxygen_api_LauncherBridge_isAndroid(JNIEnv *env, jclass clazz) {
  // TODO IOS?
  return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_oxygen_api_LauncherBridge_getVersion(JNIEnv *env,
                                                                 jclass clazz) {
  return getEnv(oxygen->android_jvm)
      ->CallIntMethod(oxygen->object_OxygenBridge, oxygen->getVersionID);
  /*
  jclass buildVersionClass =
      getEnv(oxygen->android_jvm)->FindClass("android/os/Build$VERSION");
  jfieldID sdkIntField =
      getEnv(oxygen->android_jvm)
          ->GetStaticFieldID(buildVersionClass, "SDK_INT", "I");
  return getEnv(oxygen->android_jvm)
      ->GetStaticIntField(buildVersionClass, sdkIntField);*/
}

JNIEXPORT jlong JNICALL
Java_oxygen_api_LauncherBridge_getNativeHeap(JNIEnv *env, jclass clazz) {
  return getEnv(oxygen->android_jvm)
      ->CallLongMethod(oxygen->object_OxygenBridge, oxygen->getNativeHeapID);
  /*
  jclass debugClass =
      getEnv(oxygen->android_jvm)->FindClass("android/os/Debug");
  jmethodID getNativeHeapAllocatedSize =
      getEnv(oxygen->android_jvm)
          ->GetStaticMethodID(debugClass, "getNativeHeapAllocatedSize", "()J");
  return getEnv(oxygen->android_jvm)
      ->CallStaticLongMethod(debugClass, getNativeHeapAllocatedSize);
  */
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

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_showFileChooser(
    JNIEnv *env, jclass clazz, jboolean open, jstring title, jobject cons,
    jobject error, jobjectArray extensions) {
  auto consJ = JNIRefManager::instanceJVM().create(env, cons);
  auto errorJ = JNIRefManager::instanceJVM().create(env, error);
  auto envA = getEnv(oxygen->android_jvm);
  envA->CallVoidMethod(
      oxygen->object_OxygenBridge, oxygen->showFileChooserID, open,
      conveyStr(env, envA, title), createCallback([consJ](const char *text) {
        std::string str(text);
        auto env = getEnv(oxygen->jvm);
        env->CallVoidMethod(JNIRefManager::instanceJVM().get(consJ),
                            oxygen->StrConsInvokeID, createString(env, str));
      }),
      createCallback([errorJ]() {
        getEnv(oxygen->jvm)
            ->CallVoidMethod(JNIRefManager::instanceJVM().get(errorJ),
                             oxygen->VoidFuncInvokeID);
      }),
      createCallback([consJ, errorJ]() {
        auto env = getEnv(oxygen->jvm);
        JNIRefManager::instanceJVM().release(env, consJ);
        JNIRefManager::instanceJVM().release(env, errorJ);
      }),
      conveyStrArray(env, envA, extensions));
}

JNIEXPORT jboolean JNICALL
Java_oxygen_api_LauncherBridge_haveExternalPermission(JNIEnv *env,
                                                      jclass clazz) {
  return getEnv(oxygen->android_jvm)
      ->CallBooleanMethod(oxygen->object_OxygenBridge,
                          oxygen->haveExternalPermissionID);
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_getExternalPermission(
    JNIEnv *env, jclass clazz, jint code) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge,
                       oxygen->getExternalPermissionID);
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_hide(JNIEnv *env,
                                                           jclass clazz) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->hideID);
}

JNIEXPORT void JNICALL
Java_oxygen_api_LauncherBridge_beginForceLandscape(JNIEnv *env, jclass clazz) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge,
                       oxygen->beginForceLandscapeID);
}

JNIEXPORT void JNICALL
Java_oxygen_api_LauncherBridge_endForceLandscape(JNIEnv *env, jclass clazz) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge,
                       oxygen->endForceLandscapeID);
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_setAllSettings(
    JNIEnv *env, jclass clazz, jstring text) {
  auto envA = getEnv(oxygen->android_jvm);
  envA->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->setAllSettingsID,
                       conveyStr(env, envA, text));
}

JNIEXPORT jstring JNICALL
Java_oxygen_api_LauncherBridge_getAllSettings(JNIEnv *env, jclass clazz) {
  auto envA = getEnv(oxygen->android_jvm);
  return conveyStr(envA, env,
                   (jstring)envA->CallObjectMethod(oxygen->object_OxygenBridge,
                                                   oxygen->getAllSettingsID));
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_setGameSettings(
    JNIEnv *env, jclass clazz, jstring text) {
  auto envA = getEnv(oxygen->android_jvm);
  envA->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->setGameSettingsID,
                       conveyStr(env, envA, text));
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_setGameDefault(
    JNIEnv *env, jclass clazz, jstring text) {
  auto envA = getEnv(oxygen->android_jvm);
  envA->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->setGameDefaultID,
                       conveyStr(env, envA, text));
}

JNIEXPORT jstring JNICALL
Java_oxygen_api_LauncherBridge_getGameSettings(JNIEnv *env, jclass clazz) {
  auto envA = getEnv(oxygen->android_jvm);
  return conveyStr(envA, env,
                   (jstring)envA->CallObjectMethod(oxygen->object_OxygenBridge,
                                                   oxygen->getGameSettingsID));
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_startLoop(JNIEnv *env,
                                                                jclass clazz) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->startLoopID);
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_endLoop(JNIEnv *env,
                                                              jclass clazz) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->endLoopID);
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_setupInput(JNIEnv *env,
                                                                 jclass clazz) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->setupInputID);
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_postCacheFile(
    JNIEnv *env, jclass clazz, jstring str) {
  auto envA = getEnv(oxygen->android_jvm);
  envA->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->postCacheFileID,
                       conveyStr(env, envA, str));
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_createsurface(
    JNIEnv *env, jclass clazz, jboolean create) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->createsurfaceID,
                       create);
}

JNIEXPORT jboolean JNICALL
Java_oxygen_api_LauncherBridge_isFinishing(JNIEnv *env, jclass clazz) {
  return getEnv(oxygen->android_jvm)
      ->CallBooleanMethod(oxygen->object_OxygenBridge, oxygen->isFinishingID);
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_getTextInput(
    JNIEnv *env, jclass clazz, jstring title, jstring message, jstring text,
    jboolean numeric, jboolean multiline, jint maxLength, jboolean allowEmpty,
    jobject onAccepted, jobject onCanceled) {
  auto envA = getEnv(oxygen->android_jvm);

  auto accepted = JNIRefManager::instanceJVM().create(env, onAccepted);
  auto cancled = JNIRefManager::instanceJVM().create(env, onCanceled);
  envA->CallVoidMethod(
      oxygen->object_OxygenBridge, oxygen->getTextInputID,
      conveyStr(env, envA, title), conveyStr(env, envA, message),
      conveyStr(env, envA, text), numeric, multiline, maxLength, allowEmpty,
      createCallback([accepted](const char *text) {
        std::string str(text);
        auto env = getEnv(oxygen->jvm);
        env->CallVoidMethod(JNIRefManager::instanceJVM().get(accepted),
                            oxygen->StrConsInvokeID, createString(env, str));
      }),
      createCallback([cancled]() {
        getEnv(oxygen->jvm)
            ->CallVoidMethod(JNIRefManager::instanceJVM().get(cancled),
                             oxygen->VoidFuncInvokeID);
      }),
      createCallback([accepted, cancled]() {
        auto env = getEnv(oxygen->jvm);
        JNIRefManager::instanceJVM().release(env, accepted);
        JNIRefManager::instanceJVM().release(env, cancled);
      }));
}

JNIEXPORT jboolean JNICALL
Java_oxygen_api_LauncherBridge_isShowingTextInput(JNIEnv *env, jclass clazz) {
  return getEnv(oxygen->android_jvm)
      ->CallBooleanMethod(oxygen->object_OxygenBridge,
                          oxygen->isShowingTextInputID);
}

JNIEXPORT void JNICALL
Java_oxygen_api_LauncherBridge_setOnscreenKeyboardVisible(JNIEnv *env,
                                                          jclass clazz,
                                                          jboolean visible) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge,
                       oxygen->setOnscreenKeyboardVisibleID, visible);
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_vibrate__I(
    JNIEnv *env, jclass clazz, jint milliseconds) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->vibrate1ID,
                       milliseconds);
}

JNIEXPORT void JNICALL Java_oxygen_api_LauncherBridge_vibrate__JI(
    JNIEnv *env, jclass clazz, jlongArray pattern, jint repeat) {
  auto envA = getEnv(oxygen->android_jvm);
  envA->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->vibrate2ID,
                       conveyjlongArray(env, envA, pattern), repeat);
}

JNIEXPORT void JNICALL
Java_oxygen_api_LauncherBridge_cancelVibrate(JNIEnv *env, jclass clazz) {
  getEnv(oxygen->android_jvm)
      ->CallVoidMethod(oxygen->object_OxygenBridge, oxygen->cancelVibrateID);
}
}
