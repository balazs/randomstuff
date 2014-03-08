#include <stdio.h>

extern "C" {

void weak(int) __attribute__((weak, visibility("default")));

void weak(int) {
    printf("lol I'm weak\n");
}

}

void call_weak(int x) __attribute__((visibility("default")));
void call_weak(int x) {
  weak(x);
}
