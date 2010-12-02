package org.jdelaunay.delaunay;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author alexis
 */
public class ConstrainedMesh {

	//The list of triangles contained in the mesh
	private ArrayList<MyTriangle> triangleList;
	//The lis of constraints used during the triangulation
	private ArrayList<MyEdge> constraintEdges;
	//The list of points used during the triangulation
	private ArrayList<MyPoint> points;
	//
	private double precision;
	//The minimum distance between two distinct points
	private double tolerance;
	//The two following lists are used only during computation.
	//The bad edge queue list contains all the edges that coud be changed
	//during a flip-flap operation
	private LinkedList<MyEdge> badEdgesQueueList;
	//boundaryEdges contains the Envelope of the CURRENT geometry.
	private LinkedList<MyEdge> boundaryEdges;
	// constants
	public static final double EPSILON = 0.00001;
	public static final int MAXITER = 5;
	public static final int REFINEMENT_MAX_AREA = 1;
	public static final int REFINEMENT_MIN_ANGLE = 2;
	public static final int REFINEMENT_SOFT_INTERPOLATE = 4;
	public static final int REFINEMENT_OBTUSE_ANGLE = 8;

	public ConstrainedMesh() {
		triangleList = new ArrayList<MyTriangle>();
		constraintEdges = new ArrayList<MyEdge>();
		points = new ArrayList<MyPoint>();

		precision = 0;
		tolerance = 0.00001;

		badEdgesQueueList = new LinkedList<MyEdge>();
		boundaryEdges = new LinkedList<MyEdge>();
	}

	/**
	 * Get the list of edges that are to be processed by the flip flap algorithm
	 * @return
	 */
	public LinkedList<MyEdge> getBadEdgesQueueList() {
		return badEdgesQueueList;
	}

	/**
	 * Set the list of edges that are to be processed by the flip flap algorithm
	 * @param badEdgesQueueList
	 */
	public void setBadEdgesQueueList(LinkedList<MyEdge> badEdgesQueueList) {
		this.badEdgesQueueList = badEdgesQueueList;
	}

	/**
	 * Get the list of edges that form the current convex hull of the triangulation
	 * @return
	 */
	public LinkedList<MyEdge> getBoundaryEdges() {
		return boundaryEdges;
	}

	/**
	 * Set the list of edges that form the current convex hull of the triangulation
	 * @param boundaryEdges
	 */
	public void setBoundaryEdges(LinkedList<MyEdge> boundaryEdges) {
		this.boundaryEdges = boundaryEdges;
	}

	/**
	 * Get the list of edges that are used as constraints during triangulation
	 * @return
	 */
	public ArrayList<MyEdge> getConstraintEdges() {
		return constraintEdges;
	}

	/**
	 * Set the list of edges that are used as constraints during triangulation
	 * As we can't be sure the constraintEdges is already sorted, we sort it first
	 * and add all the corresponding points to the point list.
	 * @param constraintEdges
	 */
	public void setConstraintEdges(ArrayList<MyEdge> constraint) {
		this.constraintEdges = new ArrayList<MyEdge>();
		for (MyEdge e : constraint) {
			addPoint(e.getStart());
			addPoint(e.getEnd());
			addConstraintEdge(e);
		}
	}

	/**
	 * Add an edge to the list of constraint edges.
	 * @param e
	 *	the edge we want to add
	 */
	public void addConstraintEdge(MyEdge e) {
		if (constraintEdges == null) {
			constraintEdges = new ArrayList<MyEdge>();
		}
		addEdgeToLeftSortedList(constraintEdges, e);
		addPoint(e.getStart());
		addPoint(e.getEnd());
	}

	/**
	 * This method will sort the edges using the coordinates of the left point
	 * of the edges.
	 * @return
	 */
	public List<MyEdge> sortEdgesLeft(List<MyEdge> inputList) {
		ArrayList<MyEdge> outputList = new ArrayList<MyEdge>();
		for (MyEdge e : inputList) {
			addEdgeToLeftSortedList(outputList, e);
		}
		return outputList;
	}

