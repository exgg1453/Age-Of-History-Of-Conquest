package com.nx.aohc.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

public class MapRenderer {

    private static final int PALETTE_SIZE = 256;

    private final ProvinceMap provinceMap;
    private final ShaderProgram shader;
    private final Mesh mesh;
    private final Pixmap palettePixmap;
    private final Texture paletteTexture;
    private final Color borderColor = new Color(0.08f, 0.08f, 0.10f, 1f);
    private final Color seaColor = new Color(0.10f, 0.20f, 0.36f, 1f);
    private float borderStrength = 0.85f;
    private boolean paletteDirty = true;

    public MapRenderer(ProvinceMap provinceMap) {
        this.provinceMap = provinceMap;

        String vertexSource = Gdx.files.internal("shaders/map.vert").readString();
        String fragmentSource = Gdx.files.internal("shaders/map.frag").readString();
        ShaderProgram.pedantic = false;
        this.shader = new ShaderProgram(vertexSource, fragmentSource);
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

    public void setProvinceColor(int provinceId, Color color) {
        if (provinceId <= 0 || provinceId >= PALETTE_SIZE * PALETTE_SIZE) {
            return;
        }
        int column = provinceId % PALETTE_SIZE;
        int row = provinceId / PALETTE_SIZE;
        palettePixmap.setColor(color);
        palettePixmap.drawPixel(column, row);
        paletteDirty = true;
    }

    public void setProvinceColor(int provinceId, float red, float green, float blue, float alpha) {
        if (provinceId <= 0 || provinceId >= PALETTE_SIZE * PALETTE_SIZE) {
            return;
        }
        int column = provinceId % PALETTE_SIZE;
        int row = provinceId / PALETTE_SIZE;
        palettePixmap.setColor(red, green, blue, alpha);
        palettePixmap.drawPixel(column, row);
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

    public void render(Matrix4 combined) {
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

    public void dispose() {
        mesh.dispose();
        shader.dispose();
        paletteTexture.dispose();
        palettePixmap.dispose();
    }
}
