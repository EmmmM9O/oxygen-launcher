#ifndef CALLBACK_MANAGER_H
#define CALLBACK_MANAGER_H

#include <atomic>
#include <functional>
#include <jni.h>
#include <memory>
#include <mutex>
#include <shared_mutex>
#include <string>
#include <type_traits>
#include <unordered_map>
#include <vector>

namespace callback {
using Handle = jlong;
constexpr Handle INVALID_HANDLE = 0;
class CallbackBase {
public:
  virtual ~CallbackBase() = default;
  virtual void invalidate() = 0;
  virtual bool isValid() const = 0;
};
template <typename Signature> class CallbackWrapper : public CallbackBase {
public:
  using FunctionType = std::function<Signature>;

  explicit CallbackWrapper(FunctionType func)
      : callback_(std::move(func)), valid_(true) {}

  template <typename... Args>
  auto invoke(Args &&...args) -> std::invoke_result_t<FunctionType, Args...> {
    if (!valid_) {
      throw std::runtime_error("Callback has been invalidated");
    }
    return callback_(std::forward<Args>(args)...);
  }

  void invalidate() override {
    std::lock_guard lock(mutex_);
    valid_ = false;
    callback_ = nullptr;
  }

  bool isValid() const override { return valid_ && callback_ != nullptr; }

private:
  FunctionType callback_;
  bool valid_;
  mutable std::mutex mutex_;
};

class CallbackManager {
public:
  static CallbackManager &instance() {
    static CallbackManager manager;
    return manager;
  }

  template <typename Func> Handle registerCallback(Func &&callback) {
    using Signature = typename FunctionTraits<Func>::Signature;

    auto wrapper = std::make_unique<CallbackWrapper<Signature>>(
        std::forward<Func>(callback));

    Handle handle = generateHandle();

    {
      std::unique_lock lock(mutex_);
      callbacks_.emplace(handle,
                         std::unique_ptr<CallbackBase>(wrapper.release()));
    }

    return handle;
  }

  template <typename Ret,typename... Args> Ret invoke(Handle handle, Args &&...args) {
    std::shared_lock lock(mutex_);

    auto it = callbacks_.find(handle);
    if (it == callbacks_.end()) {
      throw std::runtime_error("Invalid handle: " + std::to_string(handle));
    }

    auto *wrapper =
        dynamic_cast<CallbackWrapper<Ret(Args...)> *>(it->second.get());

    if (!wrapper) {
      throw std::runtime_error("Callback type mismatch");
    }

    return wrapper->invoke(std::forward<Args>(args)...);
  } 

  bool remove(Handle handle) {
    std::unique_lock lock(mutex_);
    auto it = callbacks_.find(handle);
    if (it != callbacks_.end()) {
      it->second->invalidate();
      callbacks_.erase(it);
      return true;
    }
    return false;
  }

  bool invalidate(Handle handle) {
    std::shared_lock lock(mutex_);
    auto it = callbacks_.find(handle);
    if (it != callbacks_.end()) {
      it->second->invalidate();
      return true;
    }
    return false;
  }

  bool isValid(Handle handle) const {
    std::shared_lock lock(mutex_);
    auto it = callbacks_.find(handle);
    return it != callbacks_.end() && it->second->isValid();
  }

  std::vector<Handle> getAllValidHandles() const {
    std::shared_lock lock(mutex_);
    std::vector<Handle> result;
    for (const auto &[handle, callback] : callbacks_) {
      if (callback->isValid()) {
        result.push_back(handle);
      }
    }
    return result;
  }

  void clear() {
    std::unique_lock lock(mutex_);
    for (auto &[handle, callback] : callbacks_) {
      callback->invalidate();
    }
    callbacks_.clear();
  }

  size_t size() const {
    std::shared_lock lock(mutex_);
    return callbacks_.size();
  }

private:
  CallbackManager() = default;
  ~CallbackManager() = default;

  CallbackManager(const CallbackManager &) = delete;
  CallbackManager &operator=(const CallbackManager &) = delete;
  CallbackManager(CallbackManager &&) = delete;
  CallbackManager &operator=(CallbackManager &&) = delete;

  Handle generateHandle() {
    static std::atomic<Handle> nextHandle{1};
    return nextHandle.fetch_add(1, std::memory_order_relaxed);
  }

  template <typename T> struct FunctionTraits;

  template <typename Ret, typename... Args>
  struct FunctionTraits<Ret(Args...)> {
    using Signature = Ret(Args...);
  };

  template <typename Ret, typename... Args>
  struct FunctionTraits<Ret (*)(Args...)> : FunctionTraits<Ret(Args...)> {};

  template <typename Ret, typename... Args>
  struct FunctionTraits<std::function<Ret(Args...)>>
      : FunctionTraits<Ret(Args...)> {};

  template <typename Lambda>
  struct FunctionTraits : FunctionTraits<decltype(&Lambda::operator())> {};

  template <typename Class, typename Ret, typename... Args>
  struct FunctionTraits<Ret (Class::*)(Args...) const>
      : FunctionTraits<Ret(Args...)> {};

  template <typename Class, typename Ret, typename... Args>
  struct FunctionTraits<Ret (Class::*)(Args...)>
      : FunctionTraits<Ret(Args...)> {};

  mutable std::shared_mutex mutex_;
  std::unordered_map<Handle, std::unique_ptr<CallbackBase>> callbacks_;
};

} // namespace callback

#endif // !CALLBACK_MANAGER_H