	/**
	 * This method will insert an edge in an already sorted list, as described
	 * in sortEdgesLeft.
	 * The sorted list is not checked here, so be careful when using this method !
	 * If an edge is already present in the list, it is not added.
	 *
	 * if two edges have the same left point, they are sorted using the other one.
	 * @param sorted
	 * @param edge
	 */
	private void addEdgeToLeftSortedList(ArrayList<MyEdge> sorted, MyEdge edge) {
		if (sorted.isEmpty()) {
			sorted.add(edge);
			return;
		}
		MyEdge temp = sorted.get(0);
		MyPoint left = edge.getPointLeft();
		int s = sorted.size();
		if (left.compareTo2D(temp.getPointLeft()) == -1 || left.compareTo2D(temp.getPointLeft()) == 0) {
			//left is on the left of the first edge in the list, we put it there
			sorted.add(0, edge);
			return;
		}
		temp = sorted.get(s - 1);
		if (temp.getPointLeft().compareTo2D(left) == -1 || temp.getPointLeft().compareTo2D(left) == 0) {
			//left is on the right of the leftmost edge of the last element.
			sorted.add(edge);
			return;
		}
		int c;
		int i = s / 2;
		int delta = s / 2;
		boolean next = true;
		MyEdge other;
		while (next) {
			other = sorted.get(i);
			c = edge.sortLeftRight(other);
			switch (c) {
				case -1:
					other = sorted.get(i - 1);
					c = edge.sortLeftRight(other);
					switch (c) {
						case -1:
							delta = (delta / 2 > 0 ? delta / 2 : 1);
							i = i - delta;
							break;
						case 0:
							return;
						case 1:
							sorted.add(i, edge);
							return;
					}
					break;
				case 0:
					return;
				case 1:
					other = sorted.get(i - 1);
					c = edge.sortLeftRight(other);
					switch (c) {
						case -1:
							sorted.add(i + 1, edge);
							return;
						case 0:
							return;
						default:
							delta = (delta / 2 > 0 ? delta / 2 : 1);
							i = i + delta;
							break;
					}
			}
		}
	}

	/**
	 * Get the precision
	 * @return
	 */
	public double getPrecision() {
		return precision;
	}

	/**
	 * Set the precision
	 * @param precision
	 */
	public void setPrecision(double precision) {
		this.precision = precision;
	}

	/**
	 * Get the value used to compute the minimum distance between two points
	 * @return
	 */
	public double getTolerance() {
		return tolerance;
	}

	/**
	 * Set the value used to compute the minimum distance between two points
	 * @param tolerance
	 */
	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

	/**
	 * Get the list of triangles already computed and added in this mesh
	 * @return
	 */
	public ArrayList<MyTriangle> getTriangleList() {
		return triangleList;
	}

	/**y
	 * Set the list of triangles already computed in this mesh.
	 * @param triangleList
	 */
	public void setTriangleList(ArrayList<MyTriangle> triangleList) {
		this.triangleList = triangleList;
	}

	/**
	 * Get the points contained in this mesh
	 * @return
	 */
	public ArrayList<MyPoint> getPoints() {
		return points;
	}

	/**
	 * Set the list of points to be used during the triangulation
	 * @param points
	 */
	public void setPoints(ArrayList<MyPoint> points) {
		this.points = points;
	}

	/**
	 * Add a new point in the list that will be used to perform the triangulation.
	 * The list of points is supposed to be sorted.
	 * @param point
	 */
	public void addPoint(MyPoint point) {
		addPointToSortedList(point, points);
	}

	/**
	 * Add a new point in a sorted list.
	 * @param point
	 * @param sortedList
	 */
	private void addPointToSortedList(MyPoint point, List<MyPoint> sortedList) {
		int s = sortedList.size();
		if (s == 0) {
			sortedList.add(point);
		} else {
			int p = s / 2;
			int delta = s / 2;
			int ret = -1;
			int c;
			//If the point is inferior to the first element of te list, we place it at the beginning
			if (point.compareTo2D(sortedList.get(0)) == -1) {
				p = 0;
				ret = 0;
				//If the point is superior to the last element of the list, we place it at the end
			} else if (point.compareTo2D(sortedList.get(s - 1)) == 1) {
				p = s;
				ret = s;
			}
			while (ret != p) {
				delta = (delta / 2 > 0 ? delta / 2 : 1);
				c = point.compareTo2D(sortedList.get(p));
				switch (c) {
					case -1:
						//point < points.get(p)
						//We must move left
						c = point.compareTo2D(sortedList.get(p - 1));
						switch (c) {
							case -1:
								p = p - delta;
								break;
							case 0:
								p = -1;
								break;
							default:
								ret = p;
						}
						break;
					case 0:
						p = -1;
						break;
					default:
						p = p + delta;
				}
			}
			if (p != -1) {
				sortedList.add(p, point);
			}
		}
	}

	/**
	 * This methods will search the point p in the list.
	 * @param p
	 * @return the index of p, -1 if it's not in the list
	 */
	public int listContainsPoint(MyPoint p) {
		int s = points.size();
		int c = p.compareTo2D(points.get(0));
		if (c == -1) {	//p<first, p is not in the sorted list
			return -1;
		} else if (c == 0) {
			return 0;
		}
		c = p.compareTo2D(points.get(s - 1));
		if (c == 1) {	//p>last, p is not in the sorted list
			return -1;
		} else if (c == 0) {
			return points.size() - 1;
		}
		//p is ppotentially in the list, and is not one of the extremities.
		int delta = points.size() / 2;
		int i = points.size() / 2;
		while (delta > 0) {
			c = p.compareTo2D(points.get(i));
			switch (c) {
				case -1://p is on the left of points(i)...
					if (i == 0) {
						return -1;
					}
					c = p.compareTo2D(points.get(i - 1));
					switch (c) {
						case 1://...and on the right of points(i-1), so it's no in the list
							return -1;
						case 0:
							return i - 1;
						default://...and on the left of points(i-1), we continue
							delta = delta / 2;
							i = i - delta;
					}
					break;
				case 0:
					return i;
				case 1://p is on the right of points(i)
					if (i == points.size() - 1) {
						return -1;
					}
					c = p.compareTo2D(points.get(i + 1));
					switch (c) {
						case -1://...and on the left of points(i-1), so it's no in the list
							return -1;
						case 0:
							return i + 1;
						default://...and on the right of points(i-1), we continue
							delta = delta / 2;
							i = i + delta;
					}

			}
		}
		return -1;
	}

