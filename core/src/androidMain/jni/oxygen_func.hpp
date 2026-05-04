#ifndef OXYGEN_FUNC_H
#define OXYGEN_FUNC_H
#include "callback_manager.hpp"
#include "log.hpp"
#include <functional>
#include <jni.h>
using namespace callback;
using VoidFunc = std::function<void()>;
using StrCons = std::function<void(const char *)>;
template <typename F> jlong createCallback(F &&lambda) {
  return CallbackManager::instance().registerCallback(std::move(lambda));
}

template <typename... Args> void invokeCallback(jlong handle, Args... args) {
  try {
    CallbackManager::instance().invoke<void>(
        static_cast<Handle>(handle), std::forward<Args>(args)...);
  } catch (const std::exception &e) {
    LOGE("Oxygen", "Can not invoke {} in {}", (void *)handle,
         __PRETTY_FUNCTION__);
  }
}
static void releaseCallback(jlong handle) {
  CallbackManager::instance().remove(static_cast<Handle>(handle));
}

template <typename... Args>
void invokeCallbackOnce(jlong handle, Args... args) {
  invokeCallback(handle, std::forward<Args>(args)...);
  releaseCallback(handle);
}
#endif // !OXYGEN_FUNC_H
