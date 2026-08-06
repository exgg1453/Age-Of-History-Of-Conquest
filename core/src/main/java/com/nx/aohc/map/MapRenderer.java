package com.nx.aohc.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

import com.nx.aohc.graphics.QualitySettings;

public class MapRenderer {

    private static final int PALETTE_SIZE = 256;

    private final ProvinceMap provinceMap;
    private final QualitySettings qualitySettings;
    private final ShaderProgram shader;
    private final Mesh mesh;
    private final Pixmap palettePixmap;
    private final Texture paletteTexture;
    private final Matrix4 screenProjection = new Matrix4();
    private final Color borderColor = new Color(0.08f, 0.08f, 0.10f, 1f);
    private final Color seaColor = new Color(0.10f, 0.20f, 0.36f, 1f);

    private FrameBuffer frameBuffer;
    private int frameBufferWidth;
    private int frameBufferHeight;
    private int screenWidth;
    private int screenHeight;
    private float borderStrength = 0.85f;
    private boolean paletteDirty = true;

    public MapRenderer(ProvinceMap provinceMap, QualitySettings qualitySettings) {
        this.provinceMap = provinceMap;
        this.qualitySettings = qualitySettings;

        String vertexSource = Gdx.files.internal("shaders/map.vert").readString();
        String fragmentSource = Gdx.files.internal("shaders/map.frag").readString();
        String defines = "#define BORDER_MODE " + qualitySettings.getBorderMode() + "\n";

        ShaderProgram.pedantic = false;
        this.shader = new ShaderProgram(vertexSource, injectDefines(fragmentSource, defines));
        if (!shader.isCompiled()) {
            throw new IllegalStateException("Map shader failed to compile: " + shader.getLog());
        }

        this.palettePixmap = new Pixmap(PALETTE_SIZE, PALETTE_SIZE, Pixmap.Format.RGBA8888);
        this.palettePixmap.setBlending(Pixmap.Blending.None);
        this.palettePixmap.setColor(0f, 0f, 0f, 1f);
        this.palettePixmap.fill();
        this.paletteTexture = new Texture(palettePixmap);
        this.paletteTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        this.paletteTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

        this.mesh = buildQuad(provinceMap.getWidth(), provinceMap.getHeight());
    }

    private String injectDefines(String source, String defines) {
        int precisionEnd = source.indexOf("#endif");
        if (source.startsWith("#ifdef GL_ES") && precisionEnd >= 0) {
            int insertAt = precisionEnd + "#endif".length();
            return source.substring(0, insertAt) + "\n" + defines + source.substring(insertAt);
        }
        return defines + source;
    }

    private Mesh buildQuad(float width, float height) {
        Mesh quad = new Mesh(true, 4, 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        float[] vertices = new float[]{
                0f, 0f, 0f, 1f,
                width, 0f, 1f, 1f,
                width, height, 1f, 0f,
                0f, height, 0f, 0f
        };
        short[] indices = new short[]{0, 1, 2, 2, 3, 0};

        quad.setVertices(vertices);
        quad.setIndices(indices);
        return quad;
    }

    public void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        screenProjection.setToOrtho2D(0f, 0f, width, height);

        int targetWidth = Math.max(320, Math.round(width * qualitySettings.getRenderScale()));
        int targetHeight = Math.max(240, Math.round(height * qualitySettings.getRenderScale()));

        if (qualitySettings.getRenderScale() >= 0.999f) {
            disposeFrameBuffer();
            return;
        }

        if (frameBuffer != null && targetWidth == frameBufferWidth && targetHeight == frameBufferHeight) {
            return;
        }

        disposeFrameBuffer();
        frameBufferWidth = targetWidth;
        frameBufferHeight = targetHeight;
        frameBuffer = new FrameBuffer(Pixmap.Format.RGB565, frameBufferWidth, frameBufferHeight, false);

        Texture texture = frameBuffer.getColorBufferTexture();
        Texture.TextureFilter filter = qualitySettings.isLinearMapFiltering()
                ? Texture.TextureFilter.Linear
                : Texture.TextureFilter.Nearest;
        texture.setFilter(filter, filter);
    }

    public void setProvinceColor(int provinceId, Color color) {
        if (provinceId <= 0 || provinceId >= PALETTE_SIZE * PALETTE_SIZE) {
            return;
        }
        palettePixmap.setColor(color);
        palettePixmap.drawPixel(provinceId % PALETTE_SIZE, provinceId / PALETTE_SIZE);
        paletteDirty = true;
    }

    public void uploadPaletteIfNeeded() {
        if (!paletteDirty) {
            return;
        }
        paletteTexture.bind();
        Gdx.gl.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, PALETTE_SIZE, PALETTE_SIZE, 0,
                GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, palettePixmap.getPixels());
        paletteDirty = false;
    }

    public void render(Matrix4 combined, SpriteBatch batch) {
        if (frameBuffer == null) {
            renderMap(combined);
            return;
        }

        frameBuffer.begin();
        Gdx.gl.glClearColor(seaColor.r, seaColor.g, seaColor.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderMap(combined);
        frameBuffer.end();

        batch.setShader(null);
        batch.setProjectionMatrix(screenProjection);
        batch.disableBlending();
        batch.begin();
        batch.draw(frameBuffer.getColorBufferTexture(), 0f, 0f, screenWidth, screenHeight, 0f, 1f, 1f, 0f);
        batch.end();
        batch.enableBlending();
    }

    private void renderMap(Matrix4 combined) {
        uploadPaletteIfNeeded();

        provinceMap.getProvinceTexture().bind(1);
        paletteTexture.bind(2);

        shader.bind();
        shader.setUniformMatrix("u_projTrans", combined);
        shader.setUniformi("u_provinceTexture", 1);
        shader.setUniformi("u_paletteTexture", 2);
        shader.setUniformf("u_texelSize", 1f / provinceMap.getWidth(), 1f / provinceMap.getHeight());
        shader.setUniformf("u_borderColor", borderColor.r, borderColor.g, borderColor.b, borderColor.a);
        shader.setUniformf("u_seaColor", seaColor.r, seaColor.g, seaColor.b, seaColor.a);
        shader.setUniformf("u_borderStrength", borderStrength);

        mesh.render(shader, GL20.GL_TRIANGLES);

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
    }

    public void setBorderStrength(float value) {
        this.borderStrength = value;
    }

    public Color getSeaColor() {
        return seaColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    private void disposeFrameBuffer() {
        if (frameBuffer != null) {
            frameBuffer.dispose();
            frameBuffer = null;
        }
    }

    public void dispose() {
        disposeFrameBuffer();
        mesh.dispose();
        shader.dispose();
        paletteTexture.dispose();
        palettePixmap.dispose();
    }
}
