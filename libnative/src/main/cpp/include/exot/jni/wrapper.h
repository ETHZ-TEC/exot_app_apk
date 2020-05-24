// Copyright (c) 2015-2020, Swiss Federal Institute of Technology (ETH Zurich)
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// * Redistributions of source code must retain the above copyright notice, this
//   list of conditions and the following disclaimer.
// 
// * Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
// 
// * Neither the name of the copyright holder nor the names of its
//   contributors may be used to endorse or promote products derived from
//   this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 
/**
 * @file libnative/src/main/cpp/include/exot/jni/wrapper.h
 * @author     Bruno Klopott
 * @brief      The JNI wrapper class template for lifecycle management of
 *             Manager instances.
 */

#pragma once

#include <jni.h>

#include <chrono>
#include <thread>
#include <type_traits>
#include <utility>

#include <exot/jni/log.h>
#include <exot/jni/manager.h>

inline namespace details {
template <template <typename...> class T, typename U>
struct is_specialization_of : std::false_type {};

template <template <typename...> class T, typename... Us>
struct is_specialization_of<T, T<Us...>> : std::true_type {};

}  // namespace details

namespace exot::jni {

/**
 * @brief Populates Java environment pointers/references
 *
 * @param env            The pointer to the JNI environment
 * @param instance       The reference to the "this" Java object
 * @param java_vm        The pointer to the Java VM
 * @param java_instance  The reference to the Java instance
 * @param java_class     The reference to the object class of the Java instance
 * @param java_method_id The reference to the method ID of the top app
 * @param jni_version    The JNI version
 * @return True if populating was successful, false otherwise.
 */
auto populate_java_pointers(JNIEnv* env, const jobject& instance,
                            JavaVM* java_vm, jobject& java_instance,
                            jclass& java_class, jmethodID& java_method_id,
                            jint& jni_version) {
  const char* TAG = "ExOT/Native/Helper";
  auto ok         = true;

  env->GetJavaVM(&java_vm);
  java_instance = reinterpret_cast<jclass>(env->NewGlobalRef(instance));
  java_class    = reinterpret_cast<jclass>(env->GetObjectClass(java_instance));
  java_method_id =
      env->GetMethodID(java_class, "getTopApp", "()Ljava/lang/String;");
  jni_version = env->GetVersion();

  if (java_vm == NULL) {
    Log.e(TAG, "{}(): java_vm was null", __func__);
    ok = false;
  }
  if (java_instance == NULL) {
    Log.e(TAG, "{}(): java_instance was null", __func__);
    ok = false;
  }
  if (java_class == NULL) {
    Log.e(TAG, "{}(): java_class was null", __func__);
    ok = false;
  }
  if (java_method_id == NULL) {
    Log.e(TAG, "{}(): java_method_id was null", __func__);
    ok = false;
  }

  return ok;
}

/**
 * @brief Produces a tuple with the JNI info reinterpreted as integers
 *
 * @param java_vm        The pointer to the Java VM
 * @param java_instance  The reference to the Java instance
 * @param java_class     The reference to the object class of the Java instance
 * @param java_method_id The reference to the method ID of the top app
 * @param jni_version    The JNI version
 * @return The tuple with the reinterpreted pointers
 */
auto produce_references(JavaVM* java_vm, const jobject& java_instance,
                        const jclass& java_class,
                        const jmethodID& java_method_id, jint jni_version) {
  return std::make_tuple(reinterpret_cast<std::uintptr_t>(java_vm),
                         reinterpret_cast<std::uintptr_t>(java_instance),
                         reinterpret_cast<std::uintptr_t>(java_class),
                         reinterpret_cast<std::uintptr_t>(java_method_id),
                         static_cast<int>(jni_version));
}

/**
 * @brief The Wrapper class for Manager objects
 * @details The Wrapper provides a convienient way to manage the lifecycle of
 *          the Manager (with methods like 'create', 'start', 'stop') via the
 *          JNI interface.
 *
 * @tparam T         The 'Manager' template specialisation
 * @tparam <unnamed> Template helper (only accepts specialisations of 'Manager')
 */
template <typename T, typename = std::enable_if_t<
                          is_specialization_of<exot::jni::Manager, T>::value>>
class Wrapper {
  static inline const char* TAG = "ExOT/Native/Wrapper";

 public:
  std::unique_ptr<T> manager_ptr_ = nullptr;

  Wrapper() = default;

