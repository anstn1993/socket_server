package Chat;

import java.util.ArrayList;

//ä�� �� Ŭ����
public class ChatRoom {
	//�� ��ȣ
	int roomNum;
	//�濡 �������� ����� ����Ʈ
	ArrayList<UserData> userLists;
	//������ ����
	public ChatRoom(int roomNum, ArrayList<UserData> userLists) {
		super();
		this.roomNum = roomNum;
		this.userLists = userLists;
	}
	
	
	
}
