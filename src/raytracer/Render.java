package raytracer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import LukesBits.Vector;
import java.awt.*;

/**
 *
 * @author Luke
 */
public class Render {

    public World world;
    private int threads, slices, currentThreads, finishedRendering;
    protected int width, height, sliceWidth;
    private long startTime;
    String imageName;
    public Graphics2D[] graphics;
    public BufferedImage[] images;
    protected Colour black;

    //private boolean progress;
    private long totalPixels, renderedPixels;

    private int scaleDown;

    //how far aware from ourselves to advance light rays
    //double littleBodge = 0.1;
    private AnimationInterface animation;

    public Render(World _world, int _threads, int _width, int _height) {//, boolean _progress) {
        this(_world, _threads, _width, _height, null);//_progress,
    }

    public Render(World _world, int _threads, int _width, int _height, AnimationInterface _animation) {//, boolean _progress,
        width = _width;
        height = _height;
        threads = _threads;
        slices = threads * 16;//todo work out better system?
        world = _world;
        graphics = new Graphics2D[slices];
        images = new BufferedImage[slices];
        sliceWidth = width / slices;
        //show a progress bar?
        //progress=true;
        //this.photonMap=photonMap;
        black = new Colour(0, 0, 0);
        //black= new Colour(100,100,255);

        renderedPixels = 0;
        totalPixels = width * height;

        System.out.println("Rendering with " + threads + " threads");
        startTime = System.currentTimeMillis();
        animation = _animation;
    }

    public static boolean isSunlit(World world, Vector pos, Shape checkMe) {

        Ray ray = new Ray(pos, world.getSunlight().getAngle());

        ray.onSurfaceOf = checkMe;

        Collision collide = getNearestCollision(world, ray, true);

        return !collide.collide;
//        
//        boolean convex = checkMe.isConvex();
//        
//        for (Shape s : world.scene) {
//            if (s == checkMe || s.getSurface().isSkybox() || (convex && s==checkMe.getParent())) {// || s==ignore.getParent()
//                //ignore the ignored shape and any skyboxes
//                //if the shape we're interested in is a convex poly, we can skip checking that entire poly
//                continue;
//            }
//            //ray.onSurfaceOf=s;
//            
//             Collision collide = s.collide(ray);
//             
//             if(collide.collide){
//                 //an object is in the way.
//                 if(collide.with==checkMe){
//                     //if checkMe is part of a non-convex polygon, we want to ignore colliding with it.
//                     //continue;
//                 }
//                 return false;
//             }
//        }
//        return true;
    }

    public static boolean isLit(World world, Shape shape, Ray ray) {//Vector pos,

        //ray goes from the light to the collision point
        //Ray ray = new Ray(light.pos,pos.subtract(light.pos));
        Collision c = getNearestCollision(world, ray);//, new ArrayList<Shape>());

        if (c.collide && (c.with == shape)) {//c.with.getParent()==shape || 
            return true;
        }

        return false;
    }

    //check to see if we can reach a light
    public static boolean isUnLit(World world, Ray ray, Shape ignore, Double lightDistance) {

        //square this
        lightDistance *= lightDistance;
        ArrayList<Shape> scene = world.scene;
        for (int i = 0; i < scene.size(); i++) {
            if (scene.get(i) == ignore) {
                continue;
            }
            Collision collide = scene.get(i).collide(ray);
            if (collide.collide && collide.where.subtract(ray.start).getMagnitudeSqrd() < lightDistance) {
                //we've collided with another shape that is this side of the light
                return true;
            }
        }
        return false;
    }

