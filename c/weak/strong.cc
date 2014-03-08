#include <stdio.h>

extern "C" {

//void strong(int) __attribute__ ((visibility("default")));
void strong(int) {
    printf("I'm strong!\n");
}

void weak(int) __attribute__((visibility("default"), alias("strong")));

}
