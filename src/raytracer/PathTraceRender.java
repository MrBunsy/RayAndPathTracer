/*
 * Copyright Luke Wallin 2012
 */
package raytracer;

import LukesBits.Vector;
import java.awt.Graphics2D;
import java.util.Random;
import static raytracer.Render.getNearestCollision;

/**
 *
 * @author Luke
 */
public class PathTraceRender extends Render {

    //how many samples per pixel
    private int samples;

    public PathTraceRender(World _world, int _threads, int _width, int _height, int _samples) {
        super(_world, _threads, _width, _height);
        samples = _samples;
        noLight = new LightPath();
    }
    
    private LightPath noLight;
    
    
    private LightPath findLight(Ray ray, int iteration, double currentN){
        if(iteration <=0){
            return noLight;
        }
        Collision collision = getNearestCollision(world, ray);

        if (collision.collide) {
            Surface surface = collision.with.getSurface();
            Vector normal = collision.normal;
            Vector collisionPoint = collision.where;
            Vector texturePoint = collision.with.getParent().getTextureCoords(collisionPoint);
            
            if (surface.isLight()) {
                //this surface belongs to a light

                LightPath l = new LightPath();
                //colour of the light
                l.colour = surface.getColour(texturePoint);
                
                l.intensity = surface.getIntensity();
                
                l.distance = collisionPoint.subtract(ray.start).getMagnitude();
                //that's it, we hit a light and have provided the brightness and colour of the light
                //at the start of this ray
                return l;
            }
            
            //choose which ray to fire
            
            
            if(surface.isDiffuse() || surface.isGloss() || surface.isReflective() || surface.isClear()){
                //there is something else to do
                
                int doThis=0;
                boolean foundValidRay=false;
                //randomly choose which route to take
                while(!foundValidRay){
                    doThis=(int)Math.floor(Math.random()*4);
                    if(doThis==0 && surface.isDiffuse()){
                        foundValidRay=true;
                    }
                    if(doThis==1 && surface.isGloss()){
                        foundValidRay=true;
                    }
                    if(doThis==2 && surface.isReflective()){
                        foundValidRay=true;
                    }
                    if(doThis==3 && surface.isClear()){
                        foundValidRay=true;
                    }
                }
                
                Vector rayDir=null;
                
                switch(doThis){
                    case 0:
                        //diffuse
                        //bounce off in random direction in the direction of the surface normal
                        rayDir = collision.normal.randomThisDirection();
                        break;
                    case 1:
                        //gloss
                        //limit the randomness of the bouncing ray to be 

                        //get the direction of a reflected ray
                        Vector r = ray.dir.subtract(normal.multiply(2 * normal.dot(ray.dir)));
                        //note - this might self intersect?
                        rayDir = r.randomThisDirection(Math.PI * Math.pow(0.5, surface.getGloss()), new Random());
                        break;
                    case 2:
                        //reflect
                        //get the direction of a reflected ray
                        rayDir = ray.dir.subtract(normal.multiply(2 * normal.dot(ray.dir)));
                        break;
                    case 3:
                        //refract
                        double n1, n2;
                        n1 = currentN;
                        //if we are inside a shape, the other n is the outside world, otherwise we are in the outside world and the other n is the surface of the shape
                        n2 = collision.inside ? (surface.hasOutsideN() ? surface.getOutsideN() : world.getAirN()) : surface.n;

                        Refraction answers = refraction(n1, n2, normal, ray.dir);
                        Vector refract = answers.refract;
                        Vector reflect = answers.reflect;
                        double T = answers.T;
                        double R = answers.R;
                        boolean tir = answers.tir();
                        
                        boolean doReflect = false;
                        boolean doRefract=false;
                        
                        if(R > 0.002 && (!tir && T > 0.002)){
                            //both reflection and refraction are possible. Choose
                            if(Math.random() < 0.50){
                                rayDir = reflect;
                                currentN=n1;
                            }else{
                                rayDir = refract;
                                currentN=n2;
                            }
                        }else{
                            //only one (or none?) is possible
                            if(R > 0.002){
                                rayDir = reflect;
                                currentN=n1;
                            }else{
                            //if((!tir && T > 0.002)){
                                rayDir = refract;
                                currentN=n2;
                            }
                        } 
                        break;
                }

                Ray continuedRay = new Ray(collisionPoint, rayDir);
                continuedRay.onSurfaceOf = collision.with;
                
                LightPath p = findLight(continuedRay, iteration-1, currentN);
               
                //TODO take into account reducing intensity as a result of non-perfect reflections and 
                //http://en.wikipedia.org/wiki/Lambert%27s_cosine_law
                p.distance+=collisionPoint.subtract(ray.start).getMagnitude();
                
                return p;
            }
            
            
        }else{
            //no collision
            if (world.isSunlight()) {
                //check to see if this ray is in the direction of the sunlight
                if (world.getSunlight().getAngle().dot(ray.dir) > 0) {
                    //in direction of sunlight!
                    //return world.getSunlight().getColour().setIntensity(world.getSunlight().getBrightness());
                    LightPath l = new LightPath();
                    l.colour=world.getSunlight().getColour();
                    l.distance=0;
                    l.intensity=world.getSunlight().getBrightness();
                    return l;
                }
            }
        }
        
        return noLight;
    }

