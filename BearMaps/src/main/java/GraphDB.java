//import jdk.nashorn.internal.ir.LiteralNode;
import org.xml.sax.SAXException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Wraps the parsing functionality of the MapDBHandler as an example.
 * You may choose to add to the functionality of this class if you wish.
 * @author Alan Yao
 */
public class GraphDB {
    /**
     * Example constructor shows how to create and start an XML parser.
     * @param db_path Path to the XML file to be parsed.
     */
    HashMap<Long, Node> allNodes;
//    HashMap<String, LinkedList<Node>> nameToNodes;
    HashMap<String, Long> nameToNodes;

    public GraphDB(String dbPath) {
        allNodes = new HashMap<>();
        nameToNodes = new HashMap<>();
        try {
            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            MapDBHandler maphandler = new MapDBHandler(this);
            saxParser.parse(inputFile, maphandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        ArrayList<Long> idToRemove = new ArrayList<>();
        Collection<Node> nodes = allNodes.values();
        for (Node node: nodes) {
            if (node.con.size() == 0) {
                idToRemove.add(node.id);
            }
        }
        for (Long id: idToRemove) {
            allNodes.remove(id);
        }
    }
}
