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
 * @file libnative/src/main/cpp/include/exot/jni/manager.h
 * @author     Bruno Klopott
 * @brief      The Manager variadic class template which creates and spawns
 *             components and presents a lifecycle management interface.
 */

#pragma once

#include <chrono>
#include <initializer_list>
#include <string>
#include <utility>

#include <fmt/format.h>
#include <spdlog/spdlog.h>
#include <nlohmann/json.hpp>

#include <spdlog/details/registry.h>

#include <exot/framework/state.h>
#include <exot/jni/log.h>
#include <exot/utilities/configuration.h>
#include <exot/utilities/logging.h>
#include <exot/utilities/main.h>
#include <exot/utilities/types.h>

namespace exot::jni {

template <typename... Components>
struct Manager {
  using clock_t     = std::chrono::system_clock;
  using duration_t  = std::chrono::nanoseconds;
  using executor_t  = exot::framework::ThreadExecutor;
  using state_t     = exot::framework::State;
  using state_ptr_t = typename state_t::state_pointer;
  using json_t      = nlohmann::json;

  using component_ptrs_t =
      exot::utilities::unique_ptr_tuple<exot::utilities::Logging,
                                        Components...>;
  using settings_tuple_t = std::tuple<exot::utilities::Logging::settings,
                                      typename Components::settings...>;

  /**
   * @brief Constructs a new Manager object
   * @todo  The Android-specific modules must be manually added to a list in the
   *        for loop in this constructor.
   *
   * @param json_string        The JSON config as a string
   * @param java_vm_ptr        The pointer to the Java VM
   * @param java_instance_ptr  The reference to the Java instance
   * @param java_class_ptr     The reference to the object class of the instance
   * @param java_method_id_ptr The reference to the method ID of the top app
   * @param jni_version        The JNI version
   */
  explicit Manager(std::string json_string, std::uintptr_t java_vm_ptr,
                   std::uintptr_t java_instance_ptr,
                   std::uintptr_t java_class_ptr,
                   std::uintptr_t java_method_id_ptr, int jni_version) {
    using namespace exot::utilities;
    using namespace std::literals::string_literals;

    json_object_ = nlohmann::json::parse(json_string);

    Log.d(TAG,
          "{}(): "
          "java_vm_ptr: {:#0x}, java_instance_ptr: {:#0x}, "
          "java_class_ptr: {:#0x}, java_method_id_ptr: {:#0x}, "
          "jni_version: {:#0x}",
          __func__,                            //
          java_vm_ptr, java_instance_ptr,      //
          java_class_ptr, java_method_id_ptr,  //
          jni_version);

    // For each key-value pair, where the value is a JSON object, add the
    // fields for Java-specific pointers.
    for (auto& [key, value] : json_object_.items()) {
      if (value.is_object()) {
        value["jvm"]        = java_vm_ptr;
        value["jinstance"]  = java_instance_ptr;
        value["jclazz"]     = java_class_ptr;
        value["jmid"]       = java_method_id_ptr;
        value["jniversion"] = jni_version;
      }
    }

    //! TODO: If new modules are added, make sure to add them to the list below.
    //! TODO: Consider making a separate base class for Android-specific
    //!       components, such that all Android settings can be at a single
    //!       configuration object, e.g. with name "android". Similar ones
    //!       are provided for base_bitset and base_shared_memory meters.
    for (const auto android_module : {"process_android"s}) {
      // Make sure that all android modules have a valid config field, with
      // Java-specific pointers.
      if (json_object_.find(android_module) == json_object_.end()) {
        json_object_[android_module] = {{"jvm", java_vm_ptr},
                                        {"jinstance", java_instance_ptr},
                                        {"jclazz", java_class_ptr},
                                        {"jmid", java_method_id_ptr},
                                        {"jniversion", jni_version}};
      }
    }

    create(std::move(json_object_));
  }

  /**
   * @brief Destroys the Manager object (and terminates it)
   *
   */
  ~Manager() { terminate(); }

  /**
   * @brief Starts the Manager object and logs starting time
   *
   */
  void start() {
    spdlog::details::registry::instance().flush_all();

    state_->start();
    started_at_ = clock_t::now();
  }

