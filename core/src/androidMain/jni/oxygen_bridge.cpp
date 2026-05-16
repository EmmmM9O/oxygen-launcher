#include "callback_manager.hpp"
#include "log.hpp"
#include "object_manager.hpp"
#include "oxygen.h"

#include <cstdlib>
#include <cstring>
#include <jni.h>
#include <string>

struct Oxygen *oxygen;

__attribute__((constructor)) void env_init() {
  char *strptr_env = getenv("OXYGEN_ENVIRON");
  if (strptr_env == nullptr) {
    LOGI("Environ", "No Oxygen environ found, creating...");
    oxygen = new Oxygen();
    std::memset(oxygen, 0, sizeof(Oxygen));
    std::string addr_str = std::to_string(reinterpret_cast<uintptr_t>(oxygen));
    setenv("OXYGEN_ENVIRON", addr_str.c_str(), 1);
  } else {
    LOGI("Environ", "Found existing Oxygen environ: {}", strptr_env);
    oxygen =
        reinterpret_cast<Oxygen *>(std::strtoul(strptr_env, nullptr, 0x10));
  }
  LOGI("Environ", "Oxygen environ: {}", (void *)oxygen);
}

extern "C" {
JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_setBridge(
    JNIEnv *env, jobject thiz, jobject bridge) {
  oxygen->object_OxygenBridge = (jclass)env->NewGlobalRef(thiz);
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_setenv(JNIEnv *env,
                                                              jobject thiz,
                                                              jstring key,
                                                              jstring value) {
  const char *keyChars = env->GetStringUTFChars(key, nullptr);
  const char *valueChars = env->GetStringUTFChars(value, nullptr);

  setenv(keyChars, valueChars, 1);

  env->ReleaseStringUTFChars(key, keyChars);
  env->ReleaseStringUTFChars(value, valueChars);
}

JNIEXPORT jstring JNICALL Java_oxygen_bridge_OxygenBridge_getenv(JNIEnv *env,
                                                                 jobject thiz,
                                                                 jstring key) {
  const char *keyChars = env->GetStringUTFChars(key, nullptr);

  const char *result = getenv(keyChars);

  env->ReleaseStringUTFChars(key, keyChars);

  if (result == nullptr) {
    return nullptr;
  }

  return env->NewStringUTF(result);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  if (oxygen->android_jvm == nullptr) {
    LOGI("Oxygen", "Saving Android JVM");
    oxygen->android_jvm = vm;
    JNIEnv *env = nullptr;
    jint result = oxygen->android_jvm->AttachCurrentThread(&env, nullptr);
    if (result != JNI_OK || env == nullptr) {
      LOGE("Oxygen", "Failed to attach to JVM");
      abort();
    }
    jclass class_OxygenBridge = env->FindClass("oxygen/bridge/OxygenBridge");
    if (class_OxygenBridge == nullptr) {
      LOGE("Oxygen", "Failed to find class: oxygen/bridge/OxygenBridge");
      abort();
    }
    oxygen->class_OxygenBridge = (jclass)env->NewGlobalRef(class_OxygenBridge);
    oxygen->logID = env->GetMethodID(oxygen->class_OxygenBridge, "log",
                                     "(Ljava/lang/String;)V");
    oxygen->getClipboardTextID = env->GetMethodID(
        oxygen->class_OxygenBridge, "getClipboardText", "()Ljava/lang/String;");

    oxygen->setClipboardTextID =
        env->GetMethodID(oxygen->class_OxygenBridge, "setClipboardText",
                         "(Ljava/lang/String;)V");

    oxygen->openFolderID = env->GetMethodID(
        oxygen->class_OxygenBridge, "openFolder", "(Ljava/lang/String;)Z");

    oxygen->openURIID = env->GetMethodID(oxygen->class_OxygenBridge, "openURI",
                                         "(Ljava/lang/String;)Z");
    oxygen->createsurfaceID =
        env->GetMethodID(oxygen->class_OxygenBridge, "createsurface", "(Z)V");

    oxygen->isFinishingID =
        env->GetMethodID(oxygen->class_OxygenBridge, "isFinishing", "()Z");

    oxygen->showFileChooserID =
        env->GetMethodID(oxygen->class_OxygenBridge, "showFileChooser",
                         "(ZLjava/lang/String;JJJ[Ljava/lang/String;)V");

    oxygen->haveExternalPermissionID = env->GetMethodID(
        oxygen->class_OxygenBridge, "haveExternalPermission", "()Z");

    oxygen->getExternalPermissionID = env->GetMethodID(
        oxygen->class_OxygenBridge, "getExternalPermission", "(I)V");
    oxygen->getVersionID =
        env->GetMethodID(oxygen->class_OxygenBridge, "getVersion", "()I");
    oxygen->getNativeHeapID =
        env->GetMethodID(oxygen->class_OxygenBridge, "getNativeHeap", "()J");
    oxygen->hideID =
        env->GetMethodID(oxygen->class_OxygenBridge, "hide", "()V");
    oxygen->beginForceLandscapeID = env->GetMethodID(
        oxygen->class_OxygenBridge, "beginForceLandscape", "()V");
    oxygen->endForceLandscapeID = env->GetMethodID(oxygen->class_OxygenBridge,
                                                   "endForceLandscape", "()V");
    oxygen->postCacheFileID = env->GetMethodID(
        oxygen->class_OxygenBridge, "postCacheFile", "(Ljava/lang/String;)V");

    oxygen->setAllSettingsID = env->GetMethodID(
        oxygen->class_OxygenBridge, "setAllSettings", "(Ljava/lang/String;)V");
    oxygen->setGameSettingsID = env->GetMethodID(
        oxygen->class_OxygenBridge, "setGameSettings", "(Ljava/lang/String;)V");
    oxygen->setGameDefaultID = env->GetMethodID(
        oxygen->class_OxygenBridge, "setGameDefault", "(Ljava/lang/String;)V");

    oxygen->getAllSettingsID = env->GetMethodID(
        oxygen->class_OxygenBridge, "getAllSettings", "()Ljava/lang/String;");
    oxygen->getGameSettingsID = env->GetMethodID(
        oxygen->class_OxygenBridge, "getGameSettings", "()Ljava/lang/String;");

    oxygen->startLoopID =
        env->GetMethodID(oxygen->class_OxygenBridge, "startLoop", "()V");
    oxygen->endLoopID =
        env->GetMethodID(oxygen->class_OxygenBridge, "endLoop", "()V");
    oxygen->setupInputID =
        env->GetMethodID(oxygen->class_OxygenBridge, "setupInput", "()V");

    oxygen->getTextInputID = env->GetMethodID(
        oxygen->class_OxygenBridge, "getTextInput",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZIZJJJ)V");
    oxygen->isShowingTextInputID = env->GetMethodID(
        oxygen->class_OxygenBridge, "isShowingTextInput", "()Z");
    oxygen->setOnscreenKeyboardVisibleID = env->GetMethodID(
        oxygen->class_OxygenBridge, "setOnscreenKeyboardVisible", "(Z)V");
    oxygen->vibrate1ID =
        env->GetMethodID(oxygen->class_OxygenBridge, "vibrate", "(I)V");
    oxygen->vibrate2ID =
        env->GetMethodID(oxygen->class_OxygenBridge, "vibrate", "([JI)V");
    oxygen->cancelVibrateID =
        env->GetMethodID(oxygen->class_OxygenBridge, "cancelVibrate", "()V");
  } else if (oxygen->android_jvm != vm) {
    LOGI("Oxygen", "Saving JVM");
    oxygen->jvm = vm;
    JNIEnv *env = nullptr;
    jint result = oxygen->jvm->AttachCurrentThread(&env, nullptr);
    if (result != JNI_OK || env == nullptr) {
      LOGE("Oxygen", "Failed to attach to JVM");
      abort();
    }
    jclass class_callback = env->FindClass("oxygen/api/LauncherBridgeCallback");
    if (class_callback == nullptr) {
      LOGE("Oxygen", "Failed to find class: oxygen/api/LauncherBridgeCallback");
      abort();
    }
    oxygen->class_callback = (jclass)env->NewGlobalRef(class_callback);

    oxygen->loopID = env->GetMethodID(oxygen->class_callback, "loop", "()V");
    oxygen->onWindowFocusChangedID = env->GetMethodID(
        oxygen->class_callback, "onWindowFocusChanged", "(Z)V");
    oxygen->onPauseID =
        env->GetMethodID(oxygen->class_callback, "onPause", "()V");
    oxygen->onResumeID =
        env->GetMethodID(oxygen->class_callback, "onResume", "()V");
    oxygen->onDestroyID =
        env->GetMethodID(oxygen->class_callback, "onDestroy", "()V");
    oxygen->onExitID =
        env->GetMethodID(oxygen->class_callback, "onExit", "()V");
    oxygen->onConfigurationChangedID =
        env->GetMethodID(oxygen->class_callback, "onConfigurationChanged",
                         "(Ljava/lang/String;)V");
    oxygen->onActivityResultID = env->GetMethodID(
        oxygen->class_callback, "onActivityResult", "(IILjava/lang/String;)V");
    oxygen->onSurfaceCreatedID =
        env->GetMethodID(oxygen->class_callback, "onSurfaceCreated", "(J)V");
    oxygen->onSurfaceChangedID =
        env->GetMethodID(oxygen->class_callback, "onSurfaceChanged", "(II)V");
    oxygen->onSurfaceDestroyedID =
        env->GetMethodID(oxygen->class_callback, "onSurfaceDestroyed", "()V");
    oxygen->handleTouchID =
        env->GetMethodID(oxygen->class_callback, "handleTouch", "([I[F)Z");
    oxygen->handleGenericMotionID = env->GetMethodID(
        oxygen->class_callback, "handleGenericMotion", "([I[F)Z");
    oxygen->handleKeyID = env->GetMethodID(oxygen->class_callback, "handleKey",
                                           "(I[ILjava/lang/String;)Z");

    jclass clazz = env->FindClass("oxygen/api/VoidFunc");
    if (clazz == nullptr) {
      LOGE("Oxygen", "Failed to find class: oxygen/api/VoidFunc");
      abort();
    }
    oxygen->VoidFuncInvokeID = env->GetMethodID(clazz, "invoke", "()V");

    clazz = env->FindClass("oxygen/api/StrCons");
    if (clazz == nullptr) {
      LOGE("Oxygen", "Failed to find class: oxygen/api/StrCons");
      abort();
    }
    oxygen->StrConsInvokeID =
        env->GetMethodID(clazz, "invoke", "(Ljava/lang/String;)V");
  }
  return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
  LOGI("Oxygen", "JNI Unload {}", (void *)vm);
  releaseJNI(vm);
}
}
void releaseJNI(JavaVM *vm) {
  if (vm == oxygen->android_jvm) {
    LOGI("Oxygen", "Release Oxygen JNI Android");
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) !=
        JNI_OK) {
      return;
    }
    if (oxygen->class_OxygenBridge != nullptr) {
      env->DeleteGlobalRef(oxygen->class_OxygenBridge);
      oxygen->class_OxygenBridge = nullptr;
    }
    if (oxygen->object_OxygenBridge != nullptr) {
      env->DeleteGlobalRef(oxygen->object_OxygenBridge);
      oxygen->object_OxygenBridge = nullptr;
    }
    callback::CallbackManager::instance().clear();
    JNIRefManager::instanceAndroid().clear(env);
  } else if (vm == oxygen->jvm) {
    LOGI("Oxygen", "Release Oxygen JNI JVM");
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) !=
        JNI_OK) {
      return;
    }
    if (oxygen->class_callback != nullptr) {
      env->DeleteGlobalRef(oxygen->class_callback);
      oxygen->class_callback = nullptr;
    }
    if (oxygen->object_callback != nullptr) {
      env->DeleteGlobalRef(oxygen->object_callback);
      oxygen->object_callback = nullptr;
      oxygen->callback_init = false;
    }
    JNIRefManager::instanceJVM().clear(env);
  }
}
