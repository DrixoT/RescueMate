# CMakeLists.txt for llama.cpp Android integration

cmake_minimum_required(VERSION 3.22.1)

project("llama-android")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Find the Android NDK log library
find_library(log-lib log)

# Configuration for llama.cpp
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Build tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Build examples" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "Build tools" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Build server" FORCE)

# Enable common library as we need sampling functions from it
set(LLAMA_BUILD_COMMON ON CACHE BOOL "Build common" FORCE)

# Disable networking features in common library to avoid dependencies
set(LLAMA_CURL OFF CACHE BOOL "Use curl" FORCE)
set(LLAMA_HTTPLIB OFF CACHE BOOL "Use httplib" FORCE)

# Add llama.cpp as a subdirectory
# This will define targets: llama, ggml, common
add_subdirectory(llama.cpp)

# Suppress deprecation warnings for the common library
# The common library uses deprecated std::wstring_convert which triggers warnings
# This is a third-party library issue, so we suppress the warning
if(TARGET common)
    # Suppress deprecated declarations warning for Clang (Android NDK) and GCC
    if(CMAKE_CXX_COMPILER_ID MATCHES "Clang" OR CMAKE_CXX_COMPILER_ID MATCHES "GNU")
        target_compile_options(common PRIVATE -Wno-deprecated-declarations)
    elseif(CMAKE_CXX_COMPILER_ID MATCHES "MSVC")
        target_compile_options(common PRIVATE /wd4996)  # MSVC equivalent
    endif()
endif()

# JNI source
add_library(llama-android SHARED
    llama-android.cpp
)

# Include directories for our JNI code
target_include_directories(llama-android PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/common
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/ggml/include
)

# Link libraries
target_link_libraries(llama-android
    llama
    common
    ggml
    ${log-lib}
    android
)
