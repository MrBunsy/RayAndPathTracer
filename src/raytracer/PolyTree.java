package raytracer;

import LukesBits.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import javatree.IBranch;
import javatree.Tree;

/**
 *
 * @author Luke
 * 
 * Very similar to Poly, but for a whole tree, so I can optimise this for trees.
 * 
 * idea: instead of just a single encasing sphere, traverse the branches so taht groups of branches are enclosed in their own spheres
 * 
 */
public class PolyTree implements Shape,IPoly{

    private Surface surface;
    private ArrayList<PolyCylinder> polyCylinders;
    Vector centre;
    protected Sphere encasingSphere;
    
    @Override
    public ShapeName getShapeName(){
        return ShapeName.Poly;
    }
    
    @Override
    public boolean isConvex() {
        //this can and should cast shadows on itself!
        return false;
    }
    
    public PolyTree(Tree tree,int quality) {
        //super(new Surface(new Colour(200, 200, 100)), new Triangle[1]);
        //surface=new Surface(new Colour(200, 200, 100));
        surface=new Surface(new Colour(145, 133, 121));

        IBranch[] branches = tree.getBranches().toArray(new IBranch[1]);

        //ArrayList<Shape> shapes = new ArrayList<Shape>();
        polyCylinders=new ArrayList<PolyCylinder>();
        ArrayList<PolyCylinderBranchStore> cylinders = new ArrayList<PolyCylinderBranchStore>();
        
        ArrayList<Vector> endPoints = new ArrayList<Vector>();
        
        for (int i = 0; i < branches.length; i++) {//6

            //shapes.add(getCylinder(_surface, branches[i].getStump(), branches[i].getAbsoluteAngle(), branches[i].getLength(), branches[i].getRadius(), branches[i].getParentRadius(), 10, 1,branches[i].getParentAbsoluteAngle()));

            endPoints.add(branches[i].getTip());
            if(i==0){
                continue;
                //skip the 'trunk'
            }
            
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
            PolyCylinder c = new PolyCylinder(surface, branches[i].getStump(), branches[i].getAbsoluteAngle(), branches[i].getLength(), branches[i].getRadius(), baseRadius, quality, 1, baseAngle, !branches[i].hasChildren(), parentCylinder);

            c.setParent(this);
            
            cylinders.add(new PolyCylinderBranchStore(branches[i], c));

            polyCylinders.add(c);
                       
        }
        
        //go through all the end points and find the furthest apart pair
            
        Vector furthest1=endPoints.get(0),furthest2=endPoints.get(1);

        double furthest=endPoints.get(0).subtract(endPoints.get(1)).getMagnitudeSqrd();
        
        for(int i=0;i<endPoints.size();i++){
            for(int j=i+1;j<endPoints.size();j++){
                double distance=endPoints.get(i).subtract(endPoints.get(j)).getMagnitudeSqrd();
                if(distance>furthest){
                    furthest=distance;
                    furthest1=endPoints.get(i);
                    furthest2=endPoints.get(j);
                }
            }
        }
        
        centre=furthest1.add(furthest2).multiply(0.5);
        double radius = furthest1.subtract(furthest2).getMagnitude()+ tree.getBaseRadius();
        
        encasingSphere=new Sphere(surface, centre, radius);
    }

    @Override
    public Surface getSurface() {
        return surface;
    }

    private Shape parent;
    
    @Override
    public Shape getParent() {
        return parent == null ? this : parent.getParent();
    }
    
    @Override
    public void setParent(Shape _parent){
        parent=_parent;
    }
    
    @Override
    public Vector getTextureCoords(Vector collision) {
        return new Vector(0,0);
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
        //copied from Poly.collide

        if (encasingSphere.collide(ray).collide || ray.start.subtract(encasingSphere.pos).getMagnitudeSqrd() < encasingSphere.r2) {

            //only perform proper collision detection if the encasing spehere is collided with
            ArrayList<CollisionStore> collisions = new ArrayList<CollisionStore>();


            for (PolyCylinder t : polyCylinders) {
                Collision c = t.collide(ray);
                if (c.collide) {
                    collisions.add(new CollisionStore(c, c.where.subtract(ray.start).getMagnitudeSqrd()));
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
    public Sphere getEncasingSphere() {
        return encasingSphere;
    }
    
    public static Poly getLeaf(Vector pos, Vector dir, double size){
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        Surface leafSurface = new Surface(new Colour(50,255,50));
        
        
        
        Poly poly = new Poly(leafSurface, triangles.toArray(new Triangle[0]),false);
        poly.setEncasingRadius(pos.add(dir,size/2.0), size/2.0);

        return poly;
    }
    
    @Override
    public Triangle[] getRealtimeTriangles() {
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        
        for (PolyCylinder t : polyCylinders) {
            triangles.addAll(Arrays.asList(t.getRealtimeTriangles()));
        }
        
        return triangles.toArray(new Triangle[0]);
    }
    
}
