package fr.zak.cubesedge.movement.client;

import java.awt.Color;
import java.math.BigDecimal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import fr.zak.cubesedge.IMovementClient;
import fr.zak.cubesedge.entity.EntityPlayerCustom;

public class MovementSprintClient extends IMovementClient {

	private static long lastTime = -1;
	public static double speed = 0; //actuellement en 2d, enleverles commentaires pour la vitesse en 3d
	private static double prevPosX;
	//private static double prevPosY;
	private static double prevPosZ;
	private Minecraft mc;

	@SubscribeEvent
	public void onRenderInGame(RenderGameOverlayEvent.Post event) {
		if (mc == null)
			mc = Minecraft.getMinecraft();
		calculateSpeed();
		if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
			this.drawString(
						c.fontRenderer,
						"Speed : " + speedToStr() " blocks/s",
						event.resolution.getScaledWidth() - 115, event.resolution.getScaledHeight() - 15, new Color(255, 255, 255).getRGB()
					);
		}
	}

	public void drawString(FontRenderer par1FontRenderer, String par2Str,
			int par3, int par4, int par5) {
		par1FontRenderer.drawStringWithShadow(par2Str, par3, par4, par5);
	}
	
	public static String speedToStr()
	{
		if (speed == 0)
			return "0";
		return round(speed, 2)
	}
	
	private void calculateSpeed()
	{
		long now = System.currentTimeMillis();
		long time = lastTime < 0 ? 0 : now - lastTime;
		
		speed = 0;
		if (prevPosX != mc.thePlayer.posX || prevPosZ != mc.thePlayer.posZ /*|| prevPosY != mc.thePlayer.posY*/ )
		{
			double vx, vz /*, vy*/;
			double dist/*, dist2*/;
			vx = Math.max(prevPosX, mc.thePlayer.posX) - Math.min(prevPosX, mc.thePlayer.posX));
			vz = Math.max(prevPosZ, mc.thePlayer.posZ) - Math.min(prevPosZ, mc.thePlayer.posZ));
			//vy = Math.max(prevPosY, mc.thePlayer.posY) - Math.min(prevPosY, mc.thePlayer.posY));
			dist = Math.sqrt(vx * vx + vz * vz);
			//dist2 = Math.sqrt(vy * vy + vz * vz);
			//dist = Math.sqrt(dist * dist + dist2 * dist2);
			speed = dist / time / 1000;
		}
		lastTime = now;
		prevPosX = mc.thePlayer.posX;
		//prevPosY = mc.thePlayer.posY;
		prevPosZ = mc.thePlayer.posZ;
	}

	public static double round(double d, int decimalPlace) {
		if (places < 0)
			throw new IllegalArgumentException();

    		BigDecimal bd = new BigDecimal(value);
    		bd = bd.setScale(places, RoundingMode.HALF_UP);
    		return bd.doubleValue();
	}

	@Override
	public void renderTick(EntityPlayerCustom playerCustom) {
		float f1 = playerCustom.slow ? 0.03F / 4F : 0.03F;
		if (!playerCustom.animLeft && !playerCustom.animRight) {
			if (Minecraft.getMinecraft().thePlayer.isSprinting()) {
				if (playerCustom.tickRunningRight < 0.5F
						&& !playerCustom.animRunnig) {
					playerCustom.tickRunningRight += f1;
				}
				if (playerCustom.tickRunningRight >= 0.5F
						&& !playerCustom.animRunnig) {
					playerCustom.animRunnig = true;
				}
			} else {
				playerCustom.animRunnig = false;
				playerCustom.tickRunningLeft = 0;
				playerCustom.tickRunningRight = 0;
			}
			if (playerCustom.animRunnig) {
				if (playerCustom.tickRunningLeft < 0.5F
						&& !playerCustom.backLeft) {
					playerCustom.tickRunningLeft += f1;
				}
				if (playerCustom.tickRunningLeft >= 0.5F
						&& !playerCustom.backLeft) {
					playerCustom.backLeft = true;
				}
				if (playerCustom.tickRunningLeft > 0 && playerCustom.backLeft) {
					playerCustom.tickRunningLeft -= f1;
				}
				if (playerCustom.tickRunningLeft <= 0 && playerCustom.backLeft) {
					playerCustom.backLeft = false;
				}
				if (playerCustom.tickRunningRight > 0
						&& !playerCustom.backRight) {
					playerCustom.tickRunningRight -= f1;
				}
				if (playerCustom.tickRunningRight <= 0
						&& !playerCustom.backRight) {
					playerCustom.backRight = true;
				}
				if (playerCustom.tickRunningRight < 0.5F
						&& playerCustom.backRight) {
					playerCustom.tickRunningRight += f1;
				}
				if (playerCustom.tickRunningRight >= 0.5F
						&& playerCustom.backRight) {
					playerCustom.backRight = false;
				}
			}
		} else if (playerCustom.animLeft || playerCustom.animRight) {
			playerCustom.tickRunningLeft = 0;
			playerCustom.tickRunningRight = 0;
		}

	}

	@Override
	public String getName() {
		return "Sprint Client";
	}

	@Override
	public void controlClient(EntityPlayerCustom playerCustom,
			EntityPlayer player) {
		
	}

}
