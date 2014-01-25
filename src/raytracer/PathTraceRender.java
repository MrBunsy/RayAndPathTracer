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
    }

    @Override
    public Colour renderRay(Ray ray, int iteration, double currentN, double currentPower) {
        if (iteration <= 0 || currentPower < 0.002) {
            return black;
        }
        
        Colour rendered= black;
        
        /*
        
        Plan:
        
        compute direct lighting as per normal
        
        //then fire off ray to compute indirect lighting
        
        hit a diffuse surface - fire ray in random direction that is in hemisphere of surface normal
        (non-negative dot product)
        
        glossy surface - fire ray in direction of cone which is centred around the reflection
        size of cone decreases with increased glossyness
        how about pi*(0.5^gloss) for max angle?
        
        mirror - reflection as per normal
        
        I think light sources are going to require a physical presence? (will direct lighting for each ray resolve this?)
        
        
        combine the two 
        
        // surface * lighting
        vec3 tcol = scol * (dcol + icol);
        where scol is surface colour, d=direct and i=indirect
        
        says http://www.iquilezles.org/www/articles/simplepathtracing/simplepathtracing.htm
        
        */
        //this finds the nearest collision
        Collision collision = getNearestCollision(world, ray);//, ignore);

        if (collision.collide) {
            Surface surface = collision.with.getSurface();
            Vector normal = collision.normal;
            Vector collisionPoint = collision.where;
            Vector texturePoint = collision.with.getParent().getTextureCoords(collisionPoint);
            
            Colour directLighting = directLighting(collision.with, normal, collisionPoint, surface, ray, texturePoint);
            
            //reflection and refraction
            //Colour bouncedLighting = new Colour(0,0,0);
            
            //atm this is straight copied from overriden render, TODO abstract out
            if (surface.reflective > 0) {//!inside && 
                //this surface is slightly reflective
                //d=incomming vector, n=normal, r= reflected
                //r=d - 2(n.d)n
                //bounce a ray off and see where it goes!
                Vector r = ray.dir.subtract(normal.multiply(2 * normal.dot(ray.dir)));

                Ray reflectedRay = new Ray(collisionPoint, r);

                //no longer ignoring shapes
                //instead mentioning the specific shape we're on the surface of.
                //slightly hackyish
                reflectedRay.onSurfaceOf = collision.with;

                Colour reflectedColour = renderRay(reflectedRay, iteration - 1, currentN, currentPower * surface.reflective);//inSceneArray,

                Colour tempColour = reflectedColour.dim(surface.reflective);
                directLighting = directLighting.add(tempColour);
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
                    Colour reflectedColour = this.renderRay(reflectedRay, iteration - 1, n1, currentPower * R);
                    
                    //adjust the colours by the relative powers of the refracted and reflected light
                    reflectedColour = reflectedColour.dim(R);
                    //multiplying these colours by how much the shape is clear - no idea if this is physically accurate!
                    directLighting = directLighting.add(reflectedColour.dim(surface.clear));
                }

                if (!tir && T > 0.002) {
                    Ray refractedRay = new Ray(collisionPoint, refract);
                    refractedRay.onSurfaceOf = collision.with;
                    Colour refractedColour = this.renderRay(refractedRay, iteration - 1, n2, currentPower * T);//ignore,
                    refractedColour = refractedColour.dim(T);
                    directLighting = directLighting.add(refractedColour.dim(surface.clear));
                }
            }
            
            
            //indirect lighting
            Vector rayDir = null;
            Colour indirectColour = black;
            if(surface.isDiffuse()){
                //bounce off in random direction in the direction of the surface normal
               rayDir = collision.normal.randomThisDirection();
            }else if(surface.isGloss()){
                //limit the randomness of the bouncing ray to be 
                
                //get the direction of a reflected ray
                Vector r = ray.dir.subtract(normal.multiply(2 * normal.dot(ray.dir)));
                //note - this might self intersect?
               rayDir = r.randomThisDirection(Math.PI*Math.pow(0.2,surface.getGloss()), new Random());
               
              
            }
            
            if(rayDir!=null){
                Ray indirectRay = new Ray(collisionPoint, rayDir);
                //TODO work out real value to reduce power by
                //is power actually used by anything anymore?  should it be?
                indirectColour = this.renderRay(indirectRay, iteration - 1,  currentN, currentPower * 0.9);
            }
            
            return directLighting.add(indirectColour);
            
        } else {
            //no collision
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
                    p.add(dx, Math.random());
                    p.add(dy, Math.random());

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

                //now stick the pixel on the image
                setPixel(_g, new Colour(r, g, b), x, y);
            }

            renderedPixels(height);

        }

    }
}
