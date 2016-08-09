//package mc.alk.util.handlers;
//
//import org.bukkit.Location;
//import org.bukkit.entity.Player;
//import org.bukkit.util.Vector;
//
//public interface ParticleHandler {
//
//    public enum ParticleEffects {
//        
//        EXPLOSION_NORMAL("explode"),
//        EXPLOSION_LARGE("largeexplode"),
//        EXPLOSION_HUGE("hugeexplosion"),
//        FIREWORKS_SPARK("fireworksSpark"),
//        WATER_BUBBLE("bubble"),
//        WATER_SPLASH("splash"),
//        WATER_WAKE("wake"), // no 1.7 equivalent
//        SUSPENDED("suspended"),
//        SUSPENDED_DEPTH("depthsuspend"),
//        CRIT("crit"),
//        CRIT_MAGIC("magicCrit"),
//        SMOKE_NORMAL("smoke"),
//        SMOKE_LARGE("largesmoke"),
//        SPELL("spell"),
//        SPELL_INSTANT("instantSpell"),
//        SPELL_MOB("mobSpell"),
//        SPELL_MOB_AMBIENT("mobSpellAmbient"),
//        SPELL_WITCH("witchMagic"),
//        DRIP_WATER("dripWater"),
//        DRIP_LAVA("dripLava"),
//        VILLAGER_ANGRY("angryVillager"),
//        VILLAGER_HAPPY("happyVillager"),
//        TOWN_AURA("townaura"),
//        NOTE("note"),
//        PORTAL("portal"),
//        ENCHANTMENT_TABLE("enchantmenttable"),
//        FLAME("flame"),
//        LAVA("lava"),
//        FOOTSTEP("footstep"),
//        CLOUD("cloud"),
//        REDSTONE("reddust"),
//        SNOWBALL("snowballpoof"),
//        SNOW_SHOVEL("snowshovel"),
//        SLIME("slime"),
//        HEART("heart"),
//        BARRIER("barrier"), 
//        ITEM_CRACK("iconcrack_"),
//        BLOCK_CRACK("blockcrack_"),
//        BLOCK_DUST("blockdust"),
//        WATER_DROP("droplet"), 
//        ITEM_TAKE("take"), 
//        MOB_APPEARANCE("mobappearance"); 
//        
//        private String particleName;
//
//        ParticleEffects(String _particleName) {
//            particleName = _particleName;
//        }
//
//        public String getParticleName() {
//            return particleName;
//        }
//
//    }
//
//    void sendEffect(Player player, ParticleEffects effectType, Location location, Vector offSet, int speed, int count);
//      
//}
