#include <stdio.h>
#include "main.h"

// 生成补丁包例子
int main() {
    const char *oldFile = "D:\\1.txt";
    const char *newFile = "D:\\2.txt";
    const char *patchFile = "D:\\1_patched_2.txt";

    int result = patch(oldFile, newFile, patchFile);
    printf("Patch result: %d\n", result);

    return result;
}
// 合并补丁包例子
//int main() {
//    const char *oldFile = "D:\\1.txt";
//    const char *newFile = "D:\\2.txt";
//    const char *patchFile = "D:\\1_patched_2.txt";
//
//    int result = merge(oldFile, patchFile,newFile );
//    printf("Patch result: %d\n", result);
//
//    return result;
//}
