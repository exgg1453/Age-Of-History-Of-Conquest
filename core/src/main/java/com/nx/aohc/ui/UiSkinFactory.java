package com.nx.aohc.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.graphics.g2d.NinePatch;

public class UiSkinFactory {

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                    + "ÇÖÜĞİŞçöüğışÁÂÄÀÉÊËÈÍÎÏÌÓÔÕÒÚÛÙÑáâäàéêëèíîïìóôõòúûùñ"
                    + ".,:;!?'\"()[]{}<>+-*/=%&#@~_|\\^$€₺£¥°·…–—«»\u00A0 ";

    public static final Color BACKGROUND = new Color(0.07f, 0.09f, 0.12f, 1f);
    public static final Color PANEL = new Color(0.12f, 0.15f, 0.19f, 0.96f);
    public static final Color PANEL_LIGHT = new Color(0.18f, 0.22f, 0.27f, 1f);
    public static final Color ACCENT = new Color(0.83f, 0.66f, 0.31f, 1f);
    public static final Color TEXT = new Color(0.92f, 0.93f, 0.95f, 1f);
    public static final Color TEXT_DIM = new Color(0.66f, 0.70f, 0.76f, 1f);

    public static Skin create(float scale) {
        Skin skin = new Skin();

        skin.add("white", createSolidTexture(Color.WHITE));
        skin.add("panel", createRoundedDrawable(PANEL, 14, scale));
        skin.add("panel-light", createRoundedDrawable(PANEL_LIGHT, 12, scale));
        skin.add("button-up", createRoundedDrawable(new Color(0.16f, 0.20f, 0.25f, 1f), 12, scale));
        skin.add("button-down", createRoundedDrawable(new Color(0.24f, 0.30f, 0.37f, 1f), 12, scale));
        skin.add("button-accent", createRoundedDrawable(ACCENT, 12, scale));
        skin.add("selection", createRoundedDrawable(new Color(0.83f, 0.66f, 0.31f, 0.35f), 8, scale));

        BitmapFont regular = generateFont("fonts/ui-regular.ttf", Math.round(18 * scale));
        BitmapFont bold = generateFont("fonts/ui-bold.ttf", Math.round(20 * scale));
        BitmapFont title = generateFont("fonts/ui-bold.ttf", Math.round(34 * scale));
        BitmapFont small = generateFont("fonts/ui-regular.ttf", Math.round(15 * scale));

        skin.add("default", regular);
        skin.add("bold", bold);
        skin.add("title", title);
        skin.add("small", small);

        Label.LabelStyle labelStyle = new Label.LabelStyle(regular, TEXT);
        skin.add("default", labelStyle);

        Label.LabelStyle boldStyle = new Label.LabelStyle(bold, TEXT);
        skin.add("bold", boldStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle(title, ACCENT);
        skin.add("title", titleStyle);

        Label.LabelStyle smallStyle = new Label.LabelStyle(small, TEXT_DIM);
        skin.add("small", smallStyle);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.getDrawable("button-up");
        buttonStyle.down = skin.getDrawable("button-down");
        buttonStyle.over = skin.getDrawable("button-down");
        buttonStyle.font = bold;
        buttonStyle.fontColor = TEXT;
        skin.add("default", buttonStyle);

        TextButton.TextButtonStyle accentStyle = new TextButton.TextButtonStyle();
        accentStyle.up = skin.getDrawable("button-accent");
        accentStyle.down = skin.getDrawable("button-down");
        accentStyle.font = bold;
        accentStyle.fontColor = new Color(0.08f, 0.09f, 0.11f, 1f);
        skin.add("accent", accentStyle);

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = regular;
        listStyle.fontColorSelected = ACCENT;
        listStyle.fontColorUnselected = TEXT;
        listStyle.selection = skin.getDrawable("selection");
        listStyle.background = skin.getDrawable("panel-light");
        skin.add("default", listStyle);

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = skin.getDrawable("panel");
        skin.add("default", scrollStyle);

        return skin;
    }

    private static BitmapFont generateFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = CHARACTERS;
        parameter.magFilter = Texture.TextureFilter.Linear;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.hinting = FreeTypeFontGenerator.Hinting.Slight;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    private static Texture createSolidTexture(Color color) {
        Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private static Drawable createRoundedDrawable(Color color, int radius, float scale) {
        int scaledRadius = Math.max(2, Math.round(radius * scale));
        int size = scaledRadius * 2 + 4;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(color);
        pixmap.fillCircle(scaledRadius, scaledRadius, scaledRadius);
        pixmap.fillCircle(size - scaledRadius - 1, scaledRadius, scaledRadius);
        pixmap.fillCircle(scaledRadius, size - scaledRadius - 1, scaledRadius);
        pixmap.fillCircle(size - scaledRadius - 1, size - scaledRadius - 1, scaledRadius);
        pixmap.fillRectangle(scaledRadius, 0, size - scaledRadius * 2, size);
        pixmap.fillRectangle(0, scaledRadius, size, size - scaledRadius * 2);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        NinePatch patch = new NinePatch(texture, scaledRadius + 1, scaledRadius + 1, scaledRadius + 1, scaledRadius + 1);
        return new NinePatchDrawable(patch);
    }
}
