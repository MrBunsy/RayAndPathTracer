package raytracer;
import LukesBits.Vector;
import java.io.Serializable;

/**
 *
 * @author Luke
 */
public class Screen{
    public Vector topLeft,topRight,bottomLeft,bottomRight;
    //of the image:
    //public width;
    //public height;
    public double ratio;//height/width

    //todo, replace this with camera distance and direction
    public Screen(double _ratio){
        ratio=_ratio;
        //this.width=width;
        //this.height=height;
    }

    public void setManually(Vector _topLeft,Vector _bottomLeft,Vector _topRight,Vector _bottomRight){
        topLeft=_topLeft;
        topRight=_topRight;
        bottomLeft=_bottomLeft;
        bottomRight=_bottomRight;
    }

    public void setFromCamera(Camera camera,Vector dir,double distance,double size){//size=100 default


        //dir=direction camera is facing, distance is distance of screen
        //ratio=this.ratio;
        //of the screen:
        double width=size;
        double height=size*ratio;

        //v2 is horizontal vector
        //v1 is the other vector

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

        /*v1y=1;

        v1z=-(n.x*n.x + n.y*n.y)/(n.z*n.y);

        //if(v1z<0){
         //   v1z*=-1;
        //}

         v1x=n.x/n.y;

         */

        double v1z=1;//want this to face upwards where up is +ve z

        double v1y=-(n.z*n.y)/(n.x*n.x + n.y*n.y);

        double v1x=v1y*n.x/n.y;

        double v2x=1;
        double v2y=-n.x/n.y;
        double v2z=0;//horizontal!

        Vector v1=new Vector(v1x,v1y,v1z);
        v1=v1.getUnit().multiply(height/2);
        Vector v2=new Vector(v2x,v2y,v2z);
        v2=v2.getUnit().multiply(width/2);
        

        //centre of the screen1
        Vector centre=camera.pos.add(n,distance);

        this.topLeft=centre.subtract(v2).add(v1);
        this.topRight=centre.add(v2).add(v1);

        this.bottomLeft=centre.subtract(v2).subtract(v1);
        this.bottomRight=centre.add(v2).subtract(v1);

        //exit("v1: v1, v2: v2<Br>TL: this.topLeft "."TR: this.topRight "."BL: this.bottomLeft "."BR: this.bottomRight ");

    }
}
