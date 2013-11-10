package raytracer;
import LukesBits.Vector;
import java.io.Serializable;
import java.util.ArrayList;
/**
 *
 * @author Luke
 */
public class Plane implements Shape{
    
    public Vector pos;

    //unit Vector
    public Vector angle;
    
    //which way does the texture on the surface face?
    public Vector surfaceAngle;

    public Surface surface;
    
    public Plane(Surface _surface, Vector _pos, Vector _angle){
        surface=_surface;
        pos=_pos;
        angle=_angle;
        
        //default surfaceAngle:
        if(angle.x==1 && angle.y==0 && angle.z==0){
            setSurfaceAngle(new Vector(0,0,1));
        }else{
            setSurfaceAngle(new Vector(1,0,0));
        }
    }
    
    public static ArrayList<Plane> getSkyBox(Surface _surface, Vector _pos,double width,double height,double depth){
        ArrayList<Plane> planes = new ArrayList<Plane>();
        
        Vector x = new Vector(1,0,0);
        Vector y = new Vector(0,1,0);
        Vector z = new Vector(0,0,1);
        
        //above
        planes.add(new Plane(_surface,_pos.add(z,depth/2),z.multiply(-1.0)));
        //below
        planes.add(new Plane(_surface,_pos.subtract(z,depth/2),z));
        //left
        planes.add(new Plane(_surface,_pos.add(x,width/2),x.multiply(-1.0)));
        //right
        planes.add(new Plane(_surface,_pos.subtract(x,width/2),x));
        //top
        planes.add(new Plane(_surface,_pos.add(y,height/2),y.multiply(-1.0)));
        //bottom
        planes.add(new Plane(_surface,_pos.subtract(y,height/2),y));
        
        return planes;//.toArray(new Plane[1]);
    }
    
    @Override
    public Vector getSurfaceAngle(){
        return surfaceAngle;
    }
    
    @Override
    public void setSurfaceAngle(Vector _surfaceAngle){
        
        //ensure this is perpendicular to the normal of the plane:
        double dirInNormal = _surfaceAngle.dot(angle);
        
        _surfaceAngle=_surfaceAngle.subtract(angle,dirInNormal);
        
        surfaceAngle=_surfaceAngle.getUnit();
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
    
    //n.(r - r0) = 0
    //n= normal to plane
    //r0=point on the plane
    //r=vector

    @Override
    public Collision collide(Ray ray){
        Vector d=ray.dir;
        Vector s=ray.start;

        if(d.dot(angle)==0){
            //the normal and the line are perpendicular - therefore the line is in parralelle with the plane

            //does a point on the line line on the plane?
            //if(this.angle.dot(s.minus(d))==0){
            //    return array(s,this.angle);
                //plane normal . (
           // }else{
                return Collision.noCollision;
            //}
        }else{
            //not parallel
            //line:  r=s + td
            //t = (point on plane - s ) . n / d.n

            double t=pos.subtract(s).dot(angle)/d.dot(angle);

            if((t>0  && ray.onSurfaceOf!=this) || (t>0.001 &&ray.onSurfaceOf==this)){
                //positive t, we're moving from the start of the ray to this point
                //however if we're on the surface of this plane, we have to be a little way away - this is the littlebodge
                Vector collision=s.add(d,t);
                //if we're 'behind' the plane, count this as inside
                boolean inside = ray.start.dot(this.angle) < 0;
                return new Collision(true,collision,angle,this,inside);
            }
            return Collision.noCollision;
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
        Vector relPos = collision.subtract(pos);
        
        //angle is normal to the surface
        //surfaceAngle is along the surface and essentially 'up' for the texture - therefore is yDir
        //xDir, for the texture, is therefore angle crossed with yDir
        
        Vector xDir = surfaceAngle.cross(angle).getUnit();
        
        Vector textureCoords = new Vector(relPos.dot(xDir),relPos.dot(surfaceAngle));
        
        return textureCoords;
    }

    @Override
    public ShapeName getShapeName() {
        return Shape.ShapeName.Plane;
    }
    
    @Override
    public boolean isConvex() {
        //the triangle can't cast shadows on itself
        return true;
    }
    
    @Override
    public Triangle[] getRealtimeTriangles() {
        return new Triangle[0];
    }
}