    /**
     * Fire off a ray and see what colour it produces
     *
     * currently this is being bodged so it's basically returning a light -
     * using intensity in the colour intensity is actually being used backwards
     * - it's the brightness at the point where renderRay was called
     *
     * @param ray
     * @param iteration decrease for each recursion, so we don't go forever
     * @param currentN value of n for current medium ray is in
     * @param currentPower power of this light
     * @return
     */
    @Override
    public Colour renderRay(Ray ray, int iteration, double currentN, double currentPower) {
        if (iteration <= 0 || currentPower < 0.002) {
            return black;
        }

        /*
        Find a shape
        if its surface is diffuse or gloss, compute lighting for it by firing off an lightPath request.
        if it is reflective or refractive, fire off another (set) of render rays as appropriet (like conventional raytracing)
        */
        
        Colour rendered = black;

        //this finds the nearest collision
        Collision collision = getNearestCollision(world, ray);//, ignore);

        if (collision.collide) {

            Surface surface = collision.with.getSurface();
            Vector normal = collision.normal;
            Vector collisionPoint = collision.where;
            Vector texturePoint = collision.with.getParent().getTextureCoords(collisionPoint);
            
            //Colour directLighting = directLighting(collision.with, normal, collisionPoint, surface, ray, texturePoint);
            //reflection and refraction
            //Colour bouncedLighting = new Colour(0,0,0);
            if (surface.isLight()) {
                //this surface belongs to a light

               return surface.getColour(texturePoint);
            }
            
            //TODO consider direct lighting?
            //Colour colour = directLighting(collision.with, normal, collisionPoint, surface, ray, texturePoint);            
            Colour colour = black;
            
            //if here we haven't hit a light...yet
           
            Colour collisionPointLight = black;

            boolean intensityTest = false;

            if (surface.reflective > 0) {//!inside && 
                //this surface is slightly reflective
                //d=incomming vector, n=normal, r= reflected
                //r=d - 2(n.d)n
                //bounce a ray off and see where it goes!
                Vector r = ray.dir.subtract(normal.multiply(2 * normal.dot(ray.dir)));

                Ray reflectedRay = new Ray(collisionPoint, r);

                //no longer ignoring shapes
                //ArrayList<Shape> inSceneArray = new ArrayList<Shape>();
                //inSceneArray.add(parentShape);
                //instead mentioning the specific shape we're on the surface of.
                //slightly hackyish
                reflectedRay.onSurfaceOf = collision.with;

                Colour reflectedColour = renderRay(reflectedRay, iteration - 1, currentN, currentPower * surface.reflective);//inSceneArray,

                Colour tempColour = reflectedColour.dim(surface.reflective);
                colour = colour.add(tempColour);
            }

            if (surface.clear > 0) {
                double n1, n2;
                //TODO, look at dispersion where different colours refract at different angles
                //maybe spray out lots of rays and colour adjust the ones that come back?

                n1 = currentN;
                //if we are inside a shape, the other n is the outside world, otherwise we are in the outside world and the other n is the surface of the shape
                n2 = collision.inside ? (surface.hasOutsideN() ? surface.getOutsideN() : world.getAirN()) : surface.n;

                Refraction answers = refraction(n1, n2, normal, ray.dir);
                Vector refract = answers.refract;
                Vector reflect = answers.reflect;
                double T = answers.T;
                double R = answers.R;
                boolean tir = answers.tir();

                //only send off other rays if they're relevant
                if (R > 0.002) {

                    Ray reflectedRay = new Ray(collisionPoint, reflect);

                    reflectedRay.onSurfaceOf = collision.with;

                    //TODO work out if this strange insideShape thing is needed
                    //we have hit a clear shape, and there is a small amount of reflection, this is calculating the colour returned from the reflected ray
                    Colour reflectedColour = this.renderRay(reflectedRay, iteration - 1, n1, currentPower * R);//ignore,

                    //adjust the colours by the relative powers of the refracted and reflected light
                    reflectedColour = reflectedColour.dim(R);

                    //multiplying these colours by how much the shape is clear - no idea if this is physically accurate!
                    colour = colour.add(reflectedColour.dim(surface.clear));
                }

                if (!tir && T > 0.002) {

                    Ray refractedRay = new Ray(collisionPoint, refract);
                    refractedRay.onSurfaceOf = collision.with;

                    Colour refractedColour = this.renderRay(refractedRay, iteration - 1, n2, currentPower * T);//ignore,
                    refractedColour = refractedColour.dim(T);
                    colour = colour.add(refractedColour.dim(surface.clear));
                }

            }

            //indirect lighting, reflection and refraction have been dealt with, light sources have been found,
            //this is where it is all put together
            Vector rayDir = null;
            Colour indirectColour = black;
            //do diffuse if surface isn't gloss, otherwise only do it half the time if surface is gloss too
            if (surface.isDiffuse() && (!surface.isGloss() || Math.random()<0.5)) {
                //bounce off in random direction in the direction of the surface normal
                rayDir = collision.normal.randomThisDirection();
            } else if (surface.isGloss()) {
                //limit the randomness of the bouncing ray to be 

                //get the direction of a reflected ray
                Vector r = ray.dir.subtract(normal.multiply(2 * normal.dot(ray.dir)));
                //note - this might self intersect?
                rayDir = r.randomThisDirection(Math.PI * Math.pow(0.2, surface.getGloss()), new Random());

            }

            //now we have the pathtracing search for light based on diffuse/gloss
            //and the search for light based on reflection and refraction
            //pool all these together to find the brightness and colour of the light
            if (rayDir != null) {
                //System.out.println(rayDir);
                Ray indirectRay = new Ray(collisionPoint, rayDir);
                //TODO work out real value to reduce power by
                //is power actually used by anything anymore?  should it be?
                
                LightPath p = findLight(indirectRay, iteration, currentN);
                
                double distance = Math.max(1,p.distance);
                
                double brightness = p.intensity/Math.pow(distance,2);
                
                Colour indirectLit = colourFromLight(surface, normal, brightness, indirectRay.dir, surface.getColour(collisionPoint), p.colour, ray.dir);
                
                //this is essentially another light source
                colour = colour.add(indirectLit);
            }

            
            return colour;

        } else {
            //no collision

//            if (world.isSunlight()) {
//                //check to see if this ray is in the direction of the sunlight
//                return world.getSunlight().getColour();
//            }

            return black;
        }

        //return black;
    }

