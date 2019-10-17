package Chat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class SNSChatServer {
    public static final int port = 8888;
    // 채팅방 번호가 키,채팅방 객체가 값으로 구성되는 해시맵
    HashMap<Integer, ChatRoom> chatRoomLists;

    // 소켓 서버로 접근한 사용자 정보를 저장하기 위한 해시맵
    HashMap<String, UserData> userDataLists;

    ServerSocket serverSocket = null;

    public static void main(String[] args) {
        new SNSChatServer().start();
    }

    public void start() {

        // 채팅방 목록 해시맵 초기화
        chatRoomLists = new HashMap<>();
        // 사용자 정보 목록 해시맵 초기화
        userDataLists = new HashMap<>();

        try {
            // 서버 소켓 생성
            serverSocket = new ServerSocket(port);
            // 바인딩
//			String hostAddress = InetAddress.getLocalHost().getHostAddress();
//			serverSocket.bind(new InetSocketAddress(hostAddress, port));
//			consoleLog("연결 기다림-" + hostAddress + ":" + port);
            consoleLog("연결 기다림:" + port);

            // 요청 대기
            while (true) {
                Socket userSocket = serverSocket.accept();
                new ChatServerProcessThread(userSocket).start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    System.out.println("서버 소켓 종료");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("서버 소켓 에러");
            }
        }
    }

    public static void consoleLog(String log) {
        System.out.println("[server" + Thread.currentThread().getId() + "]" + log);
    }

    public class ChatServerProcessThread extends Thread {

        // 클라이언트 소켓
        Socket userSocket = null;
        // 인풋스트림
        DataInputStream dataInputStream;
        // 아웃풋스트림
        DataOutputStream dataOutputStream;
        // 이 스레드의 클라이언트 계정
        String userId;

        // 클래스 생성자
        public ChatServerProcessThread(Socket userSocket) {
            this.userSocket = userSocket;
        }

        @Override
        public void run() {

            try {
                // 클라이언트로부터 데이터를 받는 스트림
                dataInputStream = new DataInputStream(userSocket.getInputStream());
                // 클라이언트로 데이터를 보내는 스트림
                dataOutputStream = new DataOutputStream(userSocket.getOutputStream());
            } catch (IOException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }

            while (true) {
                try {
                    String request = dataInputStream.readUTF();
                    // 클라이언트에서 전송된 메세지(방 생성, 방 나가기, 생성된 방에서 사용자 초대, 메세지 전송)
                    System.out.println("클라이언트에서 넘어온 데이터--" + request);

                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObject = (JSONObject) jsonParser.parse(request);// json스트링 -> json객체
                    String type = (String) jsonObject.get("type");

                    if (request == null) {
                        consoleLog("클라이언트로부터 연결 끊김");
//						doQuit(userData, userLists);
                        break;
                    }

                    // 처음 채팅 서버 소켓에 접근해서 방에 세팅되는 경우
                    if ("join".equals(type)) {
                        setChatRoom(jsonObject, dataOutputStream);
                    } else if ("createuser".equals(type)) {
                        createUserData(jsonObject, dataOutputStream);
                    }
                    // 특정 사용자가 새롭게 방을 생성하는 경우
                    else if ("addroom".equals(type)) {
                        addChatRoom(jsonObject);
                    }
                    // 특정 사용자가 메세지를 보내는 경우
                    else if ("message".equals(type)) {
                        sendMessage(jsonObject, request);
                    }
                    // 이미지를 전송한 경우
                    else if ("image".equals(type)) {
                        sendImage(request, jsonObject);
                    } else if ("check".equals(type)) {
                        sendCheckMessage(jsonObject, request);
                    } else if ("enter".equals(type)) {
                        notifyPraricipantEntered(jsonObject, request);
                    }
                    // 특정 사용자가 방을 나가는 경우
                    else if ("exit".equals(type)) {
                        exitChatRoom(jsonObject, request);
                    }
                    // 이미 존재하는 채팅방에 특정 사용자를 추가한 경우
                    else if ("added".equals(type)) {
                        addParticipant(jsonObject, request);
                    }
                    // 특정 사용자가 방을 만들고 채팅을 보내지 않고 그냥 나왔을 때 만들어진 채팅 방을 없애는 경우.
                    else if ("removechatroom".equals(type)) {
                        removeChatRoom(jsonObject);
                    }
                    //수신자에게 영상통화 요청을 보내는 경우
                    else if ("requestFaceChat".equals(type)) {
                        sendFaceChatRequest(request, jsonObject);
                    }
                    //영상통화 거절의사를 발신자에게 보내는 경우
                    else if ("declineFaceChat".equals(type)) {
                        declineFaceChat(request, jsonObject);
                    }
                    //영상통화 수락의사를 발신자에게 보내는 경우
                    else if ("acceptFaceChat".equals(type)) {
                        acceptFaceChat(request, jsonObject);
                    }
                    //발신자가 통화를 걸었다가 통화를 취소하는 경우
                    else if ("cancelFaceChat".equals(type)) {
                        cancelFaceChat(request, jsonObject);
                    }
                    //한명이 통화 연결을 끊었을 때
                    else if ("disconnectFaceChat".equals(type)) {
                        disconnectFaceChat(jsonObject);
                    }

                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                    consoleLog(this.userId + "님이 채팅방을 나갔습니다.");
                    // 스레드 종료
                    quitSocketServer(this.userId);
                    try {
                        this.dataInputStream.close();
                        this.dataOutputStream.close();
                        this.userSocket.close();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void createUserData(JSONObject jsonObject, DataOutputStream dataOutputStream) {
            String userId = (String) jsonObject.get("account");
            String userNickname = (String) jsonObject.get("nickname");
            String userProfile = (String) jsonObject.get("profile");
            // 소켓 스레드의 주인 계정을 초기화해준다.
            this.userId = userId;
            if (!userDataLists.containsKey(userId)) {
                // 사용자 데이터 객체 생성
                UserData userData = new UserData(userSocket, userId, userNickname, userProfile, dataOutputStream, false);
                userDataLists.put(userId, userData);
                System.out.println("소켓 서버에 " + userId + "의 정보가 추가되었습니다.");
            }

        }

        // 메세지를 확인했다는 사실을 방의 모든 사용자들에게 전달해주기 위한 메소드
        private void sendCheckMessage(JSONObject jsonObject, String request) {
            int roomNum = ((Long) jsonObject.get("roomNum")).intValue();
            ArrayList<UserData> userLists = chatRoomLists.get(roomNum).userLists;
            for (int i = 0; i < userLists.size(); i++) {
                try {
                    userLists.get(i).dataOutputStream.writeUTF(request);
                    userLists.get(i).dataOutputStream.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        private synchronized void addChatRoom(JSONObject jsonObject) {
            // 새로운 채팅방의 참여자로 넘어온 사용자의 소켓이 존재하는지를 파악해야 한다. 존재하지 않으면 어차피 채팅서버에 연결되어있지 않은 것이기
            // 때문에 따로 추가해주지 않는다.
            // 그 사용자가 추후에 서버 소켓에 접근을 하면 그때 join토큰을 통해서 서버 소켓과 연결되는 것이기 때문에 문제가 없다.
            int roomNum = ((Long) jsonObject.get("roomNum")).intValue();// 방 번호
            // 방에 참석하게 되는 사용자들의 데이터 리스트
            JSONArray participantList = (JSONArray) jsonObject.get("participantList");

            if (chatRoomLists.get(roomNum) == null) {
                ArrayList<UserData> userLists = new ArrayList<>();// 사용자 정보를 담는 리스트 생성
                // 사용자들의 데이터를 UserData객체에 담은 후 userLists리스트에 넣어준다.
                for (int i = 0; i < participantList.size(); i++) {
                    // json배열에는 그 방의 참여자들의 데이터가 json객체 형태로 들어있다.
                    JSONObject participantData = (JSONObject) participantList.get(i);
                    String userId = (String) participantData.get("account");// 사용자 계정
                    String userNickname = (String) participantData.get("nickname");// 사용자 닉네임
                    String userProfile = (String) participantData.get("profile");
                    ;// 사용자 프로필사진 명
                    // 초대된 사용자가 현재 소켓 서버에 등록된 경우에만 추가해준다. 그렇지 않으면 nullpointerexception발생
                    if (userDataLists.get(userId) != null) {
                        UserData userData = userDataLists.get(userId);
                        userLists.add(userData);
                    }
                }
                ChatRoom chatRoom = new ChatRoom(roomNum, userLists);// 채팅방 객체 생성
                chatRoomLists.put(roomNum, chatRoom);// 해시맵에 채팅방 객체를 넣어준다.
                System.out.println(chatRoomLists.get(roomNum).roomNum + "번 채팅방이 생성되었습니다.");
                for (int i = 0; i < chatRoomLists.get(roomNum).userLists.size(); i++) {
                    System.out.println(chatRoomLists.get(roomNum).roomNum + "번 채팅방에 사용자"
                            + chatRoomLists.get(roomNum).userLists.get(i).userId + "가 추가되었습니다.");
                }
            }
        }

        private synchronized void sendMessage(JSONObject jsonObject, String request) {
            // 특정 클라이언트가 작성한 채팅 내용을 모든 사용자에게 보내준다.
            String data = request;
            broadcast(data, jsonObject);
        }

        private void sendImage(String request, JSONObject jsonObject) {
            String data = request;
            broadcast(data, jsonObject);
        }

        // 채팅방에 사용자가 접근했음을 다른 사용자들에게 알려서 채팅을 확인했음을 알게 해준다.
        private void notifyPraricipantEntered(JSONObject jsonObject, String request) {
            String data = request;
            broadcast(data, jsonObject);
        }

        // 클라이언트가 접속하면 해당 클라이언트의 닉네임을 정의해주고 클라이언트 리스트에 추가를 해준다.
        private synchronized void setChatRoom(JSONObject jsonObject, DataOutputStream dataOutputStream) {

            // 사용자 계정
            String userId = (String) jsonObject.get("account");
            UserData userData = userDataLists.get(userId);
            // 채팅방 번호를 담은 json배열을 가져온다.
            JSONArray jsonArray = (JSONArray) jsonObject.get("roomNumList");
            for (int i = 0; i < jsonArray.size(); i++) {
                int roomNum = ((Long) jsonArray.get(i)).intValue();
                // 채팅방이 존재하지 않는 경우 새롭게 채팅방을 생성
                if (chatRoomLists.get(roomNum) == null) {
                    // 채팅방의 사용자 리스트
                    ArrayList<UserData> userLists = new ArrayList<>();
                    // 사용자 리스트에 넘어온 사용자 데이터 추가
                    userLists.add(userData);
                    // 채팅방 객체 생성
                    ChatRoom chatRoom = new ChatRoom(roomNum, userLists);
                    // 해시맵에 채팅방 객체를 넣어준다.
                    chatRoomLists.put(roomNum, chatRoom);

                    System.out.println("새로운 채팅방" + roomNum + "이 생성되었습니다.");

                }

                // 이미 채팅방이 존재하는 경우 그 채팅방에 사용자만 추가해준다.
                else {
                    // 채팅방에 해당 사용자가 존재하지 않는 경우에만 추가해준다.
                    int size = chatRoomLists.get(roomNum).userLists.size();
                    int overLapCount = 0;
                    ArrayList<UserData> userLists = chatRoomLists.get(roomNum).userLists;
                    for (int j = 0; j < size; j++) {
                        if (userLists.get(j).userId.equals(userId)) {
                            overLapCount += 1;
                        }
                    }
                    // 중복카운트가 증가하지 않았어야 해당 채팅방에 방금 넘어온 사용자가 없다는 것이기 때문에 새롭게 사용자를 추가해준다.
                    if (overLapCount == 0) {
                        chatRoomLists.get(roomNum).userLists.add(userData);
                        System.out.println(roomNum + "번 채팅방 리스트에 등록되었습니다.");
                    } else {
                        System.out.println("이미 " + roomNum + "번 채팅방 리스트에 등록되어있습니다.");
                    }

                }
            }

        }

        // 같은 방의 클라이언트에게 메세지 내용 전달을 해주는 메소드
        private synchronized void broadcast(String data, JSONObject jsonObject) {
            int roomNum = ((Long) jsonObject.get("roomNum")).intValue();
            String myId = (String) jsonObject.get("account");

            // 자신 제외 && 같은 방에 있는 사용자에게만 메세지 전송
            int size = chatRoomLists.get(roomNum).userLists.size();
            ArrayList<UserData> userLists = chatRoomLists.get(roomNum).userLists;
            for (int i = 0; i < size; i++) {
                if (!userLists.get(i).userId.equals(myId)) {
                    try {
                        userLists.get(i).dataOutputStream.writeUTF(data);
                        userLists.get(i).dataOutputStream.flush();

                        System.out.println(userLists.get(i).userId + "에게 메세지를 전송했습니다.");
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        private synchronized void removeChatRoom(JSONObject jsonObject) {
            int roomNum = ((Long) jsonObject.get("roomNum")).intValue();// 삭제할 채팅방 번호
            // 채팅방을 삭제해준다.
            chatRoomLists.remove(roomNum);
            System.out.println(roomNum + "번 채팅방이 삭제되었습니다.");
        }

        // 동시에 여러 사용자가 나갈때의 혼선을 방지하기 위해서 synchronized를 통해 동기적으로 처리를 해준다.
        private synchronized void exitChatRoom(JSONObject jsonObject, String request) {

            // 모든 클라이언트에게 해당 내용을 보내준다.
            String data = request;
            broadcast(data, jsonObject);

            int roomNum = ((Long) jsonObject.get("roomNum")).intValue();
            String exitId = (String) jsonObject.get("account");
            // 클라이언트 리스트에서 퇴장한 클라이언트의 정보를 지워준다.
            int size = chatRoomLists.get(roomNum).userLists.size();
            ArrayList<UserData> userLists = chatRoomLists.get(roomNum).userLists;
            for (int i = 0; i < size; i++) {
                if (userLists.get(i).userId.equals(exitId)) {
                    // 채팅방의 해당 사용자 데이터를 삭제한다.
                    chatRoomLists.get(roomNum).userLists.remove(i);
                    System.out.println(exitId + "님이 " + roomNum + "번 방에서 나가셨습니다.");
                    break;
                }
            }
        }

        // 이미 존재하는 채팅방에서 새로운 사용자를 추가한 경우 실행되는 메소드
        private synchronized void addParticipant(JSONObject jsonObject, String request) {
            // 새로운 참여자로 넘어온 사용자의 소켓이 존재하는지를 파악해야 한다. 존재하지 않으면 어차피 채팅서버에 연결되어있지 않은 것이기
            // 때문에 따로 추가해주지 않는다.
            // 그 사용자가 추후에 서버 소켓에 접근을 하면 그때 join토큰을 통해서 서버 소켓과 연결되는 것이기 때문에 문제가 없다.
            int roomNum = ((Long) jsonObject.get("roomNum")).intValue();// 방 번호

            // 새로운 사용자가 추가되었다는 사실을 새로운 사용자를 채팅방에 추가하기 전에 먼저 그 방에 속한 사용자들에게 먼저 알린다.
            String data = request;
            broadcast(data, jsonObject);

            // 새롭게 참석하게 되는 사용자들의 닉네임 리스트
            JSONArray addedParticipantList = (JSONArray) jsonObject.get("addedParticipantList");
            // 먼저 넘어온 사용자가 소켓 서버에 등록되어있는지 파악한다.
            for (String userId : userDataLists.keySet()) {
                for (int i = 0; i < addedParticipantList.size(); i++) {
                    JSONObject participantData = (JSONObject) addedParticipantList.get(i);
                    String account = (String) participantData.get("account");
                    // 사용자 데이터 중에서 추가된 사용자 데이터가 존재하는 경우
                    if (userDataLists.get(userId).userId.equals(account)) {
                        // 채팅방 데이터에 그 사용자를 추가해준다.
                        UserData userData = userDataLists.get(userId);
                        chatRoomLists.get(roomNum).userLists.add(userData);
                        System.out.println(roomNum + "번 방에 " + userData.userNickname + "님이 추가되었습니다.");
                        break;
                    }
                }
            }
        }

        //영상 통화 요청을 수신자 클라이언트에게 보내는 메소드
        private void sendFaceChatRequest(String request, JSONObject jsonObject) {
            String receiver = (String) jsonObject.get("receiver");//영상통화의 수신자
            String sender = (String) jsonObject.get("account");//영상통화의 발신자
            String roomName = (String) jsonObject.get("roomName");//방 제목
            if (userDataLists.get(receiver) != null) {//소켓 서버에 수신자의 클라이언트 데이터가 등록되어 있는 경우
                if (userDataLists.get(receiver).isFaceChatting) {//통화중인 경우
                    //발신자에게 통화중 메세지를 response해준다.
                    JSONObject isFaceChattingResponse = new JSONObject();
                    isFaceChattingResponse.put("type", "isFaceChatting");
                    isFaceChattingResponse.put("receiver", receiver);
                    try {
                        userDataLists.get(sender).dataOutputStream.writeUTF(isFaceChattingResponse.toJSONString());
                        userDataLists.get(sender).dataOutputStream.flush();
                        System.out.println("isFaceChattingResponse");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {//통화중이 아닌 경우
                    try {
                        userDataLists.get(receiver).isFaceChatting = true;//수신자를 통화중 상태로 전환
                        userDataLists.get(sender).isFaceChatting = true;//발신자를 통화중 상태로 전환
                        userDataLists.get(receiver).dataOutputStream.writeUTF(request);
                        userDataLists.get(receiver).dataOutputStream.flush();
                        JSONObject successResponse = new JSONObject();
                        successResponse.put("type", "successFaceChatRequest");
                        successResponse.put("roomName", roomName);
                        successResponse.put("receiverAccount", receiver);//수신자 계정
                        successResponse.put("receiverNickname", userDataLists.get(receiver).userNickname);//수신자 닉네임
                        successResponse.put("receiverProfile", userDataLists.get(receiver).userProfile);//수신자 프로필
                        userDataLists.get(sender).dataOutputStream.writeUTF(successResponse.toJSONString());//발신자에게는 수신자에게 요청을 성공적으로 했음을 알린다.
                        userDataLists.get(sender).dataOutputStream.flush();
                        System.out.println("successFaceChatRequest");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else {//소켓 서버에 수신자의 클라이언트 데이터가 등록되어있지 않아서 요청이 불가한 경우
                //발신자에게 연결 실패 메세지를 response해준다.
                JSONObject failResponse = new JSONObject();
                failResponse.put("type", "failFaceChatRequest");
                failResponse.put("receiver", receiver);
                try {
                    userDataLists.get(sender).dataOutputStream.writeUTF(failResponse.toJSONString());
                    userDataLists.get(sender).dataOutputStream.flush();
                    System.out.println("failFaceChatResponse");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //수신자가 영상통화 수락 의사를 발신자에게 전달하는 메소드
        private void acceptFaceChat(String request, JSONObject jsonObject) {
            String sender = (String) jsonObject.get("sender");
            try {
                userDataLists.get(sender).dataOutputStream.writeUTF(request);
                userDataLists.get(sender).dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //발신자에게 통화 거절 의사를 전달하는 메소드
        private void declineFaceChat(String request, JSONObject jsonObject) {
            String receiver = (String) jsonObject.get("receiver");//거절 메세지의 발신자(영상통화의 요청의 수신자)
            String sender = (String) jsonObject.get("sender");//거절 메세지의 수신자(영상통화 요청의 발신자)
            userDataLists.get(receiver).isFaceChatting = false;//통화중 상태 false
            userDataLists.get(sender).isFaceChatting = false;//통화중 상태 false
            try {
                userDataLists.get(receiver).dataOutputStream.writeUTF(request);
                userDataLists.get(receiver).dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //발신자가 영상통화에 진입하기 전에 통화를 취소하는 경우 호출되는 메소드
        private void cancelFaceChat(String request, JSONObject jsonObject) {
            String sender = (String) jsonObject.get("sender");//발신자
            String receiver = (String) jsonObject.get("receiver");//수신자
            userDataLists.get(receiver).isFaceChatting = false;//통화중 상태 false
            userDataLists.get(sender).isFaceChatting = false;//통화중 상태 false
            try {
                userDataLists.get(receiver).dataOutputStream.writeUTF(request);
                userDataLists.get(receiver).dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void disconnectFaceChat(JSONObject jsonObject) {
            String sender = (String) jsonObject.get("sender");//발신자
            String receiver = (String) jsonObject.get("receiver");//수신자
            userDataLists.get(sender).isFaceChatting = false;//통화중 상태 false
            userDataLists.get(receiver).isFaceChatting = false;//통화중 상태 false
        }


        // 소켓 서버에서 나가는 경우(어플을 종료하거나 강제종료됐을 때 사용자 리스트에서 제거해주는 메소드)
        // 이 메소드로 사용자를 없애주지 않으면 이후 다시 접속할 때 이미 사용자 리스트에 존재를 해서 새로운 스트림이 형성되지 않아 Broken
        // Pipe exception이 발생한다.
        public void quitSocketServer(String userId) {
            String quitId = userId;
            // 소켓서버에서 나간 사용자에 대한 데이터를 먼저 지워준다.
            userDataLists.remove(quitId);
            System.out.println(quitId + "의 데이터를 소켓서버에서 지웠습니다.");
            // 모든 방을 돌면서 나간 사용자를 찾아낸다.
            for (Integer key : chatRoomLists.keySet()) {
                // 각 방에 참여하고 있는 사용자 리스트의 size
                int size = chatRoomLists.get(key).userLists.size();
                for (int i = 0; i < size; i++) {
                    // 사용자 리스트에 나가려는 사용자가 있으면 그 사용자만 지워준다.
                    if (chatRoomLists.get(key).userLists.get(i).userId.equals(quitId)) {
                        chatRoomLists.get(key).userLists.remove(i);
                        System.out.println("사용자 " + quitId + "가 " + key + "번 방에서 나갔습니다.");
                        break;//break를 해주지 않으면 리스트의 사이즈가 줄어든 상태에서 삭제 전의 리스트 index를 참조하게 되어서 AIOE오류가 나타남!!!
                    }
                }
            }
        }

        private void consoleLog(String log) {
            System.out.println(log);
        }
    }

}
