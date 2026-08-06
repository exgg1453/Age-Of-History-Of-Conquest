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

    float left = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(-u_texelSize.x, 0.0)));
    float right = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(u_texelSize.x, 0.0)));
    float up = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(0.0, -u_texelSize.y)));
    float down = decodeIndex(texture2D(u_provinceTexture, v_texCoord + vec2(0.0, u_texelSize.y)));

    float provinceEdge = 0.0;
    if (left != index || right != index || up != index || down != index) {
        provinceEdge = 1.0;
    }

    vec4 leftColor = lookupOwnerColor(left);
    vec4 rightColor = lookupOwnerColor(right);
    vec4 upColor = lookupOwnerColor(up);
    vec4 downColor = lookupOwnerColor(down);

    float countryEdge = 0.0;
    if (leftColor != baseColor || rightColor != baseColor || upColor != baseColor || downColor != baseColor) {
        countryEdge = 1.0;
    }

    vec4 result = baseColor;

    if (provinceEdge > 0.5) {
        result = mix(result, u_borderColor, u_borderStrength * 0.35);
    }

    if (countryEdge > 0.5) {
        result = mix(result, u_borderColor, u_borderStrength);
    }

    gl_FragColor = result;
}