  /**
   * @brief Creates the Manager instance
   * @details Accepts the Java/JNI essential pointers as unsigned integers.
   *
   * @param config              The JSON config as a string
   * @param java_vm_ptr         The Java VM pointer
   * @param java_instance_ptr   The Java instance pointer
   * @param java_class_ptr      The pointer to the object class of the instance
   * @param java_method_id_ptr  The pointer to the method ID of the top app
   * @param jni_version         The JNI version
   * @return True if created successfully, False otherwise.
   */
  bool create(std::string config, std::uintptr_t java_vm_ptr,
              std::uintptr_t java_instance_ptr, std::uintptr_t java_class_ptr,
              std::uintptr_t java_method_id_ptr, int jni_version) {
    Log.d(TAG,
          "{}(): "
          "java_vm_ptr: {:#0x}, java_instance_ptr: {:#0x}, "
          "java_class_ptr: {:#0x}, java_method_id_ptr: {:#0x}, "
          "jni_version: {:#0x}",
          __func__,                            //
          java_vm_ptr, java_instance_ptr,      //
          java_class_ptr, java_method_id_ptr,  //
          jni_version);

    if (manager_ptr_ == nullptr) {
      try {
        manager_ptr_ = std::make_unique<T>(config, java_vm_ptr,
                                           java_instance_ptr, java_class_ptr,
                                           java_method_id_ptr, jni_version);
      } catch (const spdlog::spdlog_ex& e) {
        Log.e(TAG, "{}(): logging library exception thrown (permissions?): {}",
              __func__, e.what());
        return false;
      } catch (const std::exception& e) {
        Log.e(TAG, "{}(): other exception thrown: {}", __func__, e.what());
        return false;
      }

      if (manager_ptr_ == nullptr) {
        Log.e(TAG, "{}(): manager was null after creation", __func__);
        return false;
      }

      Log.i(TAG, "{}(): manager object created", __func__);

      return true;
    } else {
      Log.w(TAG, "{}(): manager object was not nullptr", __func__);
    }

    return false;
  }

  /**
   * @brief Starts the Manager instance
   *
   * @return true   Started successfully
   * @return false  Failed to start
   */
  bool start() const {
    if (manager_ptr_ != nullptr) {
      if (!manager_ptr_->is_started()) {
        manager_ptr_->start();
        Log.i(TAG, "{}(): started the service", __func__);
        return true;
      } else {
        Log.w(TAG, "{}(): object already started", __func__);
      }
    } else {
      Log.e(TAG, "{}(): manager does not exist", __func__);
    }

    return false;
  }

  /**
   * @brief Stops the Manager instance
   *
   * @return true   Stopped successfully
   * @return false  Failed to stop
   */
  bool stop() const {
    if (manager_ptr_ != nullptr) {
      if (manager_ptr_->is_started()) {
        manager_ptr_->stop();
        Log.i(TAG, "{}(): stopped the manager", __func__);
        return true;
      } else {
        Log.w(TAG, "{}(): not started", __func__);
      }
    } else {
      Log.e(TAG, "{}(): manager does not exist", __func__);
    }

    return false;
  }

  /**
   * @brief Terminates the Manager instance
   *
   * @return true   Terminated successfully
   * @return false  Failed to terminate
   */
  bool terminate() {
    if (manager_ptr_ != nullptr) {
      manager_ptr_->terminate();
      Log.i(TAG, "{}(): terminated the manager", __func__);
      return true;
    } else {
      Log.e(TAG, "{}(): manager does not exist", __func__);
    }

    return false;
  }

  /**
   * @brief Is the Manager instance started?
   *
   * @return true
   * @return false
   */
  bool is_started() const {
    if (manager_ptr_ != nullptr) {
      return manager_ptr_->is_started();
    } else {
      return false;
    }
  }

  /**
   * @brief Does the Manager instance exist?
   *
   * @return true
   * @return false
   */
  bool exists() const { return manager_ptr_ != nullptr; }

  /**
   * @brief Destroys the Manager instance
   *
   * @return true   Destroyed successfully
   * @return false  Failed to destroy
   */
  bool destroy() {
    using namespace std::literals::chrono_literals;

    if (manager_ptr_ != nullptr) {
      Log.i(TAG, "{}(): deleting the object", __func__);
      manager_ptr_->terminate();
      std::this_thread::sleep_for(100ms);
      manager_ptr_ = nullptr;
      std::this_thread::sleep_for(10ms);
      return true;
    } else {
      Log.w(TAG, "{}(): object does not exist", __func__);
      return false;
    }
  }

  /**
   * @brief Get the running time of the Manager instance
   *
   * @return std::string The running time as a string (HH:MM:SS)
   */
  std::string get_running_time() const {
    if (manager_ptr_ != nullptr) {
      if (manager_ptr_->is_started()) {
        auto ret = manager_ptr_->get_running_time();

        using std::chrono::hours;
        using std::chrono::minutes;
        using std::chrono::seconds;

        auto hrs  = std::chrono::duration_cast<hours>(ret);
        auto mins = std::chrono::duration_cast<minutes>(ret % hours{1});
        auto secs = std::chrono::duration_cast<seconds>(ret % minutes{1});

        return fmt::format("{:0>2}:{:0>2}:{:0>2}.", hrs.count(), mins.count(),
                           secs.count());
      } else {
        Log.w(TAG, "{}(): object is not started", __func__);
      }
    } else {
      Log.w(TAG, "{}(): object does not exist", __func__);
    }

    return "N/A";
  }

  /**
   * @brief Gets the state of the Manager instance
   *
   * @return std::string The textual description of the state, One of:
   *                     terminated, stopped, started, idle, missing.
   */
  std::string query_state() const {
    return manager_ptr_ != nullptr ? manager_ptr_->query_state() : "missing";
  }
};

}  // namespace exot::jni