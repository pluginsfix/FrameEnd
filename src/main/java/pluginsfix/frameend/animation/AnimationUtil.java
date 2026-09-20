package pluginsfix.frameend.animation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import pluginsfix.frameend.domain.AnchorRarity;

public final class AnimationUtil {
    private AnimationUtil() {}

    public static void playAnchorHitAnimation(Location center, AnchorRarity rarity, int hitsLeft) {
        World world = center.getWorld();
        if (world == null) return;

        BlockData blockData = (rarity == AnchorRarity.SECRET_RIFT)
                ? Material.CRYING_OBSIDIAN.createBlockData()
                : Material.RESPAWN_ANCHOR.createBlockData();

        world.spawnParticle(Particle.BLOCK, center, 15, 0.35, 0.35, 0.35, 0.1, blockData);
        world.spawnParticle(Particle.CRIT, center, 10, 0.3, 0.3, 0.3, 0.15);

        if (rarity == AnchorRarity.SECRET_RIFT) {
            world.spawnParticle(Particle.DRAGON_BREATH, center, 8, 0.25, 0.25, 0.25, 0.05);
            world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.5f);
        } else {
            world.spawnParticle(Particle.PORTAL, center, 12, 0.3, 0.3, 0.3, 0.1);
            world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.3f);
        }
    }

    public static void playAnchorBreakAnimation(Location center, AnchorRarity rarity) {
        World world = center.getWorld();
        if (world == null) return;

        BlockData blockData = (rarity == AnchorRarity.SECRET_RIFT)
                ? Material.CRYING_OBSIDIAN.createBlockData()
                : Material.RESPAWN_ANCHOR.createBlockData();

        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.BLOCK, center, 40, 0.5, 0.5, 0.5, 0.2, blockData);

        if (rarity == AnchorRarity.SECRET_RIFT) {
            world.spawnParticle(Particle.SONIC_BOOM, center, 1);
            world.spawnParticle(Particle.DRAGON_BREATH, center, 35, 0.6, 0.6, 0.6, 0.1);
            world.spawnParticle(Particle.END_ROD, center, 25, 0.5, 0.8, 0.5, 0.15);
            world.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.2f);
        } else {
            world.spawnParticle(Particle.FLASH, center, 1);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 20, 0.4, 0.4, 0.4, 0.08);
            world.spawnParticle(Particle.SWEEP_ATTACK, center, 2, 0.3, 0.3, 0.3, 0.05);
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
            world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.8f);
        }
    }

    public static void playEggHitAnimation(Location center, int hitsLeft, int maxHits) {
        World world = center.getWorld();
        if (world == null) return;

        BlockData eggData = Material.DRAGON_EGG.createBlockData();
        world.spawnParticle(Particle.BLOCK, center, 18, 0.3, 0.3, 0.3, 0.1, eggData);
        world.spawnParticle(Particle.DRAGON_BREATH, center, 8, 0.2, 0.2, 0.2, 0.03);
        world.spawnParticle(Particle.ENCHANT, center, 12, 0.3, 0.4, 0.3, 0.2);
        world.spawnParticle(Particle.CRIT, center, 8, 0.2, 0.2, 0.2, 0.1);

        world.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 1.4f);
        world.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 0.6f, 1.5f);
    }

    public static void playEggCaptureAnimation(Location center, Player winner) {
        World world = center.getWorld();
        if (world == null) return;

        world.strikeLightningEffect(center);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 60, 0.6, 1.0, 0.6, 0.3);
        world.spawnParticle(Particle.FLASH, center, 2);
        world.spawnParticle(Particle.DRAGON_BREATH, center, 40, 0.8, 0.8, 0.8, 0.1);

        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        world.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    public static void playPlacedEggBreakAnimation(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        BlockData eggData = Material.DRAGON_EGG.createBlockData();
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.BLOCK, center, 35, 0.4, 0.4, 0.4, 0.15, eggData);
        world.spawnParticle(Particle.DRAGON_BREATH, center, 20, 0.5, 0.5, 0.5, 0.05);
        world.playSound(center, Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
        world.playSound(center, Sound.ENTITY_ENDERMAN_DEATH, 0.8f, 0.8f);
    }
}
