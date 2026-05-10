
#ifndef OXYGEN_H
#define OXYGEN_H

#include <jni.h>
#include <stdlib.h>

struct Oxygen {
  JavaVM *android_jvm;
  jclass class_OxygenBridge;
  jobject object_OxygenBridge;
  FILE *logFile;
  JavaVM *jvm;
  jclass class_callback;
  jobject object_callback;
  bool callback_init;

  jmethodID logID;
  jmethodID openURIID;
  jmethodID openFolderID;
  jmethodID setClipboardTextID;
  jmethodID getClipboardTextID;
  jmethodID createsurfaceID;
  jmethodID isFinishingID;
  jmethodID getTextInputID;
  jmethodID isShowingTextInputID;
  jmethodID setOnscreenKeyboardVisibleID;
  jmethodID vibrate1ID;
  jmethodID vibrate2ID;
  jmethodID cancelVibrateID;
  jmethodID showFileChooserID;
  jmethodID haveExternalPermissionID;
  jmethodID getExternalPermissionID;
  jmethodID getVersionID;
  jmethodID getNativeHeapID;
  jmethodID hideID;
  jmethodID beginForceLandscapeID;
  jmethodID endForceLandscapeID;
  jmethodID postCacheFileID;
  jmethodID setAllSettingsID;
  jmethodID getAllSettingsID;
  jmethodID setGameSettingsID;
  jmethodID getGameSettingsID;
  jmethodID setGameDefaultID;
  jmethodID startLoopID;
  jmethodID endLoopID;

  jmethodID loopID;
  jmethodID onWindowFocusChangedID;
  jmethodID onPauseID;
  jmethodID onResumeID;
  jmethodID onDestroyID;
  jmethodID onExitID;
  jmethodID onConfigurationChangedID;
  jmethodID onActivityResultID;
  jmethodID onSurfaceCreatedID;
  jmethodID onSurfaceChangedID;
  jmethodID onSurfaceDestroyedID;
  jmethodID handleTouchID;
  jmethodID handleGenericMotionID;
  jmethodID handleKeyID;

  jmethodID VoidFuncInvokeID;
  jmethodID StrConsInvokeID;
};

extern struct Oxygen *oxygen;

[[noreturn]] void nominal_exit(int code);

#endif // OXYGEN_H
