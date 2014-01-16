#include <unistd.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <fcntl.h>
#include <sys/select.h>
#include <sys/stat.h>
#include <sys/types.h>

void die(const char* file, int line, const char* msg) {
  fprintf(stderr, "ERROR: %s:%d # %s\n", file, line, msg ? msg : "???");
  exit(1);
}

#define DIE_WITH_MSG(msg) die(__FILE__, __LINE__, msg)
#define DIE() die(__FILE__, __LINE__)
#define CHECK(cond) do { if (!(cond)) DIE_WITH_MSG(#cond); } while (0)

struct Input {
  dirent* ent;
  //FILE* file;
  int fd;
};

const int MAX_INPUTS = 100;
static Input inputs[MAX_INPUTS];
static int numInputs = 0;
fd_set fds;

int main() {
  FD_ZERO(&fds);
  DIR* dir = opendir("/dev/input");
  CHECK(dir);
  while (struct dirent* ent = readdir(dir)) {
    Input& e = inputs[numInputs];
    e.ent = ent;
    char buf[PATH_MAX];
    if (ent->d_name[0] == '.') // . and ..
      continue;
    strcpy(buf, "/dev/input/");
    strcat(buf, ent->d_name);
    printf("opening %s, ", buf);
//     e.file = fopen(buf, "r");
//     if (!e.file) {
//       fprintf(stderr, "cannot open %s, skipping\n", buf);
//       continue;
//     }
//     CHECK(e.file);
//     e.fd = fileno(e.file);
    e.fd = open(buf, O_RDONLY | O_NONBLOCK);
    CHECK(e.fd != -1);
    printf("fd=%d\n", e.fd);

    struct stat sb;
    fstat(e.fd, &sb);
    if ((sb.st_mode & S_IFMT) != S_IFCHR) {
      printf("%s is not characte device, skipping\n", buf);
      continue;
    }

    FD_SET(e.fd, &fds);
    CHECK(numInputs++ < MAX_INPUTS);
  }

  while (1) {
    fd_set rfds = fds;
    int r;
    do {
      r = select(numInputs, &rfds, 0, 0, 0);
      if ((r == -1) && (errno == EINTR))
        printf("wtf?\n");
    } while ((r == -1) && (errno == EINTR));
    //(r == -1 && errno == EINTR);
    CHECK(r != -1);
    printf("select returned\n");
    for (int i = 0; i < numInputs; ++i) {
      Input& input = inputs[i];
      if (FD_ISSET(input.fd, &rfds)) {
        const int BUF_SIZE = 1024;
        char buf[BUF_SIZE];
        ssize_t n;
        while (1) {
          n = read(input.fd, buf, BUF_SIZE);
          switch (errno) {
            case EBADF: DIE_WITH_MSG("EBADF");
            case EINVAL: DIE_WITH_MSG("EINVAL");
            case ENOMEM: DIE_WITH_MSG("EIO");
            case EISDIR: DIE_WITH_MSG("EISDIR");
          }
          CHECK(n >= 0 || errno == EAGAIN || errno == EINTR);
          if (n >= 0)
            break;
        }

        printf("%s: read %ld bytes %s\n", input.ent->d_name, n, n == BUF_SIZE ? "(at least)" : "");
      }
    }
  }

  return 0;
}
