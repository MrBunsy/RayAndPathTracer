package raytracer;

import java.util.ArrayList;
import javatree.*;
import LukesBits.Vector;
import java.io.Serializable;


/**
 *
 * @author Luke
 */
public class Poly implements Shape,IPoly {

    protected Surface surface;
    protected Triangle[] triangles;
    //a spehere which contains all the triangles
    //private double encasingRadius;
    protected Sphere encasingSphere;

    protected boolean convex;
    
    public Triangle[] getTriangles() {
        return triangles;
    }

    public Poly(Surface _surface, Triangle[] _triangles, boolean _convex) {
        surface = _surface;
        triangles = _triangles;
        convex = _convex;
        
        for(Triangle t : triangles){
            t.setParent(this);
        }
        
    }

    @Override
    public boolean isConvex() {
        //the triangle can't cast shadows on itself
        return convex;
    }
    
    @Override
    public ShapeName getShapeName(){
        return ShapeName.Poly;
    }
    
    public void setEncasingRadius(Vector centre, double r) {
        //encasingRadius=r;
        encasingSphere = new Sphere(null, centre, r);
    }
    
    @Override
    public Sphere getEncasingSphere(){
        return encasingSphere;
    }

    private Shape parent;
    
    @Override
    public Shape getParent() {
        if(parent==null){
            return this;
        }else{
            return parent.getParent();
        }
    }
    
    @Override
    public void setParent(Shape _parent){
        parent=_parent;
    }
    
//    public static Poly getGeoDome(Surface _surface) {
//        //TODO http://en.wikipedia.org/wiki/Geodesic_grid
//        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
//
//        return new Poly(_surface, triangles.toArray(new Triangle[1]));
//    }

    public static ArrayList<Shape> tree(Tree tree) {
        //return tree(tree, 1.0);
        return tree(tree,20);
    }

    //note, this scale isn't used, scale is now part of the tree
    //woops
    public static ArrayList<Shape> tree(Tree tree,int quality){//, double scale) {
        Surface _surface = new Surface(new Colour(200, 200, 100));

        IBranch[] branches = tree.getBranches().toArray(new IBranch[1]);

        ArrayList<Shape> shapes = new ArrayList<Shape>();
        ArrayList<PolyCylinderBranchStore> cylinders = new ArrayList<PolyCylinderBranchStore>();

        for (int i = 0; i < branches.length; i++) {//6

            //shapes.add(getCylinder(_surface, branches[i].getStump(), branches[i].getAbsoluteAngle(), branches[i].getLength(), branches[i].getRadius(), branches[i].getParentRadius(), 10, 1,branches[i].getParentAbsoluteAngle()));

            double baseRadius = branches[i].isTrunk() ? branches[i].getParentRadius() : branches[i].getRadius();
            Vector baseAngle = branches[i].isTrunk() ? branches[i].getParentAbsoluteAngle() : null;

            PolyCylinder parentCylinder = null;

            //find parent polyCylinder
            if (branches[i].isTrunk()) {

                for (PolyCylinderBranchStore pc : cylinders) {

                    if (pc.branch == branches[i].getParent()) {
                        parentCylinder = pc.cylinder;
                        //stop looping
                        break;
                    }
                }
            }
            //if it has no children, cap the cylinder, else no need.
            PolyCylinder c = new PolyCylinder(_surface, branches[i].getStump(), branches[i].getAbsoluteAngle(), branches[i].getLength(), branches[i].getRadius(), baseRadius, quality, 1, baseAngle, !branches[i].hasChildren(), parentCylinder);

            cylinders.add(new PolyCylinderBranchStore(branches[i], c));

            shapes.add(c);

            //looked pretty poor:
//            if(branches[i].hasChildren()){
//                shapes.add(new Sphere(_surface,branches[i].getTip(),branches[i].getRadius()));
//            }
            //cylinders[i] = getCylinder(_surface, branches[i].getStump(), branches[i].getAbsoluteAngle(), branches[i].getLength(), branches[i].getRadius(), branches[i].getRadius(), 6, 10);
        }

        return shapes;
    }

//    public static ArrayList<Triangle> getBoxTriangles(Surface _surface, Vector pos, double width, double height, double depth){
//        return getBoxTriangles(_surface, pos, width, height, depth,true);
//    }
    
