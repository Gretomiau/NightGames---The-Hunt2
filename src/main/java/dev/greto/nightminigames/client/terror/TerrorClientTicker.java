package dev.greto.nightminigames.client.terror;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import dev.greto.nightminigames.client.FlashlightClientSystem;
import dev.greto.nightminigames.flashlight.FlashlightData;
import dev.greto.nightminigames.registry.ModItems;
import java.util.*;

/** Single global client ticker for terror only; no threads or per-figure timers. */
final class TerrorClientTicker {
    private record Beam(Vec3 eye,Vec3 direction){}
    private static final TerrorTickCadence CADENCE=new TerrorTickCadence();
    private static final List<Beam> BEAMS=new ArrayList<>(16);
    private static final double[] HEIGHTS={.25,1.3,2.5};
    private static int remainingRays;
    private static boolean initialized;
    static void initialize(){
        if(initialized)return;initialized=true;
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            TerrorPreviewRenderer.maintainWorld(client);
            if(!CADENCE.advance())return;
            remainingRays=64;
            try{snapshot(client);TerrorPreviewRenderer.tickTerror(client,TerrorTickCadence.INTERVAL);}finally{BEAMS.clear();}
        });
    }
    static void reset(){CADENCE.reset();BEAMS.clear();remainingRays=0;}
    private static void snapshot(Minecraft client){
        BEAMS.clear();if(client.level==null||client.player==null)return;
        if(!TerrorPreviewRenderer.needsLightSnapshot())return;
        // Nearest lights first, with a strict cap matching the visual flashlight budget.
        var players=new ArrayList<>(client.level.players());
        players.sort(Comparator.comparingDouble(p->p.distanceToSqr(client.player)));
        for(var player:players){
            if(BEAMS.size()>=16)break;
            if(!player.isAlive()||player.distanceToSqr(client.player)>128*128)continue;
            boolean lit=player.getMainHandItem().is(ModItems.FLASHLIGHT)&&FlashlightData.isEnabled(player.getMainHandItem())
                ||player.getOffhandItem().is(ModItems.FLASHLIGHT)&&FlashlightData.isEnabled(player.getOffhandItem());
            if(!lit)continue;
            Vec3 eye=player.getEyePosition(),direction=player.getLookAngle();
            if(player==client.player&&!client.gameRenderer.getMainCamera().isDetached()){
                var camera=client.gameRenderer.getMainCamera();eye=camera.getPosition();var look=camera.getLookVector();direction=new Vec3(look.x(),look.y(),look.z());
            }
            BEAMS.add(new Beam(eye,direction));
        }
    }
    static boolean canCheckPlacement(){return remainingRays>=49;}
    static boolean lineOfSight(Minecraft client,Vec3 eye,Vec3 target){
        if(remainingRays<=0)return false;remainingRays--;
        var hit=client.level.clip(new ClipContext(eye,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,client.player));
        return hit.getType()==HitResult.Type.MISS||eye.distanceToSqr(hit.getLocation())+.05>=eye.distanceToSqr(target);
    }
    static boolean illuminated(Minecraft client,Vec3 base){
        var profile=FlashlightClientSystem.currentProfile();double edge=Math.cos(Math.toRadians(profile.coreAngleDegrees()));
        for(var beam:BEAMS)for(double height:HEIGHTS){
            Vec3 target=base.add(0,height,0),delta=target.subtract(beam.eye);
            if(!TerrorBeamMath.reaches(delta.x,delta.y,delta.z,beam.direction.x,beam.direction.y,beam.direction.z,profile.rangeBlocks(),edge))continue;
            if(lineOfSight(client,beam.eye,target))return true;
        }
        return false;
    }
}
