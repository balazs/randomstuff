#include <stdio.h>

extern "C" {

void strong(int) {
    printf("I'm strong!\n");
}

void weak(int) __attribute__((alias ("strong")));

}