    /**
     * Renders a slither of the image by firing off rays. Processing of each ray
     * occurs in getPixel
     *
     * @param _g graphics object to store result in
     * @param section id of this section
     * @param totalSections total number of sections that will be rendered
     */
    @Override
    public void generateImage(Graphics2D _g, int section, int totalSections) {//int width,int height,

        int sectionWidth = images[section].getWidth();//sliceWidth;

        Vector[] screenCorners = world.camera.getScreenCorners((double) height / (double) width);

        Vector topLeft = screenCorners[0];
        Vector bottomLeft = screenCorners[1];
        Vector topRight = screenCorners[3];

        Vector dx = topRight.add(topLeft, -1).multiply(1 / (double) width);
        Vector dy = bottomLeft.add(topLeft, -1).multiply(1 / (double) height);

        for (int x = 0; x < sectionWidth; x++) {
            for (int y = 0; y < height; y++) {
                double r = 0;
                double g = 0;
                double b = 0;
                for (int n = 0; n < samples; n++) {
                    //we'll be firing multiple rays through each pixel now for path tracing.
                    //NOTE we can also vary the exact location they are fired from to get free AA!

                    Vector p = topLeft.add(dx, x + sectionWidth * section).add(dy, y);

                    //random variation for AA
                    p = p.add(dx, Math.random());
                    p = p.add(dy, Math.random());

                    Vector rayDirection = p.subtract(world.camera.pos);

                    Ray ray = new Ray(world.camera.pos, rayDirection);
                    Colour colour = renderRay(ray, world.iterate, world.getAirN(), 1.0);
                    r += colour.getR();
                    g += colour.getG();
                    b += colour.getB();

                }

                r /= (double) samples;
                g /= (double) samples;
                b /= (double) samples;

                Colour c = new Colour(r, g, b);

                //now stick the pixel on the image
                setPixel(_g, c, x, y);
            }

            renderedPixels(height);

        }

    }
}

class LightPath{
    
    public LightPath(){
        colour = new Colour(0,0,0);
        distance = 1;
        intensity = 0;
    }
    
    public Colour colour;
    public double distance;
    public double intensity;
}