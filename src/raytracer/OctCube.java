/*
 * Copyright Luke Wallin 2012
 */
package raytracer;

import LukesBits.Vector;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author Luke
 */
public class OctCube  implements Serializable{
    public Vector pos;
    public double size,hSize,qSize;
    public OctCube[] children;
    public ArrayList<OctObject> objects;
    private int maxPerCube;
    private double minCubeSize;
    private boolean hasChildren;
    
    
    //idea - would int for size speed things up in the slightest?
    //probably not, got FP hardware in x86?
    public OctCube(ArrayList<OctObject> _objects,int _maxPerCube, double _minCubeSize,Vector _pos, double _size){
        pos=_pos;
        size=_size;
        hSize=size/2.0;
        qSize=size/4.0;
        objects=_objects;
        maxPerCube=_maxPerCube;
        minCubeSize=_minCubeSize;
        hasChildren=false;
        
        
        if(objects.size() > maxPerCube && size > minCubeSize){
            hasChildren=true;
            children = new OctCube[8];
            //divide further
            for(int i=0;i<8;i++){
                
                double xCoef = (int)(i&0x1) > 0 ? 1.0 : -1.0;
                double yCoef = (int)((i>>1)&0x1) > 0 ? 1.0 : -1.0;
                double zCoef = (int)((i>>2)&0x1) > 0 ? 1.0 : -1.0;
                //all the combinations of +ve/-ve coefficients
                //possibly a bit hacky, but couldn't think of naything else quickly
                
                Vector cubePos = pos.add(new Vector(qSize*xCoef,qSize*yCoef,qSize*zCoef));
                
                ArrayList<OctObject> childObjects = new ArrayList<OctObject>();
                
                for(int j=0;j<objects.size();j++){
                    //this is now a relative vector to the object from the centre of this cube
                    OctObject o =objects.get(j);
                    Vector toObject = o.getPos().subtract(pos);
                    
                    if(toObject.x*xCoef >= 0 && toObject.y*yCoef >= 0 && toObject.z*zCoef >= 0){
                        //this object is going to be in this new cube
                        childObjects.add(o);
                        objects.remove(j);
                        j--;
                    }
                }
                
                children[i] = new OctCube(childObjects, maxPerCube, minCubeSize, cubePos, hSize);
                
            }
            
            //objects = null;
            
        }
    }
    
    //is 'here', with a radius of r, inside this cube?
    //this might return true at times when the answer is actually false
    public boolean thisIsInside(Vector here, double r){
        if(here.x + r >= pos.x-hSize && here.x - r <= pos.x+hSize
         && here.y + r >= pos.y-hSize && here.y - r <= pos.y+hSize
         && here.z + r >= pos.z-hSize && here.z - r <= pos.z+hSize){
            //this treats here as a cube with size 2*r
            return true;
        }
        return false;
    }
    
    public ArrayList<OctObject> nearBy(Vector here, double r){
        
        ArrayList<OctObject> near = new ArrayList<OctObject>();
        
        double r2=r*r;
        if(!hasChildren){
            //this has no children, so it will have objects
            for(OctObject o : objects){
                if(o.getPos().subtract(here).getMagnitudeSqrd() < r2){
                    near.add(o);
                }
            }
        }else
        //if(hasChildren)
        {
            //has children, so will have no objects
            //Vector relative = here.subtract(pos);
            
            for(OctCube c : children){
                if(c.thisIsInside(here, r)){
                    //this intersects this child cube
                    near.addAll(c.nearBy(here, r));
                }
            }
            
            //drat, this won't work since we're interested in nearest with r
            //int child = (relative.x > 0 ? 0x1 : 0x0) | (relative.y > 0 ? (0x1<<1) : 0x0) | (relative.z > 0 ? (0x1<<2) : 0x0);
            
            
            
        }
        
        
        return near;
    }
    
    public int getSize(){
        int count=1;
        if(hasChildren){
            for(OctCube c : children){
                count+=c.getSize();
            }
        }
        
        return count;
    }
}
