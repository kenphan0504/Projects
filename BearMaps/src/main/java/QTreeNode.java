/**
 * QTreeNode represents a node in a QuadTree
 * Each node contains the name of the file, two points represent top-left and bottom-right,
 * four children, and its depth in a tree;
 */
public class QTreeNode {
    private String fileName;
    private Point upperLeftPoint;
    private Point lowerRightPoint;
    private QTreeNode nw; // upper-left child
    private QTreeNode ne; // upper-right child
    private QTreeNode sw; // lower-left child
    private QTreeNode se; // lower-rigt child
    private int depth;

    public QTreeNode(String fileName, Point upperLeftPoint, Point lowerRightPoint,
                     QTreeNode nw, QTreeNode ne, QTreeNode sw, QTreeNode se, int depth) {
        this.fileName = fileName;
        this.upperLeftPoint = upperLeftPoint;
        this.lowerRightPoint = lowerRightPoint;
        this.nw = nw;
        this.ne = ne;
        this.sw = sw;
        this.se = se;
        this.depth = depth;
    }

    public String getFileName() {
        return fileName;
    }

    public Point getUpperLeftPoint() {
        return upperLeftPoint;
    }

    public Point getLowerRightPoint() {
        return lowerRightPoint;
    }

    public QTreeNode getNE() {
        return ne;
    }

    public QTreeNode getNW() {
        return nw;
    }

    public QTreeNode getSE() {
        return se;
    }

    public QTreeNode getSW() {
        return sw;
    }

    public int getDepth() {
        return depth;
    }
}
