package Chat;

import java.util.ArrayList;

//채팅 방 클래스
public class ChatRoom {
	//방 번호
	int roomNum;
	//방에 참여중인 사용자 리스트
	ArrayList<UserData> userLists;
	//생성자 생성
	public ChatRoom(int roomNum, ArrayList<UserData> userLists) {
		super();
		this.roomNum = roomNum;
		this.userLists = userLists;
	}
	
	
	
}
