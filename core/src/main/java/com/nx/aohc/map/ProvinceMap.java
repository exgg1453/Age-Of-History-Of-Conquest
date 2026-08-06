package com.nx.aohc.map;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import com.nx.aohc.game.Province;

public class ProvinceMap {

    private final int width;
    private final int height;
    private final Pixmap pixmap;
    private final Texture provinceTexture;
    private final IntMap<Province> provinces = new IntMap<Province>();
    private int highestProvinceId;

    public ProvinceMap(FileHandle rasterFile, FileHandle metadataFile) {
        this.pixmap = new Pixmap(rasterFile);
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();

        this.provinceTexture = new Texture(pixmap, Pixmap.Format.RGBA8888, false);
        this.provinceTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        this.provinceTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

        loadMetadata(metadataFile);
    }

    private void loadMetadata(FileHandle metadataFile) {
        JsonValue root = new JsonReader().parse(metadataFile);
        JsonValue provinceArray = root.get("provinces");
        for (JsonValue entry = provinceArray.child; entry != null; entry = entry.next) {
            int id = entry.getInt("id");
            String name = entry.getString("name", "Province " + id);
            String country = entry.getString("country", "");
            int pixelCount = entry.getInt("pixels", 0);
            float centroidX = entry.getFloat("centroidX", 0f);
            float centroidY = entry.getFloat("centroidY", 0f);
            String terrain = entry.getString("terrain", "plains");

            JsonValue neighbourArray = entry.get("neighbours");
            int[] neighbours;
            if (neighbourArray != null && neighbourArray.size > 0) {
                neighbours = neighbourArray.asIntArray();
            } else {
                neighbours = new int[0];
            }

            Province province = new Province(id, name, country, pixelCount, centroidX, centroidY, terrain, neighbours);
            provinces.put(id, province);
            if (id > highestProvinceId) {
                highestProvinceId = id;
            }
        }
    }

    public int getProvinceIdAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }
        int packed = pixmap.getPixel(x, y);
        int red = (packed >>> 24) & 0xFF;
        int green = (packed >>> 16) & 0xFF;
        return red | (green << 8);
    }

    public Province getProvinceAt(int x, int y) {
        return provinces.get(getProvinceIdAt(x, y));
    }

    public Province getProvince(int id) {
        return provinces.get(id);
    }

    public IntMap<Province> getProvinces() {
        return provinces;
    }

    public int getHighestProvinceId() {
        return highestProvinceId;
    }

    public Texture getProvinceTexture() {
        return provinceTexture;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void dispose() {
        provinceTexture.dispose();
        pixmap.dispose();
    }
}
