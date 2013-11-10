package raytracer;

import LukesBits.Vector;
import java.util.ArrayList;

/**
 *
 * @author Luke
 */
public class PolyCylinder extends Poly{
    
    private Vector centre;
    //private Vector[] topPoints;
    private Vector[][] points;
    private Vector[] normals;
    
    private int rings,segments;
    
    public Vector[] getTopPoints(){
        return points[rings].clone();
    }
    
    public Vector[][] getPoints(){
        Vector[][] pointsClone = new Vector[rings+1][segments];
        
        for(int i=0;i<rings+1;i++){
            pointsClone[i]=points[i].clone();
        }
        
        return pointsClone;
    }
    
    public Vector[] getNormals(){
        return normals.clone();
    }
    
    //same radius whole way
    public PolyCylinder(Surface _surface, Vector base, Vector dir, double h, double r,int segments, int rings){
        this(_surface, base, dir, h, r, r, segments, rings, null,true,false,null);
    }
    
    public PolyCylinder(Surface _surface, Vector base, Vector dir, double h, double topr, double bottomr,int segments, int rings){
        this(_surface, base, dir, h, topr, bottomr, segments, rings, null,false,false,null);
    }
    
    public PolyCylinder(Surface _surface, Vector base, Vector dir, double h, double topr, double bottomr,int segments, int rings,Vector baseAngle, boolean capped, PolyCylinder extendMe){
        this(_surface, base, dir, h, topr, bottomr, segments, rings, baseAngle, capped, false, extendMe);
    }
    
    public PolyCylinder(Surface _surface, Vector base, Vector dir, double h, double topr, double bottomr,int segments, int rings,Vector baseAngle, boolean capped, boolean noTop){
        this(_surface, base, dir, h, topr, bottomr, segments, rings, baseAngle, capped, noTop, null);
    }
    
    //different radii at top and bottom
    //note - the points are generated bottom up
    //notop means that if it's capped the top won't be there
    public PolyCylinder(Surface _surface, Vector base, Vector dir, double h, double topr, double bottomr,int _segments, int _rings,Vector baseAngle, boolean capped, boolean noTop, PolyCylinder extendMe){
        //TODO better encasing
        
        super(_surface,new Triangle[0],true);
        
        rings=_rings;
        segments=_segments;
        
        //grr @ java, can't do this before super
        
        //if extendMe is a polyCylinder, then the base of this cylinder wants to be the top points of that cylinder
        Vector[] bottomPoints = null;
        Vector[] bottomNormals = null;
        
        if(extendMe!=null){
            bottomPoints=extendMe.getTopPoints();
            bottomNormals=extendMe.getNormals();
        }
        
        //this also sets a bunch of stuff, should probably tidy it up
        triangles=getCylinderTriangles(_surface, base, dir, h, topr, bottomr, segments, rings, baseAngle, capped,bottomPoints,bottomNormals,noTop);
        setEncasingRadius(centre, Math.max(bottomr,topr)+h/2);
        
        
        for(Triangle t : triangles){
            t.setParent(this);
        }
        
        //return poly;
    }
    
