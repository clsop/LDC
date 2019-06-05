package org.petersen.landc;

/**
 * 
 * @author Claus Petersen
 * 
 * this is for the program to distinguish between packets sent
 */
public enum PacketType {

	// Enums
	ARE_YOU_THERE((byte) 0x10),
	HERES_MY_NAME((byte) 0x12),
	LEAVE_CHAT((byte) 0x14),
	HERES_SOME_TEXT((byte) 0x18),
	REQUEST_NAME((byte) 0x26),
	NAME_TAKEN((byte) 0x42),
	NONE((byte) 0x84);

	// Enum field
	private final byte id;

	// Constructor
	PacketType(byte id) {
		this.id = id;
	}

	// Getter
	public byte ID() {
		return id;
	}

	public static PacketType getType(byte id) {
		switch (id) {
		case 0x10:
			return ARE_YOU_THERE;
		case 0x12:
			return HERES_MY_NAME;
		case 0x14:
			return LEAVE_CHAT;
		case 0x18:
			return HERES_SOME_TEXT;
		case 0x26:
			return REQUEST_NAME;
		case 0x42:
			return NAME_TAKEN;
		default:
			return NONE;
		}
	}
};
