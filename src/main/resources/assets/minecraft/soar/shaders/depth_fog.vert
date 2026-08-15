#version 120

varying vec2 vUv;

/*
 * The Java pass supplies a clip-space quad, so this shader deliberately
 * bypasses Minecraft's current world matrices.  The captured matrices are
 * instead used by depth_fog.frag to reconstruct the original world ray.
 */
void main() {
    vUv = gl_MultiTexCoord0.xy;
    gl_Position = gl_Vertex;
}
