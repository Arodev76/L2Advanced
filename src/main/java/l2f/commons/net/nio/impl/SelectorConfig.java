package l2f.commons.net.nio.impl;

import java.nio.ByteOrder;

public class SelectorConfig
{
	/**
	 * Buffer size for reading
	 */
	public int READ_BUFFER_SIZE = 65536;
	/**
	 * Buffer size for writing
	 */
	public int WRITE_BUFFER_SIZE = 131072;
	/**
	 * The maximum number of packets per write can be less than this number if the write buffer is full
	 */
	public int MAX_SEND_PER_PASS = 32;
	/**
	 * Delay in milliseconds after every pass in the SelectorThread loop
	 */
	public long SLEEP_TIME = 10;
	/**
	 * Delay before changing the planned action of interest
	 */
	public long INTEREST_DELAY = 30;
	/**
	 * Header Size
	 */
	public int HEADER_SIZE = 2;
	/**
	 * Maximum packet size
	 */
	public int PACKET_SIZE = 32768;
	/**
	 * Number of auxiliary buffers
	 */
	public int HELPER_BUFFER_COUNT = 64;
	/**
	 * Byte order
	 */
	public ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
}