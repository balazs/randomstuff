wk: main.cc strong.o libmem.so
	g++ -o wk main.cc libmem.so strong.o

strong.o: strong.cc
	g++ -c strong.cc

libmem.so: weak.cc
	g++ -fPIC -shared -o libmem.so.1 weak.cc
	ln -s libmem.so.1 libmem.so

phony clean:
	rm -f wk libmem.so* strong.o
