package project.papus.implement.features.modules.player;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import project.papus.api.event.EventHandler;
import project.papus.api.feature.module.Module;
import project.papus.api.feature.module.ModuleCategory;
import project.papus.implement.events.player.TickEvent;

import java.util.Comparator;

public class KillAura extends Module {

    private LivingEntity currentTarget;
    private long lastAttackTime = 0;
    
    // Бинд (по умолчанию R - код 82)
    private int bindKey = 82; 

    public KillAura() {
        super("KillAura", "Автоатака с критами и наводкой", ModuleCategory.COMBAT);
    }

    @Override
    public void deactivate() {
        currentTarget = null;
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        // 1. Ищем цель
        currentTarget = findTarget();
        if (currentTarget == null) return;

        // 2. Проверка дистанции
        if (mc.player.distanceTo(currentTarget) > 3.2f) return;

        // 3. ПРОВЕРКА НАВОДКИ (Сквозь стены не бьет)
        if (!mc.player.canSee(currentTarget)) return;

        // 4. Поворот камеры
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = currentTarget.getPos().add(0, currentTarget.getHeight() / 2.0, 0);

        float targetYaw = getTargetYaw(eyePos, targetPos);
        float targetPitch = getTargetPitch(eyePos, targetPos);

        mc.player.setYaw(smoothRotation(mc.player.getYaw(), targetYaw, 45f));
        mc.player.setPitch(smoothRotation(mc.player.getPitch(), targetPitch, 45f));

        // 5. Проверка, смотрим ли мы прямо на цель
        if (!isLookingAt(currentTarget)) return;

        // 6. Проверка на крит
        if (mc.player.fallDistance <= 0.0F) return;

        // 7. Логика удара (10 CPS)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= 100) {
            mc.interactionManager.attackEntity(mc.player, currentTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTime = currentTime;
        }
    }

    private LivingEntity findTarget() {
        return mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(3.5), entity -> {
            if (entity == mc.player || !entity.isAlive()) return false;
            return !entity.isInvulnerable() && entity.getHealth() > 0;
        }).stream().min(Comparator.comparingDouble(mc.player::distanceTo)).orElse(null);
    }

    private boolean isLookingAt(LivingEntity target) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        Vec3d targetVec = target.getPos().add(0, target.getHeight() / 2.0, 0).subtract(eyePos).normalize();
        return lookVec.dotProduct(targetVec) > 0.95; 
    }

    private float smoothRotation(float current, float target, float speed) {
        float delta = MathHelper.wrapDegrees(target - current);
        if (delta > speed) delta = speed;
        if (delta < -speed) delta = -speed;
        return current + delta;
    }

    private float getTargetYaw(Vec3d eyePos, Vec3d targetPos) {
        double x = targetPos.x - eyePos.x;
        double z = targetPos.z - eyePos.z;
        return (float) (Math.toDegrees(Math.atan2(z, x)) - 90.0F);
    }

    private float getTargetPitch(Vec3d eyePos, Vec3d targetPos) {
        double x = targetPos.x - eyePos.x;
        double y = targetPos.y - eyePos.y;
        double z = targetPos.z - eyePos.z;
        double dist = Math.sqrt(x * x + z * z);
        return (float) (-Math.toDegrees(Math.atan2(y, dist)));
    }
}
