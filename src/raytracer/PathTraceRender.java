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

    /**
     * Fire off a ray and see what colour it produces
     * @param ray
     * @param iteration decrease for each recursion, so we don't go forever
     * @param currentN value of n for current medium ray is in
     * @param currentPower power of this light
     * @return 
     */
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
        
        this returned ray is used as information on the brightness and colour of light (using the direct lighting for what it hit)
        
        TODO
        need options for "just find me this path" - find route to a light source
        and "find direct lighting and indirect lighitng"
        
        light sources will need a physical presence!
        
        combine the two 
        
        // surface * lighting
        vec3 tcol = scol * (dcol + icol);
        where scol is surface colour, d=direct and i=indirect
        
        says http://www.iquilezles.org/www/articles/simplepathtracing/simplepathtracing.htm
        
        */
        //this finds the nearest collision
        Collision collision = getNearestCollision(world, ray);//, ignore);

        if (collision.collide) {
            
            /*
            We have found a surface - what we need to do is:
             - fire off a ray randomly to see if it will eventually hit a light source
             - use the result of that ray to light this surface in the conventional way
            
             - maybe also combine with direct lighting?
            (direct lighting might not mean much if lighting surfaces are being used)
            
            */
            
            
            Surface surface = collision.with.getSurface();
            Vector normal = collision.normal;
            Vector collisionPoint = collision.where;
            Vector texturePoint = collision.with.getParent().getTextureCoords(collisionPoint);
            
            //Colour directLighting = directLighting(collision.with, normal, collisionPoint, surface, ray, texturePoint);
            
            //reflection and refraction
            //Colour bouncedLighting = new Colour(0,0,0);
            
            
            if(surface.isLight()){
                //this surface belongs to a light
                
                //return how much light there would be at the beginning of this ray
                
                double distance=collisionPoint.subtract(ray.start).getMagnitude();

//                if(distance==0){
//                    distance=1;
//                }
                distance = Math.max(1, distance);
                //1/r^2
                Colour c = surface.getColour(texturePoint);
                //our brightness becomes the next ray's intensity.  hopefully.
                c.setIntensity(surface.getIntensity()/Math.pow(distance,2));
                if(Double.isInfinite(c.getIntensity())){
                    System.out.println("INFINITE");

                }
                //that's it, we hit a light and have provided the brightness and colour of the light
                //at the start of this ray
                return c;
            }
            //if here we haven't hit a light...yet
            //but we can assume the ray we sent off did (if it didn't it'll return black, so all will be well)
            
            Colour result= black;
            
            //do reflection and refraction - both will just fire off more rays too add to the path to the light
            //this is to find the intensity and colour of the light at this collision point
            
            Colour collisionPointLight=black;
            
            //atm this is straight copied from overriden render, TODO abstract out
            if (surface.reflective > 0) {//!inside && 
                //this surface is slightly reflective
                //d=incomming vector, n=normal, r= reflected
                //r=d - 2(n.d)n
                //bounce a ray off and see where it goes!
                Vector r = ray.dir.subtract(normal.multiply(2 * normal.dot(ray.dir)));
                Ray reflectedRay = new Ray(collisionPoint, r);
                //mentioning the specific shape we're on the surface of.
                reflectedRay.onSurfaceOf = collision.with;
                Colour reflectedLight = renderRay(reflectedRay, iteration - 1, currentN, currentPower * surface.reflective);
                
                //TODO clear up brightness/intensity - for now the returned 'intensity' is assumed to be the brightness at the point
                //the ray originated from
                double brightness = reflectedLight.getIntensity();
                
                
                Colour reflectedColour = colourFromLight(surface, normal, brightness , normal, rendered, result, normal);
                
                reflectedColour = reflectedColour.dim(surface.reflective);
//                result = result.add(tempColour);
                //intensity here is actually the brightness caused by intensity of what the reflected ray hit
                //should really rename it
                collisionPointLight.add(reflectedColour.dim(reflectedColour.getIntensity()));
                
                
                
                collisionPointLight.setIntensity(collisionPointLight.getIntensity()+reflectedColour.getIntensity());
                if(Double.isInfinite(collisionPointLight.getIntensity())){
                    System.out.println("INFINITE");

                }
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
                    //directLighting = directLighting.add(reflectedColour.dim(surface.clear));
                    collisionPointLight = collisionPointLight.add(reflectedColour.dim(reflectedColour.getIntensity()));
                    collisionPointLight.setIntensity(collisionPointLight.getIntensity()+reflectedColour.getIntensity());
                    
                    if(Double.isInfinite(collisionPointLight.getIntensity())){
                    System.out.println("INFINITE");

                }
                }

                if (!tir && T > 0.002) {
                    Ray refractedRay = new Ray(collisionPoint, refract);
                    refractedRay.onSurfaceOf = collision.with;
                    Colour refractedColour = this.renderRay(refractedRay, iteration - 1, n2, currentPower * T);//ignore,
                    refractedColour = refractedColour.dim(T);
                    //directLighting = directLighting.add(refractedColour.dim(surface.clear));
                    collisionPointLight = collisionPointLight.add(refractedColour.dim(refractedColour.getIntensity()));
                    collisionPointLight.setIntensity(collisionPointLight.getIntensity()+refractedColour.getIntensity());
                    
                    if(Double.isInfinite(collisionPointLight.getIntensity())){
                    System.out.println("INFINITE");

                }
                }
            }
            
            
            
            //indirect lighting, reflection and refraction have been dealt with, light sources have been found,
            //this is where it is all put together
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
            
            //now we have the pathtracing search for light based on diffuse/gloss
            //and the search for light based on reflection and refraction
            //pool all these together to find the brightness and colour of the light
            
            if(rayDir!=null){
                //System.out.println(rayDir);
                Ray indirectRay = new Ray(collisionPoint, rayDir);
                //TODO work out real value to reduce power by
                //is power actually used by anything anymore?  should it be?
                indirectColour = this.renderRay(indirectRay, iteration - 1,  currentN, currentPower);
                //lambert's cosine law
                //the radiant intensity or luminous intensity observed from an ideal diffusely reflecting surface or ideal diffuse radiator is directly proportional to the cosine of the angle θ between the observer's line of sight and the surface normal
                //and
                // the irradiance (energy or photons/time/area) landing on that area element will be proportional to the cosine of the angle between the illuminating source and the normal
                
                //so only taking into account the latter
                double brightness = indirectRay.dir.dot(normal);
                
                if(Double.isInfinite(brightness)){
                    System.out.println("INFINITE");
                }
                
                collisionPointLight = collisionPointLight.add(indirectColour.dim(brightness*indirectColour.getIntensity()));
                collisionPointLight.setIntensity(collisionPointLight.getIntensity()+indirectColour.getIntensity());
                
                if(Double.isInfinite(collisionPointLight.getIntensity())){
                    System.out.println("INFINITE");

                }
            }
            
            //now we know what the lighting is at the collision point, work out the brightness of this light for the
            //position the ray initiates from
            
            double distance=collisionPoint.subtract(ray.start).getMagnitude();

            distance = Math.max(1,distance);
            //1/r^2
            //our brightness becomes the next ray's intensity.  hopefully.
            
            double intensity = collisionPointLight.getIntensity()/Math.pow(distance,2);
            
            if(Double.isInfinite(intensity)){
                System.out.println("INFINITE");
            }
            
            collisionPointLight.setIntensity(intensity);
            
            return collisionPointLight;
            
        } else {
            //no collision
            
            if(world.isSunlight()){
                //check to see if this ray is in the direction of the sunlight
                if(world.getSunlight().getAngle().dot(ray.dir) > 0){
                    //in direction of sunlight!
                    return world.getSunlight().getColour().setIntensity(world.getSunlight().getBrightness());
                }
            }
            
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
                    p=p.add(dx, Math.random());
                    p=p.add(dy, Math.random());

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
                setPixel(_g,c , x, y);
            }

            renderedPixels(height);

        }

    }
}
