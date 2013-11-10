package raytracer;
import LukesBits.Vector;

/**
 *
 * @author Luke
 */
public class Triangle implements Shape{
    
    //vertices
    public Vector v0,v1,v2;
    
    //vectors related to vertices
    public Vector u,v,n;
    
    //normals at each vertex
    public Vector n0,n1,n2;
    
    private Vector surfaceAngle;
    
    private Surface surface;
    
    private double udotv;
    private double udotu;
    private double vdotv;
    
    private boolean square;
    
    private BumpMap bumpMap;
    
    //it makes no sense for the phone shaded shape to be a square, so we don't allow that.
    public Triangle(Surface _surface,Vector _v0,Vector _v1,Vector _v2,Vector _n0,Vector _n1, Vector _n2){
        this( _surface,_v0,_v1,_v2,false);
        setVerticesN(_n0,_n1,_n2);
    }
    
    public Triangle(Surface _surface,Vector _v0,Vector _v1,Vector _v2){
        this(_surface, _v0, _v1, _v2, false);
    }
    
    //my word, this bunch of constructures is a mess.  ah well.
    public Triangle(Surface _surface,Vector _v0,Vector _v1,Vector _v2, boolean _square){
        v0=_v0;
        v1=_v1;
        v2=_v2;
        
        u=v1.subtract(v0);
        v=v2.subtract(v0);
        n=u.cross(v).getUnit();
        
        surface=_surface;//.clone();
        //default surfaceAngle:
        if(n.x==1 && n.y==0 && n.z==0){
            setSurfaceAngle(new Vector(0,0,1));
        }else{
            setSurfaceAngle(new Vector(1,0,0));
        }
        
        udotv = u.dot(v);
        udotu = u.dot(u);
        vdotv = v.dot(v);
        
        square=_square;
    }

    public boolean hasBumpMap(){
        return bumpMap!=null;
    }
    
    public void setBumpMap(BumpMap map){
        bumpMap=map;
    }
    
    public BumpMap getBumpMap(){
        return bumpMap;
    }
    
    public boolean isPhong(){
        return n0!=null;
    }
    
    public Vector[] getNs(){
        return new Vector[]{n0,n1,n2};
    }
    
    /**
     * implemented for real-time only atm
     * @return direction triangle faces
     */
    public Vector getN(){
        return n;
    }
    /**
     * implemented for real-time only atm
     * @return the position of the centre of the triangle
     */
    public Vector getCentre(){
        return v0.add(u,0.5).add(v,0.5);
    }
    
