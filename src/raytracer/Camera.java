package raytracer;
import LukesBits.Vector;
import java.io.Serializable;

/**
 *
 * @author Luke
 */
public class Camera{
    public Vector pos, dir;
    public double lensDistance,lensSize;
    //below used for realtime:
    private int xPixels,yPixels;
    private Vector xDir,yDir,screenTopLeft;
    private double width,height;

    public Camera(Vector _pos,Vector _dir,double _lensDistance,double _lensSize){
        pos=_pos;
        dir=_dir.getUnit();
        lensDistance=_lensDistance;
        lensSize=_lensSize;
    }
    /**
     * taken from javaTree
     * @param _xPixels
     * @param _yPixels 
     */
    //pre-compute some stuff for a certain resolution
    public void setupRez(int _xPixels, int _yPixels){
        xPixels=_xPixels;
        yPixels=_yPixels;
       setupScreenCorners((double)yPixels/(double)xPixels);
        
    }
    /**
     * taken from java tree
     * @param threeD
     * @return 
     */
    public Vector ThreeDTo2D(Vector threeD){
        
        //project line from position in world to camera
        //find out where this line intersects with the screen
        
        Vector s = threeD;
        //line from threeD to camera = t*l + l0
        Vector d = pos.subtract(s);
        //TODO check for if d is now zero
        //if(d.)
        
        if(dir.dot(d)==0){
            //the normal and the line are perpendicular - therefore the line is in parralelle with the plane

            //does a point on the line line on the plane?
            //if($this->angle->dot($s->minus($d))==0){
            //    return array($s,$this->angle);
                //plane normal . (
           // }else{
                //return null;new double[]{-1,-1};
            return new Vector(0,0,0);
            //}
        }else{
            //not parallel
            //line:  r=s + td
            //t = (point on plane - s ) . n / d.n

            double t=screenTopLeft.subtract(s).dot(dir)/d.dot(dir);

            //collision point
            Vector c=s.add(d.multiply(t));
            
            
            Vector screenCollision=c.subtract(screenTopLeft);
            
            //how far along top of screen
            double x = xDir.dot(screenCollision);
            double y = yDir.dot(screenCollision);
            
            x = (x/width)*xPixels;
            y = (y/height)*yPixels;
            
            return new Vector(x,y);
        
        //idea: find vector from top left corner of screen to collision point.  then get values for top of screen dot with this, and side of screen dot with this
        //this will give how far along each 'axis' of the screen the ocllision point is, and this can be scaled to x,y coords
        }
        
        //return new Vector(0,0);
    }
    /**
     * used for realtime, adapted from stuff in javatree, bit of a bodge
     * @param ratio 
     */
    public void setupScreenCorners(double ratio){
        
        height=lensSize;
        width = lensSize/ratio;
                
        Vector[] corners = getScreenCorners(ratio);
        
        screenTopLeft=corners[0];
        xDir=corners[3].subtract(corners[0]).getUnit();
        yDir=corners[1].subtract(corners[0]).getUnit();
    }
    /**
     * Returns unit vectors which point along the x and y coords of the screen
     * @return [x,y,screenDir]
     */
    public Vector[] getScreenDirections(){
        Vector n=dir.getUnit();

        //massive hack - should probably work through the maths and find the special cases.
        if(n.x==0){
            n.x=0.00001;
        }

        if(n.y==0){
            n.y=0.00001;
        }

        if(n.z==0){
            n.z=0.00001;
        }

        double v1z=1;//want this to face upwards where up is +ve z

        double v1y=-(n.z*n.y)/(n.x*n.x + n.y*n.y);

        double v1x=v1y*n.x/n.y;

        double v2x=1;
        double v2y=-n.x/n.y;
        double v2z=0;//horizontal!

        Vector v1=new Vector(v1x,v1y,v1z);
        
        Vector v2=new Vector(v2x,v2y,v2z);
        


        //HACK from javaTree, slightly tweaked, *think* this works
        //note: does need to be greater than *or equal*
        if(dir.y >=0 ){
            v2=v2.multiply(-1.0);
        }
        
        return new Vector[]{v2.getUnit(),v1.getUnit(),n};
    }
    
    public Vector[] getScreenCorners(double ratio){
        //dir=direction camera is facing, distance is distance of screen
        
        //of the screen:
        double width=lensSize;
        double height=lensSize*ratio;

        //v2 is horizontal vector
        //v1 is the other vector
        Vector[] dirs = getScreenDirections();
        
        Vector v1=dirs[1];
        Vector v2=dirs[0];
        Vector n = dirs[2];
        
        v1=v1.multiply(height/2);
        v2=v2.multiply(width/2);
        
        //centre of the screen1
        Vector centre=pos.add(n,this.lensDistance);

        Vector topLeft=centre.subtract(v2).add(v1);
        Vector topRight=centre.add(v2).add(v1);

        Vector bottomLeft=centre.subtract(v2).subtract(v1);
        Vector bottomRight=centre.add(v2).subtract(v1);

        return new Vector[]{topLeft,bottomLeft,bottomRight,topRight};
    }
}
