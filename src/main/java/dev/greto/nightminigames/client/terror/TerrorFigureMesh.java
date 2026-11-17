package dev.greto.nightminigames.client.terror;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import dev.greto.nightminigames.terror.TerrorEffectType;

/** Cached rounded geometry in model space; no entity, billboard or client light. */
final class TerrorFigureMesh {
    record Point(float x,float y,float z,float u,float v,int material,int shade){}
    private static final Map<TerrorEffectType,List<Point>> CACHE=new EnumMap<>(TerrorEffectType.class);
    static {for(var type:TerrorEffectType.values())CACHE.put(type,build(type));}
    static void draw(VertexConsumer out,PoseStack pose,TerrorEffectType kind,float alpha){
        for(var p:CACHE.get(kind))out.addVertex(pose.last().pose(),p.x,p.y,p.z).setUv(p.u,p.v).setColor(p.material,p.shade,0,Math.round(alpha*255));
    }
    private static List<Point> build(TerrorEffectType kind){
        List<Point> out=new ArrayList<>();
        if(kind==TerrorEffectType.CABEZA){
            sphere(out,new Vec3(0,.64,0),new Vec3(.29,.39,.24),10,0);
            sphere(out,new Vec3(0,.18,.08),new Vec3(.48,.20,.20),12,0);
            return List.copyOf(out);
        }
        boolean dress=kind==TerrorEffectType.SILUETA||kind==TerrorEffectType.FLOTANTE;
        sphere(out,new Vec3(.035,2.51,-.06),new Vec3(.225,.325,.21),10,-.14);
        limb(out,new Vec3(.02,2.15,.03),new Vec3(.035,2.33,-.03),.075,.08,12);
        sphere(out,new Vec3(0,1.97,.04),new Vec3(.33,.35,.16),12,0);
        sphere(out,new Vec3(0,1.58,.05),new Vec3(.205,.27,.125),12,0);
        sphere(out,new Vec3(0,1.29,.04),new Vec3(.26,.22,.17),12,0);
        if(dress){
            // Tapered, irregular cloth skirt with actual circumference and folds.
            skirt(out);
            sphere(out,new Vec3(-.17,.08,-.10),new Vec3(.09,.07,.20),12,0);
            sphere(out,new Vec3(.17,.08,-.10),new Vec3(.09,.07,.20),12,0);
        }else{
            limb(out,new Vec3(-.14,1.24,.03),new Vec3(-.21,.69,-.08),.105,.11,12);
            limb(out,new Vec3(-.21,.69,-.08),new Vec3(-.25,.16,.05),.055,.065,12);
            limb(out,new Vec3(.14,1.24,.03),new Vec3(.18,.72,.04),.105,.11,12);
            limb(out,new Vec3(.18,.72,.04),new Vec3(.24,.16,-.06),.055,.065,12);
            sphere(out,new Vec3(-.25,.09,-.08),new Vec3(.09,.08,.20),12,0);
            sphere(out,new Vec3(.24,.09,-.16),new Vec3(.09,.08,.20),12,0);
        }
        for(int side:new int[]{-1,1}){
            Vec3 shoulder=new Vec3(side*.30,2.10,.02),elbow=new Vec3(side*.42,1.52,.07),wrist=new Vec3(side*.47,.96,-.10);
            limb(out,shoulder,elbow,.072,.085,12);limb(out,elbow,wrist,.042,.055,12);
            sphere(out,wrist.add(0,-.065,0),new Vec3(.057,.13,.035),12,0);
            for(int finger=0;finger<4;finger++){
                Vec3 a=wrist.add((finger-1.5)*.026,-.14,-.01);
                limb(out,a,a.add(side*.025,-.13-(finger==1?.045:0),-.025),.011,.012,12);
            }
        }
        // Hair drapes behind the head, not a flat texture.
        if(dress)for(int side:new int[]{-1,1})sphere(out,new Vec3(side*.21,2.36,.08),new Vec3(.09,.42,.14),13,side*.12);
        return List.copyOf(out);
    }
    private static void limb(List<Point> out,Vec3 a,Vec3 b,double rx,double rz,int mat){
        Vec3 direction=b.subtract(a).normalize();Vec3 across=direction.cross(new Vec3(0,0,1)).normalize();
        Vec3 depth=across.cross(direction).normalize();Vec3 center=a.add(b).scale(.5);double length=a.distanceTo(b)/2+rx*.35;
        surface(out,center,across.scale(rx),direction.scale(length),depth.scale(rz),mat);
    }
    private static void sphere(List<Point> out,Vec3 center,Vec3 scale,int material,double tilt){
        Vec3 ax=new Vec3(Math.cos(tilt)*scale.x,Math.sin(tilt)*scale.x,0);
        Vec3 ay=new Vec3(-Math.sin(tilt)*scale.y,Math.cos(tilt)*scale.y,0);
        surface(out,center,ax,ay,new Vec3(0,0,scale.z),material);
    }
    private static void surface(List<Point> out,Vec3 center,Vec3 ax,Vec3 ay,Vec3 az,int material){
        int rings=10,slices=20;
        for(int y=0;y<rings;y++)for(int x=0;x<slices;x++){
            point(out,center,ax,ay,az,(double)x/slices,(double)y/rings,material);
            point(out,center,ax,ay,az,(double)(x+1)/slices,(double)y/rings,material);
            point(out,center,ax,ay,az,(double)(x+1)/slices,(double)(y+1)/rings,material);
            point(out,center,ax,ay,az,(double)x/slices,(double)(y+1)/rings,material);
        }
    }
    private static void point(List<Point> out,Vec3 center,Vec3 ax,Vec3 ay,Vec3 az,double u,double v,int material){
        double lon=u*Math.PI*2,lat=(v-.5)*Math.PI;
        double x=Math.cos(lon)*Math.cos(lat),y=Math.sin(lat),z=Math.sin(lon)*Math.cos(lat);
        Vec3 p=center.add(ax.scale(x)).add(ay.scale(y)).add(az.scale(z));
        int shade=(int)Math.round(255*(.24+.76*Math.max(0,-x*.32+y*.65-z*.68)));
        out.add(new Point((float)p.x,(float)p.y,(float)p.z,(float)u,(float)v,material,Math.min(255,shade)));
    }
    private static void skirt(List<Point> out){
        for(int y=0;y<10;y++)for(int x=0;x<32;x++){
            skirtPoint(out,x/32.0,y/10.0);skirtPoint(out,(x+1)/32.0,y/10.0);
            skirtPoint(out,(x+1)/32.0,(y+1)/10.0);skirtPoint(out,x/32.0,(y+1)/10.0);
        }
    }
    private static void skirtPoint(List<Point> out,double u,double v){
        double angle=u*Math.PI*2,radius=.26+(1-v)*.16+Math.sin(angle*12)*.013;
        double y=.14+v*1.18+Math.sin(angle*5)*.018*(1-v);
        int shade=(int)(100+105*Math.max(0,Math.cos(angle)*-.3-Math.sin(angle)*.8));
        out.add(new Point((float)(Math.cos(angle)*radius),(float)y,(float)(Math.sin(angle)*radius*.7), (float)u,(float)v,11,shade));
    }
}
