### Description

This repository contains an Android application that measures device utilisation.

To compile the application, run:

```bash
./gradlew --parallel --warn build
```

And to install on connected devices, run:

```bash
./gradlew --parallel --warn installDebug
```

The compiled and packaged applications are available in the [project's tags](https://gitlab.ethz.ch/klopottb/android/tags).

### Caveats

Although the preferred C++ standard library for Android, the libc++, has support for C++17, the CMake binary bundled with the Android SDK is quite old, and only supports setting the standard up to C++14. Some changes have been applied to have the application compile correctly with Gradle.

1. First of all, the main *build.gradle* contains the following declaration:
  ```gradle
  externalNativeBuild {
      cmake {
          // The covert library requires C++17 features.
          cppFlags "-std=c++1z -fexceptions"

          // Build the covert library and the native static library.
          targets "covert", "native"

          // The c++_{static,shared} STL has full support of C++17 features.
          // Also, force the CMake defined C++ standards.
          // 
          // WARNING: This option will only work for CMake version ~> 3.8, 
          // which is not the one bundled with the SDK. An external CMake
          // binary has to be provided, down below.
          arguments "-DANDROID_STL=c++_shared",
                    "-DANDROID_TOOLCHAIN=clang",
                    "-DCMAKE_CXX_STANDARD=17",
                    "-DCMAKE_CXX_STANDARD_REQUIRED=ON",
                    "-DCMAKE_CXX_EXTENSIONS=OFF"
      }
  }
  ```
2. An external CMake binary is provided. In the case of this repository, it has been set to `version "3.11.2"`, but can be anything above 3.8.
3. The Gradle build has a tendency to append spurious or multiple `-std=c++XX` definitions to the compiler command. To remove any of them, the CMakeLists.txt contains:
  ```cmake
  # Older versions of CMake do not support the C++17 standard...
  if (${CMAKE_MAJOR_VERSION}.${CMAKE_MINOR_VERSION} GREATER 3.7)
      set_target_properties(native PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED YES)
  else ()
      set(CMAKE_CXX11_EXTENSION_COMPILE_OPTION "-std=c++17")
  endif ()
  # The build system has a tendency to attach multiple -std=<...>
  # definitions, therefore we perform a rather inelegant string
  # replacement to remove the "spurious" definitions.
  string(REPLACE "-std=c++11" "" CMAKE_CXX_FLAGS ${CMAKE_CXX_FLAGS})
  string(REPLACE "-std=gnu++14" "" CMAKE_CXX_FLAGS ${CMAKE_CXX_FLAGS})
  ```