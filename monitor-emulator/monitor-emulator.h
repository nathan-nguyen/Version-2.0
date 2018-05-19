#ifndef MONITOR_EMULATOR_H_INCLUDED
#define MONITOR_EMULATOR_H_INCLUDED

#include <stddef.h>
#include <sys/time.h>
#include <stdio.h>
#include <time.h>

void emu_copy_from_user(int * localUID, int * UID, unsigned long n){
	memcpy(localUID, UID, n);
}

#endif