    private Triangle[] getCylinderTriangles(Surface _surface, Vector base, Vector dir, double h, double topr, double bottomr,int segments, int rings,Vector baseAngle, boolean capped, Vector[] bottomPoints, Vector[] bottomNormals, boolean noTop ){
        ArrayList<Triangle> _triangles = new ArrayList<Triangle>();
        
        if(dir.equals(baseAngle)){
            baseAngle=null;
        }
        
        double dTheta = 2*Math.PI/(double)segments;
        //double dAlpha = Math.PI/(double)rings;
        
        double[] thetas = new double[segments];
        //double[] alphas = new double[rings+1];
        double dH = h/(double)rings;
        double dR = (topr-bottomr)/(double)rings;
        
        dir=dir.getUnit();
        
        Vector top = base.add(dir.multiply(h));
        Vector bottom = base;
        centre = base.add(dir.multiply(h/2));
        
        
        //x and y are in the plane of hte base and top
        //z is in the dir direction
        Vector xDir;
        if(baseAngle==null){
            xDir = dir.predictableNormal();
        }else{
            //if we have a baseAngle, use this to calculate xDir so the x axis will be same for top and bottom
            xDir = baseAngle.predictableNormal();//dir.cross(baseAngle).getUnit();
        }
        Vector yDir = xDir.cross(dir).getUnit();
        
        //number of lines on contours = number of rings, so number of devisions is rings+1 which accoutns for the top/bottom
        //number of lines lengthways = number of segments, because these wrap around.
        
        //Vector[][] 
        points = new Vector[rings+1][segments];
        normals = new Vector[segments];
        
        //topPoints = new Vector[segments];
        
        for (int i=0;i<segments;i++){
            thetas[i]=dTheta*i;
        }
        
        Vector segmentDir;
        for(int i=0;i<segments;i++){
            
            //points along the base plane towards the segment
            segmentDir = xDir.multiply(Math.cos(thetas[i])).add(yDir.multiply(Math.sin(thetas[i])));//new Vector(Math.cos(thetas[i]),Math.sin(thetas[i]),0);
            normals[i]=segmentDir.getUnit();
            
            for(int j=0;j<rings+1;j++){

                double r = bottomr + (double)j*dR;
                points[j][i]=base.add(segmentDir,r).add(dir,(double)j*dH);
//                if(j==rings){
//                    //toppoints just holds the points at teh top of this cylinder
//                    topPoints[i]=points[i][rings];
//                }
                if(j==0 && bottomPoints!=null){
                    //use the toppoints from a different cylinder
                    points[0][i]=bottomPoints[i];
                }
            }
        }
        
        //realised this doesn't achieve even slightly what I want
        //for the tree, the baseradius needs to be exactly the same as the parent branch
        //TODO - base circle is just the bottomR circle in the dir of baseAngle
        //this will mean it's not a real cylinder, but should work fine for branches
        //if bottomPoints have been provided, they overrides this
        if(bottomPoints == null && baseAngle!=null){
            baseAngle=baseAngle.getUnit();
            
            Vector newYDir = xDir.cross(baseAngle).getUnit();
            
            if(newYDir.dot(yDir)<0){
                newYDir=newYDir.multiply(-1.0);
            }
            
             for(int i=0;i<segments;i++){
            
                //points along the base plane towards the segment
                segmentDir = xDir.multiply(Math.cos(thetas[i])).add(newYDir.multiply(Math.sin(thetas[i])));//new Vector(Math.cos(thetas[i]),Math.sin(thetas[i]),0);
                //normals[i]=segmentDir.getUnit();

                    points[0][i]=base.add(segmentDir,bottomr);

            }
            
//            Vector inBothPlanes = dir.cross(baseAngle);
//            double tanTheta = inBothPlanes.getMagnitude()/dir.dot(baseAngle);
//            inBothPlanes=inBothPlanes.getUnit();
//            
//            Vector alongBase = inBothPlanes.cross(baseAngle);
//            
//            for(int i=0;i<segments;i++){
//                //
//                double fakeR = bottomr*alongBase.dot(normals[i]);
//                if(fakeR==0){
//                    continue;
//                }
//                
//                double x = fakeR*tanTheta;
//                
//                //extend down in dir by x
//                points[i][0]=points[i][0].add(dir,x);
//            }
            
        }
        
        
        for(int i=0;i<segments;i++){
            //top
            if(capped && !noTop){
                _triangles.add(new Triangle(_surface,top,points[rings][i],points[rings][((i+1)%segments)]));
            }
            
            
            
            for(int j=0;j<rings;j++){
                //middle
                if(bottomNormals==null){
                    //top half of this square
                    _triangles.add(new Triangle(_surface,points[j][i],points[j+1][i],points[j][((i+1)%segments)],normals[i],normals[i],normals[((i+1)%segments)]));

                    //bottom half of this square
                    _triangles.add(new Triangle(_surface,points[j+1][((i+1)%segments)],points[j+1][i],points[j][((i+1)%segments)],normals[((i+1)%segments)],normals[i],normals[((i+1)%segments)]));
                }else{
                    //using different surface normals for the bottom of the branch
                    //todo maybe multiple rings here would be a good thing?
                    _triangles.add(new Triangle(_surface,points[j][i],points[j+1][i],points[j][((i+1)%segments)],bottomNormals[i],normals[i],bottomNormals[((i+1)%segments)]));

                    //bottom half of this square
                    _triangles.add(new Triangle(_surface,points[j+1][((i+1)%segments)],points[j+1][i],points[j][((i+1)%segments)],normals[((i+1)%segments)],normals[i],bottomNormals[((i+1)%segments)]));
                }
            }
            if(capped){
                //bottom
                _triangles.add(new Triangle(_surface,bottom,points[0][i],points[0][((i+1)%segments)]));
            }
        }
        //set all the surface normals for the triangle
        for(Triangle t :_triangles){
            t.thisIsInside(centre);
        }
        
        return _triangles.toArray(new Triangle[1]);
    }
    
}
