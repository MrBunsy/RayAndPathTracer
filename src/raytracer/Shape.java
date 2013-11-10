package raytracer;
import LukesBits.Vector;

/**
 *
 * @author Luke
 */
public interface Shape {

    public enum ShapeName{Sphere,Triangle,Poly,Square,Plane};
    
    public Surface getSurface();

    //public Vector getPos();
    //from the collision point, get an x and y coordinate which will map to the texture.
    public Vector getTextureCoords(Vector collision);

    //unsure if these two should be part of the interface or not
    public Vector getSurfaceAngle();

    public void setSurfaceAngle(Vector _surfaceAngle);

    public Collision collide(Ray ray);
    
    public ShapeName getShapeName();
    
    //still uncertain whether we want this one:
    public Shape getParent();
    
    public void setParent(Shape _parent);
    
    //if this shape can block itself, eg cast a shadow on itself, it's not convex
    public boolean isConvex();
    
    public Triangle[] getRealtimeTriangles();
}
