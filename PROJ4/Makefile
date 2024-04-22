JAVAC = javac

JFLAGS = -g

SOURCES = $(wildcard *.java)

all: $(SOURCES:.java=.class)

%.class: %.java
	$(JAVAC) $(JFLAGS) $<

.PHONY: clean

clean:
	rm -f *.class
