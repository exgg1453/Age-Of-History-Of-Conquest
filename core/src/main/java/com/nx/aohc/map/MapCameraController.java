package com.nx.aohc.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class MapCameraController extends InputAdapter {

    public interface ProvinceClickListener {
        void onProvinceClicked(int provinceId, float worldX, float worldY);
    }

    private final OrthographicCamera camera;
    private final ProvinceMap provinceMap;
    private final ProvinceClickListener listener;

    private final Vector3 temporaryVector = new Vector3();
    private final Vector2 lastSinglePointer = new Vector2();
    private final Vector2 firstPointer = new Vector2();
    private final Vector2 secondPointer = new Vector2();

    private boolean firstPointerActive;
    private boolean secondPointerActive;
    private int firstPointerId = -1;
    private int secondPointerId = -1;
    private float lastPinchDistance;
    private boolean dragged;
    private float dragDistance;

    private float minimumZoom = 0.05f;
    private float maximumZoom = 4f;

    public MapCameraController(OrthographicCamera camera, ProvinceMap provinceMap, ProvinceClickListener listener) {
        this.camera = camera;
        this.provinceMap = provinceMap;
        this.listener = listener;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!firstPointerActive) {
            firstPointerActive = true;
            firstPointerId = pointer;
            firstPointer.set(screenX, screenY);
            lastSinglePointer.set(screenX, screenY);
            dragged = false;
            dragDistance = 0f;
            return true;
        }
        if (!secondPointerActive) {
            secondPointerActive = true;
            secondPointerId = pointer;
            secondPointer.set(screenX, screenY);
            lastPinchDistance = firstPointer.dst(secondPointer);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer == firstPointerId) {
            firstPointer.set(screenX, screenY);
        } else if (pointer == secondPointerId) {
            secondPointer.set(screenX, screenY);
        } else {
            return false;
        }

        if (firstPointerActive && secondPointerActive) {
            float distance = firstPointer.dst(secondPointer);
            if (lastPinchDistance > 0f && distance > 0f) {
                float ratio = lastPinchDistance / distance;
                camera.zoom = MathUtils.clamp(camera.zoom * ratio, minimumZoom, maximumZoom);
            }
            lastPinchDistance = distance;
            dragged = true;
            clampCamera();
            return true;
        }

        if (pointer == firstPointerId) {
            float deltaX = lastSinglePointer.x - screenX;
            float deltaY = screenY - lastSinglePointer.y;
            dragDistance += Math.abs(deltaX) + Math.abs(deltaY);
            if (dragDistance > 12f) {
                dragged = true;
            }
            camera.position.add(deltaX * camera.zoom, deltaY * camera.zoom, 0f);
            lastSinglePointer.set(screenX, screenY);
            clampCamera();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        boolean wasSingleTap = firstPointerActive && !secondPointerActive && pointer == firstPointerId && !dragged;

        if (pointer == firstPointerId) {
            firstPointerActive = false;
            firstPointerId = -1;
        } else if (pointer == secondPointerId) {
            secondPointerActive = false;
            secondPointerId = -1;
        }

        if (!firstPointerActive && !secondPointerActive) {
            lastPinchDistance = 0f;
        }

        if (wasSingleTap && listener != null) {
            temporaryVector.set(screenX, screenY, 0f);
            camera.unproject(temporaryVector);
            int mapX = MathUtils.floor(temporaryVector.x);
            int mapY = provinceMap.getHeight() - 1 - MathUtils.floor(temporaryVector.y);
            int provinceId = provinceMap.getProvinceIdAt(mapX, mapY);
            listener.onProvinceClicked(provinceId, temporaryVector.x, temporaryVector.y);
        }
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom = MathUtils.clamp(camera.zoom * (1f + amountY * 0.12f), minimumZoom, maximumZoom);
        clampCamera();
        return true;
    }

    public void clampCamera() {
        float halfWidth = camera.viewportWidth * camera.zoom * 0.5f;
        float halfHeight = camera.viewportHeight * camera.zoom * 0.5f;
        float mapWidth = provinceMap.getWidth();
        float mapHeight = provinceMap.getHeight();

        if (halfWidth * 2f >= mapWidth) {
            camera.position.x = mapWidth * 0.5f;
        } else {
            camera.position.x = MathUtils.clamp(camera.position.x, halfWidth, mapWidth - halfWidth);
        }

        if (halfHeight * 2f >= mapHeight) {
            camera.position.y = mapHeight * 0.5f;
        } else {
            camera.position.y = MathUtils.clamp(camera.position.y, halfHeight, mapHeight - halfHeight);
        }

        camera.update();
    }

    public void focusOn(float worldX, float worldY) {
        camera.position.set(worldX, worldY, 0f);
        clampCamera();
    }

    public void setZoomLimits(float minimum, float maximum) {
        this.minimumZoom = minimum;
        this.maximumZoom = maximum;
    }

    public void updateViewport(int screenWidth, int screenHeight) {
        camera.viewportWidth = screenWidth;
        camera.viewportHeight = screenHeight;
        float fitZoom = Math.max(provinceMap.getWidth() / (float) screenWidth, provinceMap.getHeight() / (float) screenHeight);
        maximumZoom = fitZoom;
        camera.zoom = MathUtils.clamp(camera.zoom, minimumZoom, maximumZoom);
        clampCamera();
    }

    public float getDefaultZoom() {
        return Math.max(provinceMap.getWidth() / (float) Gdx.graphics.getWidth(),
                provinceMap.getHeight() / (float) Gdx.graphics.getHeight());
    }
}
