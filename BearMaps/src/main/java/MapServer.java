import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
//import java.nio.Buffer;
import java.util.Map;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Base64;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;

/* Maven is used to pull in these dependencies. */
import com.google.gson.Gson;
//import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import javax.imageio.ImageIO;

import static spark.Spark.*;

/**
 * This MapServer class is the entry point for running the JavaSpark web server for the BearMaps
 * application project, receiving API calls, handling the API call processing, and generating
 * requested images and routes.
 * @author Alan Yao
 */
public class MapServer {
    /**
     * The root upper left/lower right longitudes and latitudes represent the bounding box of
     * the root tile, as the images in the img/ folder are scraped.
     * Longitude == x-axis; latitude == y-axis.
     */
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    /** Each tile is 256x256 pixels. */
    public static final int TILE_SIZE = 256;
    /** HTTP failed response. */
    private static final int HALT_RESPONSE = 403;
    /** Route stroke information: typically roads are not more than 5px wide. */
    public static final float ROUTE_STROKE_WIDTH_PX = 5.0f;
    /** Route stroke information: Cyan with half transparency. */
    public static final Color ROUTE_STROKE_COLOR = new Color(108, 181, 230, 200);
    /** The tile images are in the IMG_ROOT folder. */
    private static final String IMG_ROOT = "img/";
    /**
     * The OSM XML file path. Downloaded from <a href="http://download.bbbike.org/osm/">here</a>
     * using custom region selection.
     **/
    private static final String OSM_DB_PATH = "berkeley.osm";
    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside getMapRaster(). <br>
     * ullat -> upper left corner latitude,<br> ullon -> upper left corner longitude, <br>
     * lrlat -> lower right corner latitude,<br> lrlon -> lower right corner longitude <br>
     * w -> user viewport window width in pixels,<br> h -> user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
        "lrlon", "w", "h"};
    /**
     * Each route request to the server will have the following parameters
     * as keys in the params map.<br>
     * start_lat -> start point latitude,<br> start_lon -> start point longitude,<br>
     * end_lat -> end point latitude, <br>end_lon -> end point longitude.
     **/
    private static final String[] REQUIRED_ROUTE_REQUEST_PARAMS = {"start_lat", "start_lon",
        "end_lat", "end_lon"};
    /* Define any static variables here. Do not define any instance variables of MapServer. */
    private static GraphDB g;
    private static LinkedList<Long> currentRoute; //current Route if found, contains list of IDs
                                                  // of nodes that are to be connected
    private static Set<String> autoFillNames;
    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {
        g = new GraphDB(OSM_DB_PATH);
    }

    public static void main(String[] args) {
        initialize();
        staticFileLocation("/page");
        /* Allow for all origin requests (since this is not an authenticated server, we do not
         * care about CSRF).  */
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });

        /* Define the raster endpoint for HTTP GET requests. I use anonymous functions to define
         * the request handlers. */
        get("/raster", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_RASTER_REQUEST_PARAMS);
            /* The png image is written to the ByteArrayOutputStream */
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            /* getMapRaster() does almost all the work for this API call */
            Map<String, Object> rasteredImgParams = getMapRaster(params, os);
            /* On an image query success, add the image data to the response */
            if (rasteredImgParams.containsKey("query_success")
                    && (Boolean) rasteredImgParams.get("query_success")) {
                String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
                rasteredImgParams.put("b64_encoded_image_data", encodedImage);
            }
            /* Encode response to Json */
            Gson gson = new Gson();
            return gson.toJson(rasteredImgParams);
        });

        /* Define the routing endpoint for HTTP GET requests. */
        get("/route", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_ROUTE_REQUEST_PARAMS);
            LinkedList<Long> route = findAndSetRoute(params);
            return !route.isEmpty();
        });

        /* Define the API endpoint for clearing the current route. */
        get("/clear_route", (req, res) -> {
            clearRoute();
            return true;
        });

        /* Define the API endpoint for search */
        get("/search", (req, res) -> {
            Set<String> reqParams = req.queryParams();
            String term = req.queryParams("term");
            Gson gson = new Gson();
            /* Search for actual location data. */
            if (reqParams.contains("full")) {
                List<Map<String, Object>> data = getLocations(term);
                return gson.toJson(data);
            } else {
                /* Search for prefix matching strings. */
                List<String> matches = getLocationsByPrefix(term);
                return gson.toJson(matches);
            }
        });

        /* Define map application redirect */
        get("/", (request, response) -> {
            response.redirect("/map.html", 301);
            return true;
        });
    }

    /**
     * Validate & return a parameter map of the required request parameters.
     * Requires that all input parameters are doubles.
     * @param req HTTP Request
     * @param requiredParams TestParams to validate
     * @return A populated map of input parameter to it's numerical value.
     */
    private static HashMap<String, Double> getRequestParams(
            spark.Request req, String[] requiredParams) {
        Set<String> reqParams = req.queryParams();
        HashMap<String, Double> params = new HashMap<>();
        for (String param : requiredParams) {
            if (!reqParams.contains(param)) {
                halt(HALT_RESPONSE, "Request failed - parameters missing.");
            } else {
                try {
                    params.put(param, Double.parseDouble(req.queryParams(param)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    halt(HALT_RESPONSE, "Incorrect parameters - provide numbers.");
                }
            }
        }
        return params;
    }


    /**
     * Handles raster API calls, queries for tiles and rasters the full image. <br>
     * <p>
     *     The rastered photo must have the following properties:
     *     <ul>
     *         <li>Has dimensions of at least w by h, where w and h are the user viewport width
     *         and height.</li>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *         <li>If a current route exists, lines of width ROUTE_STROKE_WIDTH_PX and of color
     *         ROUTE_STROKE_COLOR are drawn between all nodes on the route in the rastered photo.
     *         </li>
     *     </ul>
     *     Additional image about the raster is returned and is to be included in the Json response.
     * </p>
     * @param params Map of the HTTP GET request's query parameters - the query bounding box and
     *               the user viewport width and height.
     * @param os     An OutputStream that the resulting png image should be written to.
     * @return A map of parameters for the Json response as specified:
     * "raster_ul_lon" -> Double, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Double, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Double, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Double, the bounding lower right latitude of the rastered image <br>
     * "raster_width"  -> Double, the width of the rastered image <br>
     * "raster_height" -> Double, the height of the rastered image <br>
     * "depth"         -> Double, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image string. <br>
     * "query_success" -> Boolean, whether an image was successfully rastered. <br>
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public static Map<String, Object> getMapRaster(Map<String, Double> params, OutputStream os) {
        HashMap<String, Object> rasteredImageParams = new HashMap<>();
        Point rootULP = new Point(ROOT_ULLON, ROOT_ULLAT);
        Point rootLRP = new Point(ROOT_LRLON, ROOT_LRLAT);
        QuadTree quadTree = new QuadTree("root", rootULP, rootLRP);
        double queryBoxDPP = (params.get("lrlon") - params.get("ullon")) / params.get("w");
        Point queryBoxULP = new Point(params.get("ullon"), params.get("ullat"));
        Point queryBoxLRP = new Point(params.get("lrlon"), params.get("lrlat"));
        ArrayList<QTreeNode> tilesCollected =
                quadTree.intersectedTiles(queryBoxDPP, queryBoxULP, queryBoxLRP, TILE_SIZE);
        int tilesPerRow = 0;
        double startingLat;
        startingLat = tilesCollected.get(0).getUpperLeftPoint().getLatitude();
        for (QTreeNode tile : tilesCollected) {
            if (tile.getUpperLeftPoint().getLatitude() != startingLat) {
                break;
            }
            tilesPerRow += 1;
        }
        int tilesPerCol = tilesCollected.size() / tilesPerRow;
        int rasterWidth = tilesPerRow * TILE_SIZE;
        int rasterHeight = tilesPerCol * TILE_SIZE;
        int depth = tilesCollected.get(0).getDepth();
        try {
            BufferedImage im =
                    new BufferedImage(rasterWidth, rasterHeight, BufferedImage.TYPE_INT_RGB);
            Graphics graphics = im.getGraphics();
            int x = 0;
            int y = 0;
            for (QTreeNode tile : tilesCollected) {
                BufferedImage tileIm =
                        ImageIO.read(new File(IMG_ROOT + tile.getFileName() + ".png"));
                graphics.drawImage(tileIm, x, y, null);
                x += tileIm.getWidth();
                if (x >= im.getWidth()) {
                    x = 0;
                    y += tileIm.getHeight();
                }
            }
            rasteredImageParams.put("raster_ul_lon",
                tilesCollected.get(0).getUpperLeftPoint().getLongitude());
            rasteredImageParams.put("raster_ul_lat",
                tilesCollected.get(0).getUpperLeftPoint().getLatitude());
            rasteredImageParams.put("raster_lr_lon",
                tilesCollected.get(tilesCollected.size() - 1).getLowerRightPoint().getLongitude());
            rasteredImageParams.put("raster_lr_lat",
                tilesCollected.get(tilesCollected.size() - 1).getLowerRightPoint().getLatitude());
            rasteredImageParams.put("raster_width", rasterWidth);
            rasteredImageParams.put("raster_height", rasterHeight);
            rasteredImageParams.put("depth", depth);
            rasteredImageParams.put("query_success", true);
            double rasterULLon = (Double) rasteredImageParams.get("raster_ul_lon");
            double rasterULLat = (Double) rasteredImageParams.get("raster_ul_lat");
            double rasterLRLon = (Double) rasteredImageParams.get("raster_lr_lon");
            double rasterLRLat = (Double) rasteredImageParams.get("raster_lr_lat");
            double rasterDPPLon = (rasterLRLon - rasterULLon)
                    / (Integer) rasteredImageParams.get("raster_width");
            double rasterDPPLat = (rasterULLat - rasterLRLat)
                    / (Integer) rasteredImageParams.get("raster_height");
            Stroke stroke = new BasicStroke(MapServer.ROUTE_STROKE_WIDTH_PX,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            ((Graphics2D) graphics).setStroke(stroke);
            graphics.setColor(ROUTE_STROKE_COLOR);
            if (currentRoute != null) {
                for (int i = 0; i < currentRoute.size() - 1; i++) {
                    Node startNode = g.allNodes.get(currentRoute.get(i));
                    Node endNode = g.allNodes.get(currentRoute.get(i + 1));
                    int startX = (int) (Math.abs((startNode.lon - rasterULLon) / rasterDPPLon));
                    int startY = (int) (Math.abs((startNode.lat - rasterULLat) / rasterDPPLat));
                    int endX = (int) (Math.abs((endNode.lon - rasterULLon) / rasterDPPLon));
                    int endY = (int) (Math.abs((endNode.lat - rasterULLat) / rasterDPPLat));
                    graphics.drawLine(startX, startY, endX, endY);
                }
            }
            ImageIO.write(im, "png", os);
        } catch (IOException ioException) {
            System.out.println("Error! Could not load image.");
        }
        return rasteredImageParams;
    }

    /**
     * Searches for the shortest route satisfying the input request parameters, sets it to be the
     * current route, and returns a <code>LinkedList</code> of the route's node ids for testing
     * purposes. <br>
     * The route should start from the closest node to the start point and end at the closest node
     * to the endpoint. Distance is defined as the euclidean between two points (lon1, lat1) and
     * (lon2, lat2).
     * @param params from the API call described in REQUIRED_ROUTE_REQUEST_PARAMS
     * @return A LinkedList of node ids from the start of the route to the end.
     */
    public static LinkedList<Long> findAndSetRoute(Map<String, Double> params) {
        LinkedList<Long> shortestRoute = new LinkedList<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(10, new NodeComparator());
        HashSet<Long> checkedNodes = new HashSet<>();

        double startLonParam = params.get("start_lon");
        double startLatParam = params.get("start_lat");
        double endLonParam = params.get("end_lon");
        double endLatParam = params.get("end_lat");

        Node startNode = findClosestNode(startLonParam, startLatParam);
        Node endNode = findClosestNode(endLonParam, endLatParam);
        Node curNode;
        double nodePriority = 0;
        double newGValue = 0;

        double startNodeLon = startNode.lon;
        double startNodeLat = startNode.lat;
        double endNodeLon = endNode.lon;
        double endNodeLat = endNode.lat;

        startNode.priority = heuristicDist(startNodeLon, startNodeLat, endNodeLon, endNodeLat);
        startNode.gValue = 0;

        pq.add(startNode);
        curNode = pq.poll();
        checkedNodes.add(curNode.id);

        while (curNode.lon != endNodeLon && (curNode.lat != endNodeLat)) {
            for (Node connectedNode: curNode.con.getConnectedNodes()) {
                if (!checkedNodes.contains(connectedNode.id)) {
                    newGValue = curNode.gValue + heuristicDist(connectedNode.lon, connectedNode.lat,
                                                                curNode.lon, curNode.lat);
                    nodePriority = newGValue + heuristicDist(connectedNode.lon, connectedNode.lat,
                            endNodeLon, endNodeLat);
                    if (pq.contains(connectedNode) && newGValue < curNode.gValue) {
                        pq.remove(connectedNode);
                    } else {
                        Node newNode = new Node(connectedNode.id, connectedNode.lon,
                                connectedNode.lat, connectedNode.con);
                        newNode.gValue = newGValue;
                        newNode.prev = curNode;
                        newNode.priority = nodePriority;
                        pq.add(newNode);
                    }
                }
            }
            checkedNodes.add(curNode.id);
            curNode = pq.poll();
        }
        Stack<Long> ids = new Stack<>();
        Node nodePointer = curNode;
        while (nodePointer != null) {
            ids.push(nodePointer.id);
            nodePointer = nodePointer.prev;
        }
        while (!ids.empty()) {
            shortestRoute.add(ids.pop());
        }
        currentRoute = shortestRoute;
        return shortestRoute;
    }

    private static Node findClosestNode(double lon, double lat) {
        double closestDist = Integer.MAX_VALUE;
        Node closestNode = null;
        for (Node node: g.allNodes.values()) {
            double curDist = heuristicDist(node.lon, node.lat, lon, lat);
            if (curDist < closestDist) {
                closestNode = node;
                closestDist = curDist;
            }
        }
        return closestNode;
    }

    private static double heuristicDist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    private static class NodeComparator implements Comparator<Node> {
        @Override
        public int compare(Node node1, Node node2) {
            if (node1.priority > node2.priority) {
                return 1;
            } else if (node1.priority < node2.priority) {
                return -1;
            }
            return 0;
        }
    }
    /**
     * Clear the current found route, if it exists.
     */
    public static void clearRoute() {
        if (currentRoute != null) {
            currentRoute = null;
        }
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with or without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public static List<String> getLocationsByPrefix(String prefix) {
        Trie trie = new Trie();
        HashSet<String> checkedNames = new HashSet<>();
        for (String k: g.nameToNodes.keySet()) {
            if (!checkedNames.contains(k)) {
                trie.insert(k);
                checkedNames.add(k);
            }
        }
        List<String> locationNames = trie.autocomplete(prefix, g.nameToNodes);
        return locationNames;
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public static List<Map<String, Object>> getLocations(String locationName) {
//        LinkedList<Node> nodesWithSameName = g.nameToNodes.get(locationName);
//        LinkedList<Map<String, Object>> result = new LinkedList<>();
//        for (Node node: nodesWithSameName) {
//            HashMap<String, Object> curNodeMap = new HashMap<>();
//            curNodeMap.put("name", locationName);
//            curNodeMap.put("lon", node.lon);
//            curNodeMap.put("id", node.id);
//            curNodeMap.put("lat", node.lat);
//            result.add(curNodeMap);
//        }
        return new LinkedList<>();
    }
}
