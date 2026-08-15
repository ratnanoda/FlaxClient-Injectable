#version 120

uniform sampler2D SceneTexture;
uniform sampler2D DepthTexture;

uniform mat4 InvProjection;
uniform mat4 InvModelView;
uniform vec3 CameraPosition;
uniform vec3 FogColor;
uniform float FogStart;
uniform float FogEnd;
uniform float FogDensity;
uniform float AmbientFog;
uniform float Time;

varying vec2 vUv;

/*
 * Smooth trilinear value noise.  Its inputs are world positions rather than
 * screen coordinates, keeping the fog fixed in space as the camera moves.
 */
float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float valueNoise(vec3 p) {
    vec3 cell = floor(p);
    vec3 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);

    float n000 = hash31(cell + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, local.x);
    float nx10 = mix(n010, n110, local.x);
    float nx01 = mix(n001, n101, local.x);
    float nx11 = mix(n011, n111, local.x);
    float nxy0 = mix(nx00, nx10, local.y);
    float nxy1 = mix(nx01, nx11, local.y);
    return mix(nxy0, nxy1, local.z);
}

float lowFrequencyFogNoise(vec3 worldPos) {
    /* Slow advection only; this should read as drifting mist, not smoke. */
    vec3 drift = vec3(Time * 0.010, 0.0, Time * 0.006);

    float n = 0.0;
    n += valueNoise(worldPos * 0.035 + drift) * 0.55;
    n += valueNoise(worldPos * 0.070 + drift) * 0.30;
    n += valueNoise(worldPos * 0.140 + drift) * 0.15;
    return n;
}

void main() {
    vec4 sceneSample = texture2D(SceneTexture, vUv);
    float rawDepth = texture2D(DepthTexture, vUv).r;

    /* Reconstruct the real view-space position from the copied depth. */
    vec4 clipPosition = vec4(vUv * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
    vec4 viewPosition = InvProjection * clipPosition;
    viewPosition.xyz /= viewPosition.w;

    float distance = length(viewPosition.xyz);
    vec3 viewRay = normalize(viewPosition.xyz);
    vec3 worldRay = normalize((InvModelView * vec4(viewRay, 0.0)).xyz);
    vec3 worldPosition = CameraPosition + worldRay * distance;

    /* Minecraft's sky normally leaves depth at 1.0.  Give it a virtual
     * distant world position so it receives the same soft, moving fog. */
    if (rawDepth >= 0.999999) {
        distance = FogEnd + 48.0;
        worldPosition = CameraPosition + worldRay * distance;
    }

    float normalizedDensity = clamp(FogDensity, 0.0, 1.0);
    if (normalizedDensity <= 0.0001) {
        gl_FragColor = sceneSample;
        return;
    }

    float baseFog = smoothstep(FogStart, FogEnd, distance);
    float fog = pow(baseFog, 1.35) * normalizedDensity;

    float noise = lowFrequencyFogNoise(worldPosition);
    fog += (noise - 0.5) * 0.25 * fog;

    /* A restrained height bias makes lower air feel slightly denser. */
    float heightFog = exp(-(worldPosition.y - CameraPosition.y) * 0.015);
    fog *= mix(0.80, 1.20, clamp(heightFog, 0.0, 1.0));

    /*
     * The density slider still permits 0% fog, but at the requested 75%
     * default the last part of the smooth curve blends into near-opacity.
     * This is continuous (not a terminal quad), while retaining a trace of
     * noise instead of changing the distance horizon into a flat white wall.
     */
    float endBlend = smoothstep(0.72, 1.0, pow(baseFog, 1.35));
    float endpointFog = 0.985 * smoothstep(0.0, 0.20, normalizedDensity);
    float endpointNoise = clamp(endpointFog + (noise - 0.5) * 0.020, 0.0, 0.985);
    fog = mix(fog, endpointNoise, endBlend);
    fog = clamp(fog, 0.0, 0.985);

    /* Optional close-range mist.  It samples a short section of the actual
     * world ray, so the soft pattern belongs to the world instead of sticking
     * to the screen.  The over-composite preserves the existing distance fog
     * and only adds a denser atmospheric layer around the player. */
    float ambientEnabled = clamp(AmbientFog, 0.0, 1.0);
    if (ambientEnabled > 0.0) {
        float ambientDistance = min(max(distance, 1.0), 12.0);
        vec3 ambientPosition = CameraPosition + worldRay * ambientDistance;
        float ambientNoise = lowFrequencyFogNoise(ambientPosition);
        float ambientHeight = exp(
            -(ambientPosition.y - CameraPosition.y) * 0.015
        );
        float ambientHeightFactor = mix(
            0.90, 1.10, clamp(ambientHeight, 0.0, 1.0)
        );
        float ambientVariation = 0.80 + (ambientNoise - 0.5) * 0.30;
        float ambientAmount = 0.180 * ambientEnabled * normalizedDensity
                * ambientVariation * ambientHeightFactor;
        ambientAmount = clamp(ambientAmount, 0.0, 0.220);
        fog = fog + (1.0 - fog) * ambientAmount;
        fog = min(fog, 0.985);
    }

    /* Far scenery loses a little colour contrast before it is mixed into
     * the mist, rather than looking like a transparent white overlay. */
    float luminance = dot(sceneSample.rgb, vec3(0.333333));
    vec3 softenedScene = mix(sceneSample.rgb, vec3(luminance), fog * 0.15);
    vec3 finalColor = mix(softenedScene, FogColor, fog);

    gl_FragColor = vec4(finalColor, sceneSample.a);
}