	/**
	 * This method will force the integrity of the constraints used to compute
	 * the delaunay triangulation. After execution :
	 *  * duplicates are removed
	 *  * intersection points are added to the mesh points
	 *  * secant edges are split
	 */
	public void forceConstraintIntegrity() throws DelaunayError{
		//The event points are the extremities and intersections of the
		//constraint edges. This list is created empty, and filled to stay
		//sorted.
		ArrayList<MyPoint> eventPoints = new ArrayList<MyPoint>();
		//We fill the list.
		for (MyEdge edge : constraintEdges) {
			addPointToSortedList(edge.getStart(), eventPoints);
			addPointToSortedList(edge.getEnd(), eventPoints);
		}
		//we are about to perform the sweepline algorithm
		MyPoint currentEvent = null;
		//edgeBuffer will contain the edges sorted vertically
		ArrayList<MyEdge> edgeBuffer = new ArrayList<MyEdge>();
		//We keep a shallow copy of constraintEdges...
		ArrayList<MyEdge> edgeMemory = constraintEdges;
		//...and we empty it
		constraintEdges = new ArrayList<MyEdge>();
		//The absciss where we search the intersections
		double abs;
                //Used in the  loop...
		int i=0;//The first while
		int j=0;//the inner while
                MyEdge e1, e2; //the edges that will be compared in the for loop
		MyEdge inter1=null;// the edges resulting of the intersection.
		MyEdge inter2=null;
		MyEdge inter3=null;
		MyEdge inter4=null;
		MyPoint newEvent = null;//the event that will be added to the eventList
		MyEdge edgeEvent = null;//used when the intersection is an edge
		MyPoint leftMost = null;
		MyPoint rightMost = null;
		MyElement intersection = null;
		boolean rme1 = false;
		boolean rme2 = false;
		while (i < eventPoints.size()) {
			//We retrieve the event about to be processed.
			currentEvent = eventPoints.get(i);
			//We retrieve the absciss of the current event
			abs = currentEvent.getX();
			//We've reached a new event, we must be sure that our vertical
			//list is still sorted.
			sortEdgesVertically(edgeBuffer, abs);
			//We add the edges that can be added from this event.
			while (!edgeMemory.isEmpty() && currentEvent.equals(edgeMemory.get(0).getPointLeft())) {
				//We've found an edge in our memory that should be added to the buffer.
				insertEdgeVerticalList(edgeMemory.get(0), edgeBuffer, abs);
				//The edge has been added in the buffer, we can remove it.
				edgeMemory.remove(0);
			}
                        //we search for intersections only if we have at least two edges...
                        if(edgeBuffer.size()>1){
                                e2=edgeBuffer.get(0);
				j=1;
                                while(j < edgeBuffer.size()){
                                        //We walk through our buffer
                                        e1=edgeBuffer.get(j-1);
                                        e2=edgeBuffer.get(j);
                                        intersection = e1.getIntersection(e2);
					if(intersection instanceof MyPoint){
						//We have a single intersection point.
						//We must check it's not at an extremity.
						newEvent = (MyPoint) intersection;
						if(!e1.isExtremity(newEvent) || !e2.isExtremity(newEvent)){
						//We've found an intersection between two non-colinear edges
						//We must check that this intersection point is not
						//the current event point. If it is, we must process the
						//intersection.
							if(newEvent.equals(currentEvent)){//We process the intersection.
								if(!newEvent.equals(e2.getPointLeft())){
									inter2=new MyEdge(e2.getPointLeft(), newEvent);
									addConstraintEdge(inter2);
									edgeBuffer.remove(j);
								}
								j--;
								if(!newEvent.equals(e1.getPointLeft())){
									inter1=new MyEdge (e1.getPointLeft(),newEvent);
									addConstraintEdge(inter1);
									edgeBuffer.remove(j);
								}
								if(!newEvent.equals(e1.getPointRight()) && !newEvent.equals(e1.getPointLeft())){
									inter3=new MyEdge (e1.getPointRight(),newEvent);
									insertEdgeVerticalList(inter3, edgeBuffer, abs);
								}
								if(!newEvent.equals(e2.getPointRight())&& !newEvent.equals(e2.getPointLeft())){
									inter4=new MyEdge(e2.getPointRight(), newEvent);
									insertEdgeVerticalList(inter4, edgeBuffer, abs);
								}
							} else { // the intersection will be processed later.
								addPointToSortedList(newEvent, eventPoints);
							}
						} else {
							if(j>0 && e1.getPointRight().equals(currentEvent)){
								addConstraintEdge(e1);
								edgeBuffer.remove(j-1);
							}
							if(j==edgeMemory.size()-1 && e2.getPointRight().equals(currentEvent)){
								addConstraintEdge(e2);
								edgeBuffer.remove(j);
							}
						}
					} else if (intersection instanceof MyEdge){
					//The intersection is an edge. There are two possible cases :
					//The left point of the intersection is at the extremity of e1 and e2 : we
									//register the right point as an event
					//The left point is the extremity of e1 OR (exclusive) of e2. It is an event,
									//and certainly the current one.
						edgeEvent = (MyEdge) intersection;
						newEvent = edgeEvent.getPointLeft();
						if(!(e1.isExtremity(newEvent) && e2.isExtremity(newEvent))){
							addPointToSortedList(edgeEvent.getPointRight(), eventPoints);
						} else {//the intersection point is inside one of the edges.
							//We are supposed to be on it...
							if(newEvent.equals(currentEvent)){
								leftMost = (e1.getPointLeft().compareTo2D(e2.getPointLeft())<1 ?
									e1.getPointLeft() :
									e2.getPointLeft());
								rightMost = (e1.getPointRight().compareTo2D(e2.getPointRight())<1 ?
									e1.getPointRight() :
									e2.getPointRight());
								//we remove the two edges we are analyzing,
								//new edges will be inserted if necessary.
								edgeBuffer.remove(j);
								j--;
								edgeBuffer.remove(j);
								if(leftMost.compareTo2D(newEvent)==-1){
									inter1 = new MyEdge(leftMost, newEvent);
								}
								inter2 = edgeEvent;
								if(rightMost.compareTo2D(edgeEvent.getPointRight())==1){
									inter3=new MyEdge(edgeEvent.getPointRight(), rightMost);
								}
								if(inter1!=null){
									addConstraintEdge(inter1);
								}
								if(inter2.getPointRight().compareTo2D(currentEvent)==1){
									//inter2 has to be processed for further intersections
									insertEdgeVerticalList(inter2, edgeBuffer, abs);
								} else {
									//inter2 can't be implied in other intersections
									addConstraintEdge(inter2);
								}
								if(inter3!=null){
									//inter3 must be processed further.
									insertEdgeVerticalList(inter2, edgeBuffer, abs);
								}
							} else {
								throw new DelaunayError("We should already be on this event point");
							}
						}
						
					} else {//there is no intersection here, we check the
						//lowest edge.
						if(e1.getPointRight().equals(currentEvent)){
							addConstraintEdge(e1);
							edgeBuffer.remove(j-1);
						}
						if(j==edgeMemory.size()-1 && e2.getPointRight().equals(currentEvent)){
							addConstraintEdge(e2);
							edgeBuffer.remove(j);
						}
					}
					j++;
                                }
                        } else if (edgeBuffer.size()==1 && edgeBuffer.get(0).getPointRight().equals(currentEvent)){
				addConstraintEdge(edgeBuffer.get(0));
				edgeBuffer.remove(0);
			}
			i++;
		}
	}

