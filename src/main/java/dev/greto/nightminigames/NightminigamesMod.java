package dev.greto.nightminigames;

import dev.greto.nightminigames.flashlight.FlashlightSystem;
import dev.greto.nightminigames.team.TeamCommands;
import dev.greto.nightminigames.team.TeamBinding;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import dev.greto.nightminigames.team.TeamCapacitySystem;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.door.DoorSystem;
import dev.greto.nightminigames.door.DoorCommands;
import dev.greto.nightminigames.admin.AdminShowSystem;
import dev.greto.nightminigames.flashlight.FlashlightSpawnerCommands;
import dev.greto.nightminigames.feed.HudFeedSystem;
import dev.greto.nightminigames.feed.ClassificationFeedSystem;
import dev.greto.nightminigames.inventory.OffhandRestrictionSystem;
import dev.greto.nightminigames.key.KeySpawnerCommands;
import dev.greto.nightminigames.key.KeySpawnerSystem;
import dev.greto.nightminigames.key.KeyMessages;
import dev.greto.nightminigames.machine.MachineCommands;
import dev.greto.nightminigames.machine.MachineSystem;
import dev.greto.nightminigames.machine.MachineStructureRepair;
import dev.greto.nightminigames.registry.ModBlocks;
import dev.greto.nightminigames.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import dev.greto.nightminigames.environment.EnvironmentSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.registry.ModCreativeTabs;
import dev.greto.nightminigames.registry.ModItems;
import dev.greto.nightminigames.registry.ModSounds;
import dev.greto.nightminigames.region.RegionSelectionCommands;
import dev.greto.nightminigames.region.RegionSelectionSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NightminigamesMod implements ModInitializer {
    public static final String MOD_ID = "nightminigames";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.initialize();
        TeamCapacitySystem.initialize();
        dev.greto.nightminigames.integration.MapWaypointBridge.initialize();
        ModuleSystem.initialize();
        AdminShowSystem.initialize();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        dev.greto.nightminigames.persistence.WorldStructureJson.initialize();
        dev.greto.nightminigames.round.RoundResetSystem.initialize();
        ModItems.initialize();
        ModCreativeTabs.initialize();
        HudFeedSystem.initialize();
        ClassificationFeedSystem.initialize();
        FlashlightSystem.initialize();
        OffhandRestrictionSystem.initialize();
        RegionSelectionSystem.initialize();
        DoorSystem.initialize();
        KeySpawnerSystem.initialize();
        MachineSystem.initialize();
        dev.greto.nightminigames.machine.TeamExitCompletionSystem.initialize();
        MachineStructureRepair.initialize();
        EnvironmentSystem.initialize();
        dev.greto.nightminigames.terror.TerrorPreviewSystem.initialize();
        registerCommands();
        FabricLoader loader = FabricLoader.getInstance();
        LOGGER.info("[Nightminigames] Veil presente: {}", loader.isModLoaded("veil"));
        LOGGER.info("[Nightminigames] GeckoLib presente: {}", loader.isModLoaded("geckolib"));
        LOGGER.info("[Nightminigames] Linterna Veil registrada: {}", ModItems.FLASHLIGHT);
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("nightminigames")
                .requires(source -> source.hasPermission(2));

            LiteralArgumentBuilder<CommandSourceStack> darkness = Commands.literal("oscuridad");
            darkness.then(Commands.literal("activar").executes(context -> toggleDarkness(context.getSource(), true)));
            darkness.then(Commands.literal("desactivar").executes(context -> toggleDarkness(context.getSource(), false)));
            darkness.then(Commands.literal("estado").executes(context -> EnvironmentSystem.report(context.getSource())));
            root.then(darkness);

            LiteralArgumentBuilder<CommandSourceStack> music = Commands.literal("musica");
            music.then(Commands.literal("activar").executes(context -> toggleMusic(context.getSource(), true)));
            music.then(Commands.literal("desactivar").executes(context -> toggleMusic(context.getSource(), false)));
            music.then(Commands.literal("estado").executes(context -> EnvironmentSystem.report(context.getSource())));
            music.then(Commands.literal("volumen")
                .then(Commands.argument("porcentaje", IntegerArgumentType.integer(0, 100))
                    .executes(context -> setMusicVolume(context.getSource(), IntegerArgumentType.getInteger(context, "porcentaje")))));
            root.then(music);

            LiteralArgumentBuilder<CommandSourceStack> sound = Commands.literal("sonido");
            sound.then(Commands.literal("probar")
                .executes(context -> testSound(context.getSource(), SoundEvents.EXPERIENCE_ORB_PICKUP, "vanilla"))
                .then(Commands.literal("vanilla").executes(context -> testSound(context.getSource(), SoundEvents.EXPERIENCE_ORB_PICKUP, "vanilla")))
                .then(Commands.literal("linterna").executes(context -> testSound(context.getSource(), ModSounds.FLASHLIGHT_TOGGLE, "linterna")))
                .then(Commands.literal("encender").executes(context -> testSound(context.getSource(), ModSounds.POWER_ON, "encendido")))
                .then(Commands.literal("apagar").executes(context -> testSound(context.getSource(), ModSounds.POWER_OFF, "apagado"))));
            root.then(sound);

            LiteralArgumentBuilder<CommandSourceStack> flashlight = Commands.literal("linterna");
            flashlight.then(Commands.literal("dar")
                .executes(context -> giveFlashlight(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("jugador", EntityArgument.player())
                    .executes(context -> giveFlashlight(context.getSource(), EntityArgument.getPlayer(context, "jugador")))));
            flashlight.then(Commands.literal("bateria")
                .then(batteryCommand("recargar"))
                .then(batteryCommand("resetear")));
            flashlight.then(FlashlightSpawnerCommands.create());
            root.then(flashlight);
            root.then(Commands.literal("spawners")
                .then(Commands.literal("reiniciar").executes(context -> {
                    int pending = dev.greto.nightminigames.round.RoundResetSystem.resetSpawners(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> KeyMessages.message("Spawners reiniciándose: " + pending + " cargados. Los descargados se reactivarán al cargar.", ChatFormatting.GREEN), false);
                    return 1;
                })));
            root.then(Commands.literal("kit")
                .then(Commands.literal("configuracion")
                    .then(Commands.argument("equipo", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(ModuleSystem.TEAMS, builder))
                        .executes(context -> giveConfigurationKit(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "equipo")))
                        .then(Commands.argument("jugador", EntityArgument.player())
                            .executes(context -> giveConfigurationKit(context.getSource(), EntityArgument.getPlayer(context, "jugador"), StringArgumentType.getString(context, "equipo")))))));
            root.then(AdminShowSystem.commands());
            root.then(Commands.literal("hud")
                .then(Commands.literal("limpiar")
                    .executes(context -> clearHudHistory(context.getSource())))
                .then(Commands.literal("estado")
                    .executes(context -> showHudHistoryCount(context.getSource())))
                .then(ClassificationFeedSystem.commands()));
            root.then(KeySpawnerCommands.create());
            root.then(RegionSelectionCommands.create());
            root.then(DoorCommands.create());
            root.then(TeamCommands.create());
            root.then(dev.greto.nightminigames.machine.TeamExitCompletionSystem.commands());
            root.then(dev.greto.nightminigames.points.PointsAwardCommands.commands());
            root.then(dev.greto.nightminigames.integration.MachineWaypointCommands.commands());
            root.then(dev.greto.nightminigames.terror.TerrorPreviewSystem.commands());
            root.then(ModuleSystem.commands());
            root.then(ModuleSystem.startCommands());
            dispatcher.register(root);
        });
    }

    private static int giveConfigurationKit(CommandSourceStack source, ServerPlayer player, String team) {
        if (!TeamBinding.valid(team)) {
            source.sendFailure(KeyMessages.message("Equipo inválido. Usa uno de los siete equipos.", ChatFormatting.RED));
            return 0;
        }
        Item[] equipment = {
            ModItems.WAND_BLUE,
            ModItems.WAND_YELLOW,
            ModItems.WAND_RED,
            ModItems.WAND_GREEN,
            ModItems.WAND_FLASHLIGHT,
            ModItems.WAND_MODULOS,
            ModItems.WAND_REGION_ENTRADA,
            ModItems.WAND_REGION_SALIDA,
            ModItems.MACHINE,
            ModItems.DOOR_ENTRANCE,
            ModItems.DOOR_EXIT,
            ModItems.KEY_BLUE,
            ModItems.KEY_YELLOW,
            ModItems.KEY_RED,
            ModItems.KEY_GREEN,
            ModItems.FLASHLIGHT
        };
        int added = 0;
        int dropped = 0;
        for (Item item : equipment) {
            ItemStack stack = TeamBinding.label(new ItemStack(item), team);
            if (item == ModItems.WAND_MODULOS) {
                dev.greto.nightminigames.module.ModuleWandItem.selectTeam(stack, team);
            }
            if (player.getInventory().add(stack)) {
                added++;
            } else {
                player.drop(stack, false);
                dropped++;
            }
        }
        int inventoryCount = added;
        int groundCount = dropped;
        source.sendSuccess(() -> KeyMessages.message(
            "Kit de " + team.toUpperCase() + " entregado a " + player.getGameProfile().getName()
                + ": " + inventoryCount + " ítems al inventario, " + groundCount + " al suelo por falta de espacio.",
            groundCount == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        return equipment.length;
    }

    private static int clearHudHistory(CommandSourceStack source) {
        int removed = HudFeedSystem.clear(source.getServer());
        ClassificationFeedSystem.clearForRound(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("HUD general y clasificación limpiados para todos. Eventos generales eliminados: " + removed, ChatFormatting.GREEN), false);
        return 1;
    }

    private static int showHudHistoryCount(CommandSourceStack source) {
        int events = HudFeedSystem.count(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("Historial de notificaciones: " + events + " eventos guardados.", ChatFormatting.AQUA), false);
        ClassificationFeedSystem.report(source);
        return 1;
    }

    private static int toggleDarkness(CommandSourceStack source, boolean active) {
        EnvironmentSystem.setDarkness(source.getServer(), active);
        return EnvironmentSystem.report(source);
    }

    private static int toggleMusic(CommandSourceStack source, boolean active) {
        EnvironmentSystem.setMusic(source.getServer(), active);
        return EnvironmentSystem.report(source);
    }

    private static int setMusicVolume(CommandSourceStack source, int volume) {
        EnvironmentSystem.setVolume(source.getServer(), volume);
        return EnvironmentSystem.report(source);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> batteryCommand(String name) {
        return Commands.literal(name)
            .executes(context -> rechargeFlashlights(context.getSource(), context.getSource().getPlayerOrException()))
            .then(Commands.literal("todos")
                .executes(context -> rechargeAllFlashlights(context.getSource())))
            .then(Commands.argument("jugador", EntityArgument.player())
                .executes(context -> rechargeFlashlights(context.getSource(), EntityArgument.getPlayer(context, "jugador"))));
    }

    private static int rechargeFlashlights(CommandSourceStack source, ServerPlayer target) {
        int count = FlashlightSystem.rechargeAll(target);
        source.sendSuccess(() -> KeyMessages.message(count + " linterna(s) recargada(s) para " + target.getGameProfile().getName() + ".", ChatFormatting.GREEN), false);
        return count;
    }

    private static int rechargeAllFlashlights(CommandSourceStack source) {
        int count = 0;
        int players = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            int recharged = FlashlightSystem.rechargeAll(player);
            count += recharged;
            if (recharged > 0) {
                players++;
            }
        }
        int totalCount = count;
        int totalPlayers = players;
        source.sendSuccess(() -> KeyMessages.message(totalCount + " linterna(s) recargada(s) de " + totalPlayers + " jugador(es) conectado(s).", ChatFormatting.GREEN), false);
        return count;
    }

    private static int testSound(CommandSourceStack source, SoundEvent sound, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.playNotifySound(sound, SoundSource.MASTER, 1.0F, 1.0F);
        source.sendSuccess(() -> KeyMessages.message("Prueba de sonido: " + name + " (Master).", ChatFormatting.AQUA), false);
        return 1;
    }

    private static int sendState(CommandSourceStack source) {
        FabricLoader loader = FabricLoader.getInstance();
        String veil = loader.getModContainer("veil").map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("NO INSTALADO");
        String gecko = loader.getModContainer("geckolib").map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("NO INSTALADO");
        source.sendSuccess(() -> KeyMessages.message("Veil: " + veil + " | GeckoLib: " + gecko + " | llaves: 4", ChatFormatting.AQUA), false);
        return 1;
    }

    private static int giveFlashlight(CommandSourceStack source, ServerPlayer target) {
        ItemStack stack = new ItemStack(ModItems.FLASHLIGHT);
        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }
        source.sendSuccess(() -> KeyMessages.message("Linterna entregada a " + target.getGameProfile().getName(), ChatFormatting.GREEN), true);
        return 1;
    }
}
