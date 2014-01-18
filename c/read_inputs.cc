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

template <typename T>
inline T max(const T& a, const T& b) {
  return (a > b) ? a : b;
}

struct Input {
  dirent* ent;
  int fd;
};

int main() {
  const int MAX_INPUTS = 100;
  Input inputs[MAX_INPUTS];
  int num_inputs = 0;
  fd_set fds;
  FD_ZERO(&fds);
  int max_fd = -1;

  DIR* dir = opendir("/dev/input");
  CHECK(dir);
  while (struct dirent* ent = readdir(dir)) {
    Input& e = inputs[num_inputs];
    e.ent = ent;
    char buf[PATH_MAX];
    if (ent->d_name[0] == '.') // . and ..
      continue;
    strcpy(buf, "/dev/input/");
    strcat(buf, ent->d_name);
    printf("opening %s, ", buf);
    e.fd = open(buf, O_RDONLY | O_NONBLOCK);
    CHECK(e.fd != -1);
    printf("fd=%d\n", e.fd);
    max_fd = max(max_fd, e.fd);

    struct stat sb;
    fstat(e.fd, &sb);
    if ((sb.st_mode & S_IFMT) != S_IFCHR) {
      printf("%s is not characte device, skipping\n", buf);
      continue;
    }

    FD_SET(e.fd, &fds);
    CHECK(++num_inputs < MAX_INPUTS);
  }

  while (1) {
    fd_set rfds = fds;
    int nfds = max_fd + 1;
    int r;
    do {
      r = select(nfds, &rfds, 0, 0, 0);
    } while ((r == -1) && (errno == EINTR));
    CHECK(r != -1);
    for (int i = 0; i < num_inputs; ++i) {
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

        CHECK(n != 0); // no handled
        printf("%s: read %ld bytes %s\n", input.ent->d_name, n, n == BUF_SIZE ? "(at least)" : "");
      }
    }
  }

  return 0;
}
