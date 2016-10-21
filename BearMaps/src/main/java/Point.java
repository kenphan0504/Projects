/**
 * A point with x and y values represented by the longitude and latitude of the Map.
 * Mainly as an instance to represent the UPPER-LEFT POINT and LOWER-RIGHT POINT of a tile
 */
public class Point implements Comparable<Point> {
    private double lon;
    private double lat;

    public Point(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public double getLongitude() {
        return lon;
    }

    public double getLatitude() {
        return lat;
    }

    @Override
    public int compareTo(Point otherP) {
        if (this == otherP) {
            return 0;
        }
        if (lon == otherP.getLongitude() && lat == otherP.getLatitude()) {
            return 0;
        }
        if (lat == otherP.getLatitude()) {
            if (lon < otherP.getLongitude()) {
                return -1;
            }
            return 1;
        }
        if (lon == otherP.getLongitude()) {
            if (lat > otherP.getLatitude()) {
                return -1;
            }
            return 1;
        }
        if (lat > otherP.getLatitude()) {
            return -1;
        }
        return 1;
    }
}
