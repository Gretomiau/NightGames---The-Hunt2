package dev.greto.nightminigames.client.terror;

import com.mojang.blaze3d.vertex.*;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.terror.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.*;
import java.util.*;

/** World anchored, cached 3D apparitions. No entities or world illumination. */
public final class TerrorPreviewRenderer {
    private static final ResourceLocation SHADER=NightminigamesMod.id("terror/manual");
    private static final ResourceLocation TYPE=NightminigamesMod.id("terror/manual");
    private static final Map<TerrorEffectType,TerrorPreviewPayload> ACTIVE=new EnumMap<>(TerrorEffectType.class);
    private static final long EPOCH=System.nanoTime();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
    private static ByteBufferBuilder allocator;
    private static MultiBufferSource.BufferSource buffers;
    private static boolean warned;
    private static boolean initialized;
    private TerrorPreviewRenderer(){}
    public static int activeCount(){return ACTIVE.size();}
    public static void initialize(){
        if(initialized)return;initialized=true;
        ClientPlayNetworking.registerGlobalReceiver(TerrorPreviewPayload.TYPE,(payload,context)->context.client().execute(()->receive(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{clear();TerrorEncounterClient.setEnabled(false);});
        TerrorClientTicker.initialize();
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context->{
            Minecraft client=Minecraft.getInstance();
            if(client.level==null||client.player==null||(ACTIVE.isEmpty()&&TerrorEncounterClient.current()==null)||context.matrixStack()==null)return;
            var shader=VeilRenderSystem.renderer().getShaderManager().getShader(SHADER);
            var type=VeilRenderType.get(TYPE);
            if(shader==null||!shader.isValid()||type==null||!shader.hasUniform("TimeSeconds")){
                if(!warned){NightminigamesMod.LOGGER.error("[Terror] Falta shader, render type o TimeSeconds. Revisa terror/manual y recarga recursos.");warned=true;}
                return;
            }
            warned=false;
            if(buffers==null){allocator=new ByteBufferBuilder(4096);buffers=MultiBufferSource.immediate(allocator);}
            float time=(float)(((System.nanoTime()-EPOCH)/1.0E9)%3600);
            shader.getUniform("TimeSeconds").setFloat(time);
            Vec3 camera=context.camera().getPosition();
            List<TerrorPreviewPayload> entries=new ArrayList<>(ACTIVE.values());
            if(TerrorEncounterClient.current()!=null)entries.add(TerrorEncounterClient.current());
            entries.sort(Comparator.comparingDouble((TerrorPreviewPayload p)->camera.distanceToSqr(p.x(),p.y(),p.z())).reversed());
            var vertex=buffers.getBuffer(type);
            for(var p:entries){
                if(!p.dimension().equals(client.level.dimension().location().toString()))continue;
                Vec3 origin=position(p,time);
                if(camera.distanceToSqr(origin)>96*96)continue;
                var effect=TerrorEffectType.find(p.effect());if(effect==null)continue;
                double yaw=Math.toRadians(p.yaw());
                Vec3 right=new Vec3(Math.cos(yaw),0,Math.sin(yaw));
                Vec3 forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
                float beam=0;
                float width=1.6F,height=3.0F;
                if(effect==TerrorEffectType.MOVIMIENTO)origin=origin.add(right.scale(Math.sin(time*0.7)*0.12));
                if(effect==TerrorEffectType.CABEZA){width=1.2F;height=1.3F;}
                if(effect==TerrorEffectType.PRESENCIA){width=2.5F;height=3.3F;}
                PoseStack pose=context.matrixStack();pose.pushPose();
                pose.translate(origin.x-camera.x,origin.y-camera.y,origin.z-camera.z);
                if(effect==TerrorEffectType.SOMBRA){
                    Vec3 normal=Vec3.atLowerCornerOf(Direction.from3DDataValue(p.face()).getNormal());
                    Vec3 across,along;
                    if(Math.abs(normal.y)>0.5){across=right;along=forward;}
                    else{along=new Vec3(0,1,0);across=normal.cross(along).normalize();}
                    quad(vertex,pose,normal.scale(0.012),across,along,2.0F,4.0F,effect.ordinal(),beam,220);
                }else{
                    pose.mulPose(new org.joml.Quaternionf().rotationY((float)-yaw));
                    float breath=1.0F+(float)Math.sin(time*1.5)*.006F;
                    pose.scale(1,breath,1);
                    TerrorFigureMesh.draw(vertex,pose,effect,1);
                }
                pose.popPose();
            }
            buffers.endBatch(type);
        });
    }
    static void maintainWorld(Minecraft client){
        if(client.level!=lastLevel){clear();lastLevel=client.level;TerrorClientTicker.reset();}
    }
    static boolean needsLightSnapshot(){return !ACTIVE.isEmpty()||TerrorEncounterClient.needsLightSnapshot();}
    static void tickTerror(Minecraft client,int elapsedTicks){
            if(client.player!=null&&!client.player.isAlive())ACTIVE.clear();
            TerrorEncounterClient.tick(client,elapsedTicks);
            if(client.level!=null&&client.player!=null){
                var it=ACTIVE.entrySet().iterator();while(it.hasNext()){var entry=it.next();var p=entry.getValue();
                    if(illuminated(client,position(p,currentTime()))){it.remove();
                        if(ClientPlayNetworking.canSend(TerrorPreviewPayload.TYPE))ClientPlayNetworking.send(new TerrorPreviewPayload(false,p.effect(),"",0,0,0,0,1));
                    }
                }
            }

    }
    private static double currentTime(){return ((System.nanoTime()-EPOCH)/1.0E9)%3600;}
    static Vec3 currentPosition(TerrorPreviewPayload p){return position(p,currentTime());}
    static Vec3 position(TerrorPreviewPayload p,double time){
        double bob=p.effect().equals("flotante")?Math.sin(time*1.15)*.12:0;
        return new Vec3(p.x(),p.y()+bob,p.z());
    }
    private static void receive(TerrorPreviewPayload p){
        if(p.effect().equals("automatico")){TerrorEncounterClient.setEnabled(p.active());return;}
        if(!p.active()){
            if(p.effect().equals("todos"))ACTIVE.clear();else{var kind=TerrorEffectType.find(p.effect());if(kind!=null)ACTIVE.remove(kind);}return;
        }
        Minecraft client=Minecraft.getInstance();var kind=TerrorEffectType.find(p.effect());
        if(kind==null||client.level==null||!Double.isFinite(p.x())||!Double.isFinite(p.y())||!Double.isFinite(p.z())||!Float.isFinite(p.yaw())||p.face()<0||p.face()>5)return;
        if(!p.dimension().equals(client.level.dimension().location().toString()))return;
        lastLevel=client.level;ACTIVE.put(kind,p);
        NightminigamesMod.LOGGER.info("[Terror] Prueba {} recibida en {}, {}, {}",p.effect(),p.x(),p.y(),p.z());
    }
    static boolean illuminated(Minecraft client,Vec3 base){return TerrorClientTicker.illuminated(client,base);}
    private static void quad(VertexConsumer v,PoseStack pose,Vec3 offset,Vec3 across,Vec3 along,float width,float height,int kind,float beam,int alpha){
        Vec3 a=offset.add(across.scale(-width/2)),b=offset.add(across.scale(width/2));
        Vec3 c=b.add(along.scale(height)),d=a.add(along.scale(height));
        vertex(v,pose,a,0,0,kind,beam,alpha);vertex(v,pose,b,1,0,kind,beam,alpha);
        vertex(v,pose,c,1,1,kind,beam,alpha);vertex(v,pose,d,0,1,kind,beam,alpha);
    }
    private static void vertex(VertexConsumer v,PoseStack pose,Vec3 p,float u,float w,int kind,float beam,int alpha){
        v.addVertex(pose.last().pose(),(float)p.x,(float)p.y,(float)p.z).setUv(u,w).setColor(kind,(int)(beam*255),0,alpha);
    }
    private static void clear(){ACTIVE.clear();if(allocator!=null){allocator.close();allocator=null;buffers=null;}lastLevel=null;warned=false;TerrorEncounterClient.clear();}
}
