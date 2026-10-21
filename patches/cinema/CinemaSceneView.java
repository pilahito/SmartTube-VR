package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/** Perspective scenery surrounding the original player in the Android window.
 * This is not an OpenXR session and makes no claim of stereo/head tracking.
 */
public final class CinemaSceneView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<CinemaGeometry.Face> faces;
    private final List<Path> projected = new ArrayList<>();
    private int scene, brightness=75;
    private boolean eco, curvedFrame;
    private int[] screen;
    private int frameDistance=3;
    private final Path frame=new Path();
    /** Decorative bezel around the video. "Curved" only bends this frame; the video surface stays flat. */
    public void setScreenFrame(int[] rect, boolean curved, int distance) {
        screen=rect; curvedFrame=curved; frameDistance=distance; invalidate();
    }
    public CinemaSceneView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        configure(0,false,75);
    }
    public void configure(int scene, boolean eco, int brightness) {
        this.scene=Math.max(0,Math.min(CinemaGeometry.COUNT-1,scene));
        this.eco=eco;
        this.brightness=Math.max(15,Math.min(100,brightness));
        faces=CinemaGeometry.build(this.scene,eco);
        rebuildProjection(getWidth(),getHeight());
        invalidate();
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh); rebuildProjection(w,h);
    }
    private void rebuildProjection(int w,int h) {
        projected.clear();
        if(w<=0 || h<=0 || faces==null)return;
        for(CinemaGeometry.Face face:faces) {
            Path mesh=new Path();
            for(int i=0;i<9;i+=3) {
                float[] p=CinemaGeometry.project(face.vertices[i],face.vertices[i+1],face.vertices[i+2],w,h);
                if(i==0)mesh.moveTo(p[0],p[1]);else mesh.lineTo(p[0],p[1]);
            }
            mesh.close();projected.add(mesh);
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w=getWidth(),h=getHeight();
        if(w<=0 || h<=0)return;
        int top=CinemaGeometry.skyTop(scene), horizon=CinemaGeometry.skyHorizon(scene);
        paint.setShader(new LinearGradient(0,0,0,h*.55f,top,horizon,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint);
        paint.setShader(null);
        if(scene==CinemaGeometry.RETRO) {
            paint.setColor(0xffffb26b); canvas.drawCircle(w*.5f,h*.3f,h*.16f,paint);
            paint.setColor(horizon);
            for(int i=0;i<6;i++) canvas.drawRect(w*.35f,h*(.3f+i*.022f),w*.65f,h*(.306f+i*.022f),paint);
        }
        if(scene==CinemaGeometry.BEACH_SUNSET) {
            paint.setColor(0xffffd27a); canvas.drawCircle(w*CinemaGeometry.celestialX(scene),h*.37f,h*.1f,paint);
        }
        if(scene==CinemaGeometry.BEACH_TROPICAL) {
            paint.setColor(0xfffff4c2); canvas.drawCircle(w*CinemaGeometry.celestialX(scene),h*.12f,h*.07f,paint);
        }
        if(scene==CinemaGeometry.BEACH_NIGHT) {
            paint.setColor(0xffe9eeff); canvas.drawCircle(w*CinemaGeometry.celestialX(scene),h*.14f,h*.06f,paint);
        }
        if(scene==CinemaGeometry.SPACE || scene==CinemaGeometry.CYBER || scene==CinemaGeometry.BEACH_NIGHT) {
            Random rng=new Random(91); paint.setColor(0xffc9e4ff);
            for(int i=0;i<90;i++) canvas.drawCircle(rng.nextFloat()*w,rng.nextFloat()*h*.5f,i%4==0?2:1,paint);
        }
        if(scene==CinemaGeometry.MAGIC_FOREST) {
            paint.setColor(0xffe9e4ff); canvas.drawCircle(w*.78f,h*.14f,h*.06f,paint);
        }
        for(int i=0;i<projected.size();i++) {
            paint.setColor(faces.get(i).color); canvas.drawPath(projected.get(i),paint);
        }
        if(scene==CinemaGeometry.MAGIC_FOREST) {
            // Soft firefly halos, deterministic and drawn once per frame (no animation loop).
            Random rng=new Random(23);
            int glow=eco?12:36;
            for(int i=0;i<glow;i++) {
                float x=rng.nextFloat()*w, y=h*(.25f+rng.nextFloat()*.6f), r=h*(.006f+rng.nextFloat()*.01f);
                paint.setColor(i%2==0?0x55d9ff6a:0x55fff3a0); canvas.drawCircle(x,y,r*2.6f,paint);
                paint.setColor(i%2==0?0xffd9ff6a:0xfffff3a0); canvas.drawCircle(x,y,r,paint);
            }
        }
        if(scene==CinemaGeometry.LOUNGE_NIGHT) {
            // Warm lamp light pools on both sides of the screen.
            paint.setColor(0x22ffb35a);
            canvas.drawCircle(w*.12f,h*.55f,h*.28f,paint); canvas.drawCircle(w*.88f,h*.55f,h*.28f,paint);
        }
        paint.setColor(Color.argb((100-brightness)*255/100,0,0,0));
        canvas.drawRect(0,0,w,h,paint);
        if(screen!=null) {
            float l=screen[0],t=screen[1],r=screen[2],b=screen[3],pad=Math.max(4,(b-t)*.025f);
            float bend=curvedFrame?VirtualScreen.curveDepth((int)(b-t),frameDistance):0;
            frame.reset();
            // Outer bezel: on a curved screen the top/bottom edges bow outwards toward the viewer.
            frame.moveTo(l-pad,t-pad); frame.quadTo((l+r)/2,t-pad-bend*2,r+pad,t-pad);
            frame.lineTo(r+pad,b+pad); frame.quadTo((l+r)/2,b+pad+bend*2,l-pad,b+pad); frame.close();
            paint.setColor(0xff05070b); canvas.drawPath(frame,paint);
            if(curvedFrame) { paint.setColor(0x3366f5df); canvas.drawRect(l-pad,t-pad,l,b+pad,paint); canvas.drawRect(r,t-pad,r+pad,b+pad,paint); }
        }
    }
}
