#include <stdio.h>

extern "C" {

void weak(int) __attribute__((weak));

void weak(int) {
    printf("lol I'm weak\n");
}

}

void call_weak(int x) {
  weak(x);
}
