#version 150

precision mediump float;
uniform sampler2D DiffuseSampler;
uniform vec2 TexSize;
uniform float Radius;
uniform vec2 Direction;

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / TexSize;
    vec4 color = vec4(0.0);
    float totalWeight = 0.0;

    // 自前での平滑化フィルタ計算（マイクラのブラー設定は無視）
    for (float i = -Radius; i <= Radius; i++) {
        float weight = exp(-(i * i) / (2.0 * Radius * Radius));
        color += texture(DiffuseSampler, uv + (Direction * i) / TexSize) * weight;
        totalWeight += weight;
    }
    fragColor = color / totalWeight;
}