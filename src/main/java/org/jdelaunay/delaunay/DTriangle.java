package org.jdelaunay.delaunay;

/**
 * This is the class representing a Triangle in the DelaunayTriangulation. A DTriangle
 * is made of three edges, each edge sharing a point with the other ones. Consequently,
 * a DTriangle can be identified thanks to its edges, or thanks to its associated DPoint
 * instances.
 *
 * @author Jean-Yves MARTIN, Erwan BOCHER, Adelin PIAU, Alexis GUEGANNO
 * @date 2009-01-12
 * @revision 2011-03-08
 * @version 2.2
 */

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.ListIterator;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DTriangle extends Element implements Comparable<DTriangle>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int PT_NB = 3;

	private static final int HASHBASE = 5;
	private static final int HASHMULT = 97;

	/**
	 * The array of edges that constitute this triangle
	 */
	private DEdge[] edges;
	//The coordinates of the center of the circle.
	private double xCenter, yCenter, zCenter;
	private double radius;
	
	private int indicator;

	private boolean seenForFlatRemoval;

	/**
	 * Initialize data structure This method is called by every constructor
	 */
	private void init() {
		this.edges = new DEdge[PT_NB];
		this.xCenter = 0;
		this.yCenter = 0;
		zCenter = 0;
		this.radius = -1;
		this.indicator = 0;
		seenForFlatRemoval = false;
	}

	/**
	 * Not intended to be used. Private.
	 */
	private DTriangle() {
	}

	/**
	 * Create a new triangle with edges Add the points from the edges
	 *
	 * @param e1
	 * @param e2
	 * @param e3
	 * @throws DelaunayError
	 */
	public DTriangle(DEdge e1, DEdge e2, DEdge e3) throws DelaunayError {
		super();
		init();

		//We check the integrity of the edges given to build this triangle
		boolean integrityE1E2 = (e1.isExtremity(e2.getStart()) && ! e3.isExtremity((e2.getStart())))
			|| (e1.isExtremity(e2.getEnd()) && !e3.isExtremity(e2.getEnd()));
		boolean integrityE1EptNb =  (e1.isExtremity(e3.getStart()) && ! e2.isExtremity((e3.getStart())))
			|| (e1.isExtremity(e3.getEnd()) && !e2.isExtremity(e3.getEnd()));
		boolean integrityEptNbE2= (e2.isExtremity(e3.getStart()) && ! e1.isExtremity((e3.getStart())))
			|| (e2.isExtremity(e3.getEnd()) && !e1.isExtremity(e3.getEnd()));

		if(integrityE1E2 && integrityE1EptNb && integrityEptNbE2){
			edges[0] = e1;
			edges[1] = e2;
			edges[2] = e3;

			connectEdges();
			recomputeCenter();
			radius = e1.getStartPoint().squareDistance2D(xCenter, yCenter);
		} else {
			throw new DelaunayError("Problem while generating the Triangle");
		}
	}

	/**
	 * Create a DTriangle from another triangle NB : it doesn't update edges
	 * connection
	 *
	 * @param aTriangle
	 */
	public DTriangle(DTriangle aTriangle) {
		super((Element)aTriangle);
		init();
		System.arraycopy(aTriangle.edges, 0, edges, 0, PT_NB);

		xCenter = aTriangle.xCenter;
		yCenter = aTriangle.yCenter;
		radius = aTriangle.radius;
	}

	/**
	 * Get a list of points containing the DPoint that define this triangle.
	 * @return
	 */
	public final List<DPoint> getPoints(){
		List<DPoint> ret = new ArrayList<DPoint>();
		ret.add(edges[0].getStartPoint());
		ret.add(edges[0].getEndPoint());
		ret.add(getPoint(2));
		return ret;
	}

	/**
	 * Get the ith point
	 * i must be equal to 0, 1 or 2.
	 *
	 * @param i
	 * @return aPoint
	 */
	public final DPoint getPoint(int i) {
		DPoint p;
		if (i==0) {
			p = edges[0].getStartPoint();
		} else if (i==1) {
			p = edges[0].getEndPoint();
		} else {
			p = edges[1].getStartPoint();
			if ((p.equals(edges[0].getStartPoint())) || (p.equals(edges[0].getEndPoint()))) {
				p = edges[1].getEndPoint();
			}
		}
		return p;
	}

	/**
	 * Get the ith edge
	 * i must be equal to 0, 1 or 2.
	 *
	 * @param i
	 * @return anEdge
	 */
	public final DEdge getEdge(int i) {
		if ((0<=i) && (i<=2)) {
			return edges[i];
		}
		else {
			return null;
		}
	}

	/**
	 * Return the edges that form this triangle in an array.
	 * @return
	 */
	public final DEdge[] getEdges(){
		DEdge[] ret = new DEdge[PT_NB];
		System.arraycopy(this.edges, 0, ret, 0, PT_NB);
		return ret;
	}

	/**
	 * Set the ith edge
	 *
	 * @param i
	 * @param anEdge
	 */
	public final void setEdge(int i, DEdge anEdge) {
		if ((0<=i) && (i<=2)) {
			edges[i] = anEdge;
		}
	}

	/**
	 * Get the radius of the CircumCircle
	 *
	 * @return radius
	 */
	public final double getRadius() {
		return Math.sqrt(radius);
	}

	/**
	 * Get the center of the CircumCircle
	 *
	 * @return
	 */
	public final Coordinate getCircumCenter() {
		return new Coordinate(this.xCenter, this.yCenter, zCenter);
	}

	/**
	 * check if this triangle has already been encountered (and marked if flat)
	 * during the flat removal operation.
	 * @return
	 */
	public final boolean isSeenForFlatRemoval() {
		return seenForFlatRemoval;
	}

	/**
	 * Set the value of the seenForFlatRemoval attribute, that is used to
	 * process flat triangles only once during the flat tiangles removal operation.
	 * @param seenForFlatRemoval
	 */
	final void setSeenForFlatRemoval(boolean seenForFlatRemoval) {
		this.seenForFlatRemoval = seenForFlatRemoval;
	}
	
	@Override
	public final int getIndicator() {
		return indicator;
	}
	
	@Override
	public final int setIndicator(int indicator) {
		this.indicator=indicator;
		return 0;
	}
	
	@Override
	public final void removeIndicator() {
		indicator = 0;
	}
	
	
	/**
	 * get the value of a specific bit
	 * @param byteNumber
	 * @return marked
	 */
	private boolean testBit(int byteNumber) {
		return ((this.indicator & (1 << byteNumber)) != 0);
	}

	/**
	 * set the value of a specific bit
	 * @param byteNumber
	 * @param value
	 */
	private void setBit(int byteNumber, boolean value) {
		int test = (1 << byteNumber);
		if (value) {
			this.indicator = (this.indicator | test);
		}
		else {
			this.indicator = (this.indicator | test) - test;
		}
	}
	
	/**
	 * get the mark of the edge
	 * @param byteNumber
	 * @return marked
	 */
	public final boolean isMarked(int byteNumber) {
		return testBit(3+byteNumber);
	}

	/**
	 * set the mark of the edge
	 * @param byteNumber
	 * @param marked
	 */
	public final void setMarked(int byteNumber, boolean marked) {
		setBit(3+byteNumber, marked);
	}

	/**
	 * Get the minimal bounding box that encloses this triangle.
	 * @return
	 */
	@Override
	public final BoundaryBox getBoundingBox() {
		BoundaryBox aBox = new BoundaryBox();

		DPoint p1,p2,pptNb;
		p1 = edges[0].getStartPoint();
		p2 = edges[0].getEndPoint();
		pptNb = edges[1].getStartPoint();
		if ((pptNb.equals(p1))||(pptNb.equals(p2))) {
			pptNb = edges[1].getEndPoint();
		}
		aBox.alterBox( p1);
		aBox.alterBox( p2);
		aBox.alterBox( pptNb);
		
		return aBox;
	}

	/**
	 * Get the leftmost point of this triangle.
	 * @return
	 */
	public final DPoint getLeftMost(){
		DPoint p1 = edges[0].getPointLeft();
		DPoint p2 = edges[1].getPointLeft();
		return (p1.compareTo(p2) < 1 ? p1 : p2);
	}

	/**
	 * Get the last edge that form, with e1 and e2, this triangle. If e1 or e2
	 * do not belong to this triangle, return null.
	 * @param e1
	 * @param e2
	 * @return
	 */
	public final DEdge getLastEdge(DEdge e1, DEdge e2){
		if(e1.equals(edges[0])){
			if(e2.equals(edges[1])){
				return edges[2];
			} else if(e2.equals(edges[2])){
				return edges[1];
			}
		} else if(e1.equals(edges[1])){
			if(e2.equals(edges[0])){
				return edges[2];
			} else if(e2.equals(edges[2])){
				return edges[0];
			}
		}else if(e1.equals(edges[2])){
			if(e2.equals(edges[0])){
				return edges[1];
			} else if(e2.equals(edges[1])){
				return edges[0];
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.jdelaunay.delaunay.Element#contains(org.jdelaunay.delaunay.DPoint)
	 */
	@Override
	public final boolean contains(DPoint aPoint) {
		return isInside(aPoint);
	}
	
	@Override
	public final boolean contains(Coordinate c) throws DelaunayError {
		return isInside(new DPoint(c));
	}

	/**
	 * Recompute the center of the circle that joins the ptNb points : the CircumCenter
	 * @throws DelaunayError
	 */
	protected final void recomputeCenter() throws DelaunayError {
		DPoint p1,p2,pptNb;
		p1 = edges[0].getStartPoint();
		p2 = edges[0].getEndPoint();
		pptNb = edges[1].getStartPoint();
		if ((pptNb.equals(p1))||(pptNb.equals(p2))) {
			pptNb = edges[1].getEndPoint();
		}

		double p1Sq = p1.getX() * p1.getX() + p1.getY() * p1.getY();
		double p2Sq = p2.getX() * p2.getX() + p2.getY() * p2.getY();
		double pptNbSq = pptNb.getX() * pptNb.getX() + pptNb.getY() * pptNb.getY();

		double ux = p2.getX() - p1.getX();
		double uy = p2.getY() - p1.getY();
		double vx = pptNb.getX() - p1.getX();
		double vy = pptNb.getY() - p1.getY();

		double cp = ux * vy - uy * vx;
		double cx, cy;

		if (cp != 0) {
			cx = (p1Sq * (p2.getY() - pptNb.getY()) + p2Sq * (pptNb.getY() - p1.getY()) + pptNbSq
					* (p1.getY() - p2.getY()))
					/ (2.0 * cp);
			cy = (p1Sq * (pptNb.getX() - p2.getX()) + p2Sq * (p1.getX() - pptNb.getX()) + pptNbSq
					* (p2.getX() - p1.getX()))
					/ (2.0 * cp);

			xCenter = cx;
			yCenter = cy;
			zCenter = interpolateZ(new DPoint(cx, cy, 0));

			radius = p1.squareDistance2D(xCenter, yCenter);
		} else {
			xCenter = 0.0;
			yCenter = 0.0;
			radius = -1;
		}

	}

	/**
	 * Connect triangle edges to build topology
	 */
	private void connectEdges() {
		// we connect edges to the triangle
		for (int i=0; i<PT_NB; i++) {
			// Start point should be start
			DPoint aPoint = this.getAlterPoint(edges[i]);
			if (edges[i].isLeft(aPoint)) {
				if (edges[i].getLeft() == null) {
					edges[i].setLeft(this);
				}
				else {
					edges[i].setRight( this );
				}
			}
			else {
				if (edges[i].getRight() == null) {
					edges[i].setRight(this);
				}
				else {
					edges[i].setLeft( this );
				}
			}
		}
	}

	/**
	 * Check if the point is in or on the Circle 0 = outside 1 = inside 2 = on
	 * the circle
	 *
	 * @param aPoint
	 * @return position 0 = outside 1 = inside 2 = on the circle
	 */
	public final int inCircle(DPoint aPoint) {
		// default is outside the circle
		int returnedValue = 0;

		// double distance = squareDistance(Center, aPoint);
		double ux = aPoint.getX() - xCenter;
		double uy = aPoint.getY() - yCenter;
		double distance = ux * ux + uy * uy;
		if (distance < radius - Tools.EPSILON2) {
			returnedValue = 1;
		}
		else if (distance < radius + Tools.EPSILON2) {
			returnedValue = 2;
		}

		return returnedValue;
	}

	/**
	 * Check if the point is inside the triangle
	 *
	 * @param aPoint
	 * @return isInside
	 */
	public final boolean isInside(DPoint aPoint) {
		boolean isInside = true;

		int k = 0;
		while ((k < PT_NB) && (isInside)) {
			DEdge theEdge = edges[k];

			if (theEdge.getLeft() == this) {
				if (theEdge.isRight(aPoint)) {
					isInside = false;
				}
			} else {
				if (theEdge.isLeft(aPoint)) {
					isInside = false;
				}
			}
			k++;
		}

		return isInside;
	}

	/**
	 * Get Z value of a specific point in the triangle
	 *
	 * @param aPoint
	 * @return ZValue
	 */
	public final double interpolateZ(DPoint aPoint) {
		double zValue = 0;

		DPoint p1,p2,p3;
		p1 = edges[0].getStartPoint();
		p2 = edges[0].getEndPoint();
		p3 = edges[1].getStartPoint();
		if ((p3.equals(p1))||(p3.equals(p2))) {
			p3 = edges[1].getEndPoint();
		}

		double ux = p2.getX() - p1.getX();
		double uy = p2.getY() - p1.getY();
		double uz = p2.getZ() - p1.getZ();
		double vx = p3.getX() - p1.getX();
		double vy = p3.getY() - p1.getY();
		double vz = p3.getZ() - p1.getZ();

		double a = uy * vz - uz * vy;
		double b = uz * vx - ux * vz;
		double c = ux * vy - uy * vx;
		double d = -a * p1.getX() - b * p1.getY() - c * p1.getZ();

		if (Math.abs(c) > Tools.EPSILON) {
			// Non vertical triangle
			zValue = (-a * aPoint.getX() - b * aPoint.getY() - d) / c;
		}

		return zValue;
	}

	/**
	 * Get Z value of a specific point in the triangle
	 * Take into account triangles connected to the edge
	 *
	 * @param aPoint
	 * @return ZValue
	 */
	public final double softInterpolateZ(DPoint aPoint) {
		double weight = (double) PT_NB;
		double zValue = interpolateZ(aPoint) * weight;
		
		// Process connected edges
		for (int i=0; i<PT_NB; i++) {
			DEdge anEdge = edges[i];
			DTriangle aTriangle = null;
			if (anEdge != null) {
				if (anEdge.getLeft() == this) {
					aTriangle = anEdge.getRight();
				} else {
					aTriangle = anEdge.getLeft();
				}
			}
			if (aTriangle != null) {
				weight += 1.0;
				zValue += aTriangle.interpolateZ(aPoint);
			}
		}
		// Define new Z value
		zValue /= weight;

		return zValue;
	}

	/**
	 * Compute triangle area
	 *
	 * @return area
	 */
	public final double computeArea() {
		DPoint p1,p2,pptNb;
		p1 = edges[0].getStartPoint();
		p2 = edges[0].getEndPoint();
		pptNb = edges[1].getStartPoint();
		if ((pptNb.equals(p1))||(pptNb.equals(p2))) {
			pptNb = edges[1].getEndPoint();
		}

		double area = ((pptNb.getX()-p1.getX())*(p2.getY()-p1.getY())-(p2.getX()-p1.getX())*(pptNb.getY()-p1.getY()))/2;

		return area<0 ? -area : area ;
	}

	/**
	 * check if one of the triangle's angle is less than minimum
	 *
	 * @param tolarance
	 * @return minAngle
	 */
	protected final int badAngle(double tolarance) {
		double minAngle = 400;
		int returnedValue = -1;
		for (int k = 0; k < PT_NB; k++) {
			double angle = getAngle(k);
			if (angle < minAngle) {
				minAngle = angle;
				if (minAngle < tolarance) {
					returnedValue = k;
				}
			}
		}
		return returnedValue;
	}

	/**
	 * check if one of the triangle's angle is less than minimum
	 *
	 * @return maxAngle
	 */
	protected final double getMaxAngle() {
		double maxAngle = 0;
		for (int k = 0; k < PT_NB; k++) {
			double angle = getAngle(k);
			if (angle > maxAngle) {
				maxAngle = angle;
			}
		}
		return maxAngle;
	}

	/**
	 * Check if triangle topology is correct or not
	 *
	 * @return correct
	 */
	public final boolean checkTopology() {
		boolean correct = true;
		int i, j, k;

		// check if we do not have an edge twice
		i = 0;
		while ((i < PT_NB) && (correct)) {
			int foundEdge = 0;
			for (j = 0; j < PT_NB; j++) {
				if (edges[i] == edges[j]) {
					foundEdge++;
				}
			}
			if (foundEdge != 1) {
				correct = false;
			}
			i++;
		}

		// check if each edge is connected to the triangle
		i = 0;
		while ((i < PT_NB) && (correct)) {
			int foundEdge = 0;
			if (edges[i].getLeft() == this) {
				foundEdge++;
			}
			if (edges[i].getRight() == this) {
				foundEdge++;
			}

			if (foundEdge != 1) {
				correct = false;
			}
			i++;
		}

		// Check if each point in the edges is referenced 2 times
		DPoint aPoint;
		i = 0;
		while ((i < PT_NB) && (correct)) {
			DEdge anEdge = edges[i];
			for (j = 0; j < 2; j++) {
				if (j == 0) {
					aPoint = anEdge.getStartPoint();
				}
				else {
					aPoint = anEdge.getEndPoint();
				}
				int foundPoint = 0;
				for (k = 0; k < PT_NB; k++) {
					if (edges[k].getStartPoint() == aPoint) {
						foundPoint++;
					}
					if (edges[k].getEndPoint() == aPoint) {
						foundPoint++;
					}
				}
				if (foundPoint != 2) {
					correct = false;
				}
			}
			i++;
		}

		return correct;
	}

	/**
	 * Check if this triangle and the list of points given in argument respect
	 * the Delaunay criterium : This method returns true if none of the points of pts
	 * lie inside the circumcircle of this triangle.
	 *
	 * @param pts
	 * @return
	 */
	public final boolean checkDelaunay(List<DPoint> pts) {
		boolean correct = true;
		ListIterator<DPoint> iterPoint = pts.listIterator();
		while (iterPoint.hasNext() && correct) {
			DPoint aPoint = iterPoint.next();
			if (inCircle(aPoint) == 1 && !aPoint.equals(getPoint(0))
                                        && !aPoint.equals(getPoint(1))
                                        && !aPoint.equals(getPoint(2))) {
                                // Check if it is one of the points of the triangle
                                correct = false;
			}
		}

		return correct;
	}

	/**
	 * Check if the triangle is flat or not.
	 * Check if the 3 points have the same Z.
	 *
	 * @return isFlat
	 */
	public final boolean isFlatSlope() {
		boolean isFlat = true;
		int i = 0;
		while ((i < PT_NB) && (isFlat)) {
			if (!edges[i].isFlatSlope()) {
				isFlat = false;
			}
			else {
				i++;
			}
		}
		return isFlat;
	}

	/**
	 * Get the point of the triangle that is not belong to the edge
	 *
	 * @param ed
	 * @return alterPoint
	 */
	public final DPoint getAlterPoint(DEdge ed) {
		DPoint start = ed.getStartPoint();
		DPoint end = ed.getEndPoint();
		return getAlterPoint(start, end);
	}

	/**
	 * Return the edge that is not linked to pt, or null if pt is not a
	 * point of this triangle.
	 * @param pt
	 */
	public final DEdge getOppositeEdge(DPoint pt){
		if(!contains(pt)){
			return null;
		}
		if(!edges[0].contains(pt)){
			return edges[0];
		}
		else if(!edges[1].contains(pt)){
			return edges[1];
		}
		else {
			return edges[2];
		} 

	}

	/**
	 * Get the point of the triangle that is not one of the 2 points given
	 * in argument.
	 * If one of these argument is not part of this triangle, this method will
	 * return null.
	 *
	 * @param p1
	 * @param p2
	 * @return alterPoint
	 */
	protected final DPoint getAlterPoint(DPoint p1, DPoint p2) {
		DPoint t1 = getPoint(0);
		DPoint t2 = getPoint(1);
		DPoint t3 = getPoint(2);
		if(p1.equals(t1)){
			if(p2.equals(t2)){
				return t3;
			} else if(p2.equals(t3)){
				return t2;
			}
		} else if(p1.equals(t2)) {
			if(p2.equals(t1)){
				return t3;
			} else if (p2.equals(t3)) {
				return t1;
			}
		} else if(p1.equals(t3)) {
			if(p2.equals(t1)){
				return t2;
			} else if (p2.equals(t2)) {
				return t1;
			}
		}
		return null;
	}

	/**
	 * Get the edge of the triangle that includes the two point
	 *
	 * @param p1
	 * @param p2
	 * @return alterEdge
	 */
	protected final DEdge getEdgeFromPoints(DPoint p1, DPoint p2) {
		DEdge alterEdge = null;
		DPoint test1, test2;
		int i = 0;
		while ((i < PT_NB) && (alterEdge == null)) {
			DEdge testEdge = edges[i];
			test1 = testEdge.getStartPoint();
			test2 = testEdge.getEndPoint();
			if ((test1.equals(p1)) && (test2.equals(p2))) {
				alterEdge = testEdge;
			}
			else if ((test1.equals(p2)) && (test2.equals(p1))) {
				alterEdge = testEdge;
			}
			else {
				i++;
			}
		}

		return alterEdge;
	}

	/**
	 * Check if the point belongs to the triangle
	 *
	 * @param aPoint
	 * @return belongs
	 */
	public final boolean belongsTo(DPoint aPoint) {
		boolean belongs = false;
		DEdge anEdge = this.getEdge(0);
		if (anEdge.getStartPoint().equals(aPoint)) {
			belongs = true;
		}
		else if (anEdge.getEndPoint().equals(aPoint)) {
			belongs = true;
		}
		else {
			anEdge = this.getEdge(1);
			if (anEdge.getStartPoint().equals(aPoint)) {
				belongs = true;
			}
			else if (anEdge.getEndPoint().equals(aPoint)) {
				belongs = true;
			}
		}

		return belongs;
	}
	
	/**
	 * Get the barycenter of the triangle
	 *
	 * @return isFlat
	 * @throws DelaunayError 
	 */
	public final DPoint getBarycenter() throws DelaunayError {
		double x = 0, y = 0, z = 0;
		DPoint aPoint;
		for (int i = 0; i < PT_NB; i++) {
			aPoint = getPoint(i);

			x += aPoint.getX();
			y += aPoint.getY();
			z += aPoint.getZ();
		}
		x /= (double) PT_NB;
		y /= (double) PT_NB;
		z /= (double) PT_NB;
		
		return new DPoint(x, y, z);
	}

	/**
	 * Gives a rperesentation of this triangle as a String.
	 * @return Triangle "+gid+": ["+getPoint(0).toString()+", "+getPoint(1).toString()+", "+getPoint(2).toString()+"]
	 */
	@Override
	public final String toString() {
		return "Triangle "+getGID()+": ["+getPoint(0).toString()+", "+getPoint(1).toString()+", "+getPoint(2).toString()+"]";
	}

	/**
	 * Set the edge color Must be used only when using package drawing
	 *
	 * @param g
	 */
	private void setColor(Graphics g) {
		if(property>0) {
			g.setColor(new Color(property));
		}
		else if (isFlatSlope()) {
			g.setColor(Color.green);
		}
		else {
			g.setColor(Color.yellow);
		}
	}

	/**
	 * Display the triangle in a JPanel Must be used only when using package
	 * drawing
	 * 
	 * @param g
	 * @param decalageX
	 * @param decalageY
	 * @param minX
	 * @param minY
	 * @param scaleX
	 * @param scaleY
	 */
	protected final void displayObject(Graphics g, int decalageX, int decalageY,
			double minX, double minY, double scaleX, double scaleY) {
		int[] xPoints, yPoints;
		xPoints = new int[PT_NB];
		yPoints = new int[PT_NB];
		DPoint p1, p2, pptNb;
		p1 = getPoint(0);
		p2 = getPoint(1);
		pptNb = getPoint(2);

		xPoints[0] = (int) ((p1.getX() - minX) * scaleX + decalageX);
		xPoints[1] = (int) ((p2.getX() - minX) * scaleX + decalageX);
		xPoints[2] = (int) ((pptNb.getX() - minX) * scaleX + decalageX);

		yPoints[0] = (int) ((p1.getY() - minY) * scaleY + decalageY);
		yPoints[1] = (int) ((p2.getY() - minY) * scaleY + decalageY);
		yPoints[2] = (int) ((pptNb.getY() - minY) * scaleY + decalageY);

		setColor(g);
		g.fillPolygon(xPoints, yPoints, PT_NB);

		for (int i = 0; i < PT_NB; i++) {
			edges[i].displayObject(g, decalageX, decalageY, minX, minY, scaleX,
					scaleY);
		}
	}

	/**
	 * Display the triangle in a JPanel Must be used only when using package
	 * drawing
	 *
	 * @param g
	 * @param decalageX
	 * @param decalageY
	 */
	protected final void displayObjectCircles(Graphics g, int decalageX, int decalageY) {
		double r = Math.sqrt(radius);
		g.setColor(Color.red);
		g.drawOval((int) (xCenter) + decalageX, decalageY - (int) (yCenter),
				1, 1);//FIXME not good position
		g.drawOval((int) (xCenter - r) + decalageX, decalageY
				- (int) (yCenter + r), (int) r * 2, (int) r * 2);//FIXME not good position
	}

	/**
	 * check if this DTriangle is used by a polygon
	 * @return useByPolygon
	 */
	@Override
	public final boolean isUseByPolygon() {
		return testBit(Tools.BIT_POLYGON);
	}

	/**
	 * set if triangle is used by a polygon.
	 * @param useByPolygon
	 */
	@Override
	public final void setUseByPolygon(boolean useByPolygon) {
		setBit(Tools.BIT_POLYGON, useByPolygon);
	}

	/**
	 * Used to check if this is equal to other.
	 * This and other are equal if and only if other is an instance of DTriangle
	 * and their points are the same (whatever their order in the triangle).
	 * @param other
	 * @return
	 */
	@Override
	public final boolean equals(Object other){
		if(other instanceof DTriangle){
			DTriangle otherTri = (DTriangle) other;
			boolean ret = belongsTo(otherTri.getPoint(0)) && belongsTo(otherTri.getPoint(1))
				&& belongsTo(otherTri.getPoint(2));
			return ret;
		} else {
			return false;
		}
	}

	@Override
	public final int hashCode() {
		int hash = HASHBASE;
		hash = HASHMULT * hash + Arrays.deepHashCode(this.edges);
		return hash;
	}

	/**
	 * implements the Comparable interface. The triangles will be sorted according
	 * the middle of their bounding box.
	 * As we work on a triangulation where triangles' intersection can only be an edge, a point
	 * or void, the Bounding boxes are unique.
	 *
	 * BE CAREFUL : this method is not consistent with equals ! We are making a comparison
	 * on the bounding box of two triangles, they could be equal even if the triangle
	 * are different !!!
	 * @param t
	 * @return
	 */
	@Override
	public final int compareTo(DTriangle t) {
		Coordinate midT = getBoundingBox().getMiddle();
		Coordinate midO = t.getBoundingBox().getMiddle();
		int c = midT.compareTo(midO);
		if(c==0){
			try {
				c = getBarycenter().compareTo(t.getBarycenter());
			} catch (DelaunayError ex) {
				Logger.getLogger(DTriangle.class.getName()).log(Level.WARNING, null, ex);
			}
		}
		return c;
	}

	/**
	 * retrieve the angle between number k.
	 * @param k
	 * @return
	 */
	public final double getAngle(int k){
		int k1 = (k + 1) % PT_NB;
		int k2 = (k1 + 1) % PT_NB;

		DPoint p1 = this.getPoint(k);
		DPoint p2 = this.getPoint(k1);
		DPoint pptNb = this.getPoint(k2);

		double ux = p2.getX() - p1.getX();
		double uy = p2.getY() - p1.getY();
		double vx = pptNb.getX() - p1.getX();
		double vy = pptNb.getY() - p1.getY();

		double dp = ux * vx + uy * vy;

		return Math.acos(Math.sqrt(((dp * dp))
				/ ((ux * ux + uy * uy) * (vx * vx + vy * vy))))
				* (180d / Math.PI);

	}
}