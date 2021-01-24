package com.vedantatree.wol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vedantatree.utils.config.ConfigurationManager;


/**
 * A utility to walk pc on LAN. Can be used for fun and work both.
 * Ex: If we want to wake up all PC in mid night for maintenance, patching etc
 * 
 * @author Mohit Gupta <mohit.gupta@vedantatree.com>
 */
public class WakeOnLan
{

	private static Log				logger			= LogFactory.getLog( WakeOnLan.class );

	private static final int		PORT			= 9;
	private static final String		IP_STR			= "192.168.0.255";
	private static int				waitBeforeNextPacket;
	private static int				numberOfTimesToSendPacket;

	private static final WakeOnLan	SHARED_INSTANCE	= new WakeOnLan();

	private WakeOnLan()
	{
		try
		{
			waitBeforeNextPacket = Integer
					.parseInt( ConfigurationManager.getSharedInstance().getPropertyValue( "delay" ) );
			numberOfTimesToSendPacket = Integer
					.parseInt( ConfigurationManager.getSharedInstance().getPropertyValue( "repeat" ) );
		}
		catch( Throwable th )
		{
			logger.error( "Error while initializing properties for WakeOnLan", th );
			System.exit( 1 );
		}
	}

	public static WakeOnLan getSharedInstance()
	{
		return SHARED_INSTANCE;
	}

	/**
	 * This method will send UDP packet to wake up the pc on LAN
	 * for each MAC address specific in the configuration
	 * It will send it repeatedly with a delay of some time in between
	 * both of these configurations are loaded from config file
	 * 
	 * Instruction to wake up are sent repeatedly because the delivery of UDP is not confirmed in case of heavy traffic.
	 */

	public void sendRestartPacket( String macAddress )
	{
		logger.debug( "Entering in sendRestartPacket() " );

		logger.debug(
				"delay[" + waitBeforeNextPacket + "] number-of-times-to-repeat[" + numberOfTimesToSendPacket + "]" );

		for( int i = 0; i < numberOfTimesToSendPacket; i++ )
		{
			try
			{
				sendPacket( macAddress );
				Thread.sleep( waitBeforeNextPacket );
			}
			catch( InterruptedException e )
			{
				logger.error( "Error while sending wake up UDP packet. error[" + e.getMessage() + "]" );
			}
		}

	}

	/**
	 * It prepares the mac address in right format and send UDP.
	 * UDP is broadcasted on the network by preparing a socket.
	 * The machine having the given MAC address will respond and will get booted.
	 */
	private void sendPacket( String macAddress )
	{
		try
		{
			byte[] macAddressInBytes = getMACAddressBytes( macAddress );
			byte[] enhancedMacAddressByteStore = new byte[6 + 16 * macAddressInBytes.length];

			for( int i = 0; i < 6; i++ )
			{
				enhancedMacAddressByteStore[i] = (byte) 0xff;
			}

			for( int i = 6; i < enhancedMacAddressByteStore.length; i += macAddressInBytes.length )
			{
				System.arraycopy( macAddressInBytes, 0, enhancedMacAddressByteStore, i, macAddressInBytes.length );
			}

			InetAddress address = InetAddress.getByName( IP_STR );

			DatagramPacket packet = new DatagramPacket( enhancedMacAddressByteStore, enhancedMacAddressByteStore.length,
					address, PORT );

			DatagramSocket socket = new DatagramSocket();
			socket.send( packet );
			socket.close();

			logger.debug( "Wake-on-LAN packet sent. macAddress[" + macAddress + "]" );
		}
		catch( Exception e )
		{
			logger.error( "Failed to send Wake-on-LAN packet: " + e.getMessage(), e );
			System.exit( 1 );
		}

	}

	/**
	 * getMacBytes splits the MAC address and the hexadecimal MAC is converted
	 * to integer and is returned
	 * 
	 * @param macAddress
	 * @return
	 * @throws IllegalArgumentException
	 */
	private byte[] getMACAddressBytes( String macAddress ) throws IllegalArgumentException
	{

		String[] hex = macAddress.split( "(\\:|\\-)" );

		if( hex.length != 6 )
		{
			throw new IllegalArgumentException(
					"Invalid MAC address, hex length is not 6. macAddress[" + macAddress + "]" );
		}

		byte[] macAddressBytes = new byte[6];
		try
		{
			for( int i = 0; i < 6; i++ )
			{
				macAddressBytes[i] = (byte) Integer.parseInt( hex[i], 16 );
			}
		}
		catch( NumberFormatException e )
		{
			logger.error( "Error while getting bytes for mac address" + e.getMessage(), e );

			throw new IllegalArgumentException( "Invalid hex digit in MAC address. macAddress[" + macAddress + "]" );
		}

		return macAddressBytes;
	}

}