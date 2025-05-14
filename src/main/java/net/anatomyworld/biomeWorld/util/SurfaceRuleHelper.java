// ============================================================================
//  SurfaceRuleHelper.java
//  package: net.anatomyworld.biomeWorld.util
// ============================================================================

package net.anatomyworld.biomeWorld.util;

import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

/**
 * Two tiny overrides spliced <em>ahead</em> of Mojang’s vanilla rule tree:
 * <ol>
 *   <li>fill every water column at or above {@code seaLevel} with WATER</li>
 *   <li>make sure everything one block below becomes STONE (no floating water)</li>
 * </ol>
 * The remainder of {@link SurfaceRuleData#overworld()} is left untouched.
 */
public final class SurfaceRuleHelper {

    /** Build a RuleSource suitable for {@code seaLevel}. */
    public static SurfaceRules.RuleSource overworld(int seaLevel) {

        /* 1 ─ Put WATER wherever we are in a fluid column and <= seaLevel */
        SurfaceRules.RuleSource injectWater =
                SurfaceRules.ifTrue(
                        SurfaceRules.waterBlockCheck(-1, 0),                 // inside fluid?
                        SurfaceRules.ifTrue(
                                SurfaceRules.yBlockCheck(
                                        VerticalAnchor.absolute(seaLevel), 0),
                                SurfaceRules.state(Blocks.WATER.defaultBlockState())
                        )
                );

        /* 2 ─ Force STONE below water‑surface to avoid “air pockets” */
        SurfaceRules.RuleSource forceStone =
                SurfaceRules.ifTrue(
                        SurfaceRules.yBlockCheck(
                                VerticalAnchor.absolute(seaLevel - 1), 0),
                        SurfaceRules.state(Blocks.STONE.defaultBlockState())
                );

        /* 3 ─ Our two overrides first, then vanilla logic */
        return SurfaceRules.sequence(
                injectWater,
                forceStone,
                SurfaceRuleData.overworld()          // Mojang’s full rule tree
        );
    }

    private SurfaceRuleHelper() {}
}
