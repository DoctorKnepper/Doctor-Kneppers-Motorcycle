package com.doctorknepper.motorcycles.network;

import com.doctorknepper.motorcycles.DKMotorcycles;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by the rider's client when the horn key is pressed. */
public record HornPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HornPayload> TYPE = new CustomPacketPayload.Type<>(DKMotorcycles.id("horn"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HornPayload> CODEC = StreamCodec.unit(new HornPayload());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
