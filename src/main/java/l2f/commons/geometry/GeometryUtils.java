package l2f.commons.geometry;

public class GeometryUtils
{
    public static boolean checkIfLinesIntersects(final Point2D a, final Point2D b, final Point2D c, final Point2D d) {
        return checkIfLinesIntersects(a, b, c, d, null);
    }
    
    public static boolean checkIfLinesIntersects(final Point2D a, final Point2D b, final Point2D c, final Point2D d, final Point2D r) {
        if ((a.x == b.x && a.y == b.y) || (c.x == d.x && c.y == d.y)) {
            return false;
        }
        final double Bx = b.x - a.x;
        final double By = b.y - a.y;
        double Cx = c.x - a.x;
        double Cy = c.y - a.y;
        double Dx = d.x - a.x;
        double Dy = d.y - a.y;
        final double distAB = Math.sqrt(Bx * Bx + By * By);
        final double theCos = Bx / distAB;
        final double theSin = By / distAB;
        double newX = Cx * theCos + Cy * theSin;
        Cy = (int)(Cy * theCos - Cx * theSin);
        Cx = newX;
        newX = Dx * theCos + Dy * theSin;
        Dy = (int)(Dy * theCos - Dx * theSin);
        Dx = newX;
        if (Cy == Dy) {
            return false;
        }
        final double ABpos = Dx + (Cx - Dx) * Dy / (Dy - Cy);
        if (r != null) {
            r.x = (int)(a.x + ABpos * theCos);
            r.y = (int)(a.y + ABpos * theSin);
        }
        return true;
    }
    
    public static boolean checkIfLineSegementsIntersects(final Point2D a, final Point2D b, final Point2D c, final Point2D d) {
        return checkIfLineSegementsIntersects(a, b, c, d, null);
    }
    
    public static boolean checkIfLineSegementsIntersects(final Point2D a, final Point2D b, final Point2D c, final Point2D d, final Point2D r) {
        if ((a.x == b.x && a.y == b.y) || (c.x == d.x && c.y == d.y)) {
            return false;
        }
        if ((a.x == c.x && a.y == c.y) || (b.x == c.x && b.y == c.y) || (a.x == d.x && a.y == d.y) || (b.x == d.x && b.y == d.y)) {
            return false;
        }
        final double Bx = b.x - a.x;
        final double By = b.y - a.y;
        double Cx = c.x - a.x;
        double Cy = c.y - a.y;
        double Dx = d.x - a.x;
        double Dy = d.y - a.y;
        final double distAB = Math.sqrt(Bx * Bx + By * By);
        final double theCos = Bx / distAB;
        final double theSin = By / distAB;
        double newX = Cx * theCos + Cy * theSin;
        Cy = (int)(Cy * theCos - Cx * theSin);
        Cx = newX;
        newX = Dx * theCos + Dy * theSin;
        Dy = (int)(Dy * theCos - Dx * theSin);
        Dx = newX;
        if ((Cy < 0.0 && Dy < 0.0) || (Cy >= 0.0 && Dy >= 0.0)) {
            return false;
        }
        final double ABpos = Dx + (Cx - Dx) * Dy / (Dy - Cy);
        if (ABpos < 0.0 || ABpos > distAB) {
            return false;
        }
        if (r != null) {
            r.x = (int)(a.x + ABpos * theCos);
            r.y = (int)(a.y + ABpos * theSin);
        }
        return true;
    }
}
