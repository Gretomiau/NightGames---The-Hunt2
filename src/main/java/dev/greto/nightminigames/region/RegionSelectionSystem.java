package dev.greto.nightminigames.region;

import dev.greto.nightminigames.key.KeyMessages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.team.TeamBinding;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RegionSelectionSystem {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Selection> REGIONS = new HashMap<>();
    private static final Map<UUID, Set<String>> PLAYER_STATES = new HashMap<>();
    private static MinecraftServer activeServer;
    private static boolean initialized;

    private RegionSelectionSystem() {
    }

    private static String key(String team, RegionKind kind) {
        return team + ":" + kind.id;
    }

    private static Selection selection(String team, RegionKind kind) {
        return REGIONS.computeIfAbsent(key(team, kind), ignored -> new Selection());
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playS2C().register(RegionSyncPayload.TYPE, RegionSyncPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            activeServer = server;
            load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            activeServer = null;
            PLAYER_STATES.clear();
            REGIONS.clear();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ensureServer(server);
            PLAYER_STATES.remove(handler.player.getUUID());
            if (handler.player.hasPermissions(2)) {
                sync(handler.player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PLAYER_STATES.remove(handler.player.getUUID()));
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (hand != InteractionHand.MAIN_HAND || !(player.getMainHandItem().getItem() instanceof RegionSelectionWandItem wand)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                select(serverPlayer, TeamBinding.of(player.getMainHandItem()), wand.region(), true, pos);
            }
            return InteractionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(player.getMainHandItem().getItem() instanceof RegionSelectionWandItem wand)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                select(serverPlayer, TeamBinding.of(player.getMainHandItem()), wand.region(), false, hit.getBlockPos());
            }
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(RegionSelectionSystem::tick);
    }

    public static void select(ServerPlayer player, String team, RegionKind kind, boolean first, BlockPos position) {
        if (!player.hasPermissions(2) || !TeamBinding.valid(team)) {
            dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Necesitas una varita vinculada a uno de los siete equipos.", ChatFormatting.RED));
            return;
        }
        ensureServer(player.server);
        Selection data = selection(team, kind);
        String dimension = player.serverLevel().dimension().location().toString();
        if (!dimension.equals(data.dimension)) {
            data.clear();
            data.dimension = dimension;
        }
        if (first) {
            data.first = position.immutable();
        } else {
            data.second = position.immutable();
        }
        PLAYER_STATES.clear();
        save(player.server);
        syncAll(player.server);
        dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.prefix().append(Component.literal("[" + team.toUpperCase() + "] ").withStyle(TeamBinding.color(team)))
            .append(Component.literal(kind.label + (first ? " Pos1: " : " Pos2: ") + position.toShortString())
                .withStyle(ChatFormatting.WHITE)));
    }

    public static void clear(ServerPlayer player, String team, RegionKind kind) {
        if (!player.hasPermissions(2) || !TeamBinding.valid(team)) {
            return;
        }
        ensureServer(player.server);
        selection(team, kind).clear();
        PLAYER_STATES.clear();
        save(player.server);
        syncAll(player.server);
        dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Región " + kind.label + " eliminada para " + team, TeamBinding.color(team)));
    }

    public static void sync(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            return;
        }
        for (String team : ModuleSystem.TEAMS) {
            for (RegionKind kind : RegionKind.values()) {
                Selection s = selection(team, kind);
                ServerPlayNetworking.send(player, new RegionSyncPayload(team, kind.id, s.dimension,
                    s.first != null, s.first == null ? BlockPos.ZERO : s.first,
                    s.second != null, s.second == null ? BlockPos.ZERO : s.second));
            }
        }
    }

    public static boolean occupied(MinecraftServer server, RegionKind kind, String dimension, String team) {
        Selection data = selection(team, kind);
        if (!data.complete() || !dimension.equals(data.dimension)) {
            return false;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isAlive() && !player.isSpectator() && TeamBinding.canInteractWithoutOp(player, team)
                && contains(data, player)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInside(RegionKind kind, ServerPlayer player, String team) {
        return contains(selection(team, kind), player);
    }

    public static boolean complete(RegionKind kind, String team) {
        return selection(team, kind).complete();
    }

    public static String dimension(RegionKind kind, String team) {
        Selection data = selection(team, kind);
        return data.complete() ? data.dimension : null;
    }

    public static String status(RegionKind kind, String team) {
        Selection s = selection(team, kind);
        return s.complete() ? s.dimension + " · " + s.first.toShortString() + " ↔ " + s.second.toShortString() : "Sin configurar";
    }

    private static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                sync(player);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0) {
            return;
        }
        if (server != activeServer) {
            ensureServer(server);
        }
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            online.add(id);
            Set<String> current = new HashSet<>();
            for (String team : ModuleSystem.TEAMS) {
                if (!TeamBinding.canInteractWithoutOp(player, team)) {
                    continue;
                }
                for (RegionKind kind : RegionKind.values()) {
                    if (contains(selection(team, kind), player)) {
                        current.add(key(team, kind));
                    }
                }
            }
            Set<String> previous = PLAYER_STATES.put(id, current);
            if (previous != null) {
                for (String entered : current) {
                    if (!previous.contains(entered)) {
                        announce(server, player, entered);
                    }
                }
            }
        }
        PLAYER_STATES.keySet().removeIf(id -> !online.contains(id));
    }

    private static boolean contains(Selection s, ServerPlayer player) {
        if (!s.complete() || !s.dimension.equals(player.serverLevel().dimension().location().toString())) {
            return false;
        }
        Vec3 point = player.position();
        return point.x >= Math.min(s.first.getX(), s.second.getX()) && point.x < Math.max(s.first.getX(), s.second.getX()) + 1.0D
            && point.y >= Math.min(s.first.getY(), s.second.getY()) && point.y < Math.max(s.first.getY(), s.second.getY()) + 1.0D
            && point.z >= Math.min(s.first.getZ(), s.second.getZ()) && point.z < Math.max(s.first.getZ(), s.second.getZ()) + 1.0D;
    }

    private static void announce(MinecraftServer server, ServerPlayer player, String entered) {
        String[] parts = entered.split(":", 2);
        String team = parts[0];
        RegionKind kind = RegionKind.fromId(parts[1]);
        if (kind == null) {
            return;
        }
        Component notice = Component.literal(player.getGameProfile().getName()).withStyle(TeamBinding.color(team))
            .append(Component.literal(" ha entrado en " + kind.label + " · " + team.toUpperCase())
                .withStyle(ChatFormatting.WHITE));
        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            if (recipient.hasPermissions(2)) {
                dev.greto.nightminigames.admin.AdminMessages.send(recipient, KeyMessages.prefix().append(notice));
            }
        }
    }

    private static void ensureServer(MinecraftServer server) {
        if (activeServer != server) {
            activeServer = server;
            load(server);
        }
    }

    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_team_regions.json");
    }

    private static void load(MinecraftServer server) {
        REGIONS.clear();
        PLAYER_STATES.clear();
        Path file = path(server);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String team : ModuleSystem.TEAMS) {
                if (!root.has(team) || !root.get(team).isJsonObject()) {
                    continue;
                }
                JsonObject item = root.getAsJsonObject(team);
                for (RegionKind kind : RegionKind.values()) {
                    if (!item.has(kind.id) || !item.get(kind.id).isJsonObject()) {
                        continue;
                    }
                    JsonObject saved = item.getAsJsonObject(kind.id);
                    Selection s = selection(team, kind);
                    s.dimension = saved.has("dimension") ? saved.get("dimension").getAsString() : "";
                    s.first = readPos(saved, "first");
                    s.second = readPos(saved, "second");
                }
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("Error cargando regiones por equipos", exception);
        }
    }

    private static BlockPos readPos(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray() || object.getAsJsonArray(key).size() != 3) {
            return null;
        }
        JsonArray array = object.getAsJsonArray(key);
        return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    private static void save(MinecraftServer server) {
        JsonObject root = new JsonObject();
        for (String team : ModuleSystem.TEAMS) {
            JsonObject teamObject = new JsonObject();
            for (RegionKind kind : RegionKind.values()) {
                Selection s = selection(team, kind);
                JsonObject item = new JsonObject();
                item.addProperty("dimension", s.dimension);
                writePos(item, "first", s.first);
                writePos(item, "second", s.second);
                teamObject.add(kind.id, item);
            }
            root.add(team, teamObject);
        }
        Path file = path(server);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("Error guardando regiones por equipos", exception);
        }
    }

    private static void writePos(JsonObject item, String key, BlockPos pos) {
        if (pos == null) {
            return;
        }
        JsonArray array = new JsonArray();
        array.add(pos.getX());
        array.add(pos.getY());
        array.add(pos.getZ());
        item.add(key, array);
    }

    private static final class Selection {
        private String dimension = "";
        private BlockPos first;
        private BlockPos second;

        private boolean complete() {
            return first != null && second != null && !dimension.isEmpty();
        }

        private void clear() {
            dimension = "";
            first = null;
            second = null;
        }
    }
}
