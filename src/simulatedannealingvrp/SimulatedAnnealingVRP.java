package simulatedannealingvrp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SimulatedAnnealingVRP {
    private static final int DEPOT_NODE = 1;
    private final String fileName;
    private final int truckCount;
    private final int averages;

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            System.err.println("Format: make file=A-n39-k6.vrp vehicles=6 averages=5");
            return;
        }
        var file = args[0];
        var handle = Paths.get(file);
        if (!Files.isRegularFile(handle) || !Files.isReadable(handle)) {
            System.err.println("Error: could not read file " + file);
            return;
        }
        int truckCount;
        try {
            truckCount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            truckCount = -1;
        }
        if (truckCount <= 0 || truckCount > 1_000_000) {
            var error = "Error: vehicles must be a positive integer not greater than one million";
            System.err.println(error);
            return;
        }
        int averages;
        try {
            averages = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        } catch (NumberFormatException e) {
            averages = -1;
        }
        if (averages <= 0 || averages > 1_000_000) {
            var error = "Error: averages must be a positive integer not greater than one million";
            System.err.println(error);
            return;
        }
        var vrp = new SimulatedAnnealingVRP(file, truckCount, averages);
        vrp.runLogic();
    }

    private SimulatedAnnealingVRP(String fileName, int truckCount, int averages) {
        this.fileName = fileName;
        this.truckCount = truckCount;
        this.averages = averages;
    }

    private void runLogic() {
        runCase(false, false);
        runCase(false, true);
        runCase(true, false);
        runCase(true, true);
    }

    private void runCase(boolean withService, boolean withRounding) {
        var customers = getCustomers();
        var truckRoutes = initialize(customers);
        var cost = new BestCost(Integer.MAX_VALUE, null);
        for (int i = 0; i < averages; i++) {
            var temp = bestCostFromSA(customers.get(DEPOT_NODE), truckRoutes, withService, withRounding);
            if (temp.getCost() < cost.getCost()) {
                cost = temp;
            }
        }
        var message = "With service = %b, with rounding = %b; %s\n";
        System.out.format(Locale.US, message, withService, withRounding, cost);
    }

    private Map<Integer, Customer> getCustomers() {
        var customers = new HashMap<Integer, Customer>();
        try (var in = Files.newInputStream(Paths.get(fileName));
             var reader = new BufferedReader(new InputStreamReader(in))) {
            int state = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                var cleanLine = line.trim();
                if (cleanLine.equals("NODE_COORD_SECTION") || cleanLine.equals("DEMAND_SECTION")
                        || cleanLine.equals("DEPOT_SECTION") || cleanLine.equals("EOF")) {
                    state++;
                    continue;
                }
                switch (state) {
                    case 1:
                        var coordinates = cleanLine.split(" ");
                        if (coordinates.length != 3) {
                            throw new IllegalStateException("Must have three coordinate parameters");
                        }
                        int index = Integer.parseInt(coordinates[0]);
                        int x = Integer.parseInt(coordinates[1]);
                        int y = Integer.parseInt(coordinates[2]);
                        var customer = new Customer(index, x, y);
                        customers.put(index, customer);
                        break;
                    case 2:
                        var service = cleanLine.split(" ");
                        if (service.length != 2) {
                            throw new IllegalStateException("Must have two service parameters");
                        }
                        int serviceIndex = Integer.parseInt(service[0]);
                        int serviceCost = Integer.parseInt(service[1]);
                        var existingCustomer = customers.get(serviceIndex);
                        existingCustomer.setService(serviceCost);
                        customers.put(serviceIndex, existingCustomer);
                        break;
                    case 3:
                        int depot = Integer.parseInt(cleanLine);
                        if (depot != DEPOT_NODE && depot != -1) {
                            throw new IllegalStateException("Depot configured only to be " + DEPOT_NODE);
                        }
                        break;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not open file: " + fileName);
        }
        return customers;
    }

    private List<ArrayList<Customer>> initialize(Map<Integer, Customer> customers) {
        var truckRoute = new ArrayList<ArrayList<Customer>>();
        int initRemaining = customers.size() - 1;
        int initIndex = 2;
        for (int i = 0; i < truckCount; i++) {
            var list = new ArrayList<Customer>();
            int currentInit = (int) Math.round((double) initRemaining / (truckCount - i));
            initRemaining -= currentInit;
            for (int j = 0; j < currentInit; j++) {
                list.add(customers.get(initIndex));
                initIndex++;
            }
            truckRoute.add(list);
        }
        return truckRoute;
    }

    private BestCost bestCostFromSA(Customer depot, List<ArrayList<Customer>> truckRoutes,
                                    boolean isService, boolean useRoundedDistance) {
        double initTemp = 500;
        double endTemp = 0;
        double alpha = 0.0001;
        double cost = calculateCost(depot, truckRoutes, isService, useRoundedDistance);
        for (double temperature = initTemp; temperature > endTemp; temperature -= alpha) {
            var solutionCandidate = modifyRoute(truckRoutes);
            double newCost = calculateCost(depot, solutionCandidate, isService, useRoundedDistance);
            double costChange = newCost - cost;
            if (costChange < 0 || Math.random() < Math.exp(-costChange / temperature)) {
                truckRoutes = solutionCandidate;
                cost = newCost;
            }
        }
        return new BestCost(cost, truckRoutes);
    }

    private double calculateCost(Customer depot, List<ArrayList<Customer>> truckRoutes,
                                 boolean isService, boolean useRoundedDistance) {
        double cost = 0;
        for (var tr : truckRoutes) {
            Customer first;
            Customer second = depot;
            cost += isService ? depot.getService() : 0;
            for (var c : tr) {
                first = second;
                second = c;
                cost += isService ? second.getService() : 0;
                cost += distance(first, second, useRoundedDistance);
            }
            cost += isService ? depot.getService() : 0;
            cost += distance(second, depot, useRoundedDistance);
        }
        return cost;
    }

    private double distance(Customer first, Customer second, boolean useRoundedDistance) {
        var xDiff = first.getX() - second.getX();
        var yDiff = first.getY() - second.getY();
        double dist = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
        return useRoundedDistance ? Math.round(dist) : dist;
    }

    private List<ArrayList<Customer>> modifyRoute(List<ArrayList<Customer>> truckRoutes) {
        var modifiedRoutes = new ArrayList<ArrayList<Customer>>();
        for (var tr : truckRoutes) {
            var route = new ArrayList<>(tr);
            modifiedRoutes.add(route);
        }
        while (true) {
            int removeTruckIndex = (int) (modifiedRoutes.size() * Math.random());
            var removeSpecificRoute = modifiedRoutes.get(removeTruckIndex);
            if (removeSpecificRoute.isEmpty()) {
                throw new IllegalStateException("Route can never be empty");
            }
            if (removeSpecificRoute.size() == 1) {
                continue;
            }
            int removeCustomerIndex = (int) (removeSpecificRoute.size() * Math.random());
            var customer = removeSpecificRoute.remove(removeCustomerIndex);
            int addTruckIndex = (int) (modifiedRoutes.size() * Math.random());
            var addSpecificRoute = modifiedRoutes.get(addTruckIndex);
            int addCustomerIndex = (int) ((addSpecificRoute.size() + 1) * Math.random());
            addSpecificRoute.add(addCustomerIndex, customer);
            break;
        }
        return modifiedRoutes;
    }
}
