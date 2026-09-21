package pluginsfix.frameend.listener;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import pluginsfix.frameend.domain.EventState;
import pluginsfix.frameend.event.EndWorldEventManager;

public final class DragonDamageListener implements Listener {
    private final EndWorldEventManager eventManager;

    public DragonDamageListener(EndWorldEventManager eventManager) {
        this.eventManager = eventManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonDamage(EntityDamageByEntityEvent event) {
        if (eventManager.getState() != EventState.DRAGON_PHASE) return;
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!eventManager.getDragonPhase().isEventDragon(dragon.getUniqueId())) return;

        Player damager = null;
        if (event.getDamager() instanceof Player p) {
            damager = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            damager = p;
        } else if (event.getDamager() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            damager = p;
        } else if (event.getDamager() instanceof Firework firework && firework.getShooter() instanceof Player p) {
            damager = p;
        } else if (event.getDamager() instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player p) {
            damager = p;
        }

        if (damager != null) {
            eventManager.getDragonPhase().recordDamage(damager, event.getFinalDamage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDragonDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EnderDragon dragon && eventManager.getDragonPhase().isEventDragon(dragon.getUniqueId())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            eventManager.getDragonPhase().onDragonDeath(dragon.getLocation());
        }
    }
}
