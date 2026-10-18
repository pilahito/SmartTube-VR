import com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema.CinemaGeometry;
import java.util.List;

public class CinemaGeometryTest {
    // Software-rendered on a Quest 2D panel: keep every scene cheap to draw.
    private static final int MAX_FULL_FACES = 2400, MAX_ECO_FACES = 700;
    public static void main(String[] args) {
        if(CinemaGeometry.COUNT!=16) throw new AssertionError("Expected sixteen sceneries");
        if(CinemaGeometry.names(true).length!=CinemaGeometry.COUNT || CinemaGeometry.names(false).length!=CinemaGeometry.COUNT)
            throw new AssertionError("Every scenery needs a Spanish and English name");
        if(!"Bosque mágico".equals(CinemaGeometry.names(true)[CinemaGeometry.MAGIC_FOREST])
                || !"Salón".equals(CinemaGeometry.names(true)[CinemaGeometry.LOUNGE]))
            throw new AssertionError("New scenery names are not wired to their constants");
        String[] es=CinemaGeometry.names(true);
        if(!es[CinemaGeometry.LOUNGE_MODERN].equals("Salón moderno") || !es[CinemaGeometry.LOUNGE_FUTURE].equals("Salón futurista")
                || !es[CinemaGeometry.BEACH_TROPICAL].equals("Playa tropical") || !es[CinemaGeometry.BEACH_SUNSET].equals("Playa al atardecer")
                || !es[CinemaGeometry.BEACH_NIGHT].equals("Playa de noche"))
            throw new AssertionError("Lounge/beach names are not wired to their constants");
        for(int scene=0;scene<CinemaGeometry.COUNT;scene++) {
            if(CinemaGeometry.isIndoor(scene)==CinemaGeometry.isBeach(scene) && CinemaGeometry.isIndoor(scene))
                throw new AssertionError("A scenery cannot be both indoor and beach");
            float sx=CinemaGeometry.celestialX(scene);
            if(sx<0||sx>1) throw new AssertionError("Sun/moon outside the sky");
            List<CinemaGeometry.Face> full=CinemaGeometry.build(scene,false);
            List<CinemaGeometry.Face> eco=CinemaGeometry.build(scene,true);
            if(eco.size()>=full.size()) throw new AssertionError("ECO must reduce geometry in scene "+scene);
            if(full.size()>MAX_FULL_FACES || eco.size()>MAX_ECO_FACES)
                throw new AssertionError("Scene "+scene+" exceeds face budget: "+full.size()+"/"+eco.size());
            for(CinemaGeometry.Face face:full) for(int i=0;i<9;i+=3) {
                float[] point=CinemaGeometry.project(face.vertices[i],face.vertices[i+1],face.vertices[i+2],1920,1080);
                if(!Float.isFinite(point[0])||!Float.isFinite(point[1])) throw new AssertionError("Invalid projection");
            }
            System.out.println("Scene "+scene+" ("+CinemaGeometry.names(false)[scene]+"): "+full.size()+" faces, ECO "+eco.size());
        }
        try { CinemaGeometry.build(CinemaGeometry.COUNT,false); throw new AssertionError("Scene range not checked"); }
        catch(IllegalArgumentException expected) { }
        float[] near=CinemaGeometry.project(2,2,3,1920,1080);
        float[] far=CinemaGeometry.project(2,2,6,1920,1080);
        if(Math.abs(near[0]-960)<=Math.abs(far[0]-960)) throw new AssertionError("Distance must reduce perspective size");
        try { CinemaGeometry.project(0,0,0,1920,1080); throw new AssertionError("Near plane not checked"); }
        catch(IllegalArgumentException expected) { }
        System.out.println("Sixteen scene meshes, ECO and face budgets, perspective and near-plane checks passed");
    }
}
