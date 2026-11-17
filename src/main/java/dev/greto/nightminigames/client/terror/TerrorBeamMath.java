package dev.greto.nightminigames.client.terror;

/** Pure cone/range test shared by all flashlight/body samples. World occlusion is checked separately. */
public final class TerrorBeamMath {
    private TerrorBeamMath(){}
    public static double appearanceMinimumDistance(boolean flashlightOn,double range){
        return flashlightOn?Math.max(24,Math.clamp(range,6,64)+8):18;
    }
    public static boolean reaches(double dx,double dy,double dz,double lx,double ly,double lz,double range,double edgeCos){
        if(!Double.isFinite(dx+dy+dz+lx+ly+lz+range+edgeCos)||range<=0)return false;
        double distance2=dx*dx+dy*dy+dz*dz,look2=lx*lx+ly*ly+lz*lz;
        if(distance2<.0001||distance2>range*range||look2<.0001)return false;
        return (dx*lx+dy*ly+dz*lz)/Math.sqrt(distance2*look2)>=edgeCos;
    }
}
