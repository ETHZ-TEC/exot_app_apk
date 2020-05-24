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
 * @file libnative/src/main/cpp/include/exot/jni/log.h
 * @author     Bruno Klopott
 * @brief      Android logging wrapper with fmt::format interface and a static
 *             logger instance.
 */

#pragma once

#include <android/log.h>
#include <fmt/format.h>

inline namespace android {
/**
 * @brief Logging similar to Java Android logging, based on TAG + format
 * approach. Formatting is done the same as with fmt::print or fmt::format.
 * Use the static AndroidLogger instance below, as follows:
 *
 * @code
 * const char* TAG = "myTag";
 * Log.e(TAG, "some message");
 * Log.w(TAG, "message with arguments: {}, {}, {}", 1, 2, 3);
 * Log.w(TAG, "message with arguments and formats: {:#0x}", 123);
 *
 * Log.v(TAG "/TagExtra", "message");
 * Log.v("anyTag", "any message");
 * @endcode
 *
 */
struct AndroidLogger {
  template <typename... Ts>
  static inline void i(const char* tag, Ts&&... ts) {
    __android_log_print(ANDROID_LOG_INFO, tag, "%s",
                        fmt::format(std::forward<Ts>(ts)...).c_str());
  }

  template <typename... Ts>
  static inline void d(const char* tag, Ts&&... ts) {
    __android_log_print(ANDROID_LOG_DEBUG, tag, "%s",
                        fmt::format(std::forward<Ts>(ts)...).c_str());
  }

  template <typename... Ts>
  static inline void e(const char* tag, Ts&&... ts) {
    __android_log_print(ANDROID_LOG_ERROR, tag, "%s",
                        fmt::format(std::forward<Ts>(ts)...).c_str());
  }

  template <typename... Ts>
  static inline void v(const char* tag, Ts&&... ts) {
    __android_log_print(ANDROID_LOG_VERBOSE, tag, "%s",
                        fmt::format(std::forward<Ts>(ts)...).c_str());
  }

  template <typename... Ts>
  static inline void w(const char* tag, Ts&&... ts) {
    __android_log_print(ANDROID_LOG_WARN, tag, "%s",
                        fmt::format(std::forward<Ts>(ts)...).c_str());
  }
};

/**
 * @brief The static AndroidLogger instance
 */
static inline AndroidLogger Log{};

}  // namespace android