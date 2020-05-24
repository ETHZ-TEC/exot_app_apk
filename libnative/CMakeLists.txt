# Copyright (c) 2015-2020, Swiss Federal Institute of Technology (ETH Zurich)
# All rights reserved.
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# 
# * Redistributions in binary form must reproduce the above copyright notice,
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# 
# * Neither the name of the copyright holder nor the names of its
#   contributors may be used to endorse or promote products derived from
#   this software without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
# 
cmake_minimum_required(VERSION 3.8)
project(exot-lib VERSION 1.0.0 LANGUAGES CXX)

# The build system has a tendency to attach multiple -std=<...>
# definitions, therefore we perform a rather inelegant string
# replacement to remove the "spurious" definitions.
string(REPLACE "-std=c++11" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
string(REPLACE "-std=c++14" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
string(REPLACE "-std=gnu++11" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
string(REPLACE "-std=gnu++14" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/src/exot-c++)

add_library(exot-jni SHARED "")

set(exot_jni_include_dir
  ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/include)
set(exot_jni_source_dir
  ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/src)

set(exot_jni_headers
  "${exot_jni_include_dir}/exot/jni/log.h"
  "${exot_jni_include_dir}/exot/jni/manager.h"
  "${exot_jni_include_dir}/exot/jni/wrapper.h")

set(exot_jni_sources "")

set(exot_jni_headers_and_sources ${exot_jni_headers} ${exot_jni_sources})

target_sources(exot-jni PRIVATE ${exot_jni_headers_and_sources})
target_include_directories(exot-jni PUBLIC ${exot_jni_include_dir})

if (${CMAKE_MAJOR_VERSION}.${CMAKE_MINOR_VERSION} GREATER 3.7)
    set_target_properties(exot-jni PROPERTIES
      CXX_STANDARD 17
      CXX_STANDARD_REQUIRED YES)
else ()
    set(CMAKE_CXX11_EXTENSION_COMPILE_OPTION "-std=c++17 -fexceptions")
endif ()

# Get the android logging library
find_library(android-logging log)

# Link the covert and logging libraries
target_link_libraries(exot-jni ${android-logging} exot exot-modules)
