#ifndef OBJECT_MANAGER
#define OBJECT_MANAGER

#include <cassert>
#include <jni.h>
#include <mutex>
#include <unordered_map>

class JNIRefManager {
public:
  using RefId = int32_t;
  static constexpr RefId INVALID_ID = -1;

  static JNIRefManager &instanceAndroid() {
    static JNIRefManager mgr;
    return mgr;
  }

  static JNIRefManager &instanceJVM() {
    static JNIRefManager mgr;
    return mgr;
  }

  RefId create(JNIEnv *env, jobject obj) {
    if (!env || !obj)
      return INVALID_ID;

    jobject globalRef = env->NewGlobalRef(obj);
    if (!globalRef)
      return INVALID_ID;

    std::lock_guard lock(mutex_);
    RefId id = nextId_++;
    refs_[id] = {globalRef, env->GetObjectRefType(globalRef)};
    return id;
  }

  jobject get(RefId id) const {
    std::lock_guard lock(mutex_);
    auto it = refs_.find(id);
    return (it != refs_.end()) ? it->second.obj : nullptr;
  }

  jobject getForUse(JNIEnv *env, RefId id) {
    std::lock_guard lock(mutex_);
    auto it = refs_.find(id);
    if (it == refs_.end())
      return nullptr;

    return env->NewLocalRef(it->second.obj);
  }

  void release(JNIEnv *env, RefId id) {
    std::lock_guard lock(mutex_);
    auto it = refs_.find(id);
    if (it != refs_.end()) {
      env->DeleteGlobalRef(it->second.obj);
      refs_.erase(it);
    }
  }

  RefId replace(JNIEnv *env, RefId id, jobject newObj) {
    if (!env || !newObj)
      return INVALID_ID;

    std::lock_guard lock(mutex_);
    auto it = refs_.find(id);
    if (it != refs_.end()) {
      env->DeleteGlobalRef(it->second.obj);
      it->second.obj = env->NewGlobalRef(newObj);
      return id;
    }
    return INVALID_ID;
  }

  void clear(JNIEnv *env) {
    std::lock_guard lock(mutex_);
    for (auto &[id, ref] : refs_) {
      env->DeleteGlobalRef(ref.obj);
    }
    refs_.clear();
  }

  size_t size() const {
    std::lock_guard lock(mutex_);
    return refs_.size();
  }

private:
  JNIRefManager() = default;
  ~JNIRefManager() = default;

  struct RefInfo {
    jobject obj;
    jobjectRefType type;
  };

  std::unordered_map<RefId, RefInfo> refs_;
  RefId nextId_ = 1;
  mutable std::mutex mutex_;
};
#endif // !OBJECT_MANAGER
