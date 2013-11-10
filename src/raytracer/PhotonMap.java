package raytracer;

import LukesBits.Vector;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Luke
 */
public class PhotonMap implements Serializable{

    //don't want the whole world stored with the photon map if it's serialized (not least because bits of it can't be)
    //TODO this doens't actually need a reference to the world object, it's only used for creation
    private transient World world;
    private ArrayList<Photon> photons;
    //density of fired points and the radius used to detect points afterwards
    private double density, radius, radius2;
    private int threads,maxThreads;
    
    private Octree octree;

    public PhotonMap(World _world, double _density, double _radius, int _maxThreads) {
        world = _world;
        density = _density;
        radius = _radius;
        radius2 = radius * radius;

        maxThreads=_maxThreads;
        threads=0;
        
        photons = new ArrayList<Photon>();
        System.out.println("Building photon map");
        long startTime = System.currentTimeMillis();
        generateMap();
        System.out.println("\rBuilding Octree...                                                                  ");
        //System.out.println("\rGenerating OctTree                                                                   ");
        //must be a neater way...
        ArrayList<OctObject> octObjects = new ArrayList<OctObject>();
        for(Photon p : photons){
            octObjects.add(p);
        }
        
        octree = new Octree(octObjects, 100, radius);
        
        
        long end = System.currentTimeMillis();

        long time = end - startTime;
        
        //System.out.println("\rTook: "+RayTracer.tidyTime(time)+"                                                  ");
        System.out.println("Took: "+RayTracer.tidyTime(time));
        //int treeSize=octree.getSize();
    }

    public Photon getLightHere(Vector pos) {
        double intensity = 0;
        Vector dir = new Vector(0, 0);
        Colour colour = new Colour(0, 0, 0);
        
        Photon[] photonsNear = octree.nearBy(pos, radius);
        //using the octree to find nearby photons
        
        if(photonsNear.length == 0){
            return new Photon(pos,new Vector(0,0,0),0,new Colour(0,0,0));
        }
        
        int r=0,g=0,b=0;
        
        for (Photon p : photonsNear) {
           // if (p.pos.subtract(pos).getMagnitudeSqrd() < radius2) {
                intensity += p.intensity;
                dir = dir.add(p.dir);
                colour = colour.add(p.colour);
                r+=p.colour.r;
                g+=p.colour.g;
                b+=p.colour.b;
           // }
        }
        
        //intensity *= 1.0/(radius2 + density * density);// * 10;
        //intensity/= Math.PI*radius2*density;
        //intensity/=;
        
        double p=(double)photonsNear.length;
        //trying new idea of averaging them all
        colour=new Colour((int)Math.round((double)r/p),(int)Math.round((double)g/p),(int)Math.round((double)b/p));
        //dir=dir.multiply(1.0/p);
        dir = dir.getUnit();
        intensity /=Math.PI*radius2*density*0.7;//I need to work out why this is dimmer than it should be
        
        //this seems to work quite well^^, but doesn't take into account the dimming of distance
        //I think it *does*, if the rays disperse it gets darker - which is really why it gets darker over distance
        
        //look at the current density of light and alter brightness accordingly
//        double newDensity = 1.0/(Math.PI*radius2);
//        intensity *= newDensity/density;
        
//        if(intensity > 1){
//            double blah = 3.0;
//        }
        
        //funnily neough, this now has an almost identical effect of just adding them up and dividing by Math.PI*radius2*density
        return new Photon(pos,dir.getUnit(),intensity,colour);//intensity
        
        //return new Photon(pos, dir.getUnit(), intensity, colour);
    }

    private void generateMap() {

       
        
        Vector xDir = new Vector(0, 0), yDir = new Vector(0, 0), zDir = new Vector(0, 0);
        if (world.isSunlight()) {
            //there is sunlight

            //we are looking at the shape as if the sunlight angle were our z axis
            zDir = world.getSunlight().getAngle();
            xDir = zDir.predictableNormal();
            yDir = xDir.cross(zDir).getUnit();

        }

        ArrayList<Shape> releventShapes = new ArrayList<Shape>();
        
        for (Shape shape : world.scene) {
            if (shape.getSurface().clear > 0 || shape.getSurface().reflective > 0) {
                //for a shape we're interested in (just refractive and reflective atm)
                releventShapes.add(shape);
            }
        }
        
        int i=0;
        RayTracer.printProgBar(0);
        for(Shape shape : releventShapes){
               
                if (world.isSunlight()) {

                    //if (s.getClass().equals(Sphere.class)) {
                    switch(shape.getShapeName()){
                        case Sphere:
                            {
                                //this is a sphere
                                Sphere sphere = (Sphere) (shape);
                                sunlightGrid(shape, sphere.pos.add(zDir, sphere.r + 0.1), xDir, yDir, zDir, sphere.r * 2, sphere.r * 2);
                            }
                            break;
                        case Poly:
                            {
                                Sphere sphere = (Sphere) ((IPoly)shape).getEncasingSphere();
                                sunlightGrid(shape, sphere.pos.add(zDir, sphere.r + 0.1), xDir, yDir, zDir, sphere.r * 2, sphere.r * 2);
                            }
                            break;
                            //}else if(s.getClass().equals(Triangle.class)){
                        //this probably isn't needed much..
                    //}
                    }

                }
                
                for(Light l : world.lights){
                    switch(shape.getShapeName()){
                        case Sphere:
                        {
                            Sphere sphere = (Sphere) (shape);
                            lightGrid(shape, sphere.pos,l, sphere.r);
                        }
                            break;
                        case Poly:
                        {
                            Sphere sphere = (Sphere) ((IPoly)shape).getEncasingSphere();
                            lightGrid(shape, sphere.pos,l, sphere.r);
                        }
                            break;
                    }
               // }
            }
                
                 i++;
                 RayTracer.printProgBar((double)i/(double)releventShapes.size());
               
        }
        
        
    }

//    private sunLightGridForSphere(Sphere sphere){
//        sunlightGrid(s, sphere.pos.add(zDir, sphere.r + 0.1), xDir, yDir, zDir, sphere.r * 2, sphere.r * 2);
//    }
    
