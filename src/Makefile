.DEFAULT_GOAL := create_jar

create_jar:
	javac simulatedannealingvrp/*.java
	jar cfe SimulatedAnnealingVRP.jar simulatedannealingvrp.SimulatedAnnealingVRP simulatedannealingvrp/*
	rm simulatedannealingvrp/*.class
