package Chat;

import java.io.DataOutputStream;
import java.net.Socket;

public class UserData {
	
	Socket userSocket;
	String userId, userNickname, userProfile;
	DataOutputStream dataOutputStream;
	boolean isFaceChatting;
	
	public UserData(Socket userSocket, String userId, String userNickname, String userProfile, DataOutputStream dataOutputStream, boolean isFaceChatting) {
		this.userSocket = userSocket;
		this.userId = userId;
		this.userNickname = userNickname;
		this.userProfile = userProfile;
		this.dataOutputStream = dataOutputStream;
		this.isFaceChatting = isFaceChatting;
	}
	
	

}
