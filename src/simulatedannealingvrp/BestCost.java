package simulatedannealingvrp;

import java.util.ArrayList;
import java.util.List;

final class BestCost {
    private final double cost;
    private final List<ArrayList<Customer>> truckRoutes;

    BestCost(double cost, List<ArrayList<Customer>> truckRoutes) {
        this.cost = cost;
        this.truckRoutes = truckRoutes;
    }

    double getCost() {
        return cost;
    }

    @Override
    public String toString() {
        var firstLine = "best Cost = " + cost + ", with these truck routes:\n";
        var sb = new StringBuilder();
        sb.append(firstLine);
        for (int i = 1; i <= truckRoutes.size(); i++) {
            sb.append("Truck ");
            sb.append(i);
            sb.append(':');
            for (var c : truckRoutes.get(i - 1)) {
                sb.append(' ');
                sb.append(c.getIndex());
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
