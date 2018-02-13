package l2f.commons.geometry;

public class Rectangle extends AbstractShape
{
    public Rectangle(final int x1, final int y1, final int x2, final int y2) {
        this.min.x = Math.min(x1, x2);
        this.min.y = Math.min(y1, y2);
        this.max.x = Math.max(x1, x2);
        this.max.y = Math.max(y1, y2);
    }
    
    @Override
    public Rectangle setZmax(final int z) {
        this.max.z = z;
        return this;
    }
    
    @Override
    public Rectangle setZmin(final int z) {
        this.min.z = z;
        return this;
    }
    
    @Override
    public boolean isInside(final int x, final int y) {
        return x >= this.min.x && x <= this.max.x && y >= this.min.y && y <= this.max.y;
    }
    
    @Override
    public boolean isOnPerimeter(final int x, final int y) {
        return Math.abs(x - this.min.x) < 48 && Math.abs(x - this.max.x) < 48 && Math.abs(y - this.min.y) < 48 && Math.abs(y - this.max.y) < 48;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(this.min).append(", ").append(this.max);
        sb.append("]");
        return sb.toString();
	}

	@Override
	public Point2D[] getBoundaries()
	{
		Point2D[] points = new Point2D[4];
		points[0] = new Point2D(min.x, min.y); // Bottom-Left
		points[1] = new Point2D(max.x, min.y); // Bottom-Right
		points[2] = new Point2D(max.x, max.y); // Top-Right
		points[3] = new Point2D(min.x, max.y); // Top-Left
		return null;
	}
}
