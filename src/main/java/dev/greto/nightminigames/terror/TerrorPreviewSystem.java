package dev.greto.nightminigames.terror;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.key.KeyMessages;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import java.util.*;

/** Personal OP previews only. No entities, damage, automatic encounters or persistence. */
public final class TerrorPreviewSystem {
    private static final Map<UUID, Map<TerrorEffectType, TerrorPreviewPayload>> ACTIVE = new HashMap<>();
    private static boolean initialized;
    private static boolean automatic = true;
    private TerrorPreviewSystem() {}
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.playS2C().register(TerrorPreviewPayload.TYPE, TerrorPreviewPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TerrorPreviewPayload.TYPE, TerrorPreviewPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TerrorPreviewPayload.TYPE,(payload,context)->context.server().execute(()->{
            if(payload.active())return;
            var kind=TerrorEffectType.find(payload.effect());
            var entries=ACTIVE.get(context.player().getUUID());
            if(kind!=null&&entries!=null){entries.remove(kind);if(entries.isEmpty())ACTIVE.remove(context.player().getUUID());}
        }));
        ServerLifecycleEvents.SERVER_STARTING.register(server->automatic=true);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->sendAutomatic(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ACTIVE.remove(handler.player.getUUID()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, target) -> clear(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, player, alive) -> clear(player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
    }
    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("terror").requires(source -> source.hasPermission(2))
            .then(Commands.literal("automatico")
                .then(Commands.literal("activar").executes(c->setAutomatic(c.getSource(),true)))
                .then(Commands.literal("desactivar").executes(c->setAutomatic(c.getSource(),false)))
                .then(Commands.literal("estado").executes(c->{c.getSource().sendSuccess(()->KeyMessages.message("Apariciones automáticas: "+(automatic?"ON":"OFF")+". Solo durante la oscuridad.",ChatFormatting.AQUA),false);return 1;})))
            .then(Commands.literal("poner")
                .then(Commands.argument("efecto", StringArgumentType.word())
                    .suggests((context,builder)->SharedSuggestionProvider.suggest(TerrorEffectType.ids(),builder))
                    .executes(context -> place(context.getSource(),StringArgumentType.getString(context,"efecto"),null))
                    .then(Commands.argument("posicion",Vec3Argument.vec3())
                        .executes(context -> place(context.getSource(),StringArgumentType.getString(context,"efecto"),Vec3Argument.getVec3(context,"posicion"))))))
            .then(Commands.literal("quitar")
                .executes(context -> remove(context.getSource(),"todos"))
                .then(Commands.literal("todos").executes(context -> remove(context.getSource(),"todos")))
                .then(Commands.argument("efecto",StringArgumentType.word())
                    .suggests((context,builder)->SharedSuggestionProvider.suggest(TerrorEffectType.ids(),builder))
                    .executes(context -> remove(context.getSource(),StringArgumentType.getString(context,"efecto")))))
            .then(Commands.literal("estado").executes(context -> status(context.getSource())));
    }
    private static void sendAutomatic(ServerPlayer player){
        if(ServerPlayNetworking.canSend(player,TerrorPreviewPayload.TYPE))ServerPlayNetworking.send(player,new TerrorPreviewPayload(automatic,"automatico","",0,0,0,0,1));
    }
    private static int setAutomatic(CommandSourceStack source,boolean active){
        automatic=active;for(var player:source.getServer().getPlayerList().getPlayers())sendAutomatic(player);
        source.sendSuccess(()->KeyMessages.message("Apariciones automáticas "+(active?"activadas":"desactivadas")+" para todos. Solo durante la oscuridad.",ChatFormatting.GREEN),true);return 1;
    }
    private static int place(CommandSourceStack source,String id,Vec3 requested) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        if(!source.hasPermission(2)) return 0;
        var type=TerrorEffectType.find(id);
        if(type==null){source.sendFailure(KeyMessages.message("Efecto inválido. Usa silueta, movimiento, sombra, cabeza, presencia o flotante.",ChatFormatting.RED));return 0;}
        if(!ServerPlayNetworking.canSend(player,TerrorPreviewPayload.TYPE)){source.sendFailure(KeyMessages.message("Actualiza también el cliente de Nightminigames a 1.0.8 para ver las pruebas de terror.",ChatFormatting.RED));return 0;}
        Vec3 position=requested;Direction face=Direction.UP;
        if(position==null){
            Vec3 eye=player.getEyePosition();
            var hit=player.serverLevel().clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(12)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if(hit.getType()==HitResult.Type.BLOCK){face=hit.getDirection();position=hit.getLocation().add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.03));}
            else {Vec3 flat=new Vec3(player.getLookAngle().x,0,player.getLookAngle().z);if(flat.lengthSqr()<0.001)flat=new Vec3(0,0,1);position=player.position().add(flat.normalize().scale(5));}
        }
        if(type==TerrorEffectType.FLOTANTE&&requested==null)position=position.add(0,2.5,0);
        if(!Double.isFinite(position.x)||!Double.isFinite(position.y)||!Double.isFinite(position.z)||player.position().distanceToSqr(position)>64*64){source.sendFailure(KeyMessages.message("Coloca la prueba a 64 bloques o menos y usa coordenadas válidas.",ChatFormatting.RED));return 0;}
        BlockPos pos=BlockPos.containing(position);
        if(!player.serverLevel().getWorldBorder().isWithinBounds(pos)||position.y<player.serverLevel().getMinBuildHeight()||position.y>=player.serverLevel().getMaxBuildHeight()||!player.serverLevel().hasChunkAt(pos)){
            source.sendFailure(KeyMessages.message("La posición debe estar dentro del mundo y en una zona cargada.",ChatFormatting.RED));return 0;
        }
        var payload=new TerrorPreviewPayload(true,id,player.serverLevel().dimension().location().toString(),position.x,position.y,position.z,player.getYRot(),face.get3DDataValue());
        ACTIVE.computeIfAbsent(player.getUUID(),ignored->new EnumMap<>(TerrorEffectType.class)).put(type,payload);
        ServerPlayNetworking.send(player,payload);
        Vec3 finalPosition=position;
        source.sendSuccess(()->KeyMessages.message("Prueba "+id+" colocada en "+BlockPos.containing(finalPosition).toShortString()+". Solo tú la ves. Permanece hasta /nightminigames terror quitar "+id+".",ChatFormatting.AQUA),false);
        return 1;
    }
    private static int remove(CommandSourceStack source,String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();if(!source.hasPermission(2))return 0;
        if(id.equals("todos")){clear(player);source.sendSuccess(()->KeyMessages.message("Todas tus pruebas de terror desactivadas.",ChatFormatting.GREEN),false);return 1;}
        var type=TerrorEffectType.find(id);if(type==null){source.sendFailure(KeyMessages.message("Efecto inválido.",ChatFormatting.RED));return 0;}
        var entries=ACTIVE.get(player.getUUID());if(entries!=null){entries.remove(type);if(entries.isEmpty())ACTIVE.remove(player.getUUID());}
        if(ServerPlayNetworking.canSend(player,TerrorPreviewPayload.TYPE))ServerPlayNetworking.send(player,new TerrorPreviewPayload(false,id,"",0,0,0,0,1));
        source.sendSuccess(()->KeyMessages.message("Prueba "+id+" desactivada.",ChatFormatting.GREEN),false);return 1;
    }
    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();var entries=ACTIVE.getOrDefault(player.getUUID(),Map.of());
        source.sendSuccess(()->KeyMessages.message("Pruebas manuales de terror: "+entries.size()+"/"+TerrorEffectType.values().length+". Estas pruebas no hacen daño.",ChatFormatting.AQUA),false);
        for(var type:TerrorEffectType.values()){
            var p=entries.get(type);String line=type.id+": "+(p==null?"APAGADO":"COLOCADO · "+BlockPos.containing(p.x(),p.y(),p.z()).toShortString());
            source.sendSuccess(()->KeyMessages.message(line,p==null?ChatFormatting.GRAY:ChatFormatting.YELLOW),false);
        }
        return entries.size();
    }
    private static void clear(ServerPlayer player){
        ACTIVE.remove(player.getUUID());
        if(ServerPlayNetworking.canSend(player,TerrorPreviewPayload.TYPE))ServerPlayNetworking.send(player,new TerrorPreviewPayload(false,"todos","",0,0,0,0,1));
    }
}
