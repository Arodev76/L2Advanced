package l2f.commons.geometry;

import l2f.commons.lang.ArrayUtils;

public class Polygon extends AbstractShape
{
    protected Point2D[] points;
    
    public Polygon() {
        this.points = Point2D.EMPTY_ARRAY;
    }
    
    public Polygon add(final int x, final int y) {
        this.add(new Point2D(x, y));
        return this;
    }
    
    public Polygon add(final Point2D p) {
        if (this.points.length == 0) {
            this.min.y = p.y;
            this.min.x = p.x;
            this.max.x = p.x;
            this.max.y = p.y;
        }
        else {
            this.min.y = Math.min(this.min.y, p.y);
            this.min.x = Math.min(this.min.x, p.x);
            this.max.x = Math.max(this.max.x, p.x);
            this.max.y = Math.max(this.max.y, p.y);
        }
        this.points = ArrayUtils.add(this.points, p);
        return this;
	}	

	@Override
	public Point2D[] getBoundaries()
	{
		return points;
	}

	@Override
    public Polygon setZmax(final int z) {
        this.max.z = z;
        return this;
    }
    
    @Override
    public Polygon setZmin(final int z) {
        this.min.z = z;
        return this;
    }
    
    @Override
    public boolean isInside(final int x, final int y) {
        if (x < this.min.x || x > this.max.x || y < this.min.y || y > this.max.y) {
            return false;
        }
        int hits = 0;
        final int npoints = this.points.length;
        Point2D last = this.points[npoints - 1];
        for (int i = 0; i < npoints; ++i) {
            final Point2D cur = this.points[i];
            Label_0314: {
                if (cur.y != last.y) {
                    int leftx;
                    if (cur.x < last.x) {
                        if (x >= last.x) {
                            break Label_0314;
                        }
                        leftx = cur.x;
                    }
                    else {
                        if (x >= cur.x) {
                            break Label_0314;
                        }
                        leftx = last.x;
                    }
                    double test1;
                    double test2;
                    if (cur.y < last.y) {
                        if (y < cur.y) {
                            break Label_0314;
                        }
                        if (y >= last.y) {
                            break Label_0314;
                        }
                        if (x < leftx) {
                            ++hits;
                            break Label_0314;
                        }
                        test1 = x - cur.x;
                        test2 = y - cur.y;
                    }
                    else {
                        if (y < last.y) {
                            break Label_0314;
                        }
                        if (y >= cur.y) {
                            break Label_0314;
                        }
                        if (x < leftx) {
                            ++hits;
                            break Label_0314;
                        }
                        test1 = x - last.x;
                        test2 = y - last.y;
                    }
                    if (test1 < test2 / (last.y - cur.y) * (last.x - cur.x)) {
                        ++hits;
                    }
                }
            }
            last = cur;
        }
        return (hits & 0x1) != 0x0;
    }
    
    @Override
    public boolean isOnPerimeter(final int x, final int y) {
        return this.isInside(x, y);
    }
    
    public boolean validate() {
        if (this.points.length < 3) {
            return false;
        }
        if (this.points.length > 3) {
            for (int i = 1; i < this.points.length; ++i) {
                final int ii = (i + 1 < this.points.length) ? (i + 1) : 0;
                for (int n = i; n < this.points.length; ++n) {
                    if (Math.abs(n - i) > 1) {
                        final int nn = (n + 1 < this.points.length) ? (n + 1) : 0;
                        if (GeometryUtils.checkIfLineSegementsIntersects(this.points[i], this.points[ii], this.points[n], this.points[nn])) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < this.points.length; ++i) {
            sb.append(this.points[i]);
            if (i < this.points.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
