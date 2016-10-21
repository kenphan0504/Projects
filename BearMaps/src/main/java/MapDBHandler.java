import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
//import java.util.LinkedList;
/**
 *  Parses OSM XML files using an XML SAX parser. Used to construct the graph of roads for
 *  pathfinding, under some constraints.
 *  See OSM documentation on
 *  <a href="http://wiki.openstreetmap.org/wiki/Key:highway">the highway tag</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Way">the way XML element</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Node">the node XML element</a>,
 *  and the java
 *  <a href="https://docs.oracle.com/javase/tutorial/jaxp/sax/parsing.html">SAX parser tutorial</a>.
 *  @author Alan Yao
 */
public class MapDBHandler extends DefaultHandler {
    /**
     * Only allow for non-service roads; this prevents going on pedestrian streets as much as
     * possible. Note that in Berkeley, many of the campus roads are tagged as motor vehicle
     * roads, but in practice we walk all over them with such impunity that we forget cars can
     * actually drive on them.
     */
    private static final Set<String> ALLOWED_HIGHWAY_TYPES = new HashSet<>(Arrays.asList
            ("motorway", "trunk", "primary", "secondary", "tertiary", "unclassified",
                    "residential", "living_street", "motorway_link", "trunk_link", "primary_link",
                    "secondary_link", "tertiary_link"));
    private String activeState = "";
    private String highwayType = "";
    private boolean highwayFound = false;
    private int waySize;
    private long previousID; //previous node to be connected to current node
    private long currentID;  //current node to be connected to previous node
    private ArrayList<IDPair> idPairs; //keep track of all the pairs to be connected in a Way
    private final GraphDB g;

    public MapDBHandler(GraphDB g) {
        this.g = g;
    }

    /**
     * Called at the beginning of an element. Typically, you will want to handle each element in
     * here, and you may want to track the parent element.
     * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
     *            if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace
     *                  processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are
     *              not available. This tells us which element we're looking at.
     * @param attributes The attributes attached to the element. If there are no attributes, it
     *                   shall be an empty Attributes object.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     * @see Attributes
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        /* Some example code on how you might begin to parse XML files. */
        long id = 0;
        double lon = 0;
        double lat = 0;
        if (qName.equals("node")) {
            activeState = "node";
            id = Long.parseLong(attributes.getValue("id"));
            lon = Double.parseDouble(attributes.getValue("lon"));
            lat = Double.parseDouble(attributes.getValue("lat"));
            Node newNode = new Node(id, lon, lat, new Connection());
            g.allNodes.put(id, newNode);
        } else if (qName.equals("way")) {
            activeState = "way";
            idPairs = new ArrayList<>();
            highwayFound = false;
            waySize = 0;
        } else if (activeState.equals("way") && qName.equals("nd")) {
            if (waySize == 0) {
                previousID = Long.parseLong(attributes.getValue("ref"));
                waySize += 1;
            } else {
                currentID = Long.parseLong(attributes.getValue("ref"));
                idPairs.add(new IDPair(previousID, currentID));
                previousID = currentID;
                waySize += 1;
            }
        } else if (activeState.equals("way") && qName.equals("tag")
                && attributes.getValue("k").equals("highway")) {
            highwayFound = true;
            highwayType = attributes.getValue("v");
        } else if (activeState.equals("node") && qName.equals("tag")
                && attributes.getValue("k").equals("name")) {
            String name = attributes.getValue("v");
//            Node node = new Node(id, lon, lat, new Connection());
//            if (!g.nameToNodes.containsKey(name)) {
//                LinkedList<Node> newNodeList = new LinkedList<>();
//                newNodeList.add(node);
//                g.nameToNodes.put(name, newNodeList);
//            } else {
//                g.nameToNodes.get(name).add(node);
//            }
            g.nameToNodes.put(name, id);
        }
    }

    /**
     * Receive notification of the end of an element. You may want to take specific terminating
     * actions here, like finalizing vertices or edges found.
     * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
     *            if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace
     *                  processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are
     *              not available.
     * @throws SAXException  Any SAX exception, possibly wrapping another exception.
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("way") && idPairs != null && waySize > 1) {
            if (highwayFound && ALLOWED_HIGHWAY_TYPES.contains(highwayType)) {
                for (IDPair pair : idPairs) {
                    Node leftNode = g.allNodes.get(pair.leftID);
                    Node rightNode = g.allNodes.get(pair.rightID);
                    leftNode.con.addConnection(rightNode);
                    rightNode.con.addConnection(leftNode);
                }
            }
        }
    }
}
