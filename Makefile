INPUT ?= entrada.in

.PHONY: build run clean

build:
	@javac --release 17 -encoding UTF-8 Principal.java

run: build
	@java Principal "$(INPUT)"

clean:
	rm -f Principal*.class
