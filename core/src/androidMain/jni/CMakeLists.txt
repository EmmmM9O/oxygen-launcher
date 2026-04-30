cmake_minimum_required(VERSION 3.22.1)

project("oxygen")

find_package(bytehook REQUIRED CONFIG)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

add_library(bytehook_headers_only INTERFACE)
target_link_libraries(bytehook_headers_only INTERFACE bytehook::bytehook)

set(JNI ${CMAKE_SOURCE_DIR})
file(GLOB_RECURSE CORE "${JNI}/*.cpp")
add_library(oxygen SHARED ${CORE})
target_include_directories(oxygen PRIVATE ${JNI})
target_link_libraries(oxygen bytehook_headers_only log android)
