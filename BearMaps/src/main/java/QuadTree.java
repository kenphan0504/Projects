import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Construct a QuadTree which each node correspond to an appropriate image tile.
 */
public class QuadTree {
    private int startingDepth = 0;
    private QTreeNode root;


    public QuadTree(String fileName, Point upperLeftPoint, Point lowerRightPoint) {
        root = buildTree(fileName, upperLeftPoint, lowerRightPoint, startingDepth);
    }

    public QTreeNode buildTree(String fileName, Point upperLeftPoint,
                                Point lowerRightPoint, int depth) {
        if (depth > 7) {
            return null;
        }
        String nwName, neName, swName, seName;
        if (depth == 0) {
            nwName = "1";
            neName = "2";
            swName = "3";
            seName = "4";
        } else {
            nwName = fileName + "1";
            neName = fileName + "2";
            swName = fileName + "3";
            seName = fileName + "4";
        }
        double midLongitude = (lowerRightPoint.getLongitude() + upperLeftPoint.getLongitude()) / 2;
        double midLatitude = (lowerRightPoint.getLatitude() + upperLeftPoint.getLatitude()) / 2;

        QTreeNode nw = buildTree(nwName, upperLeftPoint, new Point(midLongitude, midLatitude),
                                depth + 1);
        QTreeNode ne = buildTree(neName, new Point(midLongitude, upperLeftPoint.getLatitude()),
                        new Point(lowerRightPoint.getLongitude(), midLatitude), depth + 1);
        QTreeNode sw = buildTree(swName, new Point(upperLeftPoint.getLongitude(), midLatitude),
                        new Point(midLongitude, lowerRightPoint.getLatitude()), depth + 1);
        QTreeNode se = buildTree(seName, new Point(midLongitude, midLatitude),
                        lowerRightPoint, depth + 1);

        return new QTreeNode(fileName, upperLeftPoint, lowerRightPoint, nw, ne, sw, se, depth);
    }

    public ArrayList<QTreeNode> intersectedTiles(double queryBoxDPP, Point qBoxULP,
                                                 Point qBoxLRP, double tileSize) {
        ArrayList<QTreeNode> tiles = new ArrayList<QTreeNode>();
        intersectionQuery(tiles, queryBoxDPP, qBoxULP, qBoxLRP, root, tileSize);
        Collections.sort(tiles, new TileComparator());
        return tiles;
    }

    private void intersectionQuery(ArrayList<QTreeNode> tiles, double qBoxDPP,
                            Point qBoxULP, Point qBoxLRP, QTreeNode curNode, double tileSize) {
        if (curNode == null) {
            return;
        }
        if (!intersects(qBoxULP, qBoxLRP, curNode.getUpperLeftPoint(),
                curNode.getLowerRightPoint())) {
            return;
        }
        double tileDPP = (curNode.getLowerRightPoint().getLongitude()
                            - curNode.getUpperLeftPoint().getLongitude()) / (tileSize);
        if (tileDPP <= qBoxDPP || curNode.getDepth() == 7) {
            tiles.add(curNode);
            return;
        }
        intersectionQuery(tiles, qBoxDPP, qBoxULP, qBoxLRP, curNode.getNW(), tileSize);
        intersectionQuery(tiles, qBoxDPP, qBoxULP, qBoxLRP, curNode.getNE(), tileSize);
        intersectionQuery(tiles, qBoxDPP, qBoxULP, qBoxLRP, curNode.getSW(), tileSize);
        intersectionQuery(tiles, qBoxDPP, qBoxULP, qBoxLRP, curNode.getSE(), tileSize);
    }

    private boolean intersects(Point qBoxUpperLeft, Point qBoxLowerRight,
                               Point tileUpperLeft, Point tileLowerRight) {
        if ((qBoxUpperLeft.getLongitude() > tileLowerRight.getLongitude())
            || (tileUpperLeft.getLongitude() > qBoxLowerRight.getLongitude())) {
            return false;
        }
        if ((qBoxUpperLeft.getLatitude() < tileLowerRight.getLatitude())
            || (tileUpperLeft.getLatitude() < qBoxLowerRight.getLatitude())) {
            return false;
        }
        return true;
    }

    private class TileComparator implements Comparator<QTreeNode> {
        @Override
        public int compare(QTreeNode node1, QTreeNode node2) {
            if (node1.getUpperLeftPoint().compareTo(node2.getUpperLeftPoint()) < 0) {
                return -1;
            }
            if (node1.getUpperLeftPoint().compareTo(node2.getUpperLeftPoint()) > 0) {
                return 1;
            }
            return 0;
        }
    }
}
