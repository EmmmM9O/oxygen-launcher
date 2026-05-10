#include "log.hpp"
#include "oxygen.h"

#include <dlfcn.h>
#include <errno.h>
#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

// Uncomment to try redirect signal handling to JVM
// #define TRY_SIG2JVM
//
// PojavLancher: fixme: are these wrong?
#define FULL_VERSION "1.8.0-internal"
#define DOT_VERSION "1.8"

static const char *const_progname = "java";
static const char *const_launcher = "openjdk";
static const char **const_jargs = NULL;
static const char **const_appclasspath = NULL;
static const jboolean const_javaw = JNI_FALSE;
static const jboolean const_cpwildcard = JNI_TRUE;
static const jint const_ergo_class = 0; // DEFAULT_POLICY

char **convert_to_char_array(JNIEnv *env, jobjectArray jstringArray) {
  int num_rows = env->GetArrayLength(jstringArray);
  char **cArray = (char **)malloc(num_rows * sizeof(char *));
  jstring row;

  for (int i = 0; i < num_rows; i++) {
    row = (jstring)env->GetObjectArrayElement(jstringArray, i);
    cArray[i] = (char *)env->GetStringUTFChars(row, 0);
  }

  return cArray;
}

void free_char_array(JNIEnv *env, jobjectArray jstringArray, char **charArray) {
  int num_rows = env->GetArrayLength(jstringArray);
  jstring row;

  for (int i = 0; i < num_rows; i++) {
    row = (jstring)env->GetObjectArrayElement(jstringArray, i);
    env->ReleaseStringUTFChars(row, charArray[i]);
  }
}
typedef jint JLI_Launch_func(int argc, char **argv, /* main argc, argc */
                             int jargc, const char **jargv, /* java args */
                             int appclassc,
                             const char **appclassv,  /* app classpath */
                             const char *fullversion, /* full version defined */
                             const char *dotversion,  /* dot version defined */
                             const char *pname,       /* program name */
                             const char *lname,       /* launcher name */
                             jboolean javaargs,       /* JAVA_ARGS */
                             jboolean cpwildcard,     /* classpath wildcard*/
                             jboolean javaw,          /* windows-only javaw */
                             jint ergo /* ergonomics class policy */
);

struct {
  sigset_t tracked_sigset;
  int pipe[2];
} abort_waiter_data;

_Noreturn static void *abort_waiter_thread(void *extraArg) {
  // Don't allow this thread to receive signals this thread is tracking.
  // We should only receive them externally.
  pthread_sigmask(SIG_BLOCK, &abort_waiter_data.tracked_sigset, NULL);
  int signal;
  // Block for reading the signal ID until it arrives
  read(abort_waiter_data.pipe[0], &signal, sizeof(int));
  // Die
  nominal_exit(signal);
}

_Noreturn static void abort_waiter_handler(int signal) {
  // Write the final signal into the pipe and block forever.
  write(abort_waiter_data.pipe[1], &signal, sizeof(int));
  while (1) {
  }
}

static void abort_waiter_setup() {
  // Only abort on SIGABRT as the JVM either emits SIGABRT or SIGKILL (which we
  // can't catch)
  // when a fatal crash occurs. Still, keep expandability if we would want to
  // add more user-friendly fatal signals in the future.
  const static int tracked_signals[] = {SIGABRT};
  const static int ntracked =
      (sizeof(tracked_signals) / sizeof(tracked_signals[0]));
  struct sigaction sigactions[ntracked];
  sigemptyset(&abort_waiter_data.tracked_sigset);
  for (size_t i = 0; i < ntracked; i++) {
    sigaddset(&abort_waiter_data.tracked_sigset, tracked_signals[i]);
    sigactions[i].sa_handler = abort_waiter_handler;
  }
  if (pipe(abort_waiter_data.pipe) != 0) {
    LOGE("Oxygen", "Failed to set up aborter pipe: {}\n", strerror(errno));
    return;
  }
  pthread_t waiter_thread;
  int result;
  if ((result = pthread_create(&waiter_thread, NULL, abort_waiter_thread,
                               NULL)) != 0) {
    LOGE("Oxygen", "Failed to start up waiter thread: {}", strerror(result));
    for (int i = 0; i < 2; i++)
      close(abort_waiter_data.pipe[i]);
    return;
  }
  // Only set the sigactions *after* we have already set up the pipe and the
  // thread.
  for (size_t i = 0; i < ntracked; i++) {
    if (sigaction(tracked_signals[i], &sigactions[i], NULL) != 0) {
      // Not returning here because we may have set some handlers successfully.
      //             // Some handling is better than no handling.
      LOGE("Oxygen", "Failed to set signal hander for signal {}: {}", i,
           strerror(errno));
    }
  }
}

static jint launchJVM(int margc, char **margv) {
  void *libjli = dlopen("libjli.so", RTLD_LAZY | RTLD_GLOBAL);

  // Unset all signal handlers to create a good slate for JVM signal detection.
  struct sigaction clean_sa;
  memset(&clean_sa, 0, sizeof(struct sigaction));
  for (int sigid = SIGHUP; sigid < NSIG; sigid++) {
    // For some reason Android specifically checks if you set SIGSEGV to
    // SIG_DFL.
    // There's probably a good reason for that but the signal handler here is
    // temporary and will be replaced by the Java VM's signal/crash handler.
    // Work around the warning by using SIG_IGN for SIGSEGV
    if (sigid == SIGSEGV)
      clean_sa.sa_handler = SIG_IGN;
    else
      clean_sa.sa_handler = SIG_DFL;
    sigaction(sigid, &clean_sa, nullptr);
  }
  // Set up the thread that will abort the launcher with an user-facing dialog
  // on a signal.
  abort_waiter_setup();

  // Boardwalk: silence
  // LOGD("JLI lib = %x", (int)libjli);
  if (libjli == nullptr) {
    LOGE("Oxygen", "JLI lib = NULL: {}", dlerror());
    return -1;
  }
  LOGI("Oxygen", "Found JLI lib");
  JLI_Launch_func *pJLI_Launch = (JLI_Launch_func *)dlsym(libjli, "JLI_Launch");
  // Boardwalk: silence
  // LOGD("JLI_Launch = 0x%x", *(int*)&pJLI_Launch);

  if (pJLI_Launch == nullptr) {
    LOGE("Oxygen", "JLI_Launch = NULL");
    return -1;
  }

  LOGI("Oxygen", "Calling JLI_Launch");

  auto res = pJLI_Launch(
      margc, margv, 0,
      NULL, // sizeof(const_jargs) / sizeof(char *), const_jargs,
      0,
      NULL, // sizeof(const_appclasspath) / sizeof(char *), const_appclasspath,
      FULL_VERSION, DOT_VERSION,
      *margv, // (const_progname != NULL) ? const_progname : *margv,
      *margv, // (const_launcher != NULL) ? const_launcher : *margv,
      (const_jargs != NULL) ? JNI_TRUE : JNI_FALSE, const_cpwildcard,
      const_javaw, const_ergo_class);
  oxygen->callback_init = false;
  return res;
}

extern "C" {
JNIEXPORT jint JNICALL Java_com_oracle_dalvik_VMLauncher_launchJVM(
    JNIEnv *env, jclass clazz, jobjectArray argsArray) {
  if (!argsArray) {
    LOGE("Oxygen", "Args array null, returning");
    return 0;
  }
  int argc = env->GetArrayLength(argsArray);
  char **argv = convert_to_char_array(env, argsArray);

  LOGI("Oxygen", "Done processing args");
  jint res = launchJVM(argc, argv);

  LOGI("Oxygen", "Going to free args");
  free_char_array(env, argsArray, argv);

  LOGI("Oxygen", "Free done");
  return res;
}
}
