package evoparsons.rmishared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public interface BrokerUIInterface extends Remote 
{
    // Methods for the ITS to use
    public ParsonsPuzzle getParsonsPuzzle(String sid) throws RemoteException;
    public Auth authenticateStudent(String sid, String ssig, String skey) throws RemoteException;
    public void setParsonsEvaluation(ParsonsEvaluation data) throws RemoteException;
    public Stats getStudentStats(String sid) throws RemoteException;
    public Map<String, Stats> getStudentStats(String iid, String isig, List<String> ssig) throws RemoteException;
}
