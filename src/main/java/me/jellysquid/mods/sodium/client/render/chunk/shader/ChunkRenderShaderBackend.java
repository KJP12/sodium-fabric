package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.gl.compat.LegacyFogHelper;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL43C;

import java.util.EnumMap;

public abstract class ChunkRenderShaderBackend<T extends ChunkGraphicsState>
        implements ChunkRenderBackend<T> {
    private final EnumMap<ChunkFogMode, ChunkProgram> programs = new EnumMap<>(ChunkFogMode.class);

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected ChunkProgram activeProgram;

    public ChunkRenderShaderBackend(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
    }

    private ChunkProgram createShader(RenderDevice device, ChunkFogMode fogMode, GlVertexFormat<ChunkMeshAttribute> vertexFormat) {
        GlShader vertShader = ShaderLoader.loadShader(device, ShaderType.VERTEX,
                new Identifier("sodium", "chunk_gl20.v.glsl"), fogMode.getDefines());

        GlShader fragShader = ShaderLoader.loadShader(device, ShaderType.FRAGMENT,
                new Identifier("sodium", "chunk_gl20.f.glsl"), fogMode.getDefines());

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", ChunkShaderBindingPoints.POSITION)
                    .bindAttribute("a_Color", ChunkShaderBindingPoints.COLOR)
                    .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.TEX_COORD)
                    .bindAttribute("a_LightCoord", ChunkShaderBindingPoints.LIGHT_COORD)
                    .bindAttribute("d_ModelOffset", ChunkShaderBindingPoints.MODEL_OFFSET)
                    .build((program, name) -> new ChunkProgram(device, program, name, fogMode.getFactory()));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    @Override
    public final void createShaders(RenderDevice device) {
        this.programs.put(ChunkFogMode.NONE, this.createShader(device, ChunkFogMode.NONE, this.vertexFormat));
        this.programs.put(ChunkFogMode.LINEAR, this.createShader(device, ChunkFogMode.LINEAR, this.vertexFormat));
        this.programs.put(ChunkFogMode.EXP2, this.createShader(device, ChunkFogMode.EXP2, this.vertexFormat));
    }

    @Override
    public void begin(MatrixStack matrixStack, Matrix4f matrix4f) {
        this.activeProgram = this.programs.get(LegacyFogHelper.getFogMode());
        this.activeProgram.bind();
        //Shader shader = RenderSystem.getShader();
        //BufferRenderer.unbindAll();
        //GL43C.glBindAttribLocation(shader.getProgramRef(), ChunkShaderBindingPoints.MODEL_OFFSET.getGenericAttributeIndex(), "d_ModelOffset");
        /*for(int k = 0; k < 12; ++k) {
            int l = RenderSystem.getShaderTexture(k);
            shader.addSampler("Sampler" + k, l);
        }

        if (shader.modelViewMat != null) {
            shader.modelViewMat.set(matrixStack.peek().getModel());
        }

        if (shader.projectionMat != null) {
            shader.projectionMat.set(matrix4f);
        }

        if (shader.colorModulator != null) {
            shader.colorModulator.set(RenderSystem.getShaderColor());
        }

        if (shader.fogStart != null) {
            shader.fogStart.set(RenderSystem.getShaderFogStart());
        }

        if (shader.fogEnd != null) {
            shader.fogEnd.set(RenderSystem.getShaderFogEnd());
        }

        if (shader.fogColor != null) {
            shader.fogColor.set(RenderSystem.getShaderFogColor());
        }

        if (shader.textureMat != null) {
            shader.textureMat.set(RenderSystem.getTextureMatrix());
        }

        if (shader.gameTime != null) {
            shader.gameTime.set(RenderSystem.getShaderGameTime());
        }

        RenderSystem.setupShaderLights(shader);*/
        //shader.upload();
        //shader.bind();
        this.activeProgram.setup(matrixStack, this.vertexType.getModelScale(), this.vertexType.getTextureScale());
    }

    @Override
    public void end(MatrixStack matrixStack) {
        this.activeProgram.unbind();
        this.activeProgram = null;
    }

    @Override
    public void delete() {
        for (ChunkProgram shader : this.programs.values()) {
            shader.delete();
        }
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
