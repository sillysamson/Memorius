package com.memorius.app;

import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;

// Android 13+ only: the glass "lens". Near the rim of a capsule the backdrop is bent inwards,
// and the colour channels are pulled slightly apart (chromatic aberration).
final class Glass33 {
    private Glass33() {
    }

    private static final String AGSL = ""
            + "uniform shader content;\n"
            + "uniform float4 rect;\n"
            + "uniform float radius;\n"
            + "uniform float rim;\n"
            + "uniform float amount;\n"
            + "uniform float chroma;\n"
            + "half4 main(float2 fc) {\n"
            + "  float2 ctr = float2((rect.x + rect.z) * 0.5, (rect.y + rect.w) * 0.5);\n"
            + "  float2 hs = float2((rect.z - rect.x) * 0.5, (rect.w - rect.y) * 0.5);\n"
            + "  float2 p = fc - ctr;\n"
            + "  float2 a = float2(max(hs.x - radius, 0.0), max(hs.y - radius, 0.0));\n"
            + "  float2 q = p - clamp(p, -a, a);\n"
            + "  float ql = length(q);\n"
            + "  float d = ql - radius;\n"
            + "  if (d > 0.0) { return content.eval(fc); }\n"
            + "  float t = clamp(1.0 + d / max(rim, 0.001), 0.0, 1.0);\n"
            + "  float2 n = ql > 0.001 ? q / ql : float2(0.0, 0.0);\n"
            + "  float2 s = clamp(fc - n * (amount * t * t), rect.xy, rect.zw);\n"
            + "  float c = chroma * t * t;\n"
            + "  half4 base = content.eval(s);\n"
            + "  if (chroma < 0.01) { return base; }\n"
            + "  half4 r2 = content.eval(s - n * c);\n"
            + "  half4 b2 = content.eval(s + n * c);\n"
            + "  return half4(r2.r, base.g, b2.b, base.a);\n"
            + "}\n";

    // null when the shader cannot be built on this device: callers then simply skip the lens
    static Object makeShader() {
        try {
            return new RuntimeShader(AGSL);
        } catch (Throwable t) {
            return null;
        }
    }

    static Object lens(Object shader, float l, float t, float r, float b, float radius, float rim, float amount, float chroma) {
        if (shader == null) return null;
        try {
            RuntimeShader s = (RuntimeShader) shader;
            s.setFloatUniform("rect", l, t, r, b);
            s.setFloatUniform("radius", radius);
            s.setFloatUniform("rim", rim);
            s.setFloatUniform("amount", amount);
            s.setFloatUniform("chroma", chroma);
            return RenderEffect.createRuntimeShaderEffect(s, "content");
        } catch (Throwable t2) {
            return null;
        }
    }
}
