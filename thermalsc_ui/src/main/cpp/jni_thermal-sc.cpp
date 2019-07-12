/**
 * @file native.cpp
 * @author     Bruno Klopott
 * @brief      Contains the functions for the Java Native Interface.
 */

#include <android/log.h>
#include <jni.h>

#include <chrono>
#include <memory>

#define FMT_HEADER_ONLY
#include <fmt/format.h>
#include <spdlog/spdlog.h>

#include <Manager.h>

/* The Manager object is handled via a unique pointer, to ease the
 * creation/destruction at runtime. */
std::unique_ptr<Manager> gManagerObject = nullptr;

static const char *kTAG = "Thermal-SC-UI-meter/Native";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, kTAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, kTAG, __VA_ARGS__))



static jobject gClassLoader_;
static jmethodID gFindClassMethod_;

/**
 * @brief      Starts the manager object
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     True if started successfully, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_startManagerObject(JNIEnv *env,
                                                       jobject instance) {
  if (gManagerObject != nullptr) {
    if (gManagerObject->isInitialised()) {
      if (!gManagerObject->isStarted()) {
        gManagerObject->startService();
        LOGI("startManagerObject(): started the service");
        return true;
      } else {
        LOGI("startManagerObject(): object already started");
      }
    } else {
      LOGI("startManagerObject(): not initialised");
    }
  } else {
    LOGE("startManagerObject(): object does not exist");
  }

  return false;
}

/**
 * @brief      Stops the manager object
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     True if stopped successfully, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_stopManagerObject(JNIEnv *env,
                                                      jobject instance) {
  if (gManagerObject != nullptr) {
    if (gManagerObject->isInitialised()) {
      if (gManagerObject->isStarted()) {
        gManagerObject->stopService();
        LOGI("stopManagerObject(): stopped the object");
        return true;
      } else {
        LOGI("stopManagerObject(): not started");
      }
    } else {
      LOGI("stopManagerObject(): not initialised");
    }
  } else {
    LOGE("stopManagerObject(): object does not exist");
  }

  return false;
}

/**
 * @brief      Creates the manger object
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 * @param[in]  jpath     The save directory path
 * @param[in]  juuid     The uuid of the running user
 *
 * @return     True if created successfully, false otherwise.
 */
// TODO
JavaVM * g_vm;
jobject g_obj;
jclass g_clazz;
jmethodID g_mid;
//TODO
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_createManagerObject(JNIEnv *env,
                                                        jobject instance,
                                                        jstring jpath,
                                                        jstring juuid) {
  if (gManagerObject == nullptr) {
    if (jpath == nullptr || juuid == nullptr) {
      LOGE("createManagerObject(): null in arguments");
      throw std::logic_error("createManagerObject(): null in arguments");
    }
// TODO
  bool returnValue = true;
  // convert local to global reference
  // (local will die after this method call)
  g_obj = env->NewGlobalRef(instance);

  // save refs for callback
//  g_clazz = env->FindClass("org/eth/tik/meter/MeterService$ProcessMeter");
  jclass g_clazz = env->GetObjectClass(g_obj);
  if (g_clazz == NULL) {
    LOGI("Failed to find class");
  }

//  g_mid = env->GetStaticMethodID(g_clazz, "getTopApp", "()Ljava/lang/String;");
  g_mid = env->GetMethodID(g_clazz, "getTopApp", "()Ljava/lang/String;");
  if (g_mid == NULL) {
    LOGI("Unable to get method ref");
  }

  env->GetJavaVM(&g_vm);
//  g_vm = (JavaVM*) env->NewGlobalRef(g_vm*)

// TODO


    std::string path{env->GetStringUTFChars(jpath, 0)};
    std::string uuid{env->GetStringUTFChars(juuid, 0)};

    try {
      gManagerObject = std::make_unique<Manager>(path, uuid, g_vm, &g_obj, &g_clazz, &g_mid, env->GetVersion());
    } catch (const spdlog::spdlog_ex &e) {
      LOGE("createManagerObject(): insufficient permissions");
      return false;
    }
    assert(gManagerObject != nullptr);
    LOGI("createManagerObject(): created the object");
    return true;
  } else {
    LOGI("createManagerObject(): object already present");
    return false;
  }
}

