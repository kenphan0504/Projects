import java.util.ArrayList;

public class Connection {
    ArrayList<Node> connectedNodes;

    public Connection() {
        connectedNodes = new ArrayList<Node>();
    }

    public void addConnection(Node n) {
        connectedNodes.add(n);
    }

    public int size() {
        return connectedNodes.size();
    }

    public ArrayList<Node> getConnectedNodes() {
        return connectedNodes;
    }
}
