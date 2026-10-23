package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.liskovsoft.smartyoutubetv2.common.prefs.SponsorBlockData;
import com.liskovsoft.smartyoutubetv2.common.prefs.DeArrowData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import java.util.Locale;

/** Isolated scenery controls. Never replaces SmartTube's video engine or controls. */
public final class QuestCinemaController {
    /** Applies a resize mode and aspect to the real player immediately (SurfacePlaybackFragment). */
    public interface FormatApplier { void apply(int resizeMode, float aspect); }
    private final FormatApplier formatApplier;
    private final Context context;
    private final FrameLayout root;
    private final View video, subtitles;
    private final FrameLayout.LayoutParams originalVideo, originalSubtitles;
    private final int originalPadding;
    private final SharedPreferences prefs;
    private final CinemaSceneView scenery;
    private final Button opener;
    private final View.OnLayoutChangeListener layoutListener;
    private Dialog dialog;
    private QuestPanels.ScenesPanel scenesPanel;
    private final boolean spanish;
    private int scene, size, brightness, distance, format, curvature, height;
    private boolean enabled, eco, pip, curved;
    public QuestCinemaController(FrameLayout root, View video, View subtitles, FormatApplier formatApplier) {
        this.formatApplier=formatApplier;
        this.context=root.getContext(); this.root=root; this.video=video; this.subtitles=subtitles;
        originalVideo=new FrameLayout.LayoutParams((FrameLayout.LayoutParams)video.getLayoutParams());
        originalSubtitles=new FrameLayout.LayoutParams((FrameLayout.LayoutParams)subtitles.getLayoutParams());
        originalPadding=subtitles.getPaddingLeft();
        spanish=Locale.getDefault().getLanguage().equals("es");
        prefs=context.getSharedPreferences("quest_cinema_v1",Context.MODE_PRIVATE);
        scene=Math.max(0,Math.min(CinemaGeometry.COUNT-1,prefs.getInt("scene",0)));
        size=Math.round(VirtualScreen.clampSize(prefs.getInt("size",70))/5f)*5; // slider moves in 5 % steps
        brightness=Math.max(15,Math.min(100,prefs.getInt("brightness",75)));
        distance=VirtualScreen.clampDistance(prefs.getInt("distance",3));
        format=VirtualScreen.clampMode(prefs.getInt("format",VirtualScreen.FIT));
        curved=prefs.getBoolean("curved",false);
        curvature=VirtualScreen.clampCurvature(prefs.getInt("curvature",VirtualScreen.CURVATURE_DEFAULT));
        height=VirtualScreen.clampHeight(prefs.getInt("height",0));
        enabled=prefs.getBoolean("enabled",true); eco=prefs.getBoolean("eco",false);
        scenery=new CinemaSceneView(context);
        root.addView(scenery,0,new FrameLayout.LayoutParams(-1,-1));
        opener=button(t("Escenarios","Scenery"),this::showMenu);
        opener.setTextSize(14);
        opener.setTag(QuestHud.OPENER_TAG);
        opener.setContentDescription(t("Abrir escenarios y controles de cine","Open scenery and cinema controls"));
        FrameLayout.LayoutParams op=new FrameLayout.LayoutParams(dp(146),dp(48),Gravity.TOP|Gravity.START);
        op.setMargins(dp(16),dp(14),dp(16),0);
        root.addView(opener,op);
        layoutListener=(v,l,t,r,b,ol,ot,or,ob)->{ if(r-l!=or-ol || b-t!=ob-ot) applyLayout(); };
        root.addOnLayoutChangeListener(layoutListener);
        apply();
    }
    private String t(String es,String en) { return spanish?es:en; }
    private String[] scenes() { return CinemaGeometry.names(spanish); }
    private int dp(int n) { return (int)(n*context.getResources().getDisplayMetrics().density+.5f); }
    private GradientDrawable background(int color,int stroke) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(14));
        d.setStroke(dp(2),stroke); return d;
    }
    private Button button(String text,Runnable action) {
        Button b=new Button(context); b.setText(text); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setTextSize(16); b.setGravity(Gravity.CENTER); b.setPadding(dp(14),dp(8),dp(14),dp(8));
        b.setMinHeight(dp(52)); b.setFocusable(true);
        StateListDrawable state=new StateListDrawable();
        state.addState(new int[]{android.R.attr.state_pressed},background(0xff2f7b83,0xffa0fff0));
        state.addState(new int[]{android.R.attr.state_focused},background(0xff234f65,0xff66f5df));
        state.addState(new int[]{},background(0xff182331,0xff40546c));
        b.setBackground(state); b.setOnClickListener(v->action.run()); return b;
    }
    private void row(LinearLayout list,String text,Runnable action) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(5),0,dp(5));
        list.addView(button(text,action),p);
    }
    /** Shows a panel as a transparent dialog docked at the top right (like docs/interfaz/reproductor.jpg). */
    private void showPanel(View content,int gravity,int widthDp,int heightDp) {
        dismiss();
        Dialog d=new Dialog(context);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(content);
        d.setCanceledOnTouchOutside(true);
        d.setOnDismissListener(x->{ if(dialog==d) { dialog=null; scenesPanel=null; } });
        dialog=d; d.show();
        Window w=d.getWindow();
        if(w==null) return;
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); w.setGravity(gravity);
        int width=Math.min(dp(widthDp),Math.max(dp(280),root.getWidth()-dp(40)));
        // Never taller than the player: long panels (Más opciones, Pantalla virtual) scroll instead of overflowing.
        int maxHeight=Math.max(dp(200),root.getHeight()-dp(48));
        int height;
        if(heightDp>0) height=Math.min(dp(heightDp),maxHeight);
        else {
            content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            height=content.getMeasuredHeight()>maxHeight?maxHeight:ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        w.setLayout(width,height);
        WindowManager.LayoutParams lp=w.getAttributes(); lp.x=dp(24); lp.y=dp(24); w.setAttributes(lp);
    }
    /** "Reproductor VR": the options panel (replaces the old list of buttons). */
    public void showMenu() {
        if(pip || (dialog!=null && dialog.isShowing())) return;
        showOptions(false);
    }
    private void showOptions(boolean more) {
        // Header line as in the mockup ("4K • 16:9 • 60 FPS"); without video data, the scenery and format.
        String subtitle=QuestHud.videoSummary();
        if(subtitle==null || subtitle.isEmpty()) subtitle=scenes()[scene]+" • "+VirtualScreen.modeNames(spanish)[format];
        showPanel(QuestPanels.options(context,spanish,more,subtitle,QuestHud.subtitleLabel(),brightness,eco,enabled,QuestHud.isClassic(context),new QuestPanels.OptionsActions(){
            public void videoSettings(){if(QuestHud.isActive()){dismiss();QuestHud.openVideoSettings();}else screenMenu();}
            public void subtitles(){dismiss();QuestHud.openSubtitles();}
            public void scenes(){chooseScene();}
            public void screen(){screenMenu();}
            public void brightness(){choices(t("Brillo del escenario","Scenery brightness"),new String[]{"25%","50%","75%","100%"},new int[]{25,50,75,100},brightness,v->{brightness=v;apply();});}
            public void toggleEco(){eco=!eco;apply();showOptions(true);}
            public void sponsor(){sponsorMenu();}
            public void dearrow(){dearrowMenu();}
            public void saveFavorite(){prefs.edit().putInt("favorite",scene).apply();toast(t("Favorito guardado","Favorite saved"));}
            public void openFavorite(){int favorite=prefs.getInt("favorite",-1);if(favorite>=0 && favorite<CinemaGeometry.COUNT){scene=favorite;enabled=true;apply();dismiss();}else toast(t("Primero guarda un escenario","Save a scenery first"));}
            public void toggleScenery(){enabled=!enabled;apply();dismiss();}
            public void toggleClassic(){QuestHud.setClassic(context,!QuestHud.isClassic(context));showOptions(true);}
            public void more(){showOptions(true);}
            public void close(){if(more)showOptions(false);else dismiss();}
        }),Gravity.TOP|Gravity.END,420,0);
    }
    /** Gear of the HUD: opens "Pantalla virtual" directly. */
    public void showScreenSettings(){if(!pip)screenMenu();}
    /** "Pantalla virtual": the settings panel; every control applies live and is saved in quest_cinema_v1. */
    private void screenMenu() {
        showPanel(new QuestPanels.ScreenPanel(context,spanish,curved,curvature,format,size,distance,height,new QuestPanels.ScreenListener(){
            public void curved(boolean on){QuestCinemaController.this.curved=on;enabled=true;apply();}
            public void curvature(int percent){curvature=VirtualScreen.clampCurvature(percent);apply();}
            public void format(int mode){format=VirtualScreen.clampMode(mode);applyFormat();apply();}
            public void size(int percent){size=VirtualScreen.clampSize(percent);enabled=true;apply();}
            public void distance(int meters){distance=VirtualScreen.clampDistance(meters);enabled=true;apply();}
            public void height(int value){height=VirtualScreen.clampHeight(value);enabled=true;apply();}
            public void preset(int which){
                int[] p=VirtualScreen.preset(which);
                size=p[0];distance=p[1];curved=p[2]==1;curvature=p[3];format=p[4];height=p[5];enabled=true;applyFormat();apply();
            }
        }).view,Gravity.TOP|Gravity.END,560,0);
    }
    /** Stores the format in SmartTube's own PlayerData (so its loader reapplies it) and applies it now. */
    private void applyFormat() {
        int resize=VirtualScreen.resizeMode(format); float aspect=VirtualScreen.aspect(format);
        PlayerData data=PlayerData.instance(context);
        data.setResizeMode(resize); data.setAspectRatio(aspect);
        if(formatApplier!=null) formatApplier.apply(resize,aspect);
    }
    /** "Escenarios": 4x4 grid with the real scenery renders (docs/interfaz/escenarios.jpg). */
    private void chooseScene() {
        scenesPanel=new QuestPanels.ScenesPanel(context,spanish,scene,new QuestPanels.ScenesListener(){
            public void choose(int s){scene=s;enabled=true;apply();if(scenesPanel!=null)scenesPanel.select(s);}
            public void close(){refreshMenu();}
        });
        QuestPanels.ScenesPanel panel=scenesPanel;
        showPanel(panel.view,Gravity.CENTER,880,0);
        scenesPanel=panel;
    }
    private interface Setter { void set(int value); }
    private void choices(String title,String[] labels,int[] values,int current,Setter setter) {
        dismiss();int checked=-1;for(int i=0;i<values.length;i++)if(values[i]==current)checked=i;
        dialog=new AlertDialog.Builder(context).setTitle(title).setSingleChoiceItems(labels,checked,(d,index)->{setter.set(values[index]);d.dismiss();})
                .setNegativeButton(t("Volver","Back"),null).create();dialog.show();
    }
    private void sponsorMenu() {
        dismiss();
        String[] labels={t("Equilibrado","Balanced"),t("Intensivo","Aggressive"),t("Desactivar SponsorBlock","Disable SponsorBlock")};
        dialog=new AlertDialog.Builder(context).setTitle("SponsorBlock / AdStop")
                .setItems(labels,(d,index)->{
                    SponsorBlockData data=SponsorBlockData.instance(context);
                    if(index==2)data.setSponsorBlockEnabled(false);else data.applyAdStopPreset(index==1);
                    toast(t("Perfil guardado. Se aplica al cargar el siguiente vídeo.","Profile saved. Applies when the next video loads."));
                }).setNegativeButton(t("Volver","Back"),null).create();dialog.show();
    }
    private void dearrowMenu() {
        dismiss();
        String[] labels={t("Limpiar títulos y miniaturas","Clean titles and thumbnails"),t("Recuperar originales","Restore originals")};
        dialog=new AlertDialog.Builder(context).setTitle("DeArrow").setItems(labels,(d,index)->{
            DeArrowData data=DeArrowData.instance(context);
            if(index==0)MainUIData.instance(context).setUnlocalizedTitlesEnabled(false);
            data.setDeArrowEnabled(index==0);data.setReplaceTitlesEnabled(index==0);data.setReplaceThumbnailsEnabled(index==0);
            toast(t("Guardado. Se aplica a los vídeos que se carguen después.","Saved. Applies to videos loaded afterwards."));
        }).setNegativeButton(t("Volver","Back"),null).create();dialog.show();
    }
    private void refreshMenu(){dismiss();showMenu();}
    private void toast(String s){Toast.makeText(context,s,Toast.LENGTH_LONG).show();}
    private void dismiss(){if(dialog!=null){dialog.dismiss();dialog=null;}}
    /** Fullscreen button of the HUD: hide or show the scenery around the video. */
    public void toggleScenery(){enabled=!enabled;apply();}
    private void apply() {
        QuestTheme.apply(scene);QuestHud.refreshTheme();
        prefs.edit().putInt("scene",scene).putInt("size",size).putInt("distance",distance).putInt("brightness",brightness).putInt("format",format).putBoolean("curved",curved)
                .putInt("curvature",curvature).putInt("height",height).putBoolean("eco",eco).putBoolean("enabled",enabled).apply();
        scenery.configure(scene,eco,brightness); scenery.setVisibility(enabled&&!pip?View.VISIBLE:View.GONE);
        applyLayout();
    }
    private void applyLayout() {
        if(root.getWidth()==0 || root.getHeight()==0)return;
        // Distance uses the same perspective rule as a fixed-size virtual screen (size / distance).
        float factor=enabled&&!pip?VirtualScreen.factor(size,distance):1f;
        FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(originalVideo);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(originalSubtitles);
        int[] rect=VirtualScreen.rect(root.getWidth(),root.getHeight(),size,distance,height);
        if(enabled&&!pip) {
            // "Altura" moves the screen inside the free space: same rect for the video and the scenery frame.
            vp.width=rect[2]-rect[0];vp.height=rect[3]-rect[1];vp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
            vp.leftMargin=0;vp.rightMargin=0;vp.bottomMargin=0;vp.topMargin=rect[1];
            sp.width=vp.width;sp.height=vp.height;sp.gravity=vp.gravity;
            sp.leftMargin=0;sp.rightMargin=0;sp.bottomMargin=0;sp.topMargin=rect[1];
        }
        video.setLayoutParams(vp);subtitles.setLayoutParams(sp);
        if(enabled&&!pip) scenery.setScreenFrame(rect,curved,distance,curvature);
        else scenery.setScreenFrame(null,false,distance);
        int p=Math.round(originalPadding*factor);subtitles.setPadding(p,subtitles.getPaddingTop(),p,subtitles.getPaddingBottom());
    }
    public void setPip(boolean pip){this.pip=pip;if(pip)dismiss();opener.setVisibility(pip||QuestHud.isActive()?View.GONE:View.VISIBLE);apply();}
    public void release(){dismiss();root.removeOnLayoutChangeListener(layoutListener);root.removeView(opener);root.removeView(scenery);}
}