    /*
     * refraction ideas: inside gets set to an object if we're inside an object
     * need to carefully check that once we're inside we look to see when we
     * leave an object
     *
     */
    public static Refraction refraction(double n1, double n2, Vector n, Vector l) {

//        if(n1==n2){
//            
//            Vector r = l.subtract(n.multiply(2 * n.dot(l)));
//            
//            return new Refraction(l, r, 1, 0);
//        }
        //snell's law:

        /*
         * n1 l \ | n2 \ | thetaI \| ------------|------------ normal thetaR /|\
         * theta2 / | \ reflect / | \ refracted
         */
        double cosThetaI = -n.dot(l);

        if (cosThetaI < 0) {
            //if this is negative it means the surface normal was the same direction as the ray,
            //this in theory can't happen, but does happen a little bit with phong shading
            //it also can happen because of other problems, so watch this if things go wrong
            //as far as I can tell, the phong shading only causes it to be slightly negative
            //  cosThetaI*=-1;
        }

        double sqrtMe = 1 - Math.pow(n1 / n2, 2) * (1 - Math.pow(cosThetaI, 2));

        Vector reflect = l.add(n, 2 * cosThetaI);
        Vector refract;

        if (sqrtMe >= 0) {
            double cosTheta2 = Math.sqrt(sqrtMe);

            if (cosThetaI > 0) {
                refract = l.multiply(n1 / n2).add(n, (n1 / n2) * cosThetaI - cosTheta2);
            } else {
                refract = l.multiply(n1 / n2).subtract(n, (n1 / n2) * cosThetaI - cosTheta2);
            }

            //now we have the angles of rays reflected and reacted, need to work out the relative power of the two.
            //fresnel's law!
            //R= power reflected, T=power refracted
            //Rs for s-polarised, Rp for p-polarised, R=(Rs+Rp)/2 for non polarised light
            double Rs = Math.pow((n1 * cosThetaI - n2 * cosTheta2) / (n1 * cosThetaI + n2 * cosTheta2), 2);
            double Rp = Math.pow((n1 * cosTheta2 - n2 * cosThetaI) / (n1 * cosTheta2 + n2 * cosThetaI), 2);

            double R = (Rs + Rp) / 2;

            //hack:
            if (R > 1) {
                R = 1;
            }

            if (R < 0) {
                R = 0;
            }

            double T = 1 - R;

            return new Refraction(refract, reflect, T, R);

        } else {
            //Total internal reflection

            return new Refraction(null, reflect, 0, 1);

        }

//        if(Double.isNaN(cosTheta2)){
//            //throw new Exception("Nan costheta2");
//            System.err.println("Nan costheta2");
////        }
    }

    //normal is the normal of the surface,
    //lightbrightness is the birghtness of the light at this point
    //colourhere is the oclour of this surface at this point
    //angle is the angle from this point to the light
    //viewAngle is the direction of the ray which is looking at this surface
    public Colour colourFromLight(Surface surface, Vector normal, double lightBrightness, Vector lightAngle, Colour colourHere, Colour lightColour, Vector viewAngle) {
        Colour colour = new Colour(0, 0, 0);

        if (surface.diffuse > 0 && surface.reflective < 1 && surface.clear < 1) {
            //is this pointing at the light?
            double brightness = lightAngle.dot(normal);
            brightness *= lightBrightness * surface.diffuse;
            //the colour from diffused surfaces, we're reducing it by the reflectiveness of the surface
            //TODO take into account hte colour of the light source
            //Colour tempColour = colourHere.times(lightColour).dim(Math.max(0, brightness));
            //colour = colour.add(tempColour);
            colour = colour.add(colourHere.times(lightColour).dim(Math.max(0, Math.max(0, brightness) - surface.reflective - surface.clear)));
        }

        if (surface.gloss > 1) {
            //dot product of reflected ray with light direction
            //affected by viewing angle, but not by clear/reflectiveness
            //the viewing ray bouncing off the surface
            Vector r = viewAngle.subtract(normal.multiply(2 * normal.dot(viewAngle)));

            double brightness = Math.max(0, r.dot(lightAngle));//*(lights.get(i).lightAt(collisionPoint)*5);

            brightness = Math.pow(brightness, surface.gloss) * lightBrightness;

            //brightness*=inScene.surface.gloss;
            brightness *= 1 - 2 / (surface.gloss);

            Colour tempColour = lightColour.dim(brightness);
            colour = colour.add(tempColour);

        }

        return colour;
    }

//    public Colour getPixelFromObject(Ray ray, Shape inScene, int iteration,  double currentN){
//        return getPixelFromObject(ray, inScene, iteration, currentN, 1.0,null);
//    }
    //we want to see if a specific ray collides with a specific shape, and if it does, what colour it returns
    //todo this is not longer really a getPixelFromObject so much as a getPixelFromRay
//    public Colour getPixelFromObject(Ray ray, Shape parentShape, int iteration, double currentN, double currentPower, Collision collision) {
//        //if inside is true we are currently inside the object
//
//        
//        if(collision==null){
//            //this shouldn't ever happen within the renderer's use of this functino anymore
//            collision = parentShape.collide(ray);
//        }
//        if (collision.collide) {
//        }
//        return black;
//    }
    public static Collision getNearestCollision(World world, Ray ray) {
        return getNearestCollision(world, ray, false);
    }

