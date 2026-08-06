#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoord;

uniform sampler2D u_provinceTexture;
uniform sampler2D u_paletteTexture;

uniform vec2 u_texelSize;
uniform vec4 u_borderColor;
uniform vec4 u_seaColor;
uniform float u_borderStrength;

float decodeIndex(vec4 encoded) {
    return floor(encoded.r * 255.0 + 0.5) + floor(encoded.g * 255.0 + 0.5) * 256.0;
}

vec4 lookupOwnerColor(float index) {
    float column = mod(index, 256.0);
    float row = floor(index / 256.0);
    vec2 paletteCoord = vec2((column + 0.5) / 256.0, (row + 0.5) / 256.0);
    return texture2D(u_paletteTexture, paletteCoord);
}

void main() {
    vec4 encoded = texture2D(u_provinceTexture, v_texCoord);
    float index = decodeIndex(encoded);

    if (index < 0.5) {
        gl_FragColor = u_seaColor;
        return;
    }

    vec4 baseColor = lookupOwnerColor(index);

#if BORDER_MODE == 0
    gl_FragColor = baseColor;
#endif

#if BORDER_MODE == 1
    float rightIndex = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(u_texelSize.x, 0.0)));
    float downIndex = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(0.0, u_texelSize.y)));

    vec4 rightColor = lookupOwnerColor(rightIndex);
    vec4 downColor = lookupOwnerColor(downIndex);

    vec4 lowResult = baseColor;
    if (rightColor != baseColor || downColor != baseColor) {
        lowResult = mix(lowResult, u_borderColor, u_borderStrength);
    }
    gl_FragColor = lowResult;
#endif

#if BORDER_MODE == 2
    float leftIndex = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(-u_texelSize.x, 0.0)));
    float rightIndex = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(u_texelSize.x, 0.0)));
    float upIndex = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(0.0, -u_texelSize.y)));
    float downIndex = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(0.0, u_texelSize.y)));

    float provinceEdge = 0.0;
    if (leftIndex != index || rightIndex != index || upIndex != index || downIndex != index) {
        provinceEdge = 1.0;
    }

    vec4 leftColor = lookupOwnerColor(leftIndex);
    vec4 rightColor = lookupOwnerColor(rightIndex);
    vec4 upColor = lookupOwnerColor(upIndex);
    vec4 downColor = lookupOwnerColor(downIndex);

    float countryEdge = 0.0;
    if (leftColor != baseColor || rightColor != baseColor || upColor != baseColor || downColor != baseColor) {
        countryEdge = 1.0;
    }

    vec4 highResult = baseColor;

    if (provinceEdge > 0.5) {
        highResult = mix(highResult, u_borderColor, u_borderStrength * 0.35);
    }

    if (countryEdge > 0.5) {
        highResult = mix(highResult, u_borderColor, u_borderStrength);
    }

    gl_FragColor = highResult;
#endif
}
