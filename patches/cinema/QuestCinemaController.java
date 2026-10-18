package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.Toast;
import com.liskovsoft.smartyoutubetv2.common.prefs.SponsorBlockData;
import com.liskovsoft.smartyoutubetv2.common.prefs.DeArrowData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import java.util.Locale;

/** Isolated scenery controls. Never replaces SmartTube's video engine or controls. */
public final class QuestCinemaController {
    private final Context context;
    private final FrameLayout root;
    private final View video, subtitles;
    private final FrameLayout.LayoutParams originalVideo, originalSubtitles;
    private final int originalPadding;
    private final SharedPreferences prefs;
    private final CinemaSceneView scenery;
    private final Button opener;
    private final View.OnLayoutChangeListener layoutListener;
    private AlertDialog dialog;
    private final boolean spanish;
    private int scene, size, brightness, distance;
    private boolean enabled, eco, pip;
    public QuestCinemaController(FrameLayout root, View video, View subtitles) {
        this.context=root.getContext(); this.root=root; this.video=video; this.subtitles=subtitles;
        originalVideo=new FrameLayout.LayoutParams((FrameLayout.LayoutParams)video.getLayoutParams());
        originalSubtitles=new FrameLayout.LayoutParams((FrameLayout.LayoutParams)subtitles.getLayoutParams());
        originalPadding=subtitles.getPaddingLeft();
        spanish=Locale.getDefault().getLanguage().equals("es");
        prefs=context.getSharedPreferences("quest_cinema_v1",Context.MODE_PRIVATE);
        scene=Math.max(0,Math.min(CinemaGeometry.COUNT-1,prefs.getInt("scene",0)));
        size=Math.max(50,Math.min(90,prefs.getInt("size",72)));
        brightness=Math.max(15,Math.min(100,prefs.getInt("brightness",75)));
        distance=Math.max(2,Math.min(5,prefs.getInt("distance",3)));
        enabled=prefs.getBoolean("enabled",true); eco=prefs.getBoolean("eco",false);
        scenery=new CinemaSceneView(context);
        root.addView(scenery,0,new FrameLayout.LayoutParams(-1,-1));
        opener=button(t("Escenarios","Scenery"),this::showMenu);
        opener.setTextSize(14);
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
    public void showMenu() {
        if(pip || (dialog!=null && dialog.isShowing())) return;
        LinearLayout panel=new LinearLayout(context); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18),dp(12),dp(18),dp(12)); panel.setBackgroundColor(0xff0d1521);
        TextView title=new TextView(context); title.setText(t("CINE · ","CINEMA · ")+scenes()[scene]);
        title.setTextColor(0xff83f5dc); title.setTextSize(23); panel.addView(title);
        TextView hint=new TextView(context);
        hint.setText(t("Escenarios en ventana Android · mando, ratón o toque","Scenery in Android window · controller, mouse or touch"));
        hint.setTextColor(0xffc2ccd9); hint.setTextSize(13); hint.setPadding(0,dp(8),0,dp(10)); panel.addView(hint);
        row(panel,t("Elegir escenario","Choose scenery"),this::chooseScene);
        row(panel,t("Tamaño de pantalla: ","Screen size: ")+size+"%",()->choices(t("Tamaño de pantalla","Screen size"),new String[]{"50%","60%","72%","82%","90%"},new int[]{50,60,72,82,90},size,v->{size=v;enabled=true;apply();}));
        row(panel,t("Distancia virtual: ","Virtual distance: ")+distance+" m",()->choices(t("Distancia virtual de la pantalla","Virtual screen distance"),new String[]{"2 m","3 m","4 m","5 m"},new int[]{2,3,4,5},distance,v->{distance=v;enabled=true;apply();}));
        row(panel,t("Brillo ambiente: ","Ambient brightness: ")+brightness+"%",()->choices(t("Brillo del escenario","Scenery brightness"),new String[]{"25%","50%","75%","100%"},new int[]{25,50,75,100},brightness,v->{brightness=v;apply();}));
        row(panel,t("ECO: ","ECO: ")+(eco?t("activado","on"):t("desactivado","off")),()->{eco=!eco;apply();refreshMenu();});
        row(panel,t("Guardar este escenario como favorito","Save this scenery as favorite"),()->{prefs.edit().putInt("favorite",scene).apply();toast(t("Favorito guardado","Favorite saved"));});
        row(panel,t("Abrir favorito","Open favorite"),()->{int favorite=prefs.getInt("favorite",-1);if(favorite>=0 && favorite<CinemaGeometry.COUNT){scene=favorite;enabled=true;apply();dismiss();}else toast(t("Primero guarda un escenario","Save a scenery first"));});
        row(panel,t("SponsorBlock / AdStop","SponsorBlock / AdStop"),this::sponsorMenu);
        row(panel,t("DeArrow: títulos y miniaturas","DeArrow: titles and thumbnails"),this::dearrowMenu);
        row(panel,enabled?t("Pantalla completa sin escenario","Fullscreen without scenery"):t("Activar escenario","Enable scenery"),()->{enabled=!enabled;apply();dismiss();});
        row(panel,t("Volver al vídeo","Back to video"),this::dismiss);
        ScrollView scroll=new ScrollView(context); scroll.setFillViewport(true); scroll.addView(panel);
        dialog=new AlertDialog.Builder(context).setView(scroll).create();
        dialog.show();
        if(dialog.getWindow()!=null) dialog.getWindow().setLayout(Math.min(dp(560),Math.max(dp(280),root.getWidth()-dp(40))),Math.max(dp(200),(int)(root.getHeight()*.88f)));
    }
    private void chooseScene() {
        dismiss();
        dialog=new AlertDialog.Builder(context).setTitle(t("Elegir escenario","Choose scenery"))
                .setSingleChoiceItems(scenes(),scene,(d,which)->{scene=which;enabled=true;apply();d.dismiss();})
                .setNegativeButton(t("Volver","Back"),null).create();dialog.show();
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
    private void apply() {
        prefs.edit().putInt("scene",scene).putInt("size",size).putInt("distance",distance).putInt("brightness",brightness)
                .putBoolean("eco",eco).putBoolean("enabled",enabled).apply();
        scenery.configure(scene,eco,brightness); scenery.setVisibility(enabled&&!pip?View.VISIBLE:View.GONE);
        applyLayout();
    }
    private void applyLayout() {
        if(root.getWidth()==0 || root.getHeight()==0)return;
        // Distance uses the same perspective rule as a fixed-size virtual screen (size / distance).
        float factor=enabled&&!pip?Math.max(.35f,Math.min(.94f,size/100f*3f/distance)):1f;
        FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(originalVideo);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(originalSubtitles);
        if(enabled&&!pip) {
            vp.width=Math.round(root.getWidth()*factor);vp.height=Math.round(root.getHeight()*factor);vp.gravity=Gravity.CENTER;
            sp.width=vp.width;sp.height=vp.height;sp.gravity=Gravity.CENTER;
        }
        video.setLayoutParams(vp);subtitles.setLayoutParams(sp);
        int p=Math.round(originalPadding*factor);subtitles.setPadding(p,subtitles.getPaddingTop(),p,subtitles.getPaddingBottom());
    }
    public void setPip(boolean pip){this.pip=pip;if(pip)dismiss();opener.setVisibility(pip?View.GONE:View.VISIBLE);apply();}
    public void release(){dismiss();root.removeOnLayoutChangeListener(layoutListener);root.removeView(opener);root.removeView(scenery);}
}
