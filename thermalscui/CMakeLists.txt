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
project(thermalscui VERSION 0.0.1 LANGUAGES CXX)

## Import the covert library and the dependencies it defines
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../libnative/
                 ${CMAKE_CURRENT_SOURCE_DIR}/../libnative/)

# Define a static library, which will be imported by the Android JNI
add_library(exot-jni-thermalscui SHARED "")

set(exot-jni-thermalscui_include_dir
  ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/include)
set(exot-jni-thermalscui_source_dir
  ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/)

set(exot-jni-thermalsc_headers "")

file(GLOB_RECURSE exot-jni-thermalscui_sources
  "${exot-jni-thermalscui_source_dir}/*.cpp")
list(APPEND exot-jni-thermalscui_sources
  "${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/jni-thermalscui.cpp")

set(exot-jni-thermalscui_headers_and_sources
  ${exot-jni-thermalscui_headers}
  ${exot-jni-thermalscui_sources})

target_sources(exot-jni-thermalscui PRIVATE ${exot-jni-thermalscui_headers_and_sources})
target_include_directories(exot-jni-thermalscui PRIVATE ${exot-jni-thermalscui_include_dir})

# Link the covert and logging libraries
target_link_libraries(exot-jni-thermalscui exot-jni)
