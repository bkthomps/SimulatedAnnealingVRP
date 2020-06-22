import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VRP {

    private static final String FILE = "A-n39-k6.vrp";
    private static final int TRUCK_COUNT = 6;
    private static final int DEPOT_NODE = 1;

    private static class Customer {
        private final int index;
        private final int x;
        private final int y;
        private int service;

        Customer(int index, int x, int y) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.service = -1;
        }

        private void setService(int service) {
            if (this.service != -1) {
                throw new IllegalStateException("Can only add service once");
            }
            this.service = service;
        }

        @Override
        public boolean equals(Object o) {
            if (service == -1) {
                throw new IllegalStateException("Must set service");
            }
            return (o instanceof Customer) && index == ((Customer) o).index;
        }

        @Override
        public int hashCode() {
            if (service == -1) {
                throw new IllegalStateException("Must set service");
            }
            return Integer.hashCode(index);
        }
    }

    public static void main(String[] args) {
        var vrp = new VRP();
        vrp.runLogic();
    }

    private void runLogic() {
        var customers = getCustomers();
        var truckRoutes = initialize(customers);
        var costNoService = bestCostFromSA(customers.get(DEPOT_NODE), truckRoutes, false);
        System.out.println("Optimal cost (without service cost) is: " + costNoService);
        var costWithService = bestCostFromSA(customers.get(DEPOT_NODE), truckRoutes, true);
        System.out.println("Optimal cost (with service cost) is: " + costWithService);
    }

    private Map<Integer, Customer> getCustomers() {
        var customers = new HashMap<Integer, Customer>();
        try (var in = Files.newInputStream(Paths.get(FILE));
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
                        if (depot != 1 && depot != -1) {
                            throw new IllegalStateException("Depot configured only to be 1");
                        }
                        break;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not open file");
        }
        return customers;
    }

    private List<ArrayList<Customer>> initialize(Map<Integer, Customer> customers) {
        var truckRoute = new ArrayList<ArrayList<Customer>>();
        int initRemaining = customers.size() - 1;
        int initIndex = 2;
        for (int i = 0; i < TRUCK_COUNT; i++) {
            var list = new ArrayList<Customer>();
            int currentInit = (int) Math.round((double) initRemaining / (TRUCK_COUNT - i));
            initRemaining -= currentInit;
            for (int j = 0; j < currentInit; j++) {
                list.add(customers.get(initIndex));
                initIndex++;
            }
            truckRoute.add(list);
        }
        return truckRoute;
    }

    private double bestCostFromSA(Customer depot, List<ArrayList<Customer>> truckRoutes,
                                  boolean isService) {
        var cost = calculateCost(depot, truckRoutes, isService);
        // TODO: add SA
        return cost;
    }

    private double calculateCost(Customer depot, List<ArrayList<Customer>> truckRoutes,
                                 boolean isService) {
        double cost = 0;
        for (var tr : truckRoutes) {
            Customer first;
            Customer second = depot;
            cost += isService ? depot.service : 0;
            for (var c : tr) {
                first = second;
                second = c;
                cost += isService ? second.service : 0;
                cost += distance(first, second);
            }
            cost += isService ? depot.service : 0;
            cost += distance(second, depot);
        }
        return cost;
    }

    private double distance(Customer first, Customer second) {
        return Math.sqrt(Math.pow(first.x - second.x, 2) + Math.pow(first.y - second.y, 2));
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
