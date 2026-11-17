package dev.greto.nightminigames.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.team.TeamBinding;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import dev.greto.nightminigames.NightminigamesMod;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AdminShowSystem {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<UUID> ENABLED = new HashSet<>();
    private static final Map<UUID, String> FILTERS = new HashMap<>();
    private static boolean initialized;

    private AdminShowSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playS2C().register(AdminShowPayload.TYPE, AdminShowPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(AdminShowSystem::load);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (player.hasPermissions(2)) {
                ServerPlayNetworking.send(player, new AdminShowPayload(ENABLED.contains(player.getUUID()), FILTERS.getOrDefault(player.getUUID(), "")));
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(AdminShowSystem::save);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { ENABLED.clear(); FILTERS.clear(); });
    }

    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("show")
            .executes(context -> change(context.getSource(), null))
            .then(Commands.literal("activar").executes(context -> change(context.getSource(), true)))
            .then(Commands.literal("desactivar").executes(context -> change(context.getSource(), false)))
            .then(Commands.literal("estado").executes(context -> status(context.getSource())))
            .then(Commands.argument("equipo", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                .executes(context -> showTeam(context.getSource(), StringArgumentType.getString(context, "equipo"))));
    }

    private static int change(CommandSourceStack source, Boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean target = enabled == null ? !ENABLED.contains(player.getUUID()) : enabled;
        if (target) {
            ENABLED.add(player.getUUID());
        } else {
            ENABLED.remove(player.getUUID());
        }
        if (!target) {
            FILTERS.remove(player.getUUID());
        } else {
            FILTERS.put(player.getUUID(), "");
        }
        ServerPlayNetworking.send(player, new AdminShowPayload(target, ""));
        save(source.getServer());
        source.sendSuccess(() -> Component.literal("[Nightminigames] ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal("Visualización: " + (target ? "ACTIVADA" : "DESACTIVADA")).withStyle(target ? ChatFormatting.GREEN : ChatFormatting.GRAY))
            .append(Component.literal(" · Solo elementos en chunks cargados").withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    private static int showTeam(CommandSourceStack source, String team) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!TeamBinding.valid(team)) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ENABLED.add(player.getUUID());
        FILTERS.put(player.getUUID(), team);
        ServerPlayNetworking.send(player, new AdminShowPayload(true, team));
        save(source.getServer());
        source.sendSuccess(() -> Component.literal("Vista de configuración: " + team.toUpperCase())
            .withStyle(TeamBinding.color(team)), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        boolean enabled = ENABLED.contains(source.getPlayerOrException().getUUID());
        source.sendSuccess(() -> Component.literal("Visualización: " + (enabled ? "ACTIVA" : "INACTIVA"))
            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return 1;
    }
    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_admin_show.json");
    }

    private static void load(MinecraftServer server) {
        ENABLED.clear();
        FILTERS.clear();
        Path file = file(server);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : root.entrySet()) {
                UUID id = UUID.fromString(entry.getKey());
                JsonObject settings = entry.getValue().getAsJsonObject();
                if (settings.has("show") && settings.get("show").getAsBoolean()) {
                    ENABLED.add(id);
                    String team = settings.has("team") ? settings.get("team").getAsString() : "";
                    FILTERS.put(id, TeamBinding.valid(team) ? team : "");
                }
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo leer el show administrativo", exception);
        }
    }

    private static void save(MinecraftServer server) {
        JsonObject root = new JsonObject();
        for (UUID id : ENABLED) {
            JsonObject settings = new JsonObject();
            settings.addProperty("show", true);
            settings.addProperty("team", FILTERS.getOrDefault(id, ""));
            root.add(id.toString(), settings);
        }
        Path file = file(server);
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo guardar la visualización OP", exception);
        }
    }

}