    //fire photons down at the shape from the light
    //this grid will be circular so an even distribution of points on a sphere can be used
    private void lightGrid(Shape shape, Vector pos, Light light, double r){
        /*
         * old php raytracer:
         * $z = $this->halfRandom();
            $t = $this->piRandom();

            $r = 1 - sqrt(1 - $z * $z);

            $x = $r * cos($t);
            $y = $r * sin($t);
         */
        //http://mathworld.wolfram.com/SpherePointPicking.html
        
        //from light to the object, different to sunlight I think
        Vector zDir = pos.subtract(light.pos).getUnit();
        //these are in the plane that is perpendicular to the light source's line to the centre of the shape
        Vector xDir=zDir.predictableNormal();
        Vector yDir=zDir.cross(xDir).getUnit();
        
        //they will be used to randomly distribute points from the lightsource down to the shape by
        //evenly distributing them on the bottom of a sphere
        
        //point at pos+r in the xDir,
        Vector pointAtFurthest = pos.add(xDir,r);
        //a vector from the light source pointed at this furthest point
        Vector lightMostBend = pointAtFurthest.subtract(light.pos).getUnit();
        //the zvalue of this is the min value of z we want to use when spewing rays, that way all hte rays spewed
        //will be pointing at the circle pos pos with radius r
        //hopefully
        
        double minZ = lightMostBend.dot(zDir);
        double zRange=1.0-minZ;
        
        int numRays = (int)Math.round(Math.PI*r*r*density);
        int firedRays=0;
        
        Random random = new Random(123);
        
        ArrayList<Vector> surfacePoints = new ArrayList<Vector>();
        ArrayList<Ray> rays = new ArrayList<Ray>();
        
        while(firedRays < numRays){
            firedRays++;
            double z=minZ+random.nextDouble()*zRange;
            
            double theta = random.nextDouble()*Math.PI*2;
            
            double x = Math.sqrt(1-z*z)*Math.cos(theta);
            double y = Math.sqrt(1-z*z)*Math.sin(theta);
            
            Vector rayDir = zDir.multiply(z).add(xDir,x).add(yDir,y);
            
            Ray ray = new Ray(light.pos,rayDir);//new Vector(x,y,z));
            
            Collision c = shape.collide(ray);

                //this ray from the grid hits the shape we're interested in, where on the shape it hits
                if (c.collide) {
                    //surfacePoints[count]=c.where;
                    //count++;
                    surfacePoints.add(c.where);
                    rays.add(ray);
                    // surfacePoints.add(r.start);
                }
        }
        
        for (int i=0;i<surfacePoints.size();i++) {
            if (Render.isLit(world,shape, rays.get(i))) {
                //if this is lit, use it to cast a ray
                //adding minvalue because a point on the surface won't count as colliding with the surface
                fireRay(rays.get(i), light.lightAtDistanceSqrd(rays.get(i).start.subtract(surfacePoints.get(i)).getMagnitudeSqrd()),  1.0, light.colour, world.iterate);
            }
        }
    }
    
    //fire photons negatively down the zdir at shape
    //sunlight grid is just a square grid atm, as this seems easiest
    private void sunlightGrid(Shape shape, Vector pos, Vector xDir, Vector yDir, Vector zDir, double width, double height) {

        ArrayList<Vector> surfacePoints = new ArrayList<Vector>();

        //first, from above the shape (pos) fire the photons at the shape so as to hit its surface
        //int count=0;
        
        double addThis = 1.0/Math.sqrt(density);
        //int rays=0;
        for (double x = -width / 2.0; x <= width / 2.0; x += addThis) {
            for (double y = -height / 2.0; y <= height / 2.0; y += addThis) {
        //        rays++;
//        int firedRays=0;
//        int numRays = (int)Math.round(width*height*density);
//        Random random = new Random(123);
//        
//        while(firedRays < numRays){
//            firedRays++;
//            
//            double x = (random.nextDouble()-0.5)*width;
//            double y = (random.nextDouble()-0.5)*height;
//            
            Ray r = new Ray(pos.add(xDir, x).add(yDir, y), zDir.multiply(-1.0));

            Collision c = shape.collide(r);

            //this ray from the grid hits the shape we're interested in, where on the shape it hits
            if (c.collide) {
                //surfacePoints[count]=c.where;
                //count++;
                surfacePoints.add(c.where);
                // surfacePoints.add(r.start);
            }
      //  }
            }
        }

        //double shouldRays=width*height*density;
        
        //second, check that this point of the surface is actually sunlit
        for (Vector v : surfacePoints) {
            if (Render.isSunlit(world,v, shape)) {
                //if this is sunlit, use it to cast a ray
                //adding minvalue because a point on the surface won't count as colliding with the surface
                fireRay(new Ray(v.add(zDir, 0.1), zDir.multiply(-1.0)), world.getSunlight().getBrightness(),  world.getAirN(), world.getSunlight().getColour(), world.iterate);
            }
        }

        //third, if it's sunlight, take the positions that are the surface of the shape and then fire these down the negative zDir for the photon mapping


    }
    

