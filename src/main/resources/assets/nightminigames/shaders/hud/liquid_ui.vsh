#version 330 core
out vec2 vScreenUv;
void main() {
    vec2 xy = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
    vScreenUv = xy * 0.5;
    gl_Position = vec4(xy * 2.0 - 1.0, 0.0, 1.0);
}
