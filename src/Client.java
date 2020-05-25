
//Group 16 Vanilla Client DS-SIM


import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Client {
	private Socket socket            = null;
	private PrintWriter out = null;
	private BufferedReader input = null;
	private String input1 = "";

	private int jobCpuCores, jobMemory, jobDisk, jobSub, jobID, jobTime;
	private String serverType;
	private int serverTime, serverState, serverCpuCores, serverMemory, serverDisk;
	private int serverID;
	private int jobCount = 0;
	private int finalServerID = 0;
	private String finalServer = "";

	//allToLargest Variable

	private int biggestCPU = 0;

	//First Fit Variable

	private int first = 0; 

	//Global Variables for BF Algorithm

	private final int INT_MAX = Integer.MAX_VALUE;
	private int bestFit = INT_MAX;
	private int minAvail = INT_MAX;

	//Global Variables for WF Algorithm
	private final int INT_MIN = Integer.MIN_VALUE;
	private int altFit = INT_MIN;
	private int worstFit = INT_MIN;
	private boolean worst = false;



	public Client(String algo ,String address, int port) throws UnknownHostException, IOException, SAXException, ParserConfigurationException {
		//start connection with server
		openConnection(address,port); 
		if(newStatus("OK")) {
			sendToServer("AUTH " + System.getProperty("user.name"));
		}

		while (!newStatus("NONE")) {
			if(currentStatus("OK")) {
				sendToServer("REDY");
			} else if (input1.startsWith("JOBN")) {
				//if the input string begins with JOBN, 
				//read the input line and get job values 
				jobRecieve();

				//request all information of all servers
				sendToServer("RESC All");
				//checks if input is DATA
				if(newStatus("DATA")) {
					sendToServer("OK");
				}


				//Loop through all the servers until the input string sent is
				// . indicating end of server information
				while (!newStatus(".")) {
					//read current server state
					serverRecieve();
					
					//default algorithm 
					if(algo.equals("allToLargest")) {
						allToLargest();                        	
					}
					//if algorithm is wf call the worstFitAlgo
					if(algo.equals("wf")) {
						worstFitAlgo("dont_read");
					}
					//if algorithm is bf call the bestFitAlgo
					if(algo.equals("bf")) {
						bestFitAlgo("dont_read");
					}
					//if algorithm is ff call the firstFit
					if(algo.equals("ff")) {

						firstFit();


					}
					//send ok to to say send next server info
					sendToServer("OK");
				}
				
				//checks to see if the bestFit value has been changed
				//if not, call the bestFitAlgo with read to read the initial
				//server resource capacity
				if(algo.equals("bf") && bestFit == INT_MAX) {
					bestFitAlgo("read");

				}
				//checks to see if the worstFit & altFit values have been changed
				//if not, call the worstFitAlgo with read to read the initial
				//server resource capacity
				if(algo.contentEquals("wf") && worstFit == INT_MIN && altFit == INT_MIN && worst == false) {
					worstFitAlgo("read");
				}
				
				//check if first has been changed to 1,
				//if not read initial server resource capacity
				if(algo.equals("ff") && first == 0) {
					firstFitAlgo();
				}

				//scheduling decision
				sendToServer("SCHD " + jobCount + " " + finalServer + " " + finalServerID);
				
				//there is no jobID in the job information sent so we need to count the
				//jobs that come in and use that for the scheduling decision.
				jobCount++;
				
				//reset the first parameter after each job to make sure the 
				//firstFit function is reading the current server info
				//and can schedule accordingly. 
				first = 0;

			}
		}
		//close all open connections with server. 
		closeConnection();
	}


	//this is the default function that is called 
	//It checks the server state CPU size against a global variable biggestCPU. 
	//if its bigger, then they will change the value of the biggestCPU and 
	//update the servertype and ID to send back to the server. 
	public void allToLargest() {

		if(biggestCPU < serverCpuCores) {
			biggestCPU = serverCpuCores; 
			finalServer = serverType; 
			finalServerID = serverID; 
		}


	}

	public void bestFitAlgo(String readXML) throws SAXException, IOException, ParserConfigurationException {

		//this if statement body checks if the current server that is received after serverReveive()
		// has sufficient available resources for the current job to be scheduled
		//this body has been ran multiple times it is checking each time if there is
		//a server with a lower fitness value


		if(jobCpuCores <= serverCpuCores && jobDisk <= serverDisk && jobMemory <= serverMemory) {
			if(serverCpuCores < bestFit || (serverCpuCores == bestFit && serverTime < minAvail)) {	
				bestFit = serverCpuCores;
				minAvail = serverTime;
				finalServer = serverType;
				finalServerID = serverID; 
			}

		}

		//this is only called if we need to base the best-fit server on initial resource capacity. 
		//e.g if the current server state info does not match the above if statement. 
		else if(readXML == "read") {

			//We first call read the information from system.xml using the readFile() function
			//after we read the xml, we loop through the nodelist of servers and set the values 
			//each iteration as we are not using the data from the current server state.
			//each loop, we have the same if body to find the best server based on the initial resource capacity

			NodeList xml = readFile(); 

			for(int i = 0; i < xml.getLength(); i++) {


				serverType = xml.item(i).getAttributes().item(6).getNodeValue();

				//The xml file does not have serverID so i set to 0

				serverID = 0;  

				serverCpuCores = Integer.parseInt(xml.item(i).getAttributes().item(1).getNodeValue());
				serverMemory = Integer.parseInt(xml.item(i).getAttributes().item(4).getNodeValue());
				serverDisk = Integer.parseInt(xml.item(i).getAttributes().item(2).getNodeValue());

				if(jobCpuCores <= serverCpuCores && jobDisk <= serverDisk && jobMemory <= serverMemory) {
					if(serverCpuCores < bestFit || (serverCpuCores == bestFit && serverTime < minAvail)) {
						bestFit = serverCpuCores;
						minAvail = serverTime;
						finalServer = serverType;
						finalServerID = serverID; 
					}
				}
			}
		}
	}
	
	
	//the firstFit function checks the current server state to find the first available server
	//first we check to see if the serverTime != -1 (server active or not) and then see if the 
	//server has the capacity to schedule and if it does we set the values of finalServe and finalServerID
	//to the current server
	
	//we then set the value of first to 1 so that whenever this function is called it will only set the values
	//for the first available server with the right capacity. 
	
	//the first variable is reset after every Job is scheduled. 
	public void firstFit() {

		if(serverTime != -1) {			
			if(jobCpuCores <= serverCpuCores && jobDisk <= serverDisk && jobMemory <= serverMemory) {				
				if(first == 0) {					
					finalServer = serverType;
					finalServerID = serverID;
					first =1;				
				}		
			}

		}

	}


	//The firstFitAlgo is only called if the firstFit function cannot find a server that fits based
	//on the current server states. When this function is called, we read the server information from
	// system.xml to find the first available server based on the initial resource capacity. 
	public void firstFitAlgo() throws SAXException, IOException, ParserConfigurationException  {

		NodeList xml = readFile(); 

		for(int i = 0; i < xml.getLength(); i++) {


			serverType = xml.item(i).getAttributes().item(6).getNodeValue();

			//The xml file does not have serverID so i set to 0

			serverID = 0;  

			serverCpuCores = Integer.parseInt(xml.item(i).getAttributes().item(1).getNodeValue());
			serverMemory = Integer.parseInt(xml.item(i).getAttributes().item(4).getNodeValue());
			serverDisk = Integer.parseInt(xml.item(i).getAttributes().item(2).getNodeValue());

			if(jobCpuCores <= serverCpuCores && jobDisk <= serverDisk && jobMemory <= serverMemory) {

				finalServer = serverType;
				finalServerID = serverID; 
				return;

			}
		}

	}



	//This algorithm has a very similar implementation to the bestFit 
	//algorithm however we are now looking for the worst server to send 
	//the job to. 
	public void worstFitAlgo(String readXML) throws SAXException, IOException, ParserConfigurationException {

		//this if statement body checks if the current server that is received after serverReveive()
		// has sufficient available resources for the current job to be scheduled
		//this body has been ran multiple times it is checking each time if there is
		//a server with a higher fitness value to find the worst option that can still be scheduled
		//After we check that the server can have a job scheduled we check to see if the current server has 
		//a worse available CPU than the current one stored in the worstFit variable
		// we then check to see if the serverState is either in state 2 or 3(running or suspended)
		//so we have to wait for that server to finish so that we can start our job. 
		//If the second if statement does not pass, we set the value of altFit to that of 
		//the current server if the current altFit is smaller than the stored variable. 


		if(jobCpuCores <= serverCpuCores && jobDisk <= serverDisk && jobMemory <= serverMemory) {
			if(serverCpuCores > worstFit && (serverState == 3 || serverState == 2)) {
				worstFit = serverCpuCores;
				worst = true; 
				finalServer = serverType;
				finalServerID = serverID; 
			} else if(!worst && serverCpuCores > altFit) {
				altFit = serverCpuCores;
				finalServer = serverType;
				finalServerID = serverID;
			}

		}

		//this is only called if we need to base the worst-fit server on initial resource capacity. 
		//e.g if the current server state info does not match the above if statement. 
		else if(readXML == "read") {

			//We first call read the information from system.xml using the readFile() function
			//after we read the xml, we loop through the nodelist of servers and set the values 
			//each iteration as we are not using the data from the current server state.
			//each loop, we have the same if body to find the worst server based on the initial resource capacity

			NodeList xml = readFile(); 

			for(int i = 0; i < xml.getLength(); i++) {


				serverType = xml.item(i).getAttributes().item(6).getNodeValue();

				//The xml file does not have serverID so i set to 0

				serverID = 0;  

				serverCpuCores = Integer.parseInt(xml.item(i).getAttributes().item(1).getNodeValue());
				serverMemory = Integer.parseInt(xml.item(i).getAttributes().item(4).getNodeValue());
				serverDisk = Integer.parseInt(xml.item(i).getAttributes().item(2).getNodeValue());

				if(jobCpuCores <= serverCpuCores && jobDisk <= serverDisk && jobMemory <= serverMemory) {
					if(serverCpuCores > worstFit && (serverState == 3 || serverState == 2)) {
						worstFit = serverCpuCores;
						worst = true; 
						finalServer = serverType;
						finalServerID = serverID; 
					} else if(!worst && serverCpuCores > altFit) {
						altFit = serverCpuCores;
						finalServer = serverType;
						finalServerID = serverID;
					}
				}
			}
		}
	}



	//this function takes the input string that is initialized in the 
	//newStatus() function and read as an input stream from the server
	// the string that the server sends to the client is split based
	//on the spaces between the input string 
	public void jobRecieve() {
		String[] jobInput = input1.split("\\s+");
		jobSub = Integer.parseInt(jobInput[1]);
		jobID = Integer.parseInt(jobInput[2]);
		jobTime = Integer.parseInt(jobInput[3]);
		jobCpuCores = Integer.parseInt(jobInput[4]);
		jobMemory = Integer.parseInt(jobInput[5]);
		jobDisk = Integer.parseInt(jobInput[6]);
		bestFit = INT_MAX;
		minAvail = INT_MAX;
		worstFit = INT_MIN;
		altFit = INT_MIN;
		worst = false;
	}

	//this function works the same as the jobInput however it is called
	//when we get need to get the server state info instead of the job info.  
	public void serverRecieve() {
		String[] serverInput = input1.split("\\s+");
		serverType = serverInput[0];
		serverID = Integer.parseInt(serverInput[1]);
		serverState = Integer.parseInt(serverInput[2]);
		serverTime = Integer.parseInt(serverInput[3]);
		serverCpuCores = Integer.parseInt(serverInput[4]);
		serverMemory = Integer.parseInt(serverInput[5]);
		serverDisk = Integer.parseInt(serverInput[6]);
	}


	//close connection just closes all the input and output streams + Socket opened 
	//in the openConneciton function. 
	//We use the sendToServer() function to send the string QUIT to end the running process. 
	public void closeConnection() throws IOException {
		sendToServer("QUIT");
		input.close();
		out.close();
		socket.close();
	}

	//open connection is very similar to the close connection function
	//however it opens the socket and input and output streams then sends the string HELO
	public void openConnection(String address, int port) throws UnknownHostException, IOException {
		socket = new Socket(address, port);
		out = new PrintWriter(socket.getOutputStream());
		input = new BufferedReader( new InputStreamReader(socket.getInputStream()));
		sendToServer("HELO");
	}

	//the sent to server function utilizes PrintWriters write function to 
	//be able to send messages to the server and then we flush the output stream
	//so we can get ready to send another message. 
	public void sendToServer(String x) {
		out.write(x + "\n");
		out.flush();
	}

	//the newStatus function first initializes the input1 variable
	//and assigns it to the value if the input stream. 
	//this allows us to read the data that the server is sending to us
	//after we initialize the variable we call the value of itself so that we can use
	//it as a conditional while setting the variable at the same time.
	public boolean newStatus(String x) throws IOException {
		input1 = input.readLine();
		if(input1.equals(x)){
			return true;
		}
		return false;
	}

	//The current status function is the same as the newStatus fucntion, 
	//however it does not set the value of input1. it only checks to see if it is equal
	//to the input parameter. 
	public boolean currentStatus(String x) {
		if(input1.equals(x)){
			return true;
		}
		return false;
	}


	//The readFile function is used to read the value within the system.xml file
	//we first create an empty nodelist and then use a DOM parser to get the server values from the file
	//using the "server" tagname
	public NodeList readFile() throws SAXException, IOException, ParserConfigurationException {
		//initialize the nodelist for the xml reader
		NodeList systemXML = null;

		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("system.xml");
		doc.getDocumentElement().normalize();

		systemXML = doc.getElementsByTagName("server");

		return systemXML;



	}

	public static void main(String[] args) throws UnknownHostException, IOException, SAXException, ParserConfigurationException {

		//default algorithm 
		String algo = "allToLargest";
		
		//if there is an input parameter, set algo to the input algorithm 
		if (args.length == 2 && args[0].equals("-a")) {
			algo = args[1]; 
		}       


		Client client = new Client(algo, "127.0.0.1", 50000);
	}
}



