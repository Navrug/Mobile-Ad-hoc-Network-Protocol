package hello;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import utilities.IP;
import lsa.LSAMessage;
import lsa.LSATable;

/*
 * This class has to be concurrency safe because MessageThread
 * adds messages to the table while PacketManager uses the
 * table to create and sends new messages.
 */
public class HelloTable
{
	private final Hashtable<IP, HelloMessage> table;
	private final long creationTime = System.currentTimeMillis();
	private final Hashtable<IP, Long> arrivalTime = new Hashtable<IP, Long>();
	private static short sequenceNumber = 0;
	private final LSATable lsaTable;
	private ReentrantLock lock = new ReentrantLock();

	public HelloTable(LSATable lsaTable)
	{
		this.lsaTable = lsaTable;
		table = new Hashtable<IP, HelloMessage>();
	}

	public void addHello(IP neighbor, HelloMessage message)
	{
		lock.lock();
		try {
			table.put(neighbor, message);
			arrivalTime.put(neighbor, 
					System.currentTimeMillis()-creationTime);
		}
		finally {
			lock.unlock();
		}
	}

	public HelloMessage createHello()
	{
		HelloMessage result = new HelloMessage(IP.myIP());
		lock.lock();
		try {
			for (IP neighbor : table.keySet()) {
				if (table.get(neighbor).isSymmetric(IP.myIP()))
					result.addSymmetric(neighbor);
				else
					result.addHeard(neighbor);
			}
		}
		finally {
			lock.unlock();
		}
		return result;
	}

	public LSAMessage createLSA()
	{
		LSAMessage result = 
				new LSAMessage(IP.myIP(), sequenceNumber++);
		lock.lock();
		try {
			for (IP neighbor : table.keySet())
			{
				if (table.get(neighbor).isSymmetric(IP.myIP()))
					result.addNeighbor(neighbor);
			}
		}
		finally {
			lock.unlock();
		}
		lsaTable.addLSA(IP.myIP(), result);
		return result;
	}

	public boolean checkDeadNodes()
	{
		boolean result = false;
		long currentTime = System.currentTimeMillis()-creationTime;
		LinkedList<IP> toRemove = new LinkedList<IP>();
		lock.lock();
		try {
			for (IP neighbor : arrivalTime.keySet())
				if (currentTime - arrivalTime.get(neighbor) > 5000) {
					toRemove.add(neighbor);
					result = true;
				}
			for (IP neighbor : toRemove) {
				table.remove(neighbor);
				arrivalTime.remove(neighbor);
			}
		}
		finally {
			lock.unlock();
		}
		return result;	
	}
}
