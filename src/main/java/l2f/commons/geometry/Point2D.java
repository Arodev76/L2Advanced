package l2f.commons.geometry;

public class Point2D implements Cloneable
{
    public static final Point2D[] EMPTY_ARRAY;
    public int x;
    public int y;
    
    public Point2D() {
    }
    
    public Point2D(final int x, final int y) {
        this.x = x;
        this.y = y;
    }
 	@Override   
    public Point2D clone() {
        return new Point2D(this.x, this.y);
    }
    
    @Override
    public boolean equals(final Object o) {
        return o == this || (o != null && o.getClass() == this.getClass() && this.equals((Point2D)o));
    }
    
    @Override
    public int hashCode() {
        int hash = this.x;
        hash = 43 * hash + this.y;
        return hash;
    }
    
    public boolean equals(final Point2D p) {
        return this.equals(p.x, p.y);
    }
    
    public boolean equals(final int x, final int y) {
        return this.x == x && this.y == y;
    }
    
    public int getX() {
        return this.x;
    }
    
    public int getY() {
        return this.y;
    }
    
    @Override
    public String toString() {
        return "[x: " + this.x + " y: " + this.y + "]";
    }
    
    static {
        EMPTY_ARRAY = new Point2D[0];
    }
}
