.DEFAULT_GOAL := run

run:
	java -jar SimulatedAnnealingVRP.jar

compile:
	cd src; make; cd ..; mv src/*.jar .	
