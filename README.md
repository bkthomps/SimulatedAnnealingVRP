# SimulatedAnnealingVRP
Using simulated annealing to solve a vehicle routing problem.

VRP files can be found [here](https://neo.lcc.uma.es/vrp/vrp-instances/capacitated-vrp-instances/)

Files can then be saved to this directory. By default `A-n39-k6.vrp` is already included.

Using Java 14, you can run: `make file=A-n39-k6.vrp vehicles=6 averages=5` where you can change the
filename to any VRP file, where the vehicle count can be modified, and where averages denotes how
many times the calculations will be performed and then averaged.
