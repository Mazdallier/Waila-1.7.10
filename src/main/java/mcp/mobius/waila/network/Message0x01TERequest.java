package mcp.mobius.waila.network;

import java.util.HashSet;

import cpw.mods.fml.common.FMLCommonHandler;
import mcp.mobius.waila.utils.NBTUtil;
import mcp.mobius.waila.utils.WailaExceptionHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.DimensionManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class Message0x01TERequest extends SimpleChannelInboundHandler<Message0x01TERequest> implements IWailaMessage {
	
	public int dim;
	public int posX;
	public int posY;
	public int posZ;	
	public HashSet<String> keys = new HashSet<String> ();	
	
	public Message0x01TERequest(){}	
	
	public Message0x01TERequest(TileEntity ent, HashSet<String> keys){
		this.dim  = ent.getWorldObj().provider.dimensionId;
		this.posX = ent.xCoord;
		this.posY = ent.yCoord;
		this.posZ = ent.zCoord;
		this.keys = keys;
	}
	
	@Override
	public void encodeInto(ChannelHandlerContext ctx, IWailaMessage msg, ByteBuf target) throws Exception {
		target.writeInt(dim);
		target.writeInt(posX);
		target.writeInt(posY);
		target.writeInt(posZ);
		target.writeInt(this.keys.size());
		
		for (String key : keys)
			WailaPacketHandler.INSTANCE.writeString(target, key);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf dat, IWailaMessage rawmsg) {
		try{
			Message0x01TERequest msg = (Message0x01TERequest)rawmsg;
			msg.dim  = dat.readInt();
			msg.posX = dat.readInt();
			msg.posY = dat.readInt();
			msg.posZ = dat.readInt();
			
			int nkeys = dat.readInt();
			
			for (int i = 0; i < nkeys; i++)
				this.keys.add(WailaPacketHandler.INSTANCE.readString(dat));
		
		}catch (Exception e){
			WailaExceptionHandler.handleErr(e, this.getClass().toString(), null);
		}		
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message0x01TERequest msg) throws Exception {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        TileEntity      entity = DimensionManager.getWorld(msg.dim).getTileEntity(msg.posX, msg.posY, msg.posZ);
        if (entity != null){
        	try{
        		NBTTagCompound tag = new NBTTagCompound();
        		entity.writeToNBT(tag);
        		ctx.writeAndFlush(new Message0x02TENBTData(NBTUtil.createTag(tag, msg.keys))).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        	}catch(Throwable e){
        		WailaExceptionHandler.handleErr(e, entity.getClass().toString(), null);
        	}
        }
		
		
	}
}

