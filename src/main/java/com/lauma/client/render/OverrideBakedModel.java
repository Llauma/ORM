package com.lauma.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OverrideBakedModel implements BakedModel {
    private static final int VERTEX_STRIDE = 8;
    private static final int VERTEX_COUNT = 4;
    private static final int UV_OFFSET = 4;

    private final BakedModel inner;
    private final Sprite replacement;

    public OverrideBakedModel(BakedModel inner, Sprite replacement) {
        this.inner = inner;
        this.replacement = replacement;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        List<BakedQuad> originals = inner.getQuads(state, face, random);
        if (originals.isEmpty()) return originals;
        List<BakedQuad> result = new ArrayList<>(originals.size());
        for (BakedQuad q : originals) result.add(remap(q));
        return result;
    }

    private BakedQuad remap(BakedQuad src) {
        Sprite original = src.getSprite();
        if (original == replacement) return src;
        int[] data = src.getVertexData().clone();
        if (data.length < VERTEX_STRIDE * VERTEX_COUNT) return src;
        float origU = original.getMinU();
        float origV = original.getMinV();
        float origW = original.getMaxU() - origU;
        float origH = original.getMaxV() - origV;
        float newU = replacement.getMinU();
        float newV = replacement.getMinV();
        float newW = replacement.getMaxU() - newU;
        float newH = replacement.getMaxV() - newV;
        if (origW == 0f || origH == 0f) return src;
        for (int v = 0; v < VERTEX_COUNT; v++) {
            int uIdx = v * VERTEX_STRIDE + UV_OFFSET;
            int vIdx = uIdx + 1;
            float u = Float.intBitsToFloat(data[uIdx]);
            float vCoord = Float.intBitsToFloat(data[vIdx]);
            float relU = (u - origU) / origW;
            float relV = (vCoord - origV) / origH;
            data[uIdx] = Float.floatToRawIntBits(newU + relU * newW);
            data[vIdx] = Float.floatToRawIntBits(newV + relV * newH);
        }
        return new BakedQuad(data, src.getTintIndex(), src.getFace(), replacement, src.hasShade(), src.getLightEmission());
    }

    @Override public boolean useAmbientOcclusion() { return inner.useAmbientOcclusion(); }
    @Override public boolean hasDepth() { return inner.hasDepth(); }
    @Override public boolean isSideLit() { return inner.isSideLit(); }
    @Override public Sprite getParticleSprite() { return replacement; }
    @Override public ModelTransformation getTransformation() { return inner.getTransformation(); }
}