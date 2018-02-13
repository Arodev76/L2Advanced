package l2f.commons.geometry;

public class Circle extends AbstractShape
{
    protected final Point2D c;
    protected final int r;
    
    public Circle(final Point2D center, final int radius) {
        this.c = center;
        this.r = radius;
        this.min.x = this.c.x - this.r;
        this.max.x = this.c.x + this.r;
        this.min.y = this.c.y - this.r;
        this.max.y = this.c.y + this.r;
    }
    
    public Circle(final int x, final int y, final int radius) {
        this(new Point2D(x, y), radius);
    }
    
    @Override
    public Circle setZmax(final int z) {
        this.max.z = z;
        return this;
    }
    
    @Override
    public Circle setZmin(final int z) {
        this.min.z = z;
        return this;
    }
    
    @Override
    public boolean isInside(final int x, final int y) {
        return (int)Math.pow(x - this.c.x, 2.0) + (int)Math.pow(y - this.c.y, 2.0) <= (int)Math.pow(this.r, 2.0);
    }
    
    @Override
    public boolean isOnPerimeter(final int x, final int y) {
        return Math.abs((int)Math.pow(x - this.c.x, 2.0) + (int)Math.pow(y - this.c.y, 2.0) - (int)Math.pow(this.r, 2.0)) < 48;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(this.c).append("{ radius: ").append(this.r).append("}");
        sb.append("]");
        return sb.toString();
 	}

	@Override
	public Point2D[] getBoundaries()
	{
		return new Point2D[]{c};
	}
}