/**
 * @brief      Initialises the manager object, if existent.
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     True if initialised successfully, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_initManagerObject(JNIEnv *env,
                                                      jobject instance) {
  if (gManagerObject == nullptr) {
    LOGI("initManagerObject(): object is missing");
    return false;
  } else {
    LOGI("initManagerObject(): object already present");
    if (gManagerObject->isInitialised() || gManagerObject->isStarted()) {
      LOGI("initManagerObject(): already initialised/started");
      return false;
    } else {
      gManagerObject->initService();
      LOGI("initManagerObject(): initialised the object");
      return gManagerObject->isInitialised();
    }
  }
}

/**
 * @brief      Checks if the manager object is started
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     True if started, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_isManagerObjectStarted(JNIEnv *env,
                                                           jobject instance) {
  if (gManagerObject != nullptr) {
    return gManagerObject->isStarted();
  } else {
    return false;
  }
}

/**
 * @brief      Checks if the manager object is initialised
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     True if initialised, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_isManagerObjectInitialised(
    JNIEnv *env, jobject instance) {
  if (gManagerObject != nullptr) {
    return gManagerObject->isInitialised();
  } else {
    return false;
  }
}

/**
 * @brief      Checks if the manager object exists
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     True if exists, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_managerObjectExists(JNIEnv *env,
                                                        jobject instance) {
  return gManagerObject != nullptr;
}

/**
 * @brief      Resets the manager object, if existent
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 * @param[in]  jpath     The save directory path
 * @param[in]  juuid     The uuid of the running user
 *
 * @return     True if resetted successfully, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_resetManagerObject(JNIEnv *env,
                                                       jobject instance,
                                                       jstring jpath,
                                                       jstring juuid) {
  if (gManagerObject != nullptr) {
    LOGI("resetManagerObject(): resetting the object");
    if (gManagerObject->isStarted()) {
      gManagerObject->stopService();
    }

    std::string path{env->GetStringUTFChars(jpath, 0)};
    std::string uuid{env->GetStringUTFChars(juuid, 0)};

    // TODO gManagerObject = std::make_unique<Manager>(path, uuid, env);
    gManagerObject = std::make_unique<Manager>(path, uuid, g_vm, &g_obj, &g_clazz, &g_mid, env->GetVersion());
    std::this_thread::sleep_for(100ms);
    return true;
  } else {
    LOGI("resetManagerObject(): object does not exist");
    return false;
  }
}

/**
 * @brief      Destroys the manager object, if existent
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     True if destroyed, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_destroyManagerObject(JNIEnv *env,
                                                         jobject instance) {
  if (gManagerObject != nullptr) {
    LOGI("destroyManagerObject(): deleting the object");
    if (gManagerObject->isStarted()) {
      gManagerObject->stopService();
      std::this_thread::sleep_for(100ms);
    }

    gManagerObject = nullptr;
    std::this_thread::sleep_for(10ms);
    return true;
  } else {
    LOGI("destroyManagerObject(): object does not exist");
    return false;
  }
}

/**
 * @brief      Gets manager object's running time
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     String containing the runnint time, or 'N/A' if not applicable.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_ch_ethz_karajan_lib_BaseMeterService_managerObjectRunningTime(JNIEnv *env,
                                                             jobject instance) {
  if (gManagerObject != nullptr) {
    if (gManagerObject->isStarted()) {
      auto ret = gManagerObject->getRunningTime();

      using std::chrono::hours;
      using std::chrono::minutes;
      using std::chrono::seconds;

      auto hrs = std::chrono::duration_cast<hours>(ret);
      auto mins = std::chrono::duration_cast<minutes>(ret % hours{1});
      auto secs = std::chrono::duration_cast<seconds>(ret % minutes{1});

      return env->NewStringUTF(fmt::format("{:0>2}:{:0>2}:{:0>2}.", hrs.count(),
                                           mins.count(), secs.count())
                                   .c_str());
    } else {
      LOGI("nativeGetRunningTime(): object is not started");
    }
  } else {
    LOGI("nativeGetRunningTime(): object does not exist");
  }

  return env->NewStringUTF("N/A");
}
