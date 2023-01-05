package com.donkingliang.consecutivescroller;

import android.view.animation.Interpolator;

/**
 * OverScrollInterpolator
 * Copy from scwang: SmartUtil.
 */
public class OverScrollInterpolator implements Interpolator {

    public static int INTERPOLATOR_VISCOUS_FLUID = 0;
    public static int INTERPOLATOR_DECELERATE = 1;

    private int type;

    public OverScrollInterpolator(int type) {
        this.type = type;
    }

    /**
     * Controls the viscous fluid effect (how much of it).
     */
    private static final float VISCOUS_FLUID_SCALE = 8.0f;

    private static final float VISCOUS_FLUID_NORMALIZE;
    private static final float VISCOUS_FLUID_OFFSET;

    static {
        // must be set to 1.0 (used in viscousFluid())
        VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f);
        // account for very small floating-point error
        VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f);
    }

    private static float viscousFluid(float x) {
        x *= VISCOUS_FLUID_SCALE;
        if (x < 1.0f) {
            x -= (1.0f - (float) Math.exp(-x));
        } else {
            float start = 0.36787944117f;   // 1/e == exp(-1)
            x = 1.0f - (float) Math.exp(1.0f - x);
            x = start + x * (1.0f - start);
        }
        return x;
    }

    @Override
    public float getInterpolation(float input) {
        if (type == INTERPOLATOR_DECELERATE) {
            return (1.0f - (1.0f - input) * (1.0f - input));
        }
        final float interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluid(input);
        if (interpolated > 0) {
            return interpolated + VISCOUS_FLUID_OFFSET;
        }
        return interpolated;
    }

}
