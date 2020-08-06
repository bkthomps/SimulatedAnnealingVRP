package simulatedannealingvrp;

final class Customer {
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

    void setService(int service) {
        if (this.service >= 0) {
            throw new IllegalStateException("Can only add service once");
        }
        this.service = service;
    }

    int getIndex() {
        return index;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getService() {
        if (service < 0) {
            throw new IllegalStateException("Must set service");
        }
        return service;
    }

    @Override
    public boolean equals(Object o) {
        if (service < 0) {
            throw new IllegalStateException("Must set service");
        }
        return (o instanceof Customer) && index == ((Customer) o).index;
    }

    @Override
    public int hashCode() {
        if (service < 0) {
            throw new IllegalStateException("Must set service");
        }
        return Integer.hashCode(index);
    }
}
