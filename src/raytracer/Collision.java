package raytracer;
import LukesBits.Vector;

/**
 *
 * @author Luke
 * 
 * this class has been created when porting to java, if problems occur look here
 * 
 */
public class Collision {
    public boolean collide;
    public Vector where;
    //think this is the shape normal
    public Vector normal;
    public Shape with;
    //is this collision taking place inside 'with'?
    public boolean inside;
    
//    Collision(boolean _collide,Vector _where, Vector _normal){
//        this(_collide, _where, _normal,null);
//    }
    
    Collision(boolean _collide,Vector _where, Vector _normal, Shape _with, boolean _inside){
        collide=_collide;
        where=_where;
        normal = _normal;
        with=_with;
        inside=_inside;
    }
    
//    public static Collision noCollision(){
//        return new Collision(false,null,null);
//    }
    public static Collision noCollision = new Collision(false,null,null,null,false);
    
}