  /**
   * @brief Stops the Manager object
   *
   */
  void stop() const {
    spdlog::details::registry::instance().flush_all();

    if (is_started()) {
      state_->stop();
    } else if (!is_stopped() || !is_terminated()) {
      state_->start();
      state_->stop();
    }
  }

  /**
   * @brief Terminates the Manager object
   * @details Termination joins the executor threads and resets the state.
   *
   */
  void terminate() {
    spdlog::details::registry::instance().flush_all();

    stop();
    state_->terminate();

    executor_.join();
    state_->reset();
  }

  /**
   * @brief Queries the current state of the Manager object
   *
   * @return std::string The current state, one of: terminated, stopped,
   *                     started, idle.
   */
  inline std::string query_state() const {
    return is_terminated()
               ? "terminated"
               : (is_stopped() ? "stopped"
                               : (is_started() ? "started" : "idle"));
  }

  /**
   * @brief Is the Manager object started?
   *
   * @return true
   * @return false
   */
  inline bool is_started() const { return state_->is_started(); }

  /**
   * @brief Is the Manager object stopped?
   *
   * @return true
   * @return false
   */
  inline bool is_stopped() const { return state_->is_stopped(); }

  /**
   * @brief Is the Manager object terminated?
   *
   * @return true
   * @return false
   */
  inline bool is_terminated() const { return state_->is_terminated(); }

  /**
   * @brief Get the running time of the Manager object
   *
   * @return duration_t The running time (in chrono nanoseconds)
   */
  inline duration_t get_running_time() {
    return is_started() ? clock_t::now() - started_at_ : duration_t{0};
  }

 protected:
  /**
   * @brief Creates the Manager object
   * @details Creation entails:
   *          1) configuration of settings structures,
   *          2) initialisation of global state handlers,
   *          3) creation of unique_ptr-wrapped component objects,
   *          4) connection of component objects' in/out queues,
   *          5) spawning of component objects with the executor.
   *
   * @param config The JSON config
   */
  void create(json_t&& config) {
    using namespace utilities;

    JsonConfig jc;
    jc.get_ref() = config;

    Log.d(TAG, "{}(): JsonConfig created and assigned", __func__);

#ifdef __cpp_lib_apply
    std::apply([&jc, this](auto&... v) { configure(jc, v...); }, settings_);
#else
    const_for<0, std::tuple_size_v<settings_tuple_t>>(
        [&jc, this](const auto I) { configure(jc, std::get<I>(settings_)); });
#endif

    Log.d(TAG, "{}(): settings configured", __func__);

    exot::framework::init_global_state_handlers();

    Log.d(TAG, "{}(): state handlers initialised", __func__);

    const_for<0, std::tuple_size_v<component_ptrs_t>>([this](const auto I) {
      using component_t = typename std::decay_t<decltype(
          std::get<I>(components_))>::element_type;

      std::get<I>(components_) =
          std::make_unique<component_t>(std::get<I>(settings_));

      Log.d(TAG, "{}(): created component {}", __func__, I);
    });

    if constexpr (sizeof...(Components) > 1ull) {
      const_for<1ull, sizeof...(Components)>([this](const auto I) {
        Log.d(TAG, "{}(): connecting components {} and {}", __func__, I,
              I + 1ull);

        exot::framework::Connector().connect(*std::get<I>(components_),
                                             *std::get<I + 1ull>(components_));
      });
    }

#ifdef __cpp_lib_apply
    std::apply([this](const auto&... v) { executor_.spawn((*v)...); },
               tail(std::move(components_)));
    Log.d(TAG, "{}(): spawned {} components", __func__,
          std::tuple_size_v<settings_tuple_t> - 1);
#else
    const_for<1, std::tuple_size_v<settings_tuple_t>>([this](const auto I) {
      executor_.spawn(*std::get<I>(components_));
      Log.d(TAG, "{}(): spawned component {}", __func__, I);
    });
#endif
  }

  static inline const char* TAG = "ExOT/Native/Manager";     //! The logging tag
  state_ptr_t state_{exot::framework::GLOBAL_STATE->get()};  //! The gl. state
  json_t json_object_;              //! The JSON configuration object
  clock_t::time_point started_at_;  //! The starting time point
  executor_t executor_;             //! The component executor
  settings_tuple_t settings_;       //! The tuple holding settings structures
  component_ptrs_t components_;     //! The tuple holding components
};

}  // namespace exot::jni