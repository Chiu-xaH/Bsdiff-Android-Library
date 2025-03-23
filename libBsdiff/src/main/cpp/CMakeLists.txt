cmake_minimum_required(VERSION 3.22.1)

project("bsdiff")
# List C/C++ source files with relative paths to this CMakeLists.txt.

include_directories(bzip)

#add_executable add_library
add_library("bsdiff" SHARED
        bsdiff/bsdiff.c
        bspatch/bspatch.c
        bzip/blocksort.c
        bzip/bzlib.c
        bzip/bzlib.h
        bzip/bzlib_private.h
        bzip/compress.c
        bzip/crctable.c
        bzip/decompress.c
        bzip/huffman.c
        bzip/randtable.c
        main.h
#        main.c
        native-lib.c)

target_link_libraries("bsdiff"
        # List libraries link to the target library
        android
        log)