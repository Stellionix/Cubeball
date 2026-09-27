package me.crylonz;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.util.Vector;


import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.abs;
import static me.crylonz.CubeBall.*;

public class CubeBallListener implements Listener {
    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        rememberPlayer(event.getPlayer());
        if (match != null) match.reconnect(event.getPlayer());
    }

    @EventHandler
    public void blockChangeEvent(EntityChangeBlockEvent e) {
        if (e.getTo().equals(cubeBallBlock)) {
            if (e.getEntityType() == EntityType.FALLING_BLOCK) {
                Ball ballData = balls.values().stream().filter(ball -> e.getEntity().equals(ball.getBall())).findFirst().orElse(null);

                if (ballData != null) {
                    e.setCancelled(true);
                    String ballId = ballData.getId();

                    if (ballData.getBall() != null) {
                        Vector velocity = ballData.getBall().getVelocity();
                        double zVelocity = abs(velocity.getZ()) / listenerBounceDivisor;
                        double xVelocity = abs(velocity.getX()) / listenerBounceDivisor;
                        double maxZX = max(zVelocity, xVelocity);

                        velocity.setY(min(maxZX, listenerBounceMaxYVelocity));

                        destroyBall(ballId);
                        generateBall(ballId, e.getEntity().getLocation(), ballData.getLastVelocity());

                        ballData = balls.get(ballId);
                        ballData.getBall().setVelocity(velocity);

                        if (abs(velocity.getX() + velocity.getY() + velocity.getZ()) <= listenerStopVelocityThreshold || velocity.getY() < listenerStopMinYVelocity) {
                            ballData.getBall().setVelocity(ballData.getBall().getVelocity().zero());
                            ballData.getBall().setGravity(false);
                        } else {
                            if (abs(velocity.getX() + velocity.getY() + velocity.getZ()) > ballHitSoundThreshold) {
                                playBallHitSound(ballData.getBall().getLocation());
                            }
                        }
                    }
                }

            }
        }
    }

}