    public static Collision getNearestCollision(World world, Ray ray, boolean ignoreSunbox) {//, ArrayList<Shape> ignore){

        ArrayList<Collision> collisions = new ArrayList<Collision>();
        ArrayList<Double> distances = new ArrayList<Double>();

        //find all the collisions for this ray
        for (int i = 0; i < world.scene.size(); i++) {

            if (ignoreSunbox && world.scene.get(i).getSurface().isSkybox()) {
                continue;
            }

//            if (ignore.contains(world.scene.get(i))) {
//                //ignore everything in the ignore array
//                continue;
//            }
            Collision c = world.scene.get(i).collide(ray);

            if (c.collide) {
                //c.with=world.scene.get(i);
                collisions.add(c);
                distances.add(c.where.subtract(ray.start).getMagnitudeSqrd());
            }
        }

        //no collision
        if (collisions.isEmpty()) {
            return Collision.noCollision;
        }

        double nearest = Double.MAX_VALUE;
        Collision nearestC = null;

        //find nearest collision
        for (int i = 0; i < collisions.size(); i++) {
            if (distances.get(i) < nearest) {
                nearest = distances.get(i);
                nearestC = collisions.get(i);
            }
        }

        return nearestC;

    }

    /**
     * Given details of a ray intersecting with an object, computer colour based
     * on direct lighting
     *
     * @param shape Shape ray has hit
     * @param normal Normal of the surface where ray has hit shape
     * @param collisionPoint Vector of collision between ray and shape
     * @param surface Surface of the shape where hit
     * @param ray The ray which hit the shape
     * @param texturePoint x,y coords of the texture on the surface at this
     * point
     * @return
     */
    protected Colour directLighting(Shape shape, Vector normal, Vector collisionPoint, Surface surface, Ray ray, Vector texturePoint) {
        ArrayList<Light> lights = world.lights;
//            Vector normal = collision.normal;
//            Vector collisionPoint = collision.where;
//            Shape parentShape = collision.with.getParent();
//            
//            //Surface surface=parentShape.getSurface();
//            Surface surface = collision.with.getSurface();

            //this point relative to the position of the shape
        //Vector texturePoint = parentShape.getTextureCoords(collisionPoint);
        Colour colourHere = surface.getColour(texturePoint);
        //double distance = collisionPoint.subtract(ray.start).getMagnitude();
        Colour colour = new Colour(0, 0, 0);

        if (surface.ambient > 0) {
            //ambient lighting, takes into account reflectiveness of shape and if it's clear
            colour = colour.add(colourHere.dim(Math.max(0, surface.ambient - surface.reflective - surface.clear)));
        }

        /*
         * a ray is sent out, if it hits something:
         *
         *
         * check to see if this surface has direct line of sight to a light,
         * if it does: calculate colour depending on how diffuse this
         * surface is calculate colour based on colour of the light and
         * gloss of this surface
         *
         *
         * check if surface is reflective, if it is, send out another ray
         * check if surface is clear, if it is work out reflection and
         * refraction and send out more rays
         *
         *
         * done in this order deliberately - if reflection is done first
         * then diffusion can wipe it out.
         *
         */
            //if (!inside) {
        if (world.isSunlight()) {
            Sunlight sunlight = world.getSunlight();
            //if(isSunlit(world,collisionPoint, parentShape)){
            if (isSunlit(world, collisionPoint, shape)) {
                //this object is light by the sunlight
                colour = colour.add(colourFromLight(surface, normal, sunlight.getBrightness(), sunlight.getAngle(), colourHere, sunlight.getColour(), ray.dir));
            }
        }
        //ignore everything except the last bit of refraction if we're currently inside a shape
        for (int i = 0; i < lights.size(); i++) {
            //deal with effects from being in direct line of a light, diffusion and gloss:

            Ray lightRay = new Ray(lights.get(i).pos, collisionPoint.subtract(lights.get(i).pos));

                    //ray from collision point to the light
//                    Ray lightRay = new Ray(collisionPoint, lights.get(i).pos.subtract(collisionPoint));
//                    lightRay.start.add(lightRay.dir.multiply(littleBodge));
//
//                    //can we reach this light?
//                    if (isUnLit(world,lightRay, inScene, lights.get(i).pos.subtract(collisionPoint).getMagnitude())) {
//                        continue;
//                    }
            //this might actually be a tiny bit slower than the old one, which aborted as soon as it found osmething in the wya
            if (isLit(world, shape, lightRay)) {
                colour = colour.add(colourFromLight(surface, normal, lights.get(i).lightAt(collisionPoint), lightRay.dir.multiply(-1.0), colourHere, lights.get(i).colour, ray.dir));
            }

        }

        if (world.hasPhotonMap()) {
            //photon map here
            Photon p = world.getPhotonMap().getLightHere(collisionPoint);
            if (p.intensity > 0) {
                //colour=colour.add(colourFromLight(surface, normal, lightHere.intensity, lightHere.dir, lightHere.colour,ray.dir));
                colour = colour.add(colourFromLight(surface, normal, p.intensity, p.dir, colourHere, p.colour, ray.dir));
                //colour=new Colour(255,0,0);
            }
        }
        //}
        return colour;
    }

