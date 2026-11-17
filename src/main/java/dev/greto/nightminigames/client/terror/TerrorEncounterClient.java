package dev.greto.nightminigames.client.terror;

import dev.greto.nightminigames.client.environment.EnvironmentClientSystem;
import dev.greto.nightminigames.terror.TerrorPreviewPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;

/** One private cosmetic encounter; bounded loaded terrain probes, no chunk requests. */
final class TerrorEncounterClient {
    private static boolean enabled;
    private static TerrorPreviewPayload current;
    private static int wait=200,age;
    static TerrorPreviewPayload current(){return current;}
    static void setEnabled(boolean value){enabled=value;current=null;wait=200;age=0;}
    static void clear(){current=null;wait=200;age=0;}
    static boolean needsLightSnapshot(){return enabled&&EnvironmentClientSystem.isDark()&&(current!=null||wait<=TerrorTickCadence.INTERVAL);}
    static void tick(Minecraft client,int elapsedTicks){
        if(client.level==null||client.player==null||!client.player.isAlive()||client.player.isSpectator()){clear();return;}
        if(!enabled||!EnvironmentClientSystem.isDark()){clear();return;}
        if(current!=null){
            Vec3 base=TerrorPreviewRenderer.currentPosition(current);
            age+=elapsedTicks;
            if(age>=260||client.player.distanceToSqr(base)<49||client.player.distanceToSqr(base)>112*112||TerrorPreviewRenderer.illuminated(client,base)){
                current=null;wait=700+client.level.random.nextInt(600);
            }
            return;
        }
        wait-=elapsedTicks;if(wait>0)return;
        wait=100; // At most eight loaded-column probes per five seconds when terrain is unsuitable.
        var random=client.level.random;Vec3 look=client.player.getLookAngle();
        double heading=Math.atan2(look.z,look.x);
        boolean flashlightOn=client.player.getMainHandItem().is(dev.greto.nightminigames.registry.ModItems.FLASHLIGHT)
            &&dev.greto.nightminigames.flashlight.FlashlightData.isEnabled(client.player.getMainHandItem())
            ||client.player.getOffhandItem().is(dev.greto.nightminigames.registry.ModItems.FLASHLIGHT)
            &&dev.greto.nightminigames.flashlight.FlashlightData.isEnabled(client.player.getOffhandItem());
        double minimum=TerrorBeamMath.appearanceMinimumDistance(flashlightOn,dev.greto.nightminigames.client.FlashlightClientSystem.currentProfile().rangeBlocks());
        boolean floating=random.nextInt(3)==0;
        for(int attempt=0;attempt<8;attempt++){
            if(!TerrorClientTicker.canCheckPlacement())return;
            double angle=heading+(random.nextDouble()-.5)*2.2,distance=minimum+random.nextDouble()*10;
            int x=(int)Math.floor(client.player.getX()+Math.cos(angle)*distance),z=(int)Math.floor(client.player.getZ()+Math.sin(angle)*distance);
            for(int offset=6;offset>=-7;offset--){
                if(floating&&offset<1)break;
                BlockPos feet=new BlockPos(x,client.player.getBlockY()+offset,z);
                if(!client.level.hasChunkAt(feet)||!client.level.hasChunkAt(feet.offset(1,0,1))||!client.level.hasChunkAt(feet.offset(-1,0,-1)))break;
                if(!client.level.getWorldBorder().isWithinBounds(feet))continue;
                if(!floating&&!client.level.getBlockState(feet.below()).isFaceSturdy(client.level,feet.below(),net.minecraft.core.Direction.UP))continue;
                if(feet.getY()-.15<client.level.getMinBuildHeight()||feet.getY()+3.15>=client.level.getMaxBuildHeight())continue;
                Vec3 base=Vec3.atBottomCenterOf(feet);
                AABB bounds=new AABB(base.x-.65,base.y-(floating?.15:0),base.z-.65,base.x+.65,base.y+3.15,base.z+.65);
                if(!client.level.noCollision(bounds)||!client.level.getFluidState(feet).isEmpty())continue;
                if(!TerrorClientTicker.canCheckPlacement())return;
                Vec3 eye=client.player.getEyePosition(),head=base.add(0,2.45,0);
                if(!TerrorClientTicker.lineOfSight(client,eye,head)||TerrorPreviewRenderer.illuminated(client,base))continue;
                float yaw=(float)Math.toDegrees(Math.atan2(-(base.x-client.player.getX()),base.z-client.player.getZ()));
                String effect=floating?"flotante":random.nextBoolean()?"silueta":"presencia";
                current=new TerrorPreviewPayload(true,effect,client.level.dimension().location().toString(),base.x,base.y,base.z,yaw,1);
                age=0;return;
            }
        }
    }
}
