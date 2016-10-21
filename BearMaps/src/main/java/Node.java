public class Node {
    long id;
    double lat, lon;
    Connection con;
    double priority;
    double gValue;
    Node prev;

    public Node(long id, double lon, double lat, Connection con) {
        this.id = id;
        this.lon = lon;
        this.lat = lat;
        this.con = con;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Node node = (Node) o;

        if (id != node.id) {
            return false;
        }
        if (Double.compare(node.lat, lat) != 0) {
            return false;
        }
        if (Double.compare(node.lon, lon) != 0) {
            return false;
        }
        return con.equals(node.con);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (id ^ (id >>> 32));
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + con.hashCode();
        return result;
    }
}
