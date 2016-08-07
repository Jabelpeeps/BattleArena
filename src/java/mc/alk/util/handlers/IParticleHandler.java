package mc.alk.util.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public interface IParticleHandler {

    public enum ParticleEffects {
        
        EXPLOSION_NORMAL("explode"),
        EXPLOSION_LARGE("largeexplode"),
        EXPLOSION_HUGE("hugeexplosion"),
        FIREWORKS_SPARK("fireworksSpark"),
        WATER_BUBBLE("bubble"),
        WATER_SPLASH("splash"),
        WATER_WAKE("wake"), // no 1.7 equivalent
        SUSPENDED("suspended"),
        SUSPENDED_DEPTH("depthsuspend"),
        CRIT("crit"),
        CRIT_MAGIC("magicCrit"),
        SMOKE_NORMAL("smoke"),
        SMOKE_LARGE("largesmoke"),
        SPELL("spell"),
        SPELL_INSTANT("instantSpell"),
        SPELL_MOB("mobSpell"),
        SPELL_MOB_AMBIENT("mobSpellAmbient"),
        SPELL_WITCH("witchMagic"),
        DRIP_WATER("dripWater"),
        DRIP_LAVA("dripLava"),
        VILLAGER_ANGRY("angryVillager"),
        VILLAGER_HAPPY("happyVillager"),
        TOWN_AURA("townaura"),
        NOTE("note"),
        PORTAL("portal"),
        ENCHANTMENT_TABLE("enchantmenttable"),
        FLAME("flame"),
        LAVA("lava"),
        FOOTSTEP("footstep"),
        CLOUD("cloud"),
        REDSTONE("reddust"),
        SNOWBALL("snowballpoof"),
        SNOW_SHOVEL("snowshovel"),
        SLIME("slime"),
        HEART("heart"),
        BARRIER("barrier"), // no 1.7 equivalent
        ITEM_CRACK("iconcrack_"),
        BLOCK_CRACK("blockcrack_"),
        BLOCK_DUST("blockdust"),
        WATER_DROP("droplet"), // no 1.7 equivalent
        ITEM_TAKE("take"), // no 1.7 equivalent
        MOB_APPEARANCE("mobappearance"), // no 1.7 equivalent
        
        /** @Deprecated in favor of EXPLOSION_NORMAL */
        @Deprecated EXPLODE("explode"),
        /** @Deprecated in favor of EXPLOSION_HUGE */
        @Deprecated HUGE_EXPLOSION("hugeexplosion"), 
        /** @Deprecated in favor of EXPLOSION_LARGE */
        @Deprecated LARGE_EXPLODE("largeexplode"), 
        /** @Deprecated in favor of WATER_BUBBLE */
        @Deprecated BUBBLE("bubble"), 
        /** @Deprecated in favor of SUSPENDED */
        @Deprecated DEPTH_SUSPEND("depthsuspend"), 
        /** @Deprecated in favor of CRIT_MAGIC */
        @Deprecated MAGIC_CRIT("magicCrit"), 
        /** @Deprecated in favor of SPELL_MOB */
        @Deprecated MOB_SPELL("mobSpell"), 
        /** @Deprecated in favor of SPELL_MOB_AMBIENT */
        @Deprecated MOB_SPELL_AMBIENT("mobSpellAmbient"), 
        /** @Deprecated in favor of SPELL_INSTANT */
        @Deprecated INSTANT_SPELL("instantSpell"), 
        /** @Deprecated in favor of SPELL_WITCH */
        @Deprecated WITCH_MAGIC("witchMagic"), 
        /** @Deprecated in favor of WATER_SPLASH */
        @Deprecated SPLASH("splash"), 
        /** @Deprecated in favor of SMOKE_LARGE */
        @Deprecated LARGE_SMOKE("largesmoke"), 
        /** @Deprecated in favor of REDSTONE */
        @Deprecated RED_DUST("reddust"), 
        /** @Deprecated in favor of SNOWBALL */
        @Deprecated SNOWBALL_POOF("snowballpoof"), 
        /** @Deprecated in favor of VILLAGER_ANGRY */
        @Deprecated ANGRY_VILLAGER("angryVillager"), 
        /** @Deprecated in favor of VILLAGER_HAPPY */
        @Deprecated HAPPY_VILLAGER("happyVillager"), 
        /** @Deprecated in favor of ITEM_CRACK */
        @Deprecated ICONCRACK("iconcrack_");

        private String particleName;

        ParticleEffects(String particleName) {
            this.particleName = particleName;
        }

        public String getParticleName() {
            return this.particleName;
        }

    }

    void sendEffect(Player player, ParticleEffects effectType, Location location, Vector offSet, int speed, int count);
    
    public static final IParticleHandler NULL_HANDLER = new IParticleHandler() {

        @Override
        public void sendEffect(Player player, ParticleEffects effectType, Location location, Vector offSet, int speed, int count) {
            // do nothing
        }
    };
    
}
