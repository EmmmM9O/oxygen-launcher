
#ifndef OXYGEN_LOG_H
#define OXYGEN_LOG_H

#include "oxygen.h"

#include <android/log.h>
#include <format>
#include <source_location>
#include <string>

const char *log_priority_to_string(int priority);

template <typename... Args>
void log_android(int priority, const char *tag, std::format_string<Args...> fmt,
                 Args &&...args) {
  std::string msg = std::format(fmt, std::forward<Args>(args)...);
  __android_log_print(priority, tag, "%s", msg.c_str());
  if (oxygen != nullptr && oxygen->logFile != nullptr) {
    std::fprintf(oxygen->logFile, "[%s][%s]%s\n",
                 log_priority_to_string(priority), tag, msg.c_str());
  }
}

template <typename... Args>
void log(std::format_string<Args...> fmt, Args &&...args) {
  std::string msg = std::format(fmt, std::forward<Args>(args)...);
  std::fprintf(oxygen->logFile, "%s\n", msg.c_str());
}

template <typename... Args>
void log_internal(std::format_string<Args...> fmt, Args &&...args/*,
                  std::source_location loc = std::source_location::current()*/) {
  std::string msg =
      std::format("[OxygenLauncher] {}"/*, loc.file_name(), loc.line(), loc.function_name()*/,
                  std::format(fmt, std::forward<Args>(args)...));
  std::fprintf(oxygen->logFile, "%s\n", msg.c_str());
}

#define LOGD(tag, ...) log_android(ANDROID_LOG_DEBUG, tag, __VA_ARGS__)
#define LOGI(tag, ...) log_android(ANDROID_LOG_INFO, tag, __VA_ARGS__)
#define LOGE(tag, ...) log_android(ANDROID_LOG_ERROR, tag, __VA_ARGS__)

#endif // OXYGEN_LOG_H