    //thisShape is the shape that we're firing the extra light at - we want to ignore it as a possible endpoint for the photons
    //otherwise it is effectively lit twice
    //ray is pointing at the shape
    //idea: detect when intensity is too low to be bothered about?
    private void fireRay(Ray ray, double intensity,  double currentN, Colour colour,  int iteration) {//Light light,

        if(iteration <=0 || intensity < 0.002){
            //0.002*255=0.51, smallest intensity taht makes a difference wit hteh current 8bit colour
            return;
        }

        Collision collision = Render.getNearestCollision(world, ray);//, new ArrayList<Shape>());
        if (collision.collide ) {

            double clear = collision.with.getSurface().clear;
            double reflective = collision.with.getSurface().reflective;

//            if (light != null) {
//                //reduce intensity based on distance
//                //todo check that the intensity given at first is calculated for the start of the ray
//                //intensity = intensity / collision.where.subtract(ray.start).getMagnitudeSqrd();
//            }
            
            //quick hack
            //this shows I *think* that even if the rays go straight through the shape there is a problem
//            if(clear > 0){
//                Ray newRay = new Ray(collision.where,ray.dir);
//                
//                newRay.onSurfaceOf=collision.with;
//                fireRay(newRay,intensity,currentN,colour,iteration-1);
//                return;
//            }
            
            
            if (clear < 1 && reflective < 1){// && collision.with!=thisShape) {
                //TODO allow caustics from one object to affect another object
                //this is something the photon has hit, stop!
                //double intensity = sunlight ? world.getSunlight().getBrightness() : 1;

                photons.add(new Photon(collision.where, ray.dir.multiply(-1.0), intensity * (1 - Math.min(1, clear + reflective)), colour));

            }
            if (clear > 0) {
                //glass :D

                //code yoinked from render
                double n1, n2;
//                if (!collision.inside) {
//                    //going from outside INTO a clear thing
//                    n1 = currentN;//air
//                    n2 = collision.with.getSurface().n;
//                } else {
//                    n1 = collision.with.getSurface().n;
//                    n2 = currentN;
//                }
                
                n1 = currentN;//formerlly always air
                //if we are inside a shape, the other n is the outside world, otherwise we are in the outside world and the other n is the surface of the shape
                n2 = collision.inside ? (collision.with.getSurface().hasOutsideN() ? collision.with.getSurface().getOutsideN() : world.getAirN()) : collision.with.getSurface().n;

                Refraction answers = Render.refraction(n1, n2, collision.normal, ray.dir);
                Vector refract = answers.refract;
                Vector reflect = answers.reflect;
                double T = answers.T;
                double R = answers.R;

//                if(R>0.000001){
//                    boolean blah = true;
//                }
                
                //hack?
//                if(T>1){
//                    T=1;
//                }
//                if(T<0){
//                    T=0;
//                }
//                
//                if(R > 1){
//                    R=1;
//                }
//                
//                if(R<0){
//                    R=0;
//                }
                
                if(!answers.tir()){
                    //total internal reflection means no refracted ray
                    Ray refractedRay = new Ray(collision.where, refract);//.add(refract, 0.001)
                    refractedRay.onSurfaceOf=collision.with;
                    fireRay(refractedRay, intensity * T,  n2, colour, iteration-1);
                    
                    if(!refract.equals(ray.dir)){
                        boolean blah=true;
                    }
                    
                }
                //TODO use onsurfaceof stuff, not the +0.001 bodge
                Ray reflectedRay = new Ray(collision.where, reflect);//.add(reflect, 0.001)
                reflectedRay.onSurfaceOf=collision.with;

                fireRay(reflectedRay, intensity * R,  n1, colour, iteration-1);//inside ? thisShape : null);
                
            }

            //note this is different to render, but not sure which is actually hte behaviour I want yet
            if (!collision.inside && reflective > 0) {
                Vector r = ray.dir.subtract(collision.normal.multiply(2 * collision.normal.dot(ray.dir)));

                Ray reflectedRay = new Ray(collision.where, r);

                reflectedRay.onSurfaceOf=collision.with;
                
                fireRay(reflectedRay, intensity * reflective, currentN, colour, iteration-1);
            }

        }
    }
}