    public static ArrayList<Triangle> getBoxTriangles(Surface _surface, Vector pos, double width, double height, double depth, boolean top) {
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();

        Vector x = new Vector(1, 0, 0);
        Vector y = new Vector(0, 1, 0);
        Vector z = new Vector(0, 0, 1);

        //TODO dir is dir from 'bottom left corner' to centre - some sort of abstraction so this can be at any angle.
        Vector bottomLeft = pos.subtract(x, width / 2).subtract(y, height / 2).subtract(z, depth / 2);

        //base:
        triangles.add(new Triangle(_surface, bottomLeft, bottomLeft.add(x, width), bottomLeft.add(y, height), true));

        Vector topLeft = bottomLeft.add(z, depth);

        //left side:
        triangles.add(new Triangle(_surface, bottomLeft, bottomLeft.add(z, depth), bottomLeft.add(y, height), true));

        //bottom side:
        triangles.add(new Triangle(_surface, bottomLeft, bottomLeft.add(z, depth), bottomLeft.add(x, width), true));

        //right side:
        triangles.add(new Triangle(_surface, bottomLeft.add(x, width), bottomLeft.add(x, width).add(z, depth), bottomLeft.add(x, width).add(y, height), true));

        //top side:
        triangles.add(new Triangle(_surface, bottomLeft.add(y, height), bottomLeft.add(y, height).add(z, depth), bottomLeft.add(x, width).add(y, height), true));

        //top is deliberatly the last one so we can edit it
        //yes this is hacky
        if (top) {
            //top:
            triangles.add(new Triangle(_surface, topLeft, topLeft.add(x, width), topLeft.add(y, height), true));
        }

        return triangles;
    }

    public static Poly getBox(Surface _surface, Vector pos, double width, double height, double depth) {

        ArrayList<Triangle> triangles = getBoxTriangles(_surface, pos, width, height, depth, true);

        for (Triangle t : triangles) {
            t.thisIsInside(pos);
        }
        Poly poly = new Poly(_surface, triangles.toArray(new Triangle[0]),true);
        poly.setEncasingRadius(pos, Math.sqrt(width * width + height * height + depth * depth));

        return poly;
    }

