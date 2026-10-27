package com.doctorknepper.motorcycles.network;

import com.doctorknepper.motorcycles.DKMotorcycles;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by the rider's client when the headlight key is pressed. */
public record ToggleHeadlightPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ToggleHeadlightPayload> TYPE = new CustomPacketPayload.Type<>(DKMotorcycles.id("toggle_headlight"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ToggleHeadlightPayload> CODEC = StreamCodec.unit(new ToggleHeadlightPayload());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
