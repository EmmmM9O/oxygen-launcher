#include "log.hpp"

const char *log_priority_to_string(int priority) {
  switch (priority) {
  case ANDROID_LOG_VERBOSE:
    return "VERBOSE";
  case ANDROID_LOG_DEBUG:
    return "DEBUG";
  case ANDROID_LOG_INFO:
    return "INFO";
  case ANDROID_LOG_WARN:
    return "WARN";
  case ANDROID_LOG_ERROR:
    return "ERROR";
  case ANDROID_LOG_FATAL:
    return "FATAL";
  default:
    return "UNKNOWN";
  }
}
