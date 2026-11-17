package dev.greto.nightminigames.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.greto.nightminigames.NightminigamesMod;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20C;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class VeilHudShader {
    private static final ResourceLocation PROGRAM = NightminigamesMod.id("hud/liquid_ui");
    private static boolean requested;
    private static volatile boolean ready;
    private static boolean disabled;
    private static boolean warned;

    private VeilHudShader() {
    }

    public static boolean draw(GuiGraphics graphics, float x, float y, float width, float height, int rgb, float opacity, float progress, int mode, double time) {
        if (width <= 0 || height <= 0 || disabled || VeilRenderSystem.renderer() == null) {
            return false;
        }
        if (!requested) {
            request();
        }
        if (!ready) {
            return false;
        }
        ShaderProgram program = VeilRenderSystem.renderer().getShaderManager().getShader(PROGRAM);
        if (program == null || !program.isValid()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        float windowWidth = client.getWindow().getWidth();
        float windowHeight = client.getWindow().getHeight();
        float pixelX = windowWidth / graphics.guiWidth();
        float pixelY = windowHeight / graphics.guiHeight();
        graphics.flush();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            program.bind();
            program.getUniformSafe("uResolution").setVector(windowWidth, windowHeight);
            program.getUniformSafe("uRect").setVector(x * pixelX, y * pixelY, width * pixelX, height * pixelY);
            program.getUniformSafe("uColor").setVector(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F);
            program.getUniformSafe("uTime").setFloat((float) (time % 1000.0D));
            program.getUniformSafe("uProgress").setFloat(Mth.clamp(progress, 0.0F, 1.0F));
            program.getUniformSafe("uOpacity").setFloat(Mth.clamp(opacity, 0.0F, 1.0F));
            program.getUniformSafe("uMode").setInt(mode);
            VeilRenderSystem.drawScreenQuad();
            return true;
        } catch (RuntimeException exception) {
            disabled = true;
            if (!warned) {
                warned = true;
                NightminigamesMod.LOGGER.warn("Veil HUD shader unavailable, using native HUD renderer", exception);
            }
            return false;
        } finally {
            ShaderProgram.unbind();
            RenderSystem.depthMask(depthMask);
            if (depth) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            if (blend) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
        }
    }

    private static void request() {
        requested = true;
        try {
            var shaders = new Int2ObjectOpenHashMap<String>();
            shaders.put(GL20C.GL_VERTEX_SHADER, read("liquid_ui.vsh"));
            shaders.put(GL20C.GL_FRAGMENT_SHADER, read("liquid_ui.fsh"));
            VeilRenderSystem.renderer().getShaderManager().createDynamicProgram(PROGRAM, shaders)
                .whenComplete((program, error) -> {
                    if (error != null) {
                        disabled = true;
                        if (!warned) {
                            warned = true;
                            NightminigamesMod.LOGGER.warn("Failed to compile Veil HUD shader", error);
                        }
                    } else {
                        ready = program != null && program.isValid();
                    }
                });
        } catch (IOException | RuntimeException exception) {
            disabled = true;
            if (!warned) {
                warned = true;
                NightminigamesMod.LOGGER.warn("Could not initialize Veil HUD shader", exception);
            }
        }
    }

    private static String read(String name) throws IOException {
        String path = "/assets/nightminigames/shaders/hud/" + name;
        try (InputStream stream = VeilHudShader.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Shader resource not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
