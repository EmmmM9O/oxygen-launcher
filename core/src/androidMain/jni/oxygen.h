
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

  jmethodID onWindowFocusChangedID;
  jmethodID onPauseID;
  jmethodID onResumeID;
  jmethodID onDestroyID;
  jmethodID onConfigurationChangedID;
  jmethodID onActivityResultID;
  jmethodID onSurfaceCreatedID;
  jmethodID onSurfaceChangedID;
  jmethodID onSurfaceDestroyedID;
};

extern struct Oxygen *oxygen;

[[noreturn]] void nominal_exit(int code);

#endif // OXYGEN_H