    /**
     * this 'just' sends out a ray and finds the colour for this ray, taking
     * into account the whole world and all the rules. This is where the ray
     * tracing algorithm is implemented
     *
     * @param ray find the colour of the pixel on this ray
     * @param iteration how many objects have already been bounced off, used to
     * stop infinite recursion
     * @param currentN
     * @param currentPower
     * @return
     */
    public Colour renderRay(Ray ray, int iteration, double currentN, double currentPower) {//ArrayList<Shape> ignore,//iteration 5 by default, ignore empty and inside false

        //if this has been iterating for too long, just return black
        if (iteration <= 0 || currentPower < 0.002) {
            return black;
        }

        //this finds the nearest collision
        Collision collision = getNearestCollision(world, ray);//, ignore);

        if (collision.collide) {
            //return getPixelFromObject(ray, collision.with, iteration, collision.with == insideShape);
            //return getPixelFromObject(ray, collision.with.getParent(), iteration, currentN, currentPower,collision);

            //there *is* a collision!
            Surface surface = collision.with.getSurface();
            Vector normal = collision.normal;
            Vector collisionPoint = collision.where;
            Vector texturePoint = collision.with.getParent().getTextureCoords(collisionPoint);

            Colour colour = directLighting(collision.with, normal, collisionPoint, surface, ray, texturePoint);

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

            return colour;
        }
        return black;

    }

    /**
     * if less than the max number of threads are currently rendering, set more
     * going
     */
    protected void fillEmptyThreads() {
        try {

            //slices left = slices - finishedRendering
            //one of those "this works but I've forgotten why" lines, it adds more threads if we're under the thread limit and there're slices left
            for (int i = currentThreads; i < Math.min(threads, slices - finishedRendering); i++) {

                int w = sliceWidth;

                if (finishedRendering + i == slices - 1) {
                    //this is the final slice, alter its width so we don't miss some

                    if (sliceWidth * slices < width) {
                        w += width - sliceWidth * slices;
                    }
                }

                images[finishedRendering + i] = new BufferedImage(w, height, BufferedImage.TYPE_INT_RGB); /*
                 * change sizes of course
                 */

                graphics[finishedRendering + i] = images[finishedRendering + i].createGraphics();

                RenderThread r = new RenderThread(slices, finishedRendering + i, this, graphics[finishedRendering + i]);
                Thread t = new Thread(r);
                //t.run();
                t.start();
                currentThreads++;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * This function is called to start the rendering process and save to a file
     * when it is finished. This version saves the image without any scaling
     * down
     *
     * @param _imageName name of file to save to
     */
    public void saveImage(String _imageName) {
        saveImage(_imageName, 1);
    }

    /**
     * This function is called to start the rendering process and save to a file
     * when it is finished
     *
     * @param _imageName name of the file to save to
     * @param _scaleDown how much to scale the image down after rendering is
     * finished
     */
    public void saveImage(String _imageName, int _scaleDown) {

        finishedRendering = 0;
        imageName = _imageName;
        currentThreads = 0;

        //for AA the image, how much to reduce dimensions by
        scaleDown = _scaleDown;

        //set the threads going
        fillEmptyThreads();

    }

    /**
     * Renders a slither of the image by firing off rays. Processing of each ray
     * occurs in getPixel
     *
     * @param _g graphics object to store result in
     * @param section id of this section
     * @param totalSections total number of sections that will be rendered
     */
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
                Vector p = topLeft.add(dx, x + sectionWidth * section).add(dy, y);

                Vector rayDirection = p.subtract(world.camera.pos);

                Ray ray = new Ray(world.camera.pos, rayDirection);
                Colour colour = renderRay(ray, world.iterate, world.getAirN(), 1.0);

                setPixel(_g, colour, x, y);

            }

            renderedPixels(height);
        }

    }

