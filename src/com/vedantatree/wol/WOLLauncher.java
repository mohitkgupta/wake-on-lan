package com.vedantatree.wol;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vedantatree.utils.StringUtils;
import org.vedantatree.utils.config.ConfigurationManager;


/**
 * Launcher class for WOL program.
 * 
 * It loads the configuration for IP to MacAddress mapping, maintain it in memory for faster lookup for any request
 * It is a gateway for WOL operations based on configurations
 * 
 * @author Mohit Gupta <mohit.gupta@vedantatree.com>
 */

public class WOLLauncher
{

	private static Log				logger		= LogFactory.getLog( WOLLauncher.class );
	private static final String		INPUT_FILE	= "in.txt";

	private HashMap<String, String>	ipToMACMapping;
	private List<String>			macAddressesList;

	public WOLLauncher()
	{
		initializeWOL();
	}

	/**
	 * Initializes all the configurations which includes ip to mac address mapping and list of mac addresses
	 */
	private void initializeWOL()
	{
		FileInputStream configFileStream = null;
		BufferedReader dis = null;

		try
		{
			ConfigurationManager.loadConfigurationFile( "wol.properties" );

			configFileStream = new FileInputStream( INPUT_FILE );
			dis = new BufferedReader( new InputStreamReader( configFileStream ) );

			String oneLineFromConfig;
			String[] lineItemsInArray;
			String macAddress;
			String ipAddress;

			while( ( oneLineFromConfig = dis.readLine() ) != null )
			{
				lineItemsInArray = oneLineFromConfig.split( "#" );
				ipAddress = lineItemsInArray[0];
				macAddress = lineItemsInArray[1];

				ipToMACMapping.put( ipAddress, macAddress );
				macAddressesList.add( macAddress );
			}
		}
		catch( Exception ex )
		{
			logger.error( "Error while loading WOL configuration", ex );
		}
		finally
		{
			try
			{
				if( dis != null )
				{
					dis.close();
				}

				if( configFileStream != null )
				{
					configFileStream.close();
				}
			}
			catch( IOException ioException )
			{
				logger.error( "Error while closing file stream", ioException );
			}
		}
	}

	public void wakeUpPCByMac( String macAddress )
	{
		logger.debug( "waking up MAC[" + macAddress + "]" );

		if( macAddress == null || macAddress.length() == 0 )
		{
			throw new IllegalArgumentException( "Given mac address is not valid. macAddress[" + macAddress + "]" );
		}

		WakeOnLan.getSharedInstance().sendRestartPacket( macAddress );
	}

	public void wakeUpAllPCsFromConfig()
	{
		wakeUpPCsByMacAddresses( macAddressesList );
	}

	public void wakeUpPCsByMacAddresses( List<String> macAddressesList )
	{
		for( String macAddress : macAddressesList )
		{
			wakeUpPCByMac( macAddress );
		}
	}

	/**
	 * 
	 * It wakes up PC for given IP Address.
	 * 
	 * For functioning, MAC address for given IP Address should be given in Config file
	 */

	public void wakeUpPCByIP( String ipAddress )
	{
		StringUtils.assertQualifiedArgument( ipAddress, "IP Address" );
		wakeUpPCByMac( getMacAddressByIP( ipAddress ) );
	}

	/**
	 * It wake up all the pcs in given IP Range
	 */

	public void wakeUpPCForIPRange( String ipRangeStart, String ipRangeEnd )
	{
		wakeUpPCsByMacAddresses( getMACAddressByIPRange( ipRangeStart, ipRangeEnd ) );
	}

	private String getMacAddressByIP( String ipStr )
	{
		String macAddress = ipToMACMapping.get( ipStr );

		if( !StringUtils.isQualifiedString( macAddress ) )
		{
			logger.error( "No mac address found for given ip address. ipAddress[" + ipStr + "] ipToMacMapping["
					+ ipToMACMapping + "]" );
			throw new IllegalArgumentException( "No mac address found for given ip address. ipAddress[" + ipStr + "]" );
		}

		return macAddress;
	}

	/**
	 * It loads all the MAC addresses from the INPUT_FILE in a list for given range of IPs.
	 */
	public List<String> getMACAddressByIPRange( String ipRangeStart, String ipRangeEnd )
	{
		logger.debug( "getMacStringByIPRange. ipRangeStart[" + ipRangeStart + "] ipRangeEnd[" + ipRangeEnd + "]" );

		List<String> macAddressesListByIPRange = new ArrayList<>();
		String[] ipArray = new String[100];

		String[] lineArray = ipRangeStart.split( "\\." );
		String ipString = lineArray[3];
		int startIP = Integer.parseInt( ipString );

		lineArray = ipRangeEnd.split( "\\." );
		ipString = lineArray[3];
		int endIP = Integer.parseInt( ipString );

		logger.debug( "startIP[" + startIP + "] endIP[" + endIP + "]" );

		int i = 0;
		while( ( startIP - 1 ) != endIP )
		{

			ipString = String.valueOf( startIP );
			ipArray[i] = "192.168.0." + ipString;

			macAddressesListByIPRange.add( getMacAddressByIP( ipArray[i] ) );

			startIP++;
			i++;

		}

		return macAddressesListByIPRange;
	}

	public static void main( String[] args )
	{
		if( args.length != 1 )
		{
			logger.error( "Wrong options provided !" );
			throw new IllegalArgumentException(
					"No argument provided for WOL. Valid arguments are -all, -single, -range" );
		}

		try
		{
			WOLLauncher wolLauncher = new WOLLauncher();

			if( args[0].equalsIgnoreCase( "-all" ) )
			{

				wolLauncher.wakeUpAllPCsFromConfig();
			}
			else if( args[0].equalsIgnoreCase( "-single" ) )
			{
				String singleIP = ConfigurationManager.getSharedInstance().getPropertyValue( "singleip" );
				wolLauncher.wakeUpPCByIP( singleIP );
			}
			else if( args[0].equalsIgnoreCase( "-range" ) )
			{
				String ipStartRange = ConfigurationManager.getSharedInstance().getPropertyValue( "startip" );
				String ipEndRange = ConfigurationManager.getSharedInstance().getPropertyValue( "endip" );

				wolLauncher.wakeUpPCForIPRange( ipStartRange, ipEndRange );
			}
		}
		catch( Exception e )
		{
			logger.error( e );
		}

		System.exit( 1 );

	}

}