	/**
	 * This method will sort the edges contained in the ArrayList list by considering
	 * their intersection point with the line of equation x=a, where a is given
	 * in parameter.
	 * @param edgeList
	 * @param x
	 */
	public void sortEdgesVertically(List<MyEdge> edgeList, double abs) throws DelaunayError {
		int s = edgeList.size();
		int i = 0;
		int c = 0;
		MyEdge e1;
		MyEdge e2;
		while (i < s - 1) {
			e1 = edgeList.get(i);
			e2 = edgeList.get(i + 1);
			c = e1.verticalSort(e2,abs);
			if (c == 1) {
				edgeList.set(i, e2);
				edgeList.set(i + 1, e1);
				i = 0;
			} else {
				i++;
			}
		}
	}

	/**
	 * This method will insert a new Edge in a vertically sorted list, as described in
	 * sortEdgesVertically.
	 * Be careful when using this method. In fact, you must use the same absciss
	 * here that the one which has been used when sorting the list.
	 * @param edge
	 * @param edgeList
	 */
	public void insertEdgeVerticalList(MyEdge edge, List<MyEdge> edgeList, double abs) throws DelaunayError {
		if (edgeList == null || edgeList.isEmpty()) {
			edgeList.add(edge);
		}
		int s = edgeList.size();
		int compare = edge.verticalSort(edgeList.get(0), abs);
		if (compare == -1) {
			edgeList.add(0, edge);
			return;
		}
		compare = edge.verticalSort(edgeList.get(s-1), abs);
		if (compare == 1) {
			edgeList.add(s, edge);
			return;
		}
		int delta = s / 2;
		int i = s / 2;
		boolean duplicate = false;
		boolean stillVEquals;
		MyEdge temp = null;
		while (delta > 0) {
			compare = edge.verticalSort(edgeList.get(i), abs);
			switch(compare){
				case -1:
					compare = edge.verticalSort(edgeList.get(i-1), abs);
					switch(compare){
						case -1:
							delta = delta / 2;
							i=i-delta;
							break;
						case 1:
							edgeList.add(i, edge);
							return;
						case 0:
							return;
					}
					break;
				case 0:
					return;
				case 1:
					compare = edge.verticalSort(edgeList.get(i+1), abs);
					switch(compare){
						case 1:
							delta = delta / 2;
							i=i+delta;
							break;
						case -1:
							edgeList.add(i+1, edge);
							return;
						case 0:
							return;

					}
					break;
			}
		}

	}

	/**
	 * this method will travel through a vertically sorted list to search for duplicates
	 * Returns true if it finds one. You must give the index where the research begins.
	 * Travel from end to beginning.
	 * @param vSList
	 * @param edge
	 * @param index
	 * @param abs
	 * @return true if there is a copy of edge in vSList
	 */
//	private boolean checkForDuplicatePrevious(List<MyEdge> vSList, MyEdge edge, int index, double abs){
//		if(index < 0 || index > vSList.size()){
//			return false;
//		} else {
//			ListIterator<MyEdge> iter = vSList.listIterator(index);
//			boolean ret = false;
//			boolean stillEquals=true;
//			MyEdge temp = null;
//			while(!ret && iter.hasPrevious() && stillEquals){
//				temp=iter.previous();
//			}
//			return ret;
//		}
//	}

	/**
	 * This method simply travels the list given in argument. If edges edgelist.get(i)
	 * and edgeList.get(i+1) intersect, then we add the intersection point in
	 * the eventList.
	 * @param edgeList
	 */
	public void addPointsFromNeighbourEdges(List<MyEdge> edgeList, List<MyPoint> eventList) throws DelaunayError {
		MyEdge e1;
		MyEdge e2;
		MyElement inter = null;
		//we check that our paremeters are not null, and that our edge list contains
		//at least two edges, because they couldn't be intersections otherwise.
		if (edgeList == null || eventList == null || edgeList.size() < 2) {
			return;
		} else {
			for (int i = 0; i < edgeList.size() - 1; i++) {
				e1 = edgeList.get(i);
				e2 = edgeList.get(i + 1);
				inter = e1.getIntersection(e2);
				if (inter != null) {
					if(inter instanceof MyPoint){
						eventList.add((MyPoint) inter);
					} else {
						eventList.add(((MyEdge) inter).getPointLeft());
					}
				}
			}
		}
	}
}
