#version 330 core
in vec2 vScreenUv;
out vec4 fragColor;
uniform vec2 uResolution;
uniform vec4 uRect;
uniform vec3 uColor;
uniform float uTime;
uniform float uProgress;
uniform float uOpacity;
uniform int uMode;

float nmHash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float nmNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(nmHash(i), nmHash(i + vec2(1.0, 0.0)), f.x),
               mix(nmHash(i + vec2(0.0, 1.0)), nmHash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float nmRoundRect(vec2 point, vec2 halfSize, float rounding) {
    vec2 q = abs(point) - halfSize + vec2(rounding);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - rounding;
}

float nmDisc(vec2 p, vec2 center, float radius) {
    return 1.0 - smoothstep(radius - 1.3, radius + 1.3, length(p - center));
}

float nmSoftRing(vec2 p, vec2 center, float radius, float thickness) {
    return exp(-pow((length(p - center) - radius) / thickness, 2.0) * 1.65);
}

float nmDroplets(vec2 p, vec2 orb, float t, float rad, float direction) {
    float droplets = 0.0;
    for (int j = 0; j < 25; j++) {
        float i = float(j);
        float a = i * 2.39996323 + direction * t * 2.0;
        float offset = (0.65 + nmHash(vec2(i, 9.3)) * 0.6) * rad;
        float travel = mix(2.7, 0.48, clamp(t, 0.0, 1.0));
        vec2 center = orb + vec2(cos(a), sin(a) * 0.76) * offset * travel;
        center += vec2(sin(a * 4.0 + t * 9.0), cos(a * 3.0 + t * 7.0)) * rad * 0.065;
        float size = 1.2 + nmHash(vec2(i, 14.0)) * 2.9;
        droplets = max(droplets, nmDisc(p, center, size));
    }
    return droplets;
}

void main() {
    vec2 screen = vec2(gl_FragCoord.x, uResolution.y - gl_FragCoord.y);
    vec2 p = screen - uRect.xy;
    vec2 size = uRect.zw;
    if (p.x < -26.0 || p.y < -26.0 || p.x > size.x + 26.0 || p.y > size.y + 26.0) {
        discard;
    }
    vec2 uv = p / max(size, vec2(1.0));
    float t = uTime;
    float d = nmRoundRect(p - size * 0.5, max(vec2(2.0), size * 0.5 - vec2(3.5)), 10.0);
    float panel = 1.0 - smoothstep(-0.1, 1.5, d);
    float rim = exp(-abs(d) * 0.55);
    float halo = exp(-max(0.0, d) * 0.18) * 0.22;
    float top = pow(max(0.0, 1.0 - uv.y), 3.2);
    float sheen = pow(max(0.0, 1.0 - abs((uv.x + uv.y * 0.13 - fract(t * 0.042) * 1.6 + 0.3) * 18.0)), 3.0);
    float band = 0.5 + 0.5 * sin(uv.y * 33.0 + uv.x * 6.0 + t * 1.7);
    float wave = 0.5 + 0.5 * sin(uv.x * 18.0 - t * 2.6 + sin(uv.y * 12.0 + t * 1.1) * 1.3);
    float micro = nmNoise(uv * vec2(155.0, 70.0) + t * vec2(0.8, -0.4));
    float horizontalLines = 0.5 + 0.5 * sin(p.y * 0.72);
    float sparkle = pow(max(0.0, nmNoise(uv * vec2(56.0, 20.0) + t * 0.4)), 12.0);
    vec3 base = vec3(0.019, 0.045, 0.075);
    base += vec3(0.012, 0.022, 0.041) * (1.0 - uv.y);
    base += uColor * (top * 0.075 + wave * 0.025 + band * 0.012);
    base += vec3(micro * 0.013 + horizontalLines * 0.006);
    base += uColor * sheen * 0.068;
    vec3 color = base * panel;
    color += uColor * (rim * 0.39 + halo * 0.16);
    color += mix(uColor, vec3(1.0), 0.65) * (rim * 0.17 + sparkle * 0.18) * panel;
    float a = max(panel * 0.92, (rim + halo) * 0.48);
    vec2 orb = vec2(size.x * (uMode == 3 ? 0.171 : 0.113), size.y * 0.515);
    float orbRadius = min(size.y * 0.33, size.x * 0.14);
    float shimmer = 0.5 + 0.5 * sin(uv.x * 39.0 + t * 3.1);
    if (uMode == 0 || uMode == 1) {
        vec2 barOrigin = vec2(size.x * 0.065, size.y * 0.58);
        vec2 barSize = vec2(size.x * 0.87, size.y * 0.17);
        vec2 bar = (p - barOrigin) / barSize;
        float inBar = step(0.0, bar.x) * step(bar.x, 1.0) * step(0.0, bar.y) * step(bar.y, 1.0);
        float edgeGlow = exp(-pow((bar.x - uProgress) * 25.0, 2.0));
        color += uColor * inBar * (step(bar.x, uProgress) * (0.10 + 0.05 * shimmer) + edgeGlow * 0.18);
        color += uColor * nmSoftRing(p, orb, orbRadius * (0.77 + 0.1 * sin(t * 2.0)), 3.0) * 0.13 * panel;
        color += uColor * nmDroplets(p, orb, fract(t * 0.13), orbRadius * 0.46, 1.0) * 0.12 * panel;
    } else if (uMode == 2) {
        float gauge = 0.5 + 0.5 * sin(t * 1.6 + uv.x * 28.0);
        float bands = step(0.47, fract(uv.x * 23.0 + t * 0.08));
        color += uColor * gauge * 0.07 * panel;
        color += uColor * bands * (0.022 + 0.04 * uProgress) * panel;
    } else if (uMode == 3) {
        float reveal = smoothstep(0.49, 0.88, uProgress);
        color *= reveal;
        a *= reveal;
        float forming = smoothstep(0.06, 0.64, uProgress);
        float explosion = smoothstep(0.14, 0.42, uProgress) * (1.0 - smoothstep(0.51, 0.86, uProgress));
        float fluidRadius = orbRadius * mix(0.44, 1.07, forming);
        float liquidEdge = nmNoise(p * 0.105 + vec2(t * 0.52, -t * 0.43));
        float displaced = fluidRadius + (liquidEdge - 0.5) * 9.0 * forming;
        float mass = 1.0 - smoothstep(displaced - 1.8, displaced + 1.8, length(p - orb));
        float glowMass = exp(-abs(length(p - orb) - displaced) * 0.12);
        float dropletPhase = 1.0 - smoothstep(0.60, 0.95, uProgress);
        float droplets = nmDroplets(p, orb, forming, orbRadius * 1.2, 1.0) * dropletPhase;
        float burstRing = nmSoftRing(p, orb, orbRadius * (1.0 + explosion * 2.1), 4.0) * explosion;
        color += uColor * (mass * 0.24 + glowMass * 0.42 + droplets * 0.49 + burstRing * 0.52) * panel;
        color += mix(uColor, vec3(1.0), 0.67) * (droplets * 0.25 + burstRing * 0.22) * panel;
        a = max(a, (mass * 0.31 + droplets * 0.52 + burstRing * 0.35) * panel);
    }
    a *= clamp(uOpacity, 0.0, 1.0);
    if (a < 0.002) discard;
    fragColor = vec4(clamp(color, 0.0, 1.0), a);
}
