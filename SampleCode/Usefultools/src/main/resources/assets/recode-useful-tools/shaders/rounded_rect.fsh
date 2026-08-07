#version 150

precision mediump float;

uniform vec2 u_size;    // 四角形のサイズ
uniform float u_radius; // 角丸の半径
uniform vec4 u_color;   // 色

out vec4 fragColor;

// SDF (距離関数) を使った角丸の計算
float roundedRectSDF(vec2 p, vec2 b, float r) {
    vec2 d = abs(p) - b + r;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main() {
    // 0.0〜1.0 の座標系をピクセル座標に変換
    vec2 p = gl_FragCoord.xy;
    vec2 center = u_size * 0.5;

    // 距離を計算
    float distance = roundedRectSDF(p - center, u_size * 0.5, u_radius);

    // smoothstep を使うことで、拡大しても絶対にジャギー（ギザギザ）が出ない滑らかさを実現
    float alpha = 1.0 - smoothstep(-1.0, 1.0, distance);

    fragColor = vec4(u_color.rgb, u_color.a * alpha);
}