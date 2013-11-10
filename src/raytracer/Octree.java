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
public class Octree  implements Serializable{
    
    private ArrayList<OctObject> objects;
    private int maxPerCube;
    private double minCubeSize;
    private OctCube cube;
    
    public Octree(ArrayList<OctObject> _objects,int _maxPerCube, double _minCubeSize){
        objects=_objects;
        maxPerCube=_maxPerCube;
        minCubeSize = _minCubeSize;
        
//        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE;
//        int minY=Integer.MAX_VALUE,maxY=Integer.MIN_VALUE;
//        int minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
        double minX = Double.MAX_VALUE,maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE,maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE,maxZ = -Double.MAX_VALUE;
        
        //find furthest away objects and design size of tree with that
        for(OctObject o : objects){
            if(o.getPos().x < minX){
                minX=o.getPos().x;
            }
            if(o.getPos().y < minY){
                minY=o.getPos().y;
            }
            if(o.getPos().z < minZ){
                minZ=o.getPos().z;
            }
            
            if(o.getPos().x > maxX){
                maxX=o.getPos().x;
            }
            if(o.getPos().y > maxY){
                maxY=o.getPos().y;
            }
            if(o.getPos().z > maxZ){
                maxZ=o.getPos().z;
            }
        }
        
        //now have min and max x,y,z;
        double sizeX=maxX-minX;
        double sizeY=maxY-minY;
        double sizeZ=maxZ-minZ;
        
        //don't know how good or bad plonking the centre here will be
        Vector pos = new Vector((maxX-minX)/2.0,(maxY-minY)/2.0,(maxZ-minZ)/2.0);
        double size = Math.max(Math.max(sizeX,sizeY),sizeZ);
        
        pos = new Vector(0,0,-100);
        size*=2;
        
        cube=new OctCube(objects, maxPerCube,minCubeSize, pos, size);
    }
    
    public int getSize(){
        return cube.getSize();
    }
    
    public Photon[] nearBy(Vector here, double r){
        ArrayList<OctObject> photons = cube.nearBy(here, r);
        
        
        return photons.toArray(new Photon[0]);
    }
    
}
