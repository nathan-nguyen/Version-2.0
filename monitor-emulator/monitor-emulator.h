#ifndef MONITOR_EMULATOR_H_INCLUDED
#define MONITOR_EMULATOR_H_INCLUDED

#include <stddef.h>
#include <sys/time.h>

#define __user         __attribute__((noderef, address_space(1)))

#define KERN_SOH   "\001"          /* ASCII Start Of Header */
#define KERN_INFO  KERN_SOH "6"    /* informational */

#define GFP_KERNEL    0

#endif

