.DEFAULT_GOAL := run

run:
	java -jar SimulatedAnnealingVRP.jar $(file) $(vehicles) $(averages)

compile:
	cd src; make; cd ..; mv src/*.jar .	
