package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic perspective meshes, rendered on demand without an animation loop. */
public final class CinemaGeometry {
    public static final int FOREST = 0, RETRO = 1, RGB = 2, CYBER = 3,
            CINEMA = 4, SPACE = 5, MOUNTAINS = 6, BEACH = 7,
            MAGIC_FOREST = 8, LOUNGE = 9, LOUNGE_NIGHT = 10, LOUNGE_MODERN = 11, LOUNGE_FUTURE = 12,
            BEACH_TROPICAL = 13, BEACH_SUNSET = 14, BEACH_NIGHT = 15;
    // Append new sceneries at the end: the saved preference stores the index.
    public static final int COUNT = 16;
    private static final String[] NAMES_ES = {"Bosque natural","Retro Arcade","Gamer RGB","Cyberpunk",
            "Cine clásico","Espacio","Montañas","Playa","Bosque mágico","Salón","Salón nocturno (chimenea)",
            "Salón moderno","Salón futurista","Playa tropical","Playa al atardecer","Playa de noche"};
    private static final String[] NAMES_EN = {"Natural forest","Retro Arcade","Gamer RGB","Cyberpunk",
            "Classic cinema","Space","Mountains","Beach","Magic forest","Living room","Night lounge (fireplace)",
            "Modern lounge","Futuristic lounge","Tropical beach","Sunset beach","Beach at night"};
    /** Display names shared by the player menu and the VR home, so selectors never drift from COUNT. */
    public static String[] names(boolean spanish) { return (spanish ? NAMES_ES : NAMES_EN).clone(); }
    /** Sky gradient colors (top, horizon) used by CinemaSceneView and the preview renderer. */
    public static int skyTop(int scene) {
        return scene==FOREST?0xff071f22:scene==MOUNTAINS?0xff253c63
                :scene==BEACH?0xff347ea2:scene==MAGIC_FOREST?0xff0a0420
                :scene==LOUNGE?0xffe3d6c2:scene==LOUNGE_NIGHT?0xff1d1830
                :scene==LOUNGE_MODERN?0xfff4f4f0:scene==LOUNGE_FUTURE?0xff0d1220
                :scene==BEACH_TROPICAL?0xff1f9be0:scene==BEACH_SUNSET?0xff3b2a6b
                :scene==BEACH_NIGHT?0xff050a1e:0xff080b22;
    }
    public static int skyHorizon(int scene) {
        return scene==FOREST?0xff70a589:scene==RETRO?0xff953f8b
                :scene==BEACH?0xfff0c99c:scene==MAGIC_FOREST?0xff4a2a7a
                :scene==LOUNGE?0xffc9b79c:scene==LOUNGE_NIGHT?0xff2c2440
                :scene==LOUNGE_MODERN?0xffe6e6e2:scene==LOUNGE_FUTURE?0xff141a2a
                :scene==BEACH_TROPICAL?0xffb8f0ff:scene==BEACH_SUNSET?0xffff9a4a
                :scene==BEACH_NIGHT?0xff1c2a52:0xff293551;
    }
    /** Horizontal screen position (0..1) of the sun/moon, shared by the sky and its water reflection. */
    public static float celestialX(int scene) {
        return scene == BEACH_SUNSET ? .2f : scene == BEACH_NIGHT ? .8f : scene == BEACH_TROPICAL ? .82f : .5f;
    }
    public static boolean isIndoor(int scene) {
        return scene == LOUNGE || scene == LOUNGE_NIGHT || scene == LOUNGE_MODERN || scene == LOUNGE_FUTURE;
    }
    public static boolean isBeach(int scene) {
        return scene == BEACH || scene == BEACH_TROPICAL || scene == BEACH_SUNSET || scene == BEACH_NIGHT;
    }
    // Palettes per beach: sand, water, trunk, leaves.
    private static int[] beachPalette(int scene) {
        return scene == BEACH_TROPICAL ? new int[]{0xffe8d39a, 0xff22c3c0, 0xff8a6a45, 0xff2fa35f}
                : scene == BEACH_SUNSET ? new int[]{0xffc98a5a, 0xffb5566a, 0xff3a2a26, 0xff2a2f2a}
                : scene == BEACH_NIGHT ? new int[]{0xff4a4a5e, 0xff12264a, 0xff2a2430, 0xff1c3a34}
                : new int[]{0xffb99d6b, 0xff1d8caa, 0xff75573b, 0xff247956};
    }
    public static final class Face {
        public final float[] vertices;
        public final int color;
        final float depth;
        Face(int color, float... vertices) {
            this.vertices = vertices;
            this.color = color;
            depth = (vertices[2] + vertices[5] + vertices[8]) / 3f;
        }
    }
    private final List<Face> faces = new ArrayList<>();
    public static float[] project(float x, float y, float z, int width, int height) {
        if (z < 1f || width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid camera coordinates");
        return new float[]{width * .5f + x / z * width * .85f,
                height * .43f - (y - 1.8f) / z * width * .85f};
    }
    private void tri(int color, float... p) { faces.add(new Face(color, p)); }
    private void quad(int c, float[] a, float[] b, float[] d, float[] e) {
        tri(c, a[0],a[1],a[2], b[0],b[1],b[2], d[0],d[1],d[2]);
        tri(c, a[0],a[1],a[2], d[0],d[1],d[2], e[0],e[1],e[2]);
    }
    private static int shade(int c, float f) {
        return 0xff000000 | ((int)(((c>>16)&255)*f)<<16)
                | ((int)(((c>>8)&255)*f)<<8) | (int)((c&255)*f);
    }
    private void box(float x, float y, float z, float w, float h, float d, int color) {
        float l=x-w/2, r=x+w/2, n=z-d/2, f=z+d/2, t=y+h;
        quad(color, new float[]{l,y,n},new float[]{r,y,n},new float[]{r,t,n},new float[]{l,t,n});
        quad(shade(color,.64f), new float[]{l,y,f},new float[]{l,y,n},new float[]{l,t,n},new float[]{l,t,f});
        quad(shade(color,.78f), new float[]{r,y,n},new float[]{r,y,f},new float[]{r,t,f},new float[]{r,t,n});
        quad(shade(color,.9f), new float[]{l,t,n},new float[]{r,t,n},new float[]{r,t,f},new float[]{l,t,f});
    }
    private void peak(float x, float y, float z, float radius, float h, int color) {
        tri(color,x-radius,y,z-radius, x+radius,y,z-radius, x,y+h,z);
        tri(shade(color,.66f),x+radius,y,z-radius, x+radius,y,z+radius, x,y+h,z);
        tri(shade(color,.8f),x-radius,y,z+radius, x-radius,y,z-radius, x,y+h,z);
    }
    /** Enclosed living room: walls are split in depth slices so painter sorting stays correct. */
    private void lounge(int scene, boolean eco) {
        if(scene==LOUNGE_MODERN) { modernLounge(eco); return; }
        if(scene==LOUNGE_FUTURE) { futureLounge(eco); return; }
        boolean night = scene==LOUNGE_NIGHT;
        room(night ? 0xff2c2440 : 0xffc9b79c, night ? 0xff1d1830 : 0xffe3d6c2, eco, 0);
        // Rug in front of the screen.
        quad(night ? 0xff5a2238 : 0xff8c3b3b, new float[]{-4,-.98f,6},new float[]{4,-.98f,6},new float[]{4,-.98f,16},new float[]{-4,-.98f,16});
        // Sofas facing the screen along both sides.
        int sofa = night ? 0xff3e4f7a : 0xff4f6d5a;
        for(int s=-1;s<=1;s+=2) {
            float x=s*7.5f;
            box(x,-1,10,2.2f,1.1f,5,sofa);
            box(x+s*.9f,.1f,10,.5f,1.3f,5,shade(sofa,.85f));
            if(!eco) for(int c=0;c<3;c++) box(x-s*.1f,.1f,8.4f+c*1.6f,.9f,.6f,.3f,night ? 0xffb07ad0 : 0xffe0c070);
            // Floor lamp with a warm shade.
            box(x,-1,15.5f,.12f,4.2f,.12f,0xff3a3a3a);
            box(x,3.2f,15.5f,.9f,.8f,.9f,night ? 0xffffc46b : 0xfff7e3b0);
            // Bookshelf and plant further back.
            box(s*9.6f,-1,22,1.2f,5.5f,3,0xff6a4a2e);
            if(!eco) for(int r=0;r<4;r++) box(s*9.1f,-.3f+r*1.3f,22,.3f,.8f,2.6f,r%2==0?0xff9b3d3d:0xff3d6b9b);
            box(s*6,-1,28,.8f,.9f,.8f,0xff7a4e2e);
            peak(s*6,-.1f,28,.8f,2.2f,0xff2f7a45);
        }
        // Low coffee table and TV cabinet under the virtual screen.
        box(0,-1,12,2.6f,.6f,1.4f,0xff5b3a24);
        box(0,-1,31,9,1,1.2f,0xff3b2a20);
        // Window (day) or fireplace (night) on the back wall.
        if(night) {
            box(-6.5f,-1,33.4f,3.4f,2.6f,1,0xff4a3a36);
            box(-6.5f,-.7f,32.85f,2,1.3f,.05f,0xffff8a2b);
            if(!eco) box(-6.5f,-.6f,32.8f,1.2f,.8f,.05f,0xffffd25a);
        } else {
            box(6.5f,1.5f,33.9f,4,3,.05f,0xff9fd3ef);
            box(6.5f,2.95f,33.85f,4,.1f,.05f,0xffffffff);
        }
    }
    private static final float BACK = 34, SIDE = 11, TOP = 7;
    /** Walls and ceiling, split in depth slices; neon > 0 adds glowing edge strips per slice. */
    private void room(int wall, int ceiling, boolean eco, int neon) {
        float step = eco ? 8 : 4;
        for(float z=2;z<BACK;z+=step) {
            float z2=Math.min(BACK,z+step);
            quad(shade(wall,.72f), new float[]{-SIDE,-1,z2},new float[]{-SIDE,-1,z},new float[]{-SIDE,TOP,z},new float[]{-SIDE,TOP,z2});
            quad(shade(wall,.82f), new float[]{SIDE,-1,z},new float[]{SIDE,-1,z2},new float[]{SIDE,TOP,z2},new float[]{SIDE,TOP,z});
            quad(ceiling, new float[]{-SIDE,TOP,z},new float[]{SIDE,TOP,z},new float[]{SIDE,TOP,z2},new float[]{-SIDE,TOP,z2});
            if(neon!=0) {
                quad(neon, new float[]{-SIDE+.01f,-.6f,z2},new float[]{-SIDE+.01f,-.6f,z},new float[]{-SIDE+.01f,-.4f,z},new float[]{-SIDE+.01f,-.4f,z2});
                quad(neon, new float[]{SIDE-.01f,-.6f,z},new float[]{SIDE-.01f,-.6f,z2},new float[]{SIDE-.01f,-.4f,z2},new float[]{SIDE-.01f,-.4f,z});
                quad(neon, new float[]{-SIDE+.6f,TOP-.01f,z},new float[]{-SIDE+.9f,TOP-.01f,z},new float[]{-SIDE+.9f,TOP-.01f,z2},new float[]{-SIDE+.6f,TOP-.01f,z2});
                quad(neon, new float[]{SIDE-.9f,TOP-.01f,z},new float[]{SIDE-.6f,TOP-.01f,z},new float[]{SIDE-.6f,TOP-.01f,z2},new float[]{SIDE-.9f,TOP-.01f,z2});
            }
        }
        quad(wall, new float[]{-SIDE,-1,BACK},new float[]{SIDE,-1,BACK},new float[]{SIDE,TOP,BACK},new float[]{-SIDE,TOP,BACK});
    }
    /** Minimal white/grey lounge with a panoramic window and low sofas. */
    private void modernLounge(boolean eco) {
        room(0xffe6e6e2, 0xfff4f4f0, eco, 0);
        quad(0xff9aa3a8, new float[]{-5,-.98f,6},new float[]{5,-.98f,6},new float[]{5,-.98f,17},new float[]{-5,-.98f,17});
        for(int s=-1;s<=1;s+=2) {
            float x=s*7.2f;
            box(x,-1,11,2.6f,.75f,6,0xff5d646b);
            box(x+s*1.1f,-.25f,11,.4f,.8f,6,0xff4c5258);
            if(!eco) for(int c=0;c<2;c++) box(x-s*.2f,-.25f,9.5f+c*3,1,.5f,.25f,c==0?0xffe0a43a:0xff3a8fb0);
            // Slim arc lamp and a tall plant.
            box(x+s*.4f,-1,16.5f,.1f,5,.1f,0xff2b2b2b);
            box(x-s*.6f,3.8f,16.5f,1.4f,.1f,.1f,0xff2b2b2b);
            box(x-s*1.3f,3.3f,16.5f,.7f,.5f,.7f,0xfffff1c9);
            box(s*9.8f,-1,26,1,1.2f,1,0xffffffff);
            peak(s*9.8f,.2f,26,.9f,3,0xff2f7a45);
            if(!eco) box(s*10.4f,2,20,.1f,2.4f,3.6f,s<0?0xff2d5d8a:0xffd06a4a); // wall art
        }
        box(0,-1,12.5f,3.2f,.45f,1.6f,0xffffffff);
        box(0,-1,31,10,.7f,1.2f,0xffd8d8d4);
        // Panoramic window with a city skyline.
        box(0,1.6f,33.9f,16,4.2f,.05f,0xff8fb7d6);
        if(!eco) for(int i=0;i<8;i++) box(-7+i*2,1.6f,33.85f,1.3f,.8f+(i*37%5)*.45f,.04f,0xff6b8aa6);
        box(0,1.55f,33.8f,16.4f,.12f,.05f,0xff3a3a3a);
    }
    /** Sci-fi lounge: dark panels, neon edge strips, floating sofas and holo screens. */
    private void futureLounge(boolean eco) {
        room(0xff141a2a, 0xff0d1220, eco, 0xff2fe6ff);
        quad(0xff232c44, new float[]{-5,-.98f,6},new float[]{5,-.98f,6},new float[]{5,-.98f,17},new float[]{-5,-.98f,17});
        quad(0xffb44cff, new float[]{-5,-.97f,6},new float[]{5,-.97f,6},new float[]{5,-.97f,6.2f},new float[]{-5,-.97f,6.2f});
        for(int s=-1;s<=1;s+=2) {
            float x=s*7.4f;
            box(x,-.6f,11,2.4f,.8f,5.5f,0xff2a3350);       // hovering seat
            box(x,-.65f,11,2.2f,.05f,5.3f,0xff2fe6ff);      // under-glow
            box(x+s*1,.2f,11,.4f,1.2f,5.5f,0xff20283f);
            box(s*10.6f,1.2f,18,.05f,2.4f,4,s<0?0xff3a7bff:0xffff4bd8); // holo panels
            if(!eco) {
                box(s*10.55f,1.6f,18,.04f,.12f,3.4f,0xffd6f8ff);
                box(s*10.55f,2.2f,18,.04f,.12f,2.6f,0xffd6f8ff);
                box(s*6,-1,26,.3f,4,.3f,0xff2fe6ff);         // light pillars
            }
        }
        box(0,-.7f,12.5f,2.6f,.35f,1.6f,0xff2a3350);
        box(0,-.75f,12.5f,2.4f,.05f,1.4f,0xffb44cff);
        box(0,-1,31,10,.8f,1.2f,0xff1b2236);
        // Starfield port behind the screen.
        box(0,1.4f,33.9f,14,4.4f,.05f,0xff070a1c);
        if(!eco) for(int i=0;i<10;i++) box(-6+(i*53%12),1.8f+(i*29%4),33.85f,.08f,.08f,.04f,0xffffffff);
    }
    public static List<Face> build(int scene, boolean eco) {
        if (scene < 0 || scene >= COUNT) throw new IllegalArgumentException("Unknown scene");
        CinemaGeometry g = new CinemaGeometry();
        Random rng = new Random(137 + scene);
        int floor = scene==FOREST ? 0xff183c2b : isBeach(scene) ? beachPalette(scene)[0]
                : scene==MOUNTAINS ? 0xff283c48 : scene==MAGIC_FOREST ? 0xff1a1236
                : scene==LOUNGE ? 0xff6b4a33 : scene==LOUNGE_NIGHT ? 0xff3a2a22
                : scene==LOUNGE_MODERN ? 0xffb9a58c : scene==LOUNGE_FUTURE ? 0xff10141f : 0xff10152a;
        // Beaches end the sand where the sea starts: a single 2..120 floor would be depth-sorted over the water.
        float floorEnd = isBeach(scene) ? 36 : 120;
        g.quad(floor,new float[]{-90,-1,2},new float[]{90,-1,2},new float[]{90,-1,floorEnd},new float[]{-90,-1,floorEnd});
        int count = eco ? 10 : 26;
        if(isIndoor(scene)) {
            g.lounge(scene, eco);
            count = 0;
        }
        for(int i=0;i<count;i++) {
            float z=9+i*2.6f, side=(i%2==0?-1:1), x=side*(4.3f+rng.nextFloat()*z*.48f);
            if(scene==FOREST) {
                float h=4+rng.nextFloat()*5;
                g.box(x,-1,z,.35f,h*.55f,.4f,0xff5b4030);
                g.peak(x,.1f,z,2.1f,h,0xff246b49);
                if(!eco) g.peak(x,1.7f,z,1.7f,h*.8f,0xff35945f);
            } else if(scene==RETRO) {
                g.box(x,-1,z,1.7f,3.5f,1.4f,0xff34204d);
                g.box(x,1.1f,z-.73f,1.25f,.95f,.08f,i%2==0?0xff38e9ed:0xffe742d3);
                g.box(x,.6f,z-.85f,1.5f,.15f,.7f,0xffba42df);
                g.box(x,2.4f,z-.74f,1.5f,.22f,.08f,0xffffb347);
            } else if(scene==CYBER) {
                float h=5+rng.nextFloat()*15;
                g.box(x,-1,z,2.2f,h,2.5f,0xff22304a);
                int neon=i%2==0?0xff32d9e8:0xffda44cf;
                for(float y=0;y<h;y+=eco?3:1.8f) g.box(x,y,z-1.28f,1.5f,.12f,.06f,neon);
            } else if(scene==RGB) {
                int neon=i%3==0?0xff9d58ff:i%3==1?0xff20ead4:0xffff4ba4;
                g.box(x,-1,z,.16f,7,.16f,neon);
                g.box(x,5.8f,z,3.4f,.12f,.12f,neon);
                g.box(x,-.7f,z,2,1,1.4f,0xff263345);
            } else if(scene==CINEMA) {
                g.box(x,-1,z,1.6f,1.35f,1.8f,0xff7d243f);
                g.box(x,.3f,z+.6f,1.6f,1.3f,.3f,0xffa53956);
            } else if(scene==MOUNTAINS) {
                g.peak(x,-1,z,4+rng.nextFloat()*4,8+rng.nextFloat()*9,0xff6e8898);
            } else if(scene==MAGIC_FOREST) {
                float h=4+rng.nextFloat()*5;
                g.box(x,-1,z,.4f,h*.55f,.4f,0xff3b2a55);
                g.peak(x,.1f,z,2f,h,i%2==0?0xff2b6f7a:0xff5a3a8f);
                if(!eco) g.peak(x,1.6f,z,1.6f,h*.8f,i%3==0?0xff3fb7a8:0xff7d55c7);
                // Glowing mushrooms beside the path.
                float mx=side*(2.6f+rng.nextFloat()*1.4f);
                g.box(mx,-1,z+.8f,.12f,.45f,.12f,0xffe8dcc8);
                g.peak(mx,-.58f,z+.8f,.42f,.3f,i%2==0?0xffff5fd2:0xff57ffe0);
                // Fireflies: tiny bright cubes floating between the trees.
                int flies = eco ? 0 : 1;
                for(int f=0;f<flies;f++) g.box(side*(2+rng.nextFloat()*z*.4f),.3f+rng.nextFloat()*3.5f,
                        z+rng.nextFloat()*2,.09f,.09f,.09f,f%2==0?0xffd9ff6a:0xfffff3a0);
            } else if(isBeach(scene)) {
                int[] c=beachPalette(scene);
                if(scene!=BEACH && z>34) continue; // keep the new beaches' palms on the sand
                float h=scene==BEACH?6:5+rng.nextFloat()*2.5f;
                g.box(x,-1,z,.3f,h,.3f,c[2]);
                g.peak(x,h-1.4f,z,3,.7f,c[3]);
                if(scene==BEACH_TROPICAL && !eco) g.peak(x,h-1.1f,z,2.2f,.9f,shade(c[3],1.2f));
                if(scene!=BEACH && !eco && i%3==0) g.peak(side*(3+rng.nextFloat()*3),-1,z+1,.6f,.5f,shade(c[0],.7f)); // rocks
            } else {
                g.box(x,-1,z,.1f,.5f,.1f,0xff77cbe8);
            }
        }
        if(isBeach(scene)) {
            int water=beachPalette(scene)[1];
            g.quad(water,new float[]{-90,-.8f,36},new float[]{90,-.8f,36},new float[]{90,-.8f,120},new float[]{-90,-.8f,120});
            if(scene!=BEACH) {
                // Foam lines; sunset and night add a sun/moon reflection path.
                for(int i=0;i<(eco?1:3);i++) g.quad(shade(0xffffffff,scene==BEACH_NIGHT?.45f:.95f),
                        new float[]{-90,-.79f,36.4f+i*3},new float[]{90,-.79f,36.4f+i*3},new float[]{90,-.79f,36.7f+i*3},new float[]{-90,-.79f,36.7f+i*3});
                if(scene==BEACH_SUNSET || scene==BEACH_NIGHT) {
                    int glint=scene==BEACH_SUNSET?0xffffb35c:0xffcfd8ff;
                    for(int i=0;i<(eco?3:8);i++) { float z=44+i*8, w=1.2f+i*.5f, cx=(celestialX(scene)-.5f)/.85f*z;
                        g.quad(glint,new float[]{cx-w,-.78f,z},new float[]{cx+w,-.78f,z},new float[]{cx+w,-.78f,z+1.2f},new float[]{cx-w,-.78f,z+1.2f}); }
                }
                if(scene==BEACH_TROPICAL) {
                    g.box(-8,-1,22,4,2.2f,3,0xffa07a4a);          // beach hut
                    g.peak(-8,1.2f,22,2.8f,1.6f,0xffd8b86a);
                    if(!eco) { g.box(5.5f,-1,15,.12f,2.6f,.12f,0xffeeeeee); g.peak(5.5f,1.4f,15,1.6f,.6f,0xffff6f61); } // parasol
                }
            }
        }
        if(scene==RETRO || scene==RGB || scene==CYBER || scene==SPACE) {
            int neon=scene==RETRO?0xffb641c5:0xff28688d;
            for(int i=-12;i<=12;i+=eco?3:1) g.box(i*2,-.97f,44,.025f,.02f,80,neon);
            for(int i=5;i<90;i+=eco?12:4) g.box(0,-.96f,i,65,.02f,.025f,neon);
        }
        if(scene==MAGIC_FOREST && !eco)
            for(int i=0;i<12;i++) g.box(-6+rng.nextFloat()*12,.2f+rng.nextFloat()*3,6+rng.nextFloat()*20,.08f,.08f,.08f,0xffeaff8a);
        Collections.sort(g.faces, (a,b)->Float.compare(b.depth,a.depth));
        return Collections.unmodifiableList(g.faces);
    }
}
