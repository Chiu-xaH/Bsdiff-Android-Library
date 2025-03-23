//
// Created by Chiu-xaH on 2025/3/21.
//

#ifndef BSDIFFS_MAIN_H
#define BSDIFFS_MAIN_H

int patch(const char *oldFile, const char *newFile, const char *patchFile);
int merge(const char *oldFile, const char *patchFile, const char *newFile);

#endif //BSDIFFS_MAIN_H
