package com.modiprincekr.thedialer.liquidglass.effects

import android.graphics.RenderEffect
import android.os.Build
import com.modiprincekr.thedialer.liquidglass.BackdropEffectScope

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createChainEffect(effect, currentEffect)
        } else {
            effect
        }
}

fun BackdropEffectScope.effect(effect: androidx.compose.ui.graphics.RenderEffect) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    effect(effect.asAndroidRenderEffect())
}
