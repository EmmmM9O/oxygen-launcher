#include "log.hpp"
#include "oxygen.h"
#include <android/log.h>
#include <atomic>
#include <bytehook.h>
#include <csignal>
#include <cstddef>
#include <dlfcn.h>
#include <jni.h>
#include <pthread.h>
#include <unistd.h>

using android_update_LD_LIBRARY_PATH_t = void (*)(const char *);

static int oxyFd[2];
static pthread_t logger;
static volatile jmethodID log_method;
static JavaVM *log_pipe_jvm;

static volatile jobject exitTrap_bridge;
static volatile jmethodID exitTrap_method;
static JavaVM *exitTrap_jvm;

static std::atomic<bool> exit_tripped{false};

void correctUtfBytes(std::string &bytes) {
  size_t i = 0;
  while (i < bytes.size()) {
    uint8_t utf8 = static_cast<uint8_t>(bytes[i]);

    switch (utf8 >> 4) {
    case 0x00:
    case 0x01:
    case 0x02:
    case 0x03:
    case 0x04:
    case 0x05:
    case 0x06:
    case 0x07:
      ++i;
      break;

    case 0x08:
    case 0x09:
    case 0x0A:
    case 0x0B:
    case 0x0F:
      bytes[i] = '?';
      ++i;
      break;

    case 0x0E:
      if (i + 2 >= bytes.size() ||
          (static_cast<uint8_t>(bytes[i + 1]) & 0xC0) != 0x80 ||
          (static_cast<uint8_t>(bytes[i + 2]) & 0xC0) != 0x80) {
        bytes[i] = '?';
        ++i;
      } else {
        i += 3;
      }
      break;

    case 0x0C:
    case 0x0D:
      if (i + 1 >= bytes.size() ||
          (static_cast<uint8_t>(bytes[i + 1]) & 0xC0) != 0x80) {
        bytes[i] = '?';
        ++i;
      } else {
        i += 2;
      }
      break;

    default:
      bytes[i] = '?';
      ++i;
      break;
    }
  }
}

static void *logger_thread(void *) {
  JNIEnv *env = nullptr;
  JavaVM *vm = oxygen->android_jvm;
  if (vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
    oxygen->logFile = nullptr;
    LOGE("Oxygen", "Failed to attach logger thread");
    return nullptr;
  }

  std::string buffer;
  buffer.reserve(2048);
  jstring jstr;
  while (true) {
    buffer.resize(2047);
    ssize_t bytes_read = read(oxyFd[0], buffer.data(), buffer.capacity() - 1);
    if (bytes_read < 0) {
      oxygen->logFile = nullptr;
      LOGE("Oxygen", "Failed to attach logger thread");
      close(oxyFd[0]);
      close(oxyFd[1]);
      vm->DetachCurrentThread();
      return nullptr;
    }

    if (bytes_read == 0)
      continue;

    buffer.resize(bytes_read);
    if (buffer.empty())
      continue;

    correctUtfBytes(buffer);

    jstr = env->NewStringUTF(buffer.c_str());
    env->CallVoidMethod(oxygen->object_OxygenBridge, log_method, jstr);
    env->DeleteLocalRef(jstr);
  }
}

[[noreturn]] void nominal_exit(int code) {
  JNIEnv *env = nullptr;
  jint errorCode =
      exitTrap_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
  if (errorCode == JNI_EDETACHED)
    errorCode = exitTrap_jvm->AttachCurrentThread(&env, nullptr);
  if (errorCode != JNI_OK)
    killpg(getpgrp(), SIGTERM);

  env->CallVoidMethod(exitTrap_bridge, exitTrap_method, code);

  env->DeleteGlobalRef(exitTrap_bridge);
  jclass systemClass = env->FindClass("java/lang/System");
  jmethodID exitMethod = env->GetStaticMethodID(systemClass, "exit", "(I)V");
  env->CallStaticVoidMethod(systemClass, exitMethod, 0);

  while (true) {
  }
}
static void custom_exit(int code) {
  log_android(code == 0 ? ANDROID_LOG_INFO : ANDROID_LOG_ERROR, "Oxygen",
              "JVM exit with code {}", code);
  if (exit_tripped)
    return;
  if (!exit_tripped.exchange(true)) {
    nominal_exit(code);
  }
}
static void custom_atexit() {
  if (exit_tripped)
    return;
  if (!exit_tripped.exchange(true)) {
    nominal_exit(0);
  }
}

extern "C" {
JNIEXPORT jint JNICALL Java_oxygen_bridge_OxygenBridge_chdir(JNIEnv *env,
                                                             jobject jobject,
                                                             jstring path) {
  char const *dir = env->GetStringUTFChars(path, 0);

  int b = chdir(dir);

  env->ReleaseStringUTFChars(path, dir);
  return b;
}

JNIEXPORT jint JNICALL Java_oxygen_bridge_OxygenBridge_dlopen(JNIEnv *env,
                                                              jobject jobject,
                                                              jstring str) {
  dlerror();
  char const *lib_name = env->GetStringUTFChars(str, 0);
  void *handle;
  dlerror();
  handle = dlopen(lib_name, RTLD_GLOBAL | RTLD_LAZY);

  char *error = dlerror();
  if (error != NULL && handle == NULL) {
    LOGE("Oxygen", "DLOPEN: loading {} (error = {})", lib_name, error);
  } else {
    LOGI("Oxygen", "DLOPEN: loading {}", lib_name);
  }

  env->ReleaseStringUTFChars(str, lib_name);
  return (jlong)handle;
}

JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_setLdLibraryPath(
    JNIEnv *env, jobject jobject, jstring ldLibraryPath) {
  void *libdl_handle = dlopen("libdl.so", RTLD_LAZY);
  if (!libdl_handle) {
    LOGE("Oxygen", "Failed to dlopen libdl.so: {}", dlerror());
    return;
  }
  void *sym = dlsym(libdl_handle, "android_update_LD_LIBRARY_PATH");
  if (!sym) {
    sym = dlsym(libdl_handle, "__loader_android_update_LD_LIBRARY_PATH");
    if (!sym) {
      const char *error = dlerror();
      log_android(error ? ANDROID_LOG_ERROR : ANDROID_LOG_INFO, "Oxygen",
                  "loading libdl.so (error = {})", error);
    }
  }
  auto android_update_LD_LIBRARY_PATH =
      reinterpret_cast<android_update_LD_LIBRARY_PATH_t>(sym);

  if (!android_update_LD_LIBRARY_PATH) {
    LOGE("Oxygen", "Failed to resolve android_update_LD_LIBRARY_PATH");
    dlclose(libdl_handle);
    return;
  }
  const char *ldLibPathUtf = env->GetStringUTFChars(ldLibraryPath, nullptr);
  android_update_LD_LIBRARY_PATH(ldLibPathUtf);
  env->ReleaseStringUTFChars(ldLibraryPath, ldLibPathUtf);
  dlclose(libdl_handle);
}
JNIEXPORT jint JNICALL Java_oxygen_bridge_OxygenBridge_redirectStdio(
    JNIEnv *env, jobject jobject, jstring path) {
  // TODO may be useless and bad
  setvbuf(stdout, 0, _IOLBF, 0);
  setvbuf(stderr, 0, _IONBF, 0);
  if (pipe(oxyFd) < 0) {
    LOGE("Oxygen", "Failed to create log pipe!");
    return 1;
  }
  if (dup2(oxyFd[1], STDOUT_FILENO) != STDOUT_FILENO) {
    LOGE("Oxygen", "Failed to redirect stdout!");
    return 2;
  }
  if (dup2(oxyFd[1], STDERR_FILENO) != STDERR_FILENO) {
    LOGE("Oxygen", "Failed to redirect stderr!");
    return 2;
  }
  jclass bridge = env->FindClass("oxygen/bridge/OxygenBridge");
  if (bridge == nullptr) {
    LOGE("Oxygen", "Failed to find OxygenBridge class!");
    return 4;
  }

  log_method = env->GetMethodID(bridge, "log", "(Ljava/lang/String;)V");
  if (log_method == nullptr) {
    LOGE("Oxygen", "Failed to find log method!");
    env->DeleteLocalRef(bridge);
    return 4;
  }
  oxygen->logFile = fdopen(oxyFd[1], "a");
  log_internal("Log pipe ready.");
  env->GetJavaVM(&log_pipe_jvm);
  int result = pthread_create(&logger, nullptr, logger_thread, nullptr);
  if (result != 0) {
    LOGE("Oxygen", "Failed to create logger thread!");
    return 5;
  }
  pthread_detach(logger);

  return 0;
}
JNIEXPORT void JNICALL Java_oxygen_bridge_OxygenBridge_setupExitTrap(
    JNIEnv *env, jobject jobject1, jobject bridge) {
  exitTrap_bridge = env->NewGlobalRef(bridge);
  env->GetJavaVM(&exitTrap_jvm);
  jclass exitTrap_exitClass =
      (jclass)env->NewGlobalRef(env->FindClass("oxygen/bridge/OxygenBridge"));
  exitTrap_method = env->GetMethodID(exitTrap_exitClass, "exit", "(I)V");
  env->DeleteGlobalRef(exitTrap_exitClass);
  void *bytehook_handle = dlopen("libbytehook.so", RTLD_NOW);
  char *error = dlerror();
  if (error != NULL && bytehook_handle == NULL) {
    LOGE("Oxygen", "DLOPEN: loading bytehook (error = {})", error);
  } else {
    LOGI("Oxygen", "DLOPEN: loading bytehook");
  }

  using bytehook_hook_all_t = bytehook_stub_t (*)(
      const char *callee_path_name, const char *sym_name, void *new_func,
      bytehook_hooked_t hooked, void *hooked_arg);
  using bytehook_init_t = int (*)(int mode, bool debug);
  auto bytehook_hook_all_p =
      (bytehook_hook_all_t)dlsym(bytehook_handle, "bytehook_hook_all");
  auto bytehook_init_p =
      (bytehook_init_t)dlsym(bytehook_handle, "bytehook_init");
  if (bytehook_hook_all_p == nullptr || bytehook_init_p == nullptr) {
  }
  int bhook_status = bytehook_init_p(BYTEHOOK_MODE_AUTOMATIC, false);
  if (bhook_status == BYTEHOOK_STATUS_CODE_OK) {
    bytehook_stub_t stub = bytehook_hook_all_p(
        nullptr, "exit", (void *)custom_exit, nullptr, nullptr);
    LOGI("Oxygen", "Exit hook initialized, stub={}", (void *)stub);
  } else {
    LOGE("Oxygen", "bytehook_init failed {}", bhook_status);
    atexit(custom_atexit);
  }
}
}
