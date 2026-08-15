package me.eldodebug.soar.ui.particle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import me.eldodebug.soar.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;

public class ParticleEngine {

	private Minecraft mc = Minecraft.getMinecraft();
	
    private final List<Particle> particles = new ArrayList<>();
    private int amount;

    private int prevWidth;
    private int prevHeight;

    public void draw(int mouseX, int mouseY) {
    	
        if(particles.isEmpty() || prevWidth != mc.displayWidth || prevHeight != mc.displayHeight) {
            particles.clear();
            // Keep menu decoration bounded on high-resolution displays. The
            // previous resolution-proportional count could exceed 300 particles
            // and made the mouse-link pass quadratic every frame.
            amount = Math.min(96, Math.max(36, (mc.displayWidth + mc.displayHeight) / 16));
            create();
        }

        prevWidth = mc.displayWidth;
        prevHeight = mc.displayHeight;

        for(final Particle particle : particles) {
        	
        	if(particle.getTimer().delay(1000 / 60)) {

                particle.fall();
                
        		particle.getTimer().reset();
        	}

            int range = 50;
            final boolean mouseOver = (mouseX >= particle.getX() - range) && 
            		(mouseY >= particle.getY() - range) && 
            		(mouseX <= particle.getX() + range) && 
            		(mouseY <= particle.getY() + range);

            if(mouseOver) {
                for (Particle connectable : particles) {
                    if (connectable == particle) continue;
                    float dx = Math.abs(connectable.getX() - particle.getX());
                    float dy = Math.abs(connectable.getY() - particle.getY());
                    if (dx < range && dy < range) {
                        particle.connect(connectable.getX(), connectable.getY());
                    }
                }
            }

            RenderUtils.drawRect(particle.getX(), particle.getY(), particle.getSize(), particle.getSize(), Color.WHITE);
        }
    }

    private void create() {
    	
        Random random = new Random();

        for(int i = 0; i < amount; i++) {
            particles.add(new Particle(random.nextInt(mc.displayWidth), random.nextInt(mc.displayHeight)));
        }
    }
}
