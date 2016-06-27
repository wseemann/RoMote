package wseemann.media.romote.utils;

import java.net.*;
import java.util.ArrayList;

public class RokuScan {

	/* constants for changing terminal output color */
	private static final String BLUE = "\033[94m";
	private static final String PURPLE = "\033[95m";
	private static final String NORMAL = "\033[0m";
	
	public static boolean verbose = true;

	/**
	 * Scan the local area network for a single Roku device
	 * @return the IP Address of the first found Roku device
	 */
	public static String scanForRoku() throws Exception {
		/* create byte arrays to hold our send and response data */
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];

		/* our M-SEARCH data as a byte array */
		String MSEARCH = "M-SEARCH * HTTP/1.1\nHost: 239.255.255.250:1900\nMan: \"ssdp:discover\"\nST: roku:ecp\n"; 
		sendData = MSEARCH.getBytes();

		status("Creating MSEARCH request to 239.255.255.250 on port 1900...");
		/* create a packet from our data destined for 239.255.255.250:1900 */
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("239.255.255.250"), 1900);
		
		status("Sending multicast SSDP MSEARCH request...");
		/* send packet to the socket we're creating */
		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.send(sendPacket);
		
		status("Waiting for network response...");
		/* recieve response and store in our receivePacket */
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);

		/* get the response as a string */
		String response = new String(receivePacket.getData());

		/* close the socket */
		clientSocket.close();

		/* parse the IP from the response */
		/* the response should contain a line like:
			Location:  http://192.168.1.9:8060/
		   and we're only interested in the address -- not the port.
		   So we find the line, then split it at the http:// and the : to get the address.
		*/
		response = response.toLowerCase();
		String address = response.split("location:")[1].split("\n")[0].split("http://")[1].split(":")[0].trim();
		//String serialNumber = response.split("usn:")[1].split("\n")[0].split(":")[3].trim();
		//Log.d("RokuScan", serialNumber);
		status("Found Roku at " + address);
		
		/* return the IP */
		return address;
	}

	/**
	 * Scan the local network many times to find multiple Rokus
	 * @return a String array of Roku IP Addresses
	 */
	public static String[] scanForAllRokus() {
		/* array list to hold our found addresses */
		ArrayList<String> arrayList = new ArrayList<String>();

		//System.out.print(BLUE + "[*] " + PURPLE + "Progress: (00/20)");
		/* scan 20 times to try to get all the devices */
		String address;
		for (int i = 0; i < 5; i++) {
			/*System.out.print("\b\b\b\b\b\b");
			if (i < 9)
				System.out.print("0" + (i+1));
			else
				System.out.print(i+1);
			System.out.print("/20)");*/
			try {
				address = scanForRoku();
				if (!arrayList.contains(address)) {
					arrayList.add(address);
				}
			} catch (Exception e) {
			}
		}
		//System.out.println(NORMAL);

		/* convert our result ArrayList to an array */
		String[] returnArray = new String[arrayList.size()];
		returnArray = arrayList.toArray(returnArray);

		/* return our array of data */
		return returnArray;
	}

	/**
	 * Print a status message with a specific color & format
	 */
	private static void status(String msg) {
		//if (verbose)
		//	System.out.println(BLUE + "[*] " + PURPLE + msg + NORMAL);
	}

}
