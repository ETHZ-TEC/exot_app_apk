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
 * @file Manager.h
 * @author     Bruno Klopott
 * @brief      The Manager class wraps the framework component for handling in
 *             an Android app.
 */

#include <android/log.h>
#include <jni.h>
#include <pthread.h>

#include <atomic>
#include <chrono>
#include <memory>
#include <string>
#include <thread>

#include <fmt/format.h>
#include <fmt/ostream.h>

#define METER_SET_AFFINITY false
#define METER_LOG_SYSTEM_TIME true
#include <covert/components/meter_host_logger.h>
#include <covert/framework/state.h>
#include <covert/modules/frequency_rel.h>
#include <covert/modules/frequency_sysfs.h>
#include <covert/modules/thermal_sysfs.h>
#include <covert/modules/process_android.h>
#include <covert/modules/utilisation_procfs.h>
#include <covert/utilities/barrier.h>
#include <covert/utilities/logging.h>
#include <covert/utilities/ostream.h>
#include <covert/utilities/thread.h>

namespace {

using namespace std::chrono_literals;
using namespace std::string_literals;

using covert::modules::frequency_rel;
using covert::modules::frequency_sysfs;
using covert::modules::utilisation_procfs;
using covert::modules::thermal_sysfs;
using covert::modules::process_android;
using meter_type =
    covert::components::meter_host_logger<std::chrono::microseconds,
                                          utilisation_procfs, frequency_sysfs,
                                          frequency_rel, thermal_sysfs,
                                          process_android>;

/**
 * @brief      Gets a base class reference for individual meter modules of the
 *             combined meter settings structure.
 *
 * @param      conf    The meter_type::settings object
 *
 * @tparam     Module  The module (base class)
 * @tparam     Conf    The type of the meter settings
 *
 * @return     The reference to the individual module's settings structure.
 */
template <typename Module, typename Conf>
typename Module::settings &getModuleReference(Conf &conf) {
  typename Module::settings &reference = conf;
  return reference;
}

/**
 * @brief      Class for managing the lifetime and execution of a meter object
 */
class Manager {
 public:
  using clock_type = std::chrono::system_clock;
  using duration_type = std::chrono::nanoseconds;

  Manager() = delete;

  /**
   * @brief      Constructs the object.
   *
   * @param[in]  path  The path where log files are to be saved
   * @param[in]  uuid  The uuid of the running user
   * @param[in]  env       The pointer to the java native interface environment
   * @param[in]  instance  The instance of the calling object to the JNI
   */
  explicit Manager(std::string path, std::string uuid, JavaVM * jvm,
          jobject* jobj,
          jclass* jclazz,
          jmethodID* jmid, jint jniversion) {
    logi("Manager(): creating the Manager");

    /* Configure logging */
    logConf_.log_level = spdlog::level::debug;
    logConf_.async = true;
    logConf_.timestamp_files = true;
    logConf_.debug_log_to_file = true;
    logConf_.app_log_to_file = true;
    logConf_.rotating_logs = true;
    logConf_.rotating_logs_size = 1024*1024*1024*10; // 10 GB
    logConf_.rotating_logs_count = 1000;
    logConf_.debug_log_filename = fmt::format("{}/debug_{}.txt", path, uuid);
    logConf_.app_log_filename = fmt::format("{}/log_{}.csv", path, uuid);

    meterConf_.period     = std::chrono::milliseconds{30};
    meterConf_.jvm        = jvm;
    meterConf_.jclazz     = jclazz;
    meterConf_.jinstance  = jobj;
    meterConf_.jmid       = jmid;;
    meterConf_.jniversion = jniversion;
  };

  /**
   * @brief      Initialises the Manager, creates the managed object.
   */
  void initService() {
    /* create logging object */
    log_ = std::make_unique<covert::utilities::Logging>(logConf_);

    /* Every time initService() is called, a new meter object is created, and
     * any old one is deleted. The deletion is managed 'automatically' thanks to
     * the unique pointer. */
    meter_ = std::make_unique<meter_type>(meterConf_);

    /* Cannot use the ref. template specialisation here, hence the lambda. */
    exec_.spawn([&]() { meter_->process(); });

    initFlag = true;
  }

  /**
   * @brief      Starts a service using the global state.
   */
  void startService() {
    logi("startService()");
    /* Flush debug log to inspect running parameters. */
    spdlog::get("log")->flush();
    
    /* Start the meter host via the global state */
    startFlag.store(true, std::memory_order_release);
    state_->start();
    timeStarted_ = clock_type::now();
  }

  /**
   * @brief      Stops the service using the global state.
   */
  void stopService() {
    logi("stopService()");

    if (startFlag.load()) {
      startFlag.store(false, std::memory_order_release);
      state_->stop();
    } else if (!startFlag.load() && initFlag) {
      /* Perform additional cleanup/stop actions to prevent deadlocks. */
      state_->start();
      state_->stop();
    }

    /* Join the execution thread. These are 'recycled' in the executor. */
    exec_.join();

    /* Flush logs upon stopping */
    spdlog::get("app")->flush();
    spdlog::get("log")->flush();

    initFlag = false;
  }

  /**
   * @brief      Determines if started.
   *
   * @return     True if started, False otherwise.
   */
  bool isStarted() { return startFlag.load(); }

  /**
   * @brief      Determines if initialised.
   *
   * @return     True if initialised, False otherwise.
   */
  bool isInitialised() { return initFlag.load(); }

  /**
   * @brief      Gets the running time.
   *
   * @return     The running time.
   */
  duration_type getRunningTime() {
    if (startFlag) {
      return clock_type::now() - timeStarted_;
    } else {
      return duration_type{0};
    }
  }

  /**
   * @brief      Destroys the object.
   */
  ~Manager() {
    logi("~Manager(): destroying the object!");

    if (initFlag && !startFlag) {
    } else if (initFlag && startFlag) {
      startFlag.store(false);
    }

    /* Join the execution thread. */
    exec_.join(); 

    /* Flush logs upon destruction */
    spdlog::get("app")->flush();
    spdlog::get("log")->flush();
  }

 private:
  std::shared_ptr<covert::framework::State> state_ =
      covert::framework::GLOBAL_STATE->get();
  /* TODO: these flags remained here from the older version. */
  std::atomic_bool startFlag{false};
  std::atomic_bool initFlag{false};

  std::chrono::system_clock::time_point timeStarted_;

  covert::utilities::Logging::settings logConf_;
  meter_type::settings meterConf_;

  covert::framework::ThreadExecutor exec_;

  /* The meter is 'managed' via a unique pointer, to allow destroying/creating
   * new objects at runtime. When the unique pointer is overwritten, or set to
   * nullptr, the destructor of the previously pointed-to object will be called,
   * cleaning up its memory. */
  std::unique_ptr<covert::utilities::Logging> log_ = nullptr;
  std::unique_ptr<meter_type> meter_ = nullptr;

  inline void logi(std::string msg) {
    __android_log_print(ANDROID_LOG_INFO, "Thermal-SC-UI-meter/Native/Manager", "%s",
                        msg.c_str());
  }

  inline void logd(std::string msg) {
    __android_log_print(ANDROID_LOG_DEBUG, "Thermal-SC-UI-meter/Native/Manager", "%s",
                        msg.c_str());
  }

  inline void loge(std::string msg) {
    __android_log_print(ANDROID_LOG_ERROR, "Thermal-SC-UI-meter/Native/Manager", "%s",
                        msg.c_str());
  }
};

}  // namespace
