package fr.zak.cubesedge.ticks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Timer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import fr.zak.cubesedge.Util;
import fr.zak.cubesedge.entity.EntityPlayerCustom;
import fr.zak.cubesedge.event.KeyHandler;
import fr.zak.cubesedge.event.SpeedEvent;
import fr.zak.cubesedge.packet.PacketPlayer;
import fr.zak.cubesedge.renderer.EntityRendererCustom;

public class ClientTickHandler {

	Minecraft minecraft = Minecraft.getMinecraft();
	private boolean wasSneaking;
	private EntityRenderer renderer, prevRenderer;

	@SubscribeEvent
	public void playerUpdate(TickEvent.PlayerTickEvent event){
		if(event.phase == TickEvent.Phase.END){
			if(((EntityPlayerCustom)event.player.getExtendedProperties("Player Custom")).isGrabbing && !event.player.capabilities.isCreativeMode){
				PotionEffect potioneffect = event.player.getActivePotionEffect(Potion.jump);
				float f1 = potioneffect != null ? (float)(potioneffect.getAmplifier() + 1) : 0.0F;
				int i = MathHelper.ceiling_float_int(event.player.fallDistance - 3.0F - f1);
				if(i > 0){
					event.player.playSound(i > 4 ? "game.neutral.hurt.fall.big" : "game.neutral.hurt.fall.small", 1.0F, 1.0F);
					damageEntity(EntityLivingBase.class, event.player, DamageSource.fall, (float)i);
				}
				event.player.fallDistance = 0;
			}
			if(event.side == Side.CLIENT){
				sprintAnimation(event.player);
				ralenti(event.player);

				int heading = MathHelper.floor_double((double)(event.player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
				if(!event.player.capabilities.isFlying){
					turn(event.player, heading);
					if(!((EntityPlayerCustom)event.player.getExtendedProperties("Player Custom")).isSneaking){
						roll(heading, event.player);
						grab(heading, event.player);
						if(event.player instanceof EntityPlayerSP){
							wallJumping(heading, (EntityPlayerSP)event.player);
						}
						jump(heading, event.player);
					}
					sneak(event.player);
				}

				if(event.player.onGround){
					((EntityPlayerCustom)event.player.getExtendedProperties("Player Custom")).wallJump = false;
					((EntityPlayerCustom)event.player.getExtendedProperties("Player Custom")).animRight = false;
					((EntityPlayerCustom)event.player.getExtendedProperties("Player Custom")).animLeft = false;
					((EntityPlayerCustom)event.player.getExtendedProperties("Player Custom")).isJumpingOnWall = false;
				}
				if(event.player.capabilities.isFlying){
					((EntityPlayerCustom)event.player.getExtendedProperties("Player Custom")).animRight = false;
					((EntityPlayerCustom)event.player.getExtendedProperties("Player Custom")).animLeft = false;
				}
			}
		}
	}
	
	@SubscribeEvent
	public void tick(TickEvent.RenderTickEvent event) {
		if(event.phase == TickEvent.Phase.START && minecraft.theWorld != null){
			if(((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).isSneaking || ((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).isRolling || (minecraft.theWorld.getBlock(MathHelper.floor_double(minecraft.thePlayer.posX), MathHelper.floor_double(minecraft.thePlayer.posY), MathHelper.floor_double(minecraft.thePlayer.posZ)).isNormalCube() && (((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).wasSliding || ((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).wasRolling))){
				int x1 = MathHelper.floor_double(minecraft.thePlayer.posX);
				int y1 = MathHelper.floor_double(minecraft.thePlayer.posY);
				int z1 = MathHelper.floor_double(minecraft.thePlayer.posZ);
				ExtendedBlockStorage ebs = ((ExtendedBlockStorage[])ObfuscationReflectionHelper.getPrivateValue(Chunk.class, minecraft.thePlayer.worldObj.getChunkFromBlockCoords(x1, z1), 2))[y1 >> 4];
				if(ebs.getExtSkylightValue((x1 & 15), y1 & 15, (z1 & 15)) == 0){
					ebs.setExtSkylightValue((x1 & 15), y1 & 15, (z1 & 15), ((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).lastLightValue);
				}
				((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).lastLightValue = (byte) ebs.getExtSkylightValue((x1 & 15), y1 & 15, (z1 & 15));
				if (renderer == null) {
					renderer = new EntityRendererCustom(minecraft);
				}
				if (minecraft.entityRenderer != renderer) {
					// be sure to store the previous renderer
					prevRenderer = minecraft.entityRenderer;
					minecraft.entityRenderer = renderer;
				}
				Util.forceSetSize(Entity.class, minecraft.thePlayer, 0.6F, 0.6F);
			} else if (prevRenderer != null && minecraft.entityRenderer != prevRenderer && !minecraft.thePlayer.isSneaking() && minecraft.theWorld.getBlock(MathHelper.floor_double(minecraft.thePlayer.posX), MathHelper.floor_double(minecraft.thePlayer.posY), MathHelper.floor_double(minecraft.thePlayer.posZ)) == Blocks.air) {
				// reset the renderer
				minecraft.entityRenderer = prevRenderer;
				Util.forceSetSize(Entity.class, minecraft.thePlayer, 0.6F, 1.8F);
				((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).sneakTime = 0;
				((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).wasSliding = false;
				((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).wasRolling = false;
			}
			if(!((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).wasSliding){
				((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).wasSliding = ((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).isSneaking;
			}
			if(!((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).wasRolling){
				((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).wasRolling = ((EntityPlayerCustom)minecraft.thePlayer.getExtendedProperties("Player Custom")).isRolling;
			}
		}
	}

	private void turn(EntityPlayer player, int heading) {
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurning){
			if(!((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isOnWall){
				float yaw = MathHelper.wrapAngleTo180_float(player.rotationYaw);
				player.rotationYaw = yaw - 180;
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurning = false;
				if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isJumping != 0 && ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).turningTime == 0 && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurningOnWall){
					((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurningOnWall = true;
				}
			}
			else{
				if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).turningTime == 0 && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurningOnWall){
					float yaw = MathHelper.wrapAngleTo180_float(player.rotationYaw);
					if((player.worldObj.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 0) || (player.worldObj.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) + 1).isNormalCube() && heading == 1) || (player.worldObj.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 2) || (player.worldObj.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) - 1).isNormalCube() && heading == 3)){
						player.rotationYaw = yaw - 90;
					}
					else if((player.worldObj.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 0) || (player.worldObj.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) - 1).isNormalCube() && heading == 1) || (player.worldObj.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 2) || (player.worldObj.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) + 1).isNormalCube() && heading == 3)){
						player.rotationYaw = yaw + 90;
					}
					((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurningOnWall = true;
				}
			}
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurningOnWall){
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).turningTime < 10){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).turningTime++;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).turningTime == 10){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurning = false;
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).turningTime = 0;
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurningOnWall = false;
			}
			if((player.worldObj.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) - 1).isNormalCube() && heading == 0) || (player.worldObj.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 1) || (player.worldObj.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) + 1).isNormalCube() && heading == 2) || (player.worldObj.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ )).isNormalCube() && heading == 3)) {
				player.motionZ *= 0.95D;
				player.motionX *= 0.95D;
				player.motionY *= 0.75D;
				if(player instanceof EntityPlayerSP) {
					if(((EntityPlayerSP)player).movementInput.jump && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isJumpingOnWall){
						if(heading == 0){
							player.motionZ = 0.7F;
						}
						if(heading == 1){
							player.motionX = -0.7F;
						}
						if(heading == 2){
							player.motionZ = -0.7F;
						}
						if(heading == 3){
							player.motionX = 0.7F;
						}
						player.motionY = 0.7D;
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isJumpingOnWall = true;
					}
				}
			}
		}
	}

	private void sneak(EntityPlayer player) {
		if(!player.isSprinting() && ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wasSprinting){
			if(player.isSneaking() && player.onGround && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isRolling){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isSneaking = true;
				Util.channel.sendToServer(new PacketPlayer.CPacketPlayerSneak(true));
			}
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isSneaking && player.isSneaking()){
			if(player.isCollidedHorizontally){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).sneakTime = 16;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).sneakTime < 16 && player.onGround){
				player.motionX *= (0.98F * 0.91F) + 1;
				player.motionZ *= (0.98F * 0.91F) + 1;
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).sneakTime++;
			}
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isSneaking && !player.isSneaking()){
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isSneaking = false;
			Util.channel.sendToServer(new PacketPlayer.CPacketPlayerSneak(false));
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).sneakTime = 0;
		}
		((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wasSprinting = player.isSprinting();
	}

	private void sprintAnimation(EntityPlayer player){
		if(player.isSprinting()){
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight < 0.5F && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).beginingRunning){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight += (SpeedEvent.speed - 1) * 0.05;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight >= 0.5F && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).beginingRunning){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).beginingRunning = true;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).beginingRunning){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRunnig = true;
			}
		}
		else{
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRunnig = false;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).beginingRunning = false;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningLeft = 0;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight = 0;
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRunnig){
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningLeft < 0.5F && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backLeft){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningLeft += (SpeedEvent.speed - 1) * 0.2;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningLeft >= 0.5F && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backLeft){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backLeft = true;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningLeft > 0 && ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backLeft){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningLeft -= (SpeedEvent.speed - 1) * 0.2;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningLeft <= 0 && ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backLeft){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backLeft = false;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight > 0 && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backRight){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight -= (SpeedEvent.speed - 1) * 0.2;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight <= 0 && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backRight){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backRight = true;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight < 0.5F && ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backRight){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight += (SpeedEvent.speed - 1) * 0.2;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).tickRunningRight >= 0.5F && ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backRight){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).backRight = false;
			}
		}
	}

	private void ralenti(EntityPlayer player){
		if(KeyHandler.keyPressedRalenti && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).ralenti){
			ObfuscationReflectionHelper.setPrivateValue(Timer.class, ((Timer)ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, minecraft, 15)), 5F, 0);
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).ralenti = true;
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).ralenti){
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).temps++;
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).temps == 25 && ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).ralenti){
			ObfuscationReflectionHelper.setPrivateValue(Timer.class, ((Timer)ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, minecraft, 15)), 20F, 0);
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).temps = 0;
			minecraft.gameSettings.mouseSensitivity = KeyHandler.defaultSensitivity;
			KeyHandler.keyPressedRalenti = false;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).ralenti = false;
		}
	}

	private void roll(int heading, EntityPlayer player){
		if(player.fallDistance > 3.0F && player.fallDistance < 15F){
			if (player.worldObj.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) - 3, MathHelper.floor_double(player.posZ)).isNormalCube() || player.worldObj.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) - 4, MathHelper.floor_double(player.posZ)).isNormalCube()){
				if(player.isSneaking()){
					((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).prevRolling = true;
				}
			}
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).prevRolling && player.onGround){
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isRolling = true;
			Util.channel.sendToServer(new PacketPlayer.CPacketPlayerRoll(true));
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isRolling){
			KeyBinding.setKeyBindState(minecraft.gameSettings.keyBindForward.getKeyCode(), true);
			KeyBinding.setKeyBindState(minecraft.gameSettings.keyBindLeft.getKeyCode(), false);
			KeyBinding.setKeyBindState(minecraft.gameSettings.keyBindRight.getKeyCode(), false);
			KeyBinding.setKeyBindState(minecraft.gameSettings.keyBindBack.getKeyCode(), false);
			KeyBinding.setKeyBindState(minecraft.gameSettings.keyBindSneak.getKeyCode(), false);
			player.motionZ *= 0.3;
			player.motionX *= 0.3;
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).rollingTime < 27){
				float f2 = player.rotationPitch;
				player.rotationPitch = (float)((double)player.rotationPitch + 10);
				player.prevRotationPitch += player.rotationPitch - f2;
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).rollingTime++;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).rollingTime == 27){
				player.rotationPitch = 0F;
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).rollingTime = 0;
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).prevRolling = false;
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isRolling = false;
				Util.channel.sendToServer(new PacketPlayer.CPacketPlayerRoll(false));
				KeyBinding.setKeyBindState(minecraft.gameSettings.keyBindForward.getKeyCode(), false);
			}
		}
	}

	private void wallJumping(int heading, EntityPlayerSP player){
		if(!player.onGround && player.motionY <= 0){
			if((minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() || minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY) - 1, MathHelper.floor_double(player.posZ)).isNormalCube()) && (( heading == 0) || (heading == 2))){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isOnWall = true;
				if(player.moveForward > 0){
					if(player.movementInput.jump && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wallJump){
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRight = false;
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animLeft = false;
						if(heading == 0){
							player.motionZ = 0.7D;
							player.motionX = -0.2D;
						}
						if(heading == 2){
							player.motionZ = -0.7D;
							player.motionX = -0.2D;
						}
						player.motionY = 0.41999998688697815D;
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wallJump = true;
					}
					if(!player.movementInput.jump){
						if(heading == 2){
							((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRight = true;
						}
						if(heading == 0){
							((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animLeft = true;

						}
					}
				}
			}
			else if((minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() || minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY) - 1, MathHelper.floor_double(player.posZ)).isNormalCube()) && ((heading == 0) || (heading == 2))){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isOnWall = true;
				if(player.moveForward > 0){
					if(player.movementInput.jump && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wallJump){
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRight = false;
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animLeft = false;
						if(heading == 0){
							player.motionZ = 0.7D;
							player.motionX = 0.2D;
						}
						if(heading == 2){
							player.motionZ = -0.7D;
							player.motionX = 0.2D;
						}
						player.motionY = 0.41999998688697815D;
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wallJump = true;
					}
					if(!player.movementInput.jump){
						if(heading == 2){
							((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animLeft = true;
						}
						if(heading == 0){
							((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRight = true;
						}
					}
				}
			}
			else if((minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) + 1).isNormalCube() || minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) - 1, MathHelper.floor_double(player.posZ) + 1).isNormalCube()) && ((heading == 3) || (heading == 1))){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isOnWall = true;
				if(player.moveForward > 0){
					if(player.movementInput.jump && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wallJump){
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRight = false;
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animLeft = false;
						if(heading == 3){
							player.motionX = 0.7D;
							player.motionZ = -0.2D;
						}
						if(heading == 1){
							player.motionX = -0.7D;
							player.motionZ = -0.2D;
						}
						player.motionY = 0.41999998688697815D;
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wallJump = true;
					}
					if(!player.movementInput.jump){
						if(heading == 3){
							((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRight = true;
						}
						if(heading == 1){
							((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animLeft = true;
						}
					}
				}
			}
			else if((minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) - 1).isNormalCube() || minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) , MathHelper.floor_double(player.posY) - 1, MathHelper.floor_double(player.posZ) - 1).isNormalCube()) && ((heading == 3) || (heading == 1))){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isOnWall = true;
				if(player.moveForward > 0){
					if(player.movementInput.jump && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wallJump){
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRight = false;
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animLeft = false;
						if(heading == 3){
							player.motionX = 0.7D;
							player.motionZ = 0.2D;
						}
						if(heading == 1){
							player.motionX = -0.7D;
							player.motionZ = 0.2D;
						}
						player.motionY = 0.41999998688697815D;
						((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wallJump = true;
					}
					if(!player.movementInput.jump){
						if(heading == 3){
							((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animLeft = true;
						}
						if(heading == 1){
							((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).animRight = true;
						}
					}
				}
			}
			else {
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isOnWall = false;
			}
			//			if(player.onGround){
			//				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isOnWall = false;
			//			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isOnWall && player.moveForward > 0){
				player.motionZ *= 0.95D;
				player.motionX *= 0.95D;
				player.motionY *= 0.75D;
			}
		}
		else {
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isOnWall = false;
		}
	}

	private void jump(int heading, EntityPlayer player){
		if((minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) - 1, MathHelper.floor_double(player.posZ) - 1).isNormalCube() && heading == 2) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) - 1, MathHelper.floor_double(player.posZ) + 1).isNormalCube() && heading == 0) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY) - 1, MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 1) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY) - 1, MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 3)){
			if((!minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) - 1).isNormalCube() && heading == 2) || (!minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) + 1).isNormalCube() && heading == 0) || (!minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 1) || (!minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 3)){
				if(ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, (EntityLivingBase)player, 41)){
					player.motionY = 0.41999998688697815D;
				}
			}
			if((minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) - 1).isNormalCube() && heading == 2) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) + 1).isNormalCube() && heading == 0) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 1) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 3)){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isJumping = 3;
				if((minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) + 1, MathHelper.floor_double(player.posZ) - 1).isNormalCube() && heading == 2) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) + 1, MathHelper.floor_double(player.posZ) + 1).isNormalCube() && heading == 0) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY) + 1, MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 1) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY) + 1, MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 3)){
					((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isJumping = 4;
				}
			}
		}
		
		if(player.fallDistance == 0 && !player.onGround){
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isJumping == 3){
				player.motionY *= 0.75D;
			}
			if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isJumping == 4){
				if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).jumpTime < 1){
					player.motionY = 0.41999998688697815D;
					((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).jumpTime++;
				}
			}
		}
		if(player.onGround || ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isTurning || ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isGrabbing){
			if(!(Boolean)ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, (EntityLivingBase)player, 41)){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isJumping = 0;
			}
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).jumpTime = 0;
		}
	}

	private void grab(int heading, EntityPlayer player){
		if(((((minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) - 1).isNormalCube() && heading == 2) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ) + 1).isNormalCube() && heading == 0) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 1) || (minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 3)) && ((!minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) + 1, MathHelper.floor_double(player.posZ) - 1).isNormalCube() && heading == 2) || (!minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) + 1, MathHelper.floor_double(player.posZ) + 1).isNormalCube() && heading == 0) || (!minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) - 1, MathHelper.floor_double(player.posY) + 1, MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 1) || (!minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX) + 1, MathHelper.floor_double(player.posY) + 1, MathHelper.floor_double(player.posZ)).isNormalCube() && heading == 3)) && (!minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY) - 2, MathHelper.floor_double(player.posZ)).isNormalCube()))) && minecraft.theWorld.getBlock(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ)) != Blocks.ladder && player.getCurrentEquippedItem() == null){
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isGrabbing = true;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).rotationYaw = player.rotationYaw;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).rotationPitch = player.rotationPitch;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).prevRotationPitch = player.prevRotationPitch;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).prevRotationYaw = player.prevRotationYaw;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).grabbingDirections[heading] = true;
			if(heading == 0){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).grabbingDirections[3] = true;
			}
			else{
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).grabbingDirections[heading - 1] = true;
			}

			if(heading == 3){
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).grabbingDirections[0] = true;
			}
			else{
				((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).grabbingDirections[heading + 1] = true;
			}
		}
		else{
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isGrabbing = false;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).rotationYaw = 0;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).rotationPitch = 0;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).prevRotationPitch = 0;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).prevRotationYaw = 0;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).grabbingDirections[0] = false;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).grabbingDirections[1] = false;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).grabbingDirections[2] = false;
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).grabbingDirections[3] = false;
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isGrabbing && !((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wasSneaking && player.isSneaking()){
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wasSneaking = true;
		}
		if(!((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isGrabbing){
			((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wasSneaking = false;
		}
		if(((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).wasSneaking){
			return;
		}
		if(!player.isSneaking() && !(Boolean)ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, (EntityLivingBase)player, 41) && ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isGrabbing){
			if(heading == 1 || heading == 3){
				player.setPosition(MathHelper.floor_double(player.posX) + 0.5F, MathHelper.floor_double(player.posY) + 0.9F, player.posZ);
				player.motionX = 0;
			}
			if(heading == 2 || heading == 0){
				player.setPosition(player.posX, MathHelper.floor_double(player.posY) + 0.9F, MathHelper.floor_double(player.posZ) + 0.5F);
				player.motionZ = 0;
			}
			player.isSwingInProgress = false;
//			player.set
			player.motionY = 0.0;
		}
		else if((Boolean)ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, (EntityLivingBase)player, 41) && ((EntityPlayerCustom)player.getExtendedProperties("Player Custom")).isGrabbing){
			player.motionY = 0.8D;
		}
	}

	private void damageEntity(Class c, EntityPlayer p, DamageSource d, float f){
		try {
			Method m = c.getDeclaredMethod(Util.obfuscation ? "func_70665_d" : "damageEntity", DamageSource.class, float.class);
			m.setAccessible(true);
			m.invoke(p, d, f);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
