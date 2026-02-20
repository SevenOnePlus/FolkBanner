cmake_minimum_required(VERSION 3.22.1)
project("folkrandom")

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

add_library(folkrandom SHARED jni_interface.c)

find_library(log-lib log)

target_link_libraries(folkrandom ${log-lib})

target_compile_options(folkrandom PRIVATE -O3 -Wall -Wextra)
target_compile_definitions(folkrandom PRIVATE _GNU_SOURCE)