## Defining 3.6 as the minimum version should be safe, the one bundled
## in the Android SDK is 3.6.4.
cmake_minimum_required(VERSION 3.6)
project(karajan-lib VERSION 0.0.1 LANGUAGES CXX)

# The build system has a tendency to attach multiple -std=<...>
# definitions, therefore we perform a rather inelegant string
# replacement to remove the "spurious" definitions.
string(REPLACE "-std=c++11" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
string(REPLACE "-std=c++14" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
string(REPLACE "-std=gnu++11" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
string(REPLACE "-std=gnu++14" "" CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

# Get the android logging library
#find_library(android-logging log)

# Import the covert library and the dependencies it defines
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/src/covert)

# Link the covert and logging libraries
#target_link_libraries(covert ${android-logging})