    /**
     * Used by a rendering thread to inform the master how many pixels it has
     * rendered
     *
     * @param pixels a thread has rendered this many pixels
     */
    public synchronized void renderedPixels(long pixels) {
        renderedPixels += pixels;
        //double percent = Math.round(100.0*(double)renderedPixels/(double)totalPixels)/100.0;
        double percent = (double) renderedPixels / (double) totalPixels;

        RayTracer.printProgBar(percent);
        //System.out.print(""+percent+"%\r");
    }

    /**
     * Set a pixel on the graphics object
     *
     * @param _g graphics object to store render
     * @param colour colour to render this pixel
     * @param x x-coord of pixel
     * @param y y-coord of pixel
     */
    public synchronized void setPixel(Graphics2D _g, Colour colour, int x, int y) {
        
        Color color = colour.toColor();
        
        _g.setColor(color);
        _g.fillRect(x, y, 1, 1);
    }

    /**
     * Once a thread has finished rendering its slither of the image, the thread
     * calls this function
     */
    public synchronized void finishedRendering() {
        finishedRendering++;
        currentThreads--;

        if (finishedRendering >= slices) {
            try {

                BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                BufferedImage scaledImage;

                //int sliceWidth = width / slices;
                for (int i = 0; i < slices; i++) {
                    int[] rgbArray = new int[images[i].getWidth() * height];

                    //this works but leaves the last slice:
//                    images[i].getRGB(0, 0, sliceWidth, height, rgbArray, 0, images[i].getWidth());//images[i].getWidth());
//                    finalImage.setRGB(sliceWidth * i, 0, sliceWidth, height, rgbArray, 0, images[i].getWidth());//images[i].getWidth());
                    images[i].getRGB(0, 0, images[i].getWidth(), height, rgbArray, 0, images[i].getWidth());//images[i].getWidth());
                    finalImage.setRGB(sliceWidth * i, 0, images[i].getWidth(), height, rgbArray, 0, images[i].getWidth());//images[i].getWidth());
                }

                if (scaleDown > 1) {
                    //scaling the image down woo
                    scaledImage = getScaledInstance(finalImage, width / scaleDown, height / scaleDown, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
                    ImageIO.write(scaledImage, "png", new File(imageName + "_aa.png"));
                }

                ImageIO.write(finalImage, "png", new File(imageName + ".png"));

                long end = System.currentTimeMillis();

                long time = end - startTime;

                // System.out.println("\rTime taken: " + ((double) time / 1000.0) + "s = " + ( time / 60000) + "m, "+(time%60000)/ 1000.0+"s                                        ");
                System.out.println("\rTime taken: " + RayTracer.tidyTime(time) + "                                            ");
                System.out.println("Pixels: " + (width * height));
                System.out.println("Avg time/1000pixel: " + Math.round(((double) time * 1000 / (width * height)) * 100.0) / 100.0 + " ms");

                if (animation != null) {
                    animation.finishedImage();
                }

            } catch (IOException ex) {
                Logger.getLogger(Render.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            fillEmptyThreads();
        }
    }

    //http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
    /**
     * Convenience method that returns a scaled instance of the provided
     * {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @param hint one of the rendering hints that corresponds to
     * {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     * {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     * {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     * {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step scaling
     * technique that provides higher quality than the usual one-step technique
     * (only useful in downscaling cases, where {@code targetWidth} or
     * {@code targetHeight} is smaller than the original dimensions, and
     * generally only when the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage img,
            int targetWidth,
            int targetHeight,
            Object hint,
            boolean higherQuality) {
        int type = (img.getTransparency() == Transparency.OPAQUE)
                ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
}

class PixelFound {

    public boolean found;
    public Colour colour;
    public double distance;

    public PixelFound(boolean _found, Colour _colour, double _distance) {
        found = _found;
        colour = _colour;
        distance = _distance;
    }
}

class RenderThread implements Runnable {

    int threads, thisThread;
    Render render;
    Graphics2D graphics;

    public RenderThread(int _threads, int _thisThread, Render _render, Graphics2D _graphics) {
        threads = _threads;
        thisThread = _thisThread;
        render = _render;
        graphics = _graphics;
    }

    @Override
    public void run() {
        render.generateImage(graphics, thisThread, threads);
        render.finishedRendering();
    }
}