    /**
     * implemented for real-time only atm
     * @return array of vectors of the vertices of this triangle
     */
    public Vector[] getVertices(){
        return new Vector[]{v0,v1,v2};
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
    
    public void setVerticesN(Vector _n0,Vector _n1, Vector _n2){
        n0=_n0.getUnit();
        n1=_n1.getUnit();
        n2=_n2.getUnit();
    }
    
    //for if the triangle is in a polygon, make n point the right way
    public void thisIsInside(Vector centre){
        
        Vector thisWay=v0.subtract(centre);
        
        if(thisWay.dot(n)<0){
            n=n.multiply(-1.0);
        }
        
//        if(n0!=null && thisWay.dot(n0)>0){
//            n0=n0.multiply(-1.0);
//            n1=n1.multiply(-1.0);
//            n2=n2.multiply(-1.0);
//        }
    }
    
    //for if the triangle is in a polygon, make n point the right way
    public void thisIsOutside(Vector centre){
        
        Vector thisWay=v0.subtract(centre);
        
        if(thisWay.dot(n)>0){
            n=n.multiply(-1.0);
        }
//        if(n0!=null && thisWay.dot(n0)>0){
//            n0=n0.multiply(-1.0);
//            n1=n1.multiply(-1.0);
//            n2=n2.multiply(-1.0);
//        }
    }
    
    @Override
    public Surface getSurface() {
        return surface;
    }
    
    public void setSurface(Surface s){
        surface=s;
    }

    @Override
    public Vector getTextureCoords(Vector collision) {
        Vector relPos = collision.subtract(v0);
        
        //angle is normal to the surface
        //surfaceAngle is along the surface and essentially 'up' for the texture - therefore is yDir
        //xDir, for the texture, is therefore angle crossed with yDir
        
        Vector xDir = surfaceAngle.cross(n).getUnit();
        
        Vector textureCoords = new Vector(relPos.dot(xDir),relPos.dot(surfaceAngle));
        
        return textureCoords;
    }

    @Override
    public Vector getSurfaceAngle(){
        return surfaceAngle;
    }
    
    //this is a little meaningless atm, but required for getting textures to work
    @Override
    public void setSurfaceAngle(Vector _surfaceAngle){
        
        //ensure this is perpendicular to the normal of the plane:
        double dirInNormal = _surfaceAngle.dot(n);
        
        _surfaceAngle=_surfaceAngle.subtract(n,dirInNormal);
        
        surfaceAngle=_surfaceAngle.getUnit();
    }


    //potential TODO, add ray.onSurface detection here, although I can't think of many situations where a refractive triangle
    //wouldn't be part of a poly
    
    @Override
    public Collision collide(Ray ray){
        Vector d=ray.dir;
        Vector s=ray.start;

        if(d.dot(n)==0){
            //the normal and the line are perpendicular - therefore the line is in parralelle with the plane
                return Collision.noCollision;
        }else{
            //not parallel
            //line:  r=s + td
            //t = (point on plane - s ) . n / d.n

            double q=v0.subtract(s).dot(n)/d.dot(n);

            if(q>0){
                //positive q, we're moving from camera to screen, I think this assumption is correct
                Vector p=s.add(d,q);
                
                //now we have a collision with the plane that the triangle is in, is this collision within the triangle?
                //http://softsurfer.com/Archive/algorithm_0105/algorithm_0105.htm
                
                //
                //
                //     v0-----u----->v1
                //       \
                //        \   p
                //         v
                //          \|
                //           v2
                
                // p = s*u + t*v;
                //try and find s and t, if they are between 0and1, p is inside the triangle, if not, it's not.
                
                //another vector in the plane
                Vector w = p.subtract(v0);
                
                double wdotv=w.dot(v);
                double wdotu=w.dot(u);
                
                double denom = udotv*udotv - udotu*vdotv;
                
                double sI = (udotv*wdotv - vdotv*wdotu)/denom;
                if(sI < 0.0 || sI > 1.0){
                    return Collision.noCollision;
                }
                double tI = (udotv*wdotu - udotu*wdotv)/denom;
                
                if(tI < 0.0 || (square ? tI : (tI+sI)) > 1.0){
                    //no collision
                    return Collision.noCollision;
                }
                
                //this was a collision, return as such
                Vector normal;
                
                //TODO, if we collide with the triangle from the wrong side, not return a collision?
                
                if(n0==null && bumpMap==null){// || true
                    //TODO some decent system for turning off phong
                    //if this is a flat triangle, return the only normal.
                    normal=n;
                }else if (bumpMap==null){
                    //this is phong shaded, interpolate the three normals.
                    normal=n0.multiply(1-(tI+sI)).add(n1.multiply(sI)).add(n2.multiply(tI));
                }else{
                    //bump map
                    //todo work out how this properly corrosponds to x and y stuff, and make it work for a non-square
                    normal = bumpMap.getNormalAt(new Vector(sI*u.getMagnitude(),tI*v.getMagnitude()));
                }
                double ndotdir=n.dot(ray.dir);
                
                boolean inside = ndotdir > 0;
               // if(ndotdir <= 0){
                    return new Collision(true,p,inside ? normal.multiply(-1.0) : normal,this,inside);//getParent()
                //}else{
                //    return Collision.noCollision;
                //}
                
               // if(ndotdir <= 0){// || true
                    //also count this as a collision if the surface is clear - we want to find the collision OUT of the shape as well as in for clear!
                    
                    //only count this as a collision if we're going through the triangle in the right direction
                    //this is in an attempt to optimise rendering as there'll be less checking which shape is nearest
                    //this doesn't appear to have made any difference to speed wahtsoever
                    //if we're going in the right direction we are 'outside' the triangle, same as plane's slightly arbitary discision
                //    return new Collision(true,p,normal,this,false);
               // }else if(surface.clear>0){
                    //we are going through the shape backwards, but this is a clear shape so we want this
                    // ndotdir > 0 should always be true, and this shows we're 'inside' the shape
                
                
                //ndotdir > 0 means we are going backwards through this triangle
                
                //regardless of which way through the shape we're going, this will count as a collision
                //return new Collision(true,p, normal,this,  ndotdir > 0);
                
                    
                    
                    //}else{
               //     return Collision.noCollision;
               // }
                
            }
            return Collision.noCollision;
        }
    }

    @Override
    public ShapeName getShapeName() {
        return square ? ShapeName.Square : ShapeName.Triangle;
    }

    @Override
    public boolean isConvex() {
        //the triangle can't cast shadows on itself
        return true;
    }
    
    @Override
    public Triangle[] getRealtimeTriangles() {
        return new Triangle[]{this};
    }
    
}
