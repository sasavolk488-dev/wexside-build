package project.wexside.implement.features.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import project.wexside.api.event.EventHandler;
import project.wexside.api.feature.module.Module;
import project.wexside.api.feature.module.ModuleCategory;
import project.wexside.implement.events.player.TickEvent;

import java.util.Comparator;

public class AttackAura extends Module {
    
    private final float RANGE = 3.0f; 
    private LivingEntity target;
    private long lastAttackTime = 0;

    public AttackAura() {
        super("AttackAura", "Атака с наводкой", ModuleCategory.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        target = findTarget();
        if (target == null) return;
        if (mc.player.distanceTo(target) > RANGE) return;
        if (!mc.player.canSee(target)) return;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2.0, 0);

        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        
        float newYaw = currentYaw + MathHelper.wrapDegrees(targetYaw - currentYaw) * 0.5f;
        float newPitch = currentPitch + (targetPitch - currentPitch) * 0.5f;

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(newYaw, newPitch, mc.player.isOnGround()));

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= 100) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTime = currentTime;
        }
    }

    private LivingEntity findTarget() {
        return mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(RANGE), entity -> {
            if (entity == mc.player || !entity.isAlive()) return false;
            return !entity.isInvulnerable() && entity.getHealth() > 0;
        }).stream().min(Comparator.comparingDouble(mc.player::distanceTo)).orElse(null);
    }
  }