    //idea: use phong shading to smooth edges?
    public static ArrayList<Poly> getTumbler(Vector pos, Vector dir, double r, double tall, boolean water){
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        
        Surface glass = Surface.glass();//.setn(1.0);
        
        Surface waterSurface = Surface.water();
        
        //glass = new Surface(new Colour(200,50,50));
        //a cylinder with no top
        PolyCylinder outside = new PolyCylinder(glass, pos, dir, tall, r, r, 30, 1, null, true, true);
        double insideHeight=tall*0.8;
        Vector insideBase = pos.add(dir,tall*0.2);
        PolyCylinder inside = new PolyCylinder(glass,insideBase , dir, insideHeight, r*0.9, r*0.9, 30, water ? 3 : 1, null, true, true);
        
        
        
        Triangle[] outsideTriangles  = outside.getTriangles();
        Triangle[] insideTriangles  = inside.getTriangles();
        
        Vector centre = pos.add(dir,tall/2);
        
        //add the triangles from the outside cylinder of the cup
        for(Triangle t : outsideTriangles){
             t.thisIsInside(centre);
//            if(t.n0==null){
//                t.setSurface(new Surface(new Colour(0,255,0)));
//            }
            triangles.add(t);
        }
        
        
        
        //add the triangles from the inside cylinder of teh cup
        for(Triangle t : insideTriangles){
            t.thisIsOutside(centre);
            if(t.n0!=null){
                //the base doesn't have phong shading
                t.n0=t.n0.multiply(-1.0);
                t.n1=t.n1.multiply(-1.0);
                t.n2=t.n2.multiply(-1.0);
            }
            
            if(water && t.v0.subtract(insideBase).dot(dir) < insideHeight*0.7 && t.v1.subtract(insideBase).dot(dir) < insideHeight*0.7 && t.v2.subtract(insideBase).dot(dir) < insideHeight*0.7){
                //this needs to have an outsiden set
                //t.getSurface().setOutsideN(waterSurface.n);
                //t.setSurface(new Surface(new Colour(255, 0, 0)));
                t.setSurface(t.getSurface().clone().setOutsideN(waterSurface.n));
                
//                if(t.n0==null){
//                    t.setSurface(new Surface(new Colour(255,0,0)));
//                }
            }
            
            triangles.add(t);
        }
        //how many rings to add for the triangles stictching the top together
        //this involves a slightly raised up rim
        //min 2 for just the inside cylinder and outside cylinder
        int topRings = 4;
        
        
        //stitch together the tops
        Vector[] outsideTops = outside.getTopPoints();
        Vector[] outsideNormals = outside.getNormals();
        Vector[] insideTops = inside.getTopPoints();
        Vector[] insideNormals = inside.getNormals();
        Triangle t;
        
        for(int i=0;i<insideNormals.length;i++){
            insideNormals[i]=insideNormals[i].multiply(-1.0);
        }
        
        Vector[][] topPoints = new Vector[topRings][outsideTops.length];
        
        for(int i=0;i<topRings;i++){
            for(int j=0;j<outsideTops.length;j++){
                if(i==0){
                    topPoints[i][j]=insideTops[j].copy();
                    continue;
                }
                
                if(i==topRings-1){
                    topPoints[i][j]=outsideTops[j].copy();
                    continue;
                }
                
                Vector inToOut = outsideTops[j].subtract(insideTops[j]);//.getUnit();
                topPoints[i][j]=insideTops[j].add(inToOut,(double)i/(double)(topRings-1)).add(dir,r*0.025);//(double)(i)*r*0.025/(double)(topRings-2));//.add(dir,r*0.025);
            }
        }
        
        int tops=outsideTops.length;
        
        for(int i=0;i<topRings-1;i++){
            for(int j=0;j<tops;j++){
                //top half
                t = new Triangle(glass,topPoints[i][j],topPoints[i][(j+1)%tops],topPoints[i+1][j]);
                t.thisIsInside(centre);
                if(i==0){
                    t.setVerticesN(insideNormals[j], insideNormals[(j+1)%tops], dir);
                }
                if(i==topRings-2){
                    t.setVerticesN(dir, dir, outsideNormals[j]);
                }
                triangles.add(t);

                t = new Triangle(glass,topPoints[i][(j+1)%tops],topPoints[i+1][j],topPoints[i+1][(j+1)%tops]);
                t.thisIsInside(centre);
                if(i==0){
                    t.setVerticesN(insideNormals[(j+1)%tops], dir, dir);
                }
                if(i==topRings-2){
                    t.setVerticesN(dir, outsideNormals[j], outsideNormals[(j+1)%tops]);
                }
                triangles.add(t);
            }
        }
        
        //ArrayList<Triangle> waterTriangles = new ArrayList<Triangle>();
        
        if(water){
            
            //todo work out how to get a bumpmap working for this!
            
            //waterSurface = new Surface(new Colour(255,0,0));
            
            Vector[] waterPoints = inside.getPoints()[2];
            Vector waterCentre = insideBase.add(dir,insideHeight*2.0/3.0);
            
            for(int i=0;i<waterPoints.length;i++){
                t = new Triangle(waterSurface,waterCentre,waterPoints[i],waterPoints[(i+1)%waterPoints.length]);
                //t.thisIsInside(centre);
                
                t.n=dir;
                
                triangles.add(t);
            }
        }
        
        Poly poly = new Poly(glass, triangles.toArray(new Triangle[0]),false);
        poly.setEncasingRadius(centre, Math.sqrt(r*r + (tall/2) * (tall/2) + (r*0.025)*(r*0.025)));

        
        
        
        ArrayList<Poly> returnMe = new ArrayList<Poly>();
        
        returnMe.add(poly);
        
        return returnMe;
    }
        

//    public static double heightAtOnWater(double x, double y, Vector[] waterDrops, double[] peaks, double scale) {
//
//        double z = 0;
//
//        Vector here = new Vector(x, y, 0);
//
//        for (int i = 0; i < waterDrops.length; i++) {
//            Vector w = waterDrops[i];
//            double p = peaks[i];
//
//            double distance = here.subtract(w).getMagnitude();
//
//            //z += Math.cos(distance * scale) * p * (1.0 / (distance*scale*scale+1));
//            z += Math.cos(distance * scale) * p * (1.0 / (Math.pow(distance * scale, scale)));
//        }
//
//        return z;
//    }
//
//    public static Poly getSquareWater(Surface _surface, Vector pos, double width, double height, double depth, int xDetail, int yDetail) {
//        ArrayList<Triangle> triangles = new ArrayList<Triangle>();//getBoxTriangles(_surface, pos, width, height, depth, false);
//
//
//
//        //int xDetail=10;
//        //int yDetail=10;
//
//        Vector xDir = new Vector(1, 0, 0);
//        Vector yDir = new Vector(0, 1, 0);
//        Vector zDir = new Vector(0, 0, 1);
//
//        Vector topLeft = pos.subtract(xDir, width / 2).subtract(yDir, height / 2).add(zDir, depth / 2);
//        Vector bottomLeft = pos.subtract(xDir, width / 2).subtract(yDir, height / 2).subtract(zDir, depth / 2);
//
//        //base
//        triangles.add(new Triangle(_surface, bottomLeft, bottomLeft.add(xDir, width), bottomLeft.add(yDir, height), true));
//
//        for (Triangle t : triangles) {
//            t.thisIsInside(pos);
//        }
//
//        Vector[] waterDrops = new Vector[]{new Vector(width * 0.5, height * 0.5, 0)};
//
//        double[] peaks = new double[]{10.0};
//
//        double scale = 0.1;
//        double smallNormal = 0.01;
//
//        Vector[][] points = new Vector[xDetail][yDetail];
//        Vector[][] normals = new Vector[xDetail][yDetail];
//
//        double dx = width / (double) (xDetail - 1);
//        double dy = height / (double) (yDetail - 1);
//
//        for (int x = 0; x < xDetail; x++) {
//            for (int y = 0; y < yDetail; y++) {
//
//                double xPos = (double) x * dx;
//                double yPos = (double) y * dy;
//
//                double z = heightAtOnWater(xPos, yPos, waterDrops, peaks, scale);
//
//                Vector here = new Vector(xPos, yPos, z);
//
//                points[x][y] = topLeft.add(here);
//
//                //find the surface normal
//                Vector hereXwards = new Vector(xPos + smallNormal, yPos, heightAtOnWater(xPos + smallNormal, yPos, waterDrops, peaks, scale));
//                Vector backXwards = new Vector(xPos - smallNormal, yPos, heightAtOnWater(xPos - smallNormal, yPos, waterDrops, peaks, scale));
//
//                Vector hereYwards = new Vector(xPos, yPos + smallNormal, heightAtOnWater(xPos, yPos + smallNormal, waterDrops, peaks, scale));
//                Vector backYwards = new Vector(xPos, yPos - smallNormal, heightAtOnWater(xPos, yPos - smallNormal, waterDrops, peaks, scale));
//
//                //hereXwards=hereXwards.subtract(here);
//                //hereYwards=hereYwards.subtract(here);
//
//                Vector xWay = hereXwards.subtract(backXwards);
//                Vector yWay = hereYwards.subtract(backYwards);
//
//                Vector normalHere = xWay.cross(yWay).getUnit();
//
//                normals[x][y] = normalHere;
//            }
//        }
//        Triangle t;
//
//        //build grid of triangles
//        for (int x = 0; x < xDetail - 1; x++) {
//
//            //bottom side
//
//            //bottom triangle
//            t = new Triangle(_surface, bottomLeft.add(xDir, (double) x * dx), bottomLeft.add(xDir, (double) (x + 1) * dx), points[x][0]);
//            t.thisIsInside(pos);
//            triangles.add(t);
//            //top triangle
//            t = new Triangle(_surface, bottomLeft.add(xDir, (double) (x + 1) * dx), points[x + 1][0], points[x][0]);
//            t.thisIsInside(pos);
//            triangles.add(t);
//
//            //top side
//            t = new Triangle(_surface, bottomLeft.add(xDir, (double) x * dx).add(yDir, height), bottomLeft.add(xDir, (double) (x + 1) * dx).add(yDir, height), points[x][yDetail - 1]);
//            t.thisIsInside(pos);
//            triangles.add(t);
//            //top triangle
//            t = new Triangle(_surface, bottomLeft.add(xDir, (double) (x + 1) * dx).add(yDir, height), points[x + 1][yDetail - 1], points[x][yDetail - 1]);
//            t.thisIsInside(pos);
//            triangles.add(t);
//
//
//            for (int y = 0; y < yDetail - 1; y++) {
//
//
//                //left side
//                t = new Triangle(_surface, bottomLeft.add(yDir, (double) y * dy), bottomLeft.add(yDir, (double) (y + 1) * dy), points[0][y]);
//                t.thisIsInside(pos);
//                triangles.add(t);
//                t = new Triangle(_surface, bottomLeft.add(yDir, (double) (y + 1) * dy), points[0][y + 1], points[0][y]);
//                t.thisIsInside(pos);
//                triangles.add(t);
//
//                //right side
//                t = new Triangle(_surface, bottomLeft.add(yDir, (double) y * dy).add(xDir, width), bottomLeft.add(yDir, (double) (y + 1) * dy).add(xDir, width), points[xDetail - 1][y]);
//                t.thisIsInside(pos);
//                triangles.add(t);
//                t = new Triangle(_surface, bottomLeft.add(yDir, (double) (y + 1) * dy).add(xDir, width), points[xDetail - 1][y + 1], points[xDetail - 1][y]);
//                t.thisIsInside(pos);
//                triangles.add(t);
//
//
//                //bottom left triangle
//                //t=;
//                triangles.add(new Triangle(_surface, points[x][y], points[x + 1][y], points[x][y + 1], normals[x][y], normals[x + 1][y], normals[x][y + 1]));
//                //ensure the normal is facing the right way
//                //this works out so the normal is always pointing upwards, from the order of the points
//                //t.thisIsInside(new Vector((double)x*dx,(double)y*dy,pos.z-depth/2));
//                //top right
//                //t=;
//                triangles.add(new Triangle(_surface, points[x][y + 1], points[x + 1][y], points[x + 1][y + 1], normals[x][y + 1], normals[x + 1][y], normals[x + 1][y + 1]));
//            }
//        }
//
//
//
//
//
//        Poly poly = new Poly(_surface, triangles.toArray(new Triangle[1]));
//        poly.setEncasingRadius(pos, Math.sqrt(width * width + height * height + depth * depth));
//
//        return poly;
//    }

