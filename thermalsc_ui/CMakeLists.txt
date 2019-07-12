# Defining 3.6 as the minimum version should be safe, the one bundled
# in the Android SDK is 3.6.4.
cmake_minimum_required(VERSION 3.6)
project(thermal-sc VERSION 0.0.1 LANGUAGES CXX)

# Define a static library, which will be imported by the Android JNI
add_library(jni_thermal-sc SHARED "")

set(jni_thermal-sc_include_dir
  ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/include)
set(jni_thermal-sc_source_dir
  ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/src)

set(jni_thermal-sc_headers
  "${jni_thermal-sc_include_dir}/Manager.h")

file(GLOB_RECURSE jni_thermal-sc_sources
  "${jni_thermal-sc_source_dir}/*.cpp")
list(APPEND jni_thermal-sc_sources
  "${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/jni_thermal-sc.cpp")

set(jni_thermal-sc_headers_and_sources ${jni_thermal-sc_headers} ${jni_thermal-sc_sources})

target_sources(jni_thermal-sc PRIVATE ${jni_thermal-sc_headers_and_sources})
target_include_directories(jni_thermal-sc PRIVATE ${jni_thermal-sc_include_dir})

# Older versions of CMake do not support the C++17 standard...
if (${CMAKE_MAJOR_VERSION}.${CMAKE_MINOR_VERSION} GREATER 3.7)
    set_target_properties(jni_thermal-sc PROPERTIES
      CXX_STANDARD 17
      CXX_STANDARD_REQUIRED YES)
else ()
    set(CMAKE_CXX11_EXTENSION_COMPILE_OPTION "-std=c++17 -fexceptions")
endif ()

# The build system has a tendency to attach multiple -std=<...>
# definitions, therefore we perform a rather inelegant string
# replacement to remove the "spurious" definitions.
string(REPLACE "-std=c++11" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
string(REPLACE "-std=c++14" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
string(REPLACE "-std=gnu++11" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
string(REPLACE "-std=gnu++14" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

# Get the android logging library
find_library(android-logging log)

## Import the covert library and the dependencies it defines
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../lib/ ${CMAKE_CURRENT_SOURCE_DIR}/../lib/)

# Link the covert and logging libraries
target_link_libraries(jni_thermal-sc ${android-logging} covert)
