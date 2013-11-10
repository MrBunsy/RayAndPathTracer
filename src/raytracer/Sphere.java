package raytracer;
import LukesBits.Vector;

/**
 *
 * @author Luke
 */
public class Sphere implements Shape {

    public double r;
    public double r2;
    public Vector pos;
    public Surface surface;

    //leave angle, textures might be supported in future
    public Sphere(Surface _surface, Vector _pos, double _r) {

        surface = _surface;
        pos = _pos;
        r = _r;
        r2=_r*_r;
    }

    //does this shape collide with a line at angle lineAngle which goes throuhg linePoint?
    @Override
    public Collision collide(Ray ray) {
        //s = start of ray
        Vector s = ray.start;
        //d= direction of ray, unit
        Vector d = ray.dir;
        //c=centre of sphere
        //v=s-c
        Vector v = s.subtract(pos);

        double determinate = Math.pow(v.dot(d), 2) - (v.dot(v) - r2);

        if (determinate < 0) {
            //complex answer, no collision
            return Collision.noCollision;
        } else {
            //goes through sphere
            double t1 = -v.dot(d) + Math.sqrt(determinate);
            double t2 = -v.dot(d) - Math.sqrt(determinate);
            Vector collision;
            Vector normal;
            
            boolean onSurface=false;
            
            if(ray.onSurfaceOf==this){// && t1 < 0.1){
                if(Math.abs(t1)<0.001){
                    t1=0;
                    onSurface=true;
                }else if(Math.abs(t2)<0.001){
                    t2=0;
                    onSurface=true;
                }
                //t1=0;
            }
            
            if(t1 > 0 && t1 < t2){
                //t1 is in front of the camera, and smaller than t2, therefore this is the one of interest
                collision = s.add(d, t1);
                normal = collision.subtract(pos);
            }else if(t2 >0 && t2 < t1){
                //t2 is in front of the camera and smaller than t1
                collision = s.add(d, t2);
                normal = collision.subtract(pos);
            }else{
                //neither of these are the case, no collision
                return Collision.noCollision;
            }
            
//            if(ray.start.subtract(pos).getMagnitudeSqrd() < r2){
////                //this ray is inside the sphere
//                //not sure if this is needed, doesn't appear to make any difference to the output
//                normal=normal.multiply(-1.0);
//            }
            
            Vector rayStartToCentre = pos.subtract(ray.start);
            
            //if we had surface problems - namely the ray start should be exactly on the surface, but might be slightly inside or slightly outside
            //then work out if we're inside by working out if the ray direction is pointing at the centre of the sphere
            //otherwise, just use the distance from the start of the ray to the cetnre of the shape
            boolean inside = onSurface ? ray.dir.dot(rayStartToCentre) > 0 :  rayStartToCentre.getMagnitudeSqrd() < r2;
            
            
            return new Collision(true, collision, normal.getUnit(),this,inside);
            //less than not equals in case of surface
//            if(ray.start.subtract(pos).getMagnitudeSqrd() < r2){
//                //this ray is inside the sphere
//                
//            }else{
//            
//                //x=s + t*d, therefore value of t nearest zero represents point nearest source of the ray
//                
//                if (Math.abs(t1) < Math.abs(t2)) {
//                    if (t1 <= 0) {
//                        return Collision.noCollision;//this is behind the camera/start of ray
//                    }
//                    collision = s.add(d, t1);
//                    normal = collision.subtract(pos);
//                } else {
//                    if (t2 <= 0) {
//                        return Collision.noCollision;//this is behind the camera/start of ray
//                    }
//                    collision = s.add(d, t2);
//                    normal = collision.subtract(pos);
//                }
//
//                return new Collision(true, collision, normal.getUnit(),this);
//            }
        }



    }

    @Override
    public Surface getSurface() {
        return surface;
    }

//    @Override
//    public Vector getPos() {
//        return pos;
//    }

    @Override
    public Vector getTextureCoords(Vector collision) {
        //TODO make this do something
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
    public ShapeName getShapeName() {
        return ShapeName.Sphere;
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
    
    @Override
    public boolean isConvex() {
        //the sphere can't cast shadows on itself
        return true;
    }
    
    @Override
    public Triangle[] getRealtimeTriangles() {
        //TODO optimise this
        return Poly.getRubbishSphere(surface, pos, r).getRealtimeTriangles();
    }
}