    public static Poly getRubbishSphere(Surface _surface, Vector pos, double r) {
        return getRubbishSphere(_surface, pos, r, 10, 10);
    }

    public static Poly getRubbishSphere(Surface _surface, Vector pos, double r, int segments, int rings){
        return getRubbishSphere(_surface, pos, r, segments, rings, true);
    }
    
    //I'm not intending this sphere to get used for much more than testing the raytracer
    //if I want to draw a real sphere, I can do that.
    //NOTE - the poitns are generated top down
    /**
     * 
     * @param _surface
     * @param pos
     * @param r
     * @param segments
     * @param rings
     * @param phong NOT LISTENED TO YET
     * @return 
     */
    public static Poly getRubbishSphere(Surface _surface, Vector pos, double r, int segments, int rings, boolean phong) {
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();

        //theta = looking from top, angle from x axis, segment
        //alpha = looking from side, angle from z axis, ring

        double dTheta = 2 * Math.PI / (double) segments;
        double dAlpha = Math.PI / (double) rings;

        double[] thetas = new double[segments];
        double[] alphas = new double[rings];


        Vector top = pos.add(new Vector(0, 0, r));
        Vector bottom = pos.add(new Vector(0, 0, -r));

        //number of lines on contours = number of rings, so number of devisions is rings+1 which accoutns for the top/bottom
        //number of lines lengthways = number of segments, because these wrap around.

        Vector[][] points = new Vector[segments][rings];
        Vector[][] normals = new Vector[segments][rings];

        for (int i = 0; i < segments; i++) {
            thetas[i] = dTheta * i;
        }

        for (int i = 0; i < rings; i++) {
            alphas[i] = dAlpha * (i + 1);
        }

        Vector segmentDir, ringDir;
        for (int i = 0; i < segments; i++) {

            //points along the x-y plane towards the segment
            segmentDir = new Vector(Math.cos(thetas[i]), Math.sin(thetas[i]), 0);

            for (int j = 0; j < rings; j++) {

                //points at the ring on this segment
                ringDir = new Vector(segmentDir.x * Math.sin(alphas[j]), segmentDir.y * Math.sin(alphas[j]), Math.cos(alphas[j]));

                normals[i][j] = ringDir.getUnit();

                points[i][j] = pos.add(ringDir, r);

            }
        }



        for (int i = 0; i < segments; i++) {
            //top
            triangles.add(new Triangle(_surface, top, points[i][0], points[((i + 1) % segments)][0], new Vector(0, 0, 1), normals[i][0], normals[((i + 1) % segments)][0]));



            for (int j = 0; j < rings - 1; j++) {
                //middle

                //top half of this square
                triangles.add(new Triangle(_surface, points[i][j], points[i][j + 1], points[((i + 1) % segments)][j], normals[i][j], normals[i][j + 1], normals[((i + 1) % segments)][j]));

                //bottom half of this square
                triangles.add(new Triangle(_surface, points[((i + 1) % segments)][j + 1], points[i][j + 1], points[((i + 1) % segments)][j], normals[((i + 1) % segments)][j + 1], normals[i][j + 1], normals[((i + 1) % segments)][j]));

            }

            //bottom
            triangles.add(new Triangle(_surface, bottom, points[i][rings - 1], points[((i + 1) % segments)][rings - 1], new Vector(0, 0, -1), normals[i][rings - 1], normals[((i + 1) % segments)][rings - 1]));
        }
        //set all the surface normals for the triangle
        for (Triangle t : triangles) {
            t.thisIsInside(pos);
        }

        Poly poly = new Poly(_surface, triangles.toArray(new Triangle[1]),true);
        poly.setEncasingRadius(pos, r + Double.MIN_VALUE);

        return poly;
    }

