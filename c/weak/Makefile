cc ?= g++

wk: main.cc strong.o libmem.so
	$(cc) -o wk main.cc libmem.so strong.o

strong.o: strong.cc
	$(cc) -fvisibility=hidden -c strong.cc

libmem.so: weak.cc
	$(cc) -fvisibility=hidden -fPIC -shared -o libmem.so.1 weak.cc
	rm -f libmem.so && ln -s libmem.so.1 libmem.so

phony clean:
	rm -f wk libmem.so* strong.o
