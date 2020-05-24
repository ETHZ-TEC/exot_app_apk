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
 * @file native.cpp
 * @author     Bruno Klopott
 * @brief      Contains the functions for the Java Native Interface.
 */

#include <jni.h>

#include <chrono>
#include <utility>

#include <exot/components/meter_host_logger.h>
#include <exot/jni/log.h>
#include <exot/jni/manager.h>
#include <exot/jni/wrapper.h>
#include <exot/meters/frequency.h>
#include <exot/meters/process.h>
#include <exot/meters/thermal.h>
#include <exot/meters/utilisation.h>

using component_t = exot::components::meter_host_logger<
        std::chrono::nanoseconds, exot::modules::utilisation_procfs,
        exot::modules::frequency_sysfs, exot::modules::frequency_rel,
        exot::modules::thermal_sysfs, exot::modules::process_android>;
using manager_t     = exot::jni::Manager<component_t>;
using wrapper_t     = exot::jni::Wrapper<manager_t>;
using wrapper_ptr_t = std::unique_ptr<wrapper_t>;

wrapper_ptr_t gWrapperObject = std::make_unique<wrapper_t>();

const char* TAG = "ExOT/Native/ThermalSC";

static JavaVM* g_java_vm;
static jobject g_java_instance;
static jclass g_java_class;
static jmethodID g_java_method_id;
static jint g_jni_version;

/**
 * @brief      Creates the manger object
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 * @param[in]  json      The json string to configure the module
 *
 * @return     True if created successfully, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_exot_lib_BaseService_createManagerObject(JNIEnv* env,
                                                      jobject instance,
                                                      jstring jconfig) {
    Log.d(TAG, "{}()", __func__);

    exot::jni::populate_java_pointers(env, instance, g_java_vm, g_java_instance,
                                      g_java_class, g_java_method_id,
                                      g_jni_version);
    env->GetJavaVM(&g_java_vm);

    if (jconfig == NULL) Log.w(TAG, "{}(): jconfig == NULL", __func__);
    std::string config{jconfig != NULL ? env->GetStringUTFChars(jconfig, 0)
                                       : "{}"};

    auto [java_vm_ptr, java_instance_ptr, java_class_ptr, java_method_id_ptr,
    jni_version] =
    exot::jni::produce_references(g_java_vm, g_java_instance, g_java_class,
                                  g_java_method_id, g_jni_version);

    Log.d(TAG,
          "{}(): "
          "java_vm_ptr: {:#0x}, java_instance_ptr: {:#0x}, "
          "java_class_ptr: {:#0x}, java_method_id_ptr: {:#0x}, "
          "jni_version: {:#0x}",
          __func__,                            //
          java_vm_ptr, java_instance_ptr,      //
          java_class_ptr, java_method_id_ptr,  //
          jni_version);

    return static_cast<jboolean>(
            gWrapperObject->create(config, java_vm_ptr, java_instance_ptr,
                                   java_class_ptr, java_method_id_ptr, jni_version));
}

/**
 * @brief      Resets the manager object, if existent
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     True if resetted successfully, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_exot_lib_BaseService_resetManagerObject(JNIEnv* env,
                                                     jobject instance,
                                                     jstring jconfig) {
    using namespace std::literals::chrono_literals;
    Log.d(TAG, "{}()", __func__);

    if (gWrapperObject->exists()) {
        Log.d(TAG, "{}(): exists, destroying...", __func__);
        gWrapperObject->destroy();
    }

    exot::jni::populate_java_pointers(env, instance, g_java_vm, g_java_instance,
                                      g_java_class, g_java_method_id,
                                      g_jni_version);
    env->GetJavaVM(&g_java_vm);

    if (jconfig == NULL) Log.w(TAG, "{}(): jconfig == NULL", __func__);
    std::string config{jconfig != NULL ? env->GetStringUTFChars(jconfig, 0)
                                       : "{}"};

    auto [java_vm_ptr, java_instance_ptr, java_class_ptr, java_method_id_ptr,
    jni_version] =
    exot::jni::produce_references(g_java_vm, g_java_instance, g_java_class,
                                  g_java_method_id, g_jni_version);

    auto status =
            gWrapperObject->create(config, java_vm_ptr, java_instance_ptr,
                                   java_class_ptr, java_method_id_ptr,
                                   jni_version);

    std::this_thread::sleep_for(100ms);
    Log.d(TAG, "{}(): creation status: {}", __func__, status);
    return static_cast<jboolean>(status);
}

/**
 * @brief      Starts the manager object
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     True if started successfully, false otherwise.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_ch_ethz_exot_lib_BaseService_startManagerObject(JNIEnv* env,
                                                     jobject instance) {
    Log.d(TAG, "{}()", __func__);
    return static_cast<jboolean>(gWrapperObject->start());
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
Java_ch_ethz_exot_lib_BaseService_stopManagerObject(JNIEnv* env,
                                                    jobject instance) {
    Log.d(TAG, "{}()", __func__);
    return static_cast<jboolean>(gWrapperObject->stop());
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
Java_ch_ethz_exot_lib_BaseService_isManagerObjectStarted(JNIEnv* env,
                                                         jobject instance) {
    Log.d(TAG, "{}()", __func__);
    return static_cast<jboolean>(gWrapperObject->is_started());
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
Java_ch_ethz_exot_lib_BaseService_managerObjectExists(JNIEnv* env,
                                                      jobject instance) {
    Log.d(TAG, "{}()", __func__);
    return static_cast<jboolean>(gWrapperObject->exists());
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
Java_ch_ethz_exot_lib_BaseService_destroyManagerObject(JNIEnv* env,
                                                       jobject instance) {
    Log.d(TAG, "{}()", __func__);
    return static_cast<jboolean>(gWrapperObject->destroy());
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
Java_ch_ethz_exot_lib_BaseService_managerObjectRunningTime(JNIEnv* env,
                                                           jobject instance) {
    Log.d(TAG, "{}()", __func__);
    auto result = gWrapperObject->get_running_time();
    return env->NewStringUTF(result.c_str());
}

/**
 * @brief      Query the manager object status
 *
 * @param      env       The environment
 * @param[in]  instance  The instance
 *
 * @return     String containing the status of the manager, or 'N/A' if not applicable.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_ch_ethz_exot_lib_BaseService_queryManagerObjectStatus(JNIEnv* env,
                                                           jobject instance) {
    Log.d(TAG, "{}()", __func__);
    auto result = gWrapperObject->query_state();
    return env->NewStringUTF(result.c_str());
}