    @Override
    public Surface getSurface() {
        return surface;
    }

    @Override
    public Vector getTextureCoords(Vector collision) {
        //TODO
        return new Vector(0, 0);
    }

    @Override
    public Vector getSurfaceAngle() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSurfaceAngle(Vector _surfaceAngle) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collision collide(Ray ray) {

        if (encasingSphere == null || (encasingSphere.collide(ray).collide || ray.start.subtract(encasingSphere.pos).getMagnitudeSqrd() < encasingSphere.r2)) {

            //only perform proper collision detection if the encasing spehere is collided with
            ArrayList<CollisionStore> collisions = new ArrayList<CollisionStore>();

            //boolean onSurface=false;

            for (Triangle t : triangles) {
                Collision c = t.collide(ray);
                if (c.collide) {
                    
                    double howFar = c.where.subtract(ray.start).getMagnitudeSqrd();
                    //onSurfaceOf *should* be set to the individual triangle
                    if(!(ray.onSurfaceOf==t && howFar < 0.0001)){
                        //only count this collision if it's not one the ray starts on the surface of7
                        //this is in place of the old littlebodge
                        collisions.add(new CollisionStore(c,howFar ));
                    }//else{
                        //start of ray is on meant to be the surface of a shape, but the actual start of ray may or may not be
                        //inside the poly
                     //   onSurface=true;
                    //}
                }
            }

            if (collisions.isEmpty()) {
                return Collision.noCollision;
            }

            double nearest = Double.MAX_VALUE;
            Collision collisionFound = null;

            for (int j = 0; j < collisions.size(); j++) {
                if (collisions.get(j).distance < nearest) {
                    nearest = collisions.get(j).distance;
                    collisionFound = collisions.get(j).collision;
                }
            }
            return collisionFound;
        }

        return Collision.noCollision;
    }

    @Override
    public Triangle[] getRealtimeTriangles() {
        return getTriangles();
    }
    
}
class CollisionStore {

    public Collision collision;
    public double distance;

    public CollisionStore(Collision _collision, double _distance) {
        collision = _collision;
        distance = _distance;
    }
}

class PolyCylinderBranchStore {

    public IBranch branch;
    public PolyCylinder cylinder;

    public PolyCylinderBranchStore(IBranch _branch, PolyCylinder _cylinder) {
        branch = _branch;
        cylinder = _cylinder;
    }
}