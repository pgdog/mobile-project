package com.example.comp90018.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.comp90018.R;
import com.example.comp90018.adapter.ChatListAdapter;
import com.example.comp90018.dataBean.ChatItem;
import com.example.comp90018.dataBean.MessageItem;
import com.example.comp90018.utils.DataManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    //Views
    private TextView titleText;
    private Button backBtn;
    private RecyclerView recyclerView;
    private EditText inputText;
    private Button addBtn;
    private Button sendBtn;
    private String friendId, userId;
    private String friendPic;
    private String friendName;
    DatabaseReference mDatabaseRef;

    ChatListAdapter chatListAdapter;

    private DataManager dataManager;
    private List<ChatItem> chatItems;

    private boolean isFirstListen=true;
    private boolean haveNewMessage=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Cancel the title
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();

        }
        //Bind the layout
        setContentView(R.layout.activity_chat);

        //Initialize data
        initData();
    }

    public void initData() {
        dataManager = DataManager.getDataManager(this);
        chatItems = new ArrayList<ChatItem>();
        //Get all data here
        Intent intent = getIntent();
        friendId = intent.getStringExtra(MainViewActivity.VALUES_FRIEND_ID);
        friendPic = intent.getStringExtra("Picture");
        friendName = intent.getStringExtra("Name");
        userId = DataManager.getDataManager(getApplicationContext()).getUser().getID();
        //DataManager.getDataManager(this).createItemsForChat(friendID);
        //DataManager.getDataManager(this).setMessageRead(friendID);
        mDatabaseRef = DataManager.getDataManager(getApplicationContext()).getDatabaseReference();
        /*Log.i("test profile", "friend uid is "+friendId);
        Log.i("test profile", "user uid is "+ userId);
        Log.i("test profile", "friend photo is "+friendPhoto);
        Log.i("test profile", "user photo is "+ userPhoto);*/

        //get the chat history records from firebase
        mDatabaseRef.child("message").child(userId).child("history").child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Get the message send by the friend
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatItem item = new ChatItem();
                    item.setId(friendId);
                    item.setSelf(false);
                    item.setText(dataSnapshot.child("text").getValue().toString());
                    item.setImage(friendPic);
                    item.setDate(new Long(dataSnapshot.child("date").getValue().toString()));
                    chatItems.add(item);
                }

                //Get the message send by the user
                mDatabaseRef.child("message").child(friendId).child("history").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            ChatItem item = new ChatItem();
                            item.setId(userId);
                            item.setSelf(true);
                            item.setText(dataSnapshot.child("text").getValue().toString());
                            item.setImage(dataManager.getUser().getImage());
                            item.setDate(new Long(dataSnapshot.child("date").getValue().toString()));
                            chatItems.add(item);
                        }
                        dataManager.setChatItems(chatItems);
                        //show the view
                        initView();
                        //Delete the unreade message from the friend
                        deleteUnreadMessage();
                        //Listen the new message
                        listenTheChat();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void initView() {
        //Create view here
        titleText = (TextView) findViewById(R.id.chat_title);
        backBtn = (Button) findViewById(R.id.chat_back_btn);
        recyclerView = (RecyclerView) findViewById(R.id.chat_recycler);
        inputText = (EditText) findViewById(R.id.chat_edit_text);
        addBtn = (Button) findViewById(R.id.chat_add_btn);
        sendBtn = (Button) findViewById(R.id.chat_send_btn);

        titleText.setText(friendName);
        chatListAdapter = new ChatListAdapter(DataManager.getDataManager(this).getChatItems());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setAdapter(chatListAdapter);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.scrollToPosition(chatListAdapter.getItemCount() - 1);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(haveNewMessage){
                    setResult(MainViewActivity.RESULT_CODE_FROM_CHAT_MESSAGE_CHANGED);
                }
                finish();
            }
        });

        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                if (text.length() == 0) {
                    sendBtn.setEnabled(false);
                } else {
                    sendBtn.setEnabled(true);
                }
            }
        });

        //hide the keyboard if touch the screen outside the keyboard
        inputText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    InputMethodManager manager = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));
                    if (manager != null)
                        manager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check if the friend exist
                mDatabaseRef.child("users").child(userId).child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean friendFound=false;
                        for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                            if(dataSnapshot.getValue().toString().equals(friendId)){
                                friendFound=true;
                                break;
                            }
                        }
                        if(friendFound){
                            haveNewMessage=true;
                            String text = inputText.getText().toString();
                            ChatItem item=addDataToList(text, true);
                            addToReceiver(text,item.getDate());
                            addToRencentChatList(text,item.getDate());
                            addToFirebaseRencentChat(text,item.getDate());
                            Log.i("test date", "date is " + new Date(System.currentTimeMillis()));
                            //hide the keyboard and clear the EditText
                            inputText.setText("");
                            InputMethodManager manager = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));
                            if (manager != null)
                                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                            updateListView();
                        }else{
                            Toast.makeText(getApplicationContext(), "This user is not your friend, can't send message to him.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }
        });
    }

    /**
     * add the friend to the recent chat list
     */
    public void addToRencentChatList(String text,long date){
        MessageItem messageItem=null;
        for(MessageItem item:dataManager.getMessageItems()){
            if(item.getID().equals(friendId)){
                messageItem=item;
                break;
            }
        }
        if(messageItem==null){
            messageItem=new MessageItem(friendId,friendPic,friendName,text,0,date);
            dataManager.getMessageItems().add(messageItem);
        }else{
            messageItem.setLastMessage(text);
            messageItem.setLastMessageDate(date);
        }
    }

    public void addToFirebaseRencentChat(String text,Long date){
        //Add the message to the user's recent chat
        mDatabaseRef.child("chat").child(userId).child(friendId).child("date").setValue(String.valueOf(date));
        mDatabaseRef.child("chat").child(userId).child(friendId).child("text").setValue(text);

        //Add the message to the friend's recent chat
        mDatabaseRef.child("chat").child(friendId).child(userId).child("date").setValue(String.valueOf(date));
        mDatabaseRef.child("chat").child(friendId).child(userId).child("text").setValue(text);
    }

    public void deleteUnreadMessage(){
        mDatabaseRef.child("message").child(userId).child("unread").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    String id=dataSnapshot.child("from").getValue().toString();
                    if(id.equals(friendId)){
                        dataSnapshot.getRef().setValue(null);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    /**
     * Add a new data to chat list
     * @param text
     * @param isSelf whether the text is from the user
     * @return the data of ChatItem
     */
    public ChatItem addDataToList(String text, boolean isSelf) {
        ChatItem item = new ChatItem();
        item.setText(text);
        item.setSelf(isSelf);

        item.setDate(System.currentTimeMillis());
        if (isSelf) {
            //The text is from the user, add his id and picture
            item.setId(userId);
            item.setImage(dataManager.getUser().getImage());
        } else {
            //The text is from the friend, add the friend's id and picture
            item.setId(friendId);
            item.setImage(friendPic);
        }
        chatListAdapter.addItem(item);
        return item;
    }

    /**
     * update the List view
     */
    public void updateListView() {
        //update the RecyclerView
        chatListAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(chatListAdapter.getItemCount() - 1);
    }

//    public void addToSender(String text){
//        String key = mDatabaseRef.child("message").child(userId).child(friendId).push().getKey();
//        mDatabaseRef.child("message").child(userId).child(friendId).child(key).child("text").setValue(text);
//        mDatabaseRef.child("message").child(userId).child(friendId).child(key).child("uid").setValue(userId);
//        mDatabaseRef.child("message").child(userId).child(friendId).child(key).child("date").setValue(new Date(System.currentTimeMillis()).toString());
//        mDatabaseRef.child("message").child(userId).child(friendId).child(key).child("hasRead").setValue("0");
//    }

    public void listenTheChat() {
        mDatabaseRef.child("message").child(userId).child("history").child(friendId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(isFirstListen){
                    isFirstListen=false;
                    return;
                }
                //A new message from the friend
                DataSnapshot lastMessage = null;
                //Find it, it's the last one
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    lastMessage = dataSnapshot;
                }
                //Add it to local and update the view
                if (lastMessage != null) {
                    ChatItem item = new ChatItem();
                    item.setId(friendId);
                    item.setSelf(false);
                    String text=lastMessage.child("text").getValue().toString();
                    item.setText(text);
                    item.setImage(friendPic);
                    long date=new Long(lastMessage.child("date").getValue().toString());
                    item.setDate(date);
                    chatListAdapter.addItem(item);

                    haveNewMessage=true;
                    //update the view
                    updateListView();

                    //delete it from unread message
                    deleteUnreadMessage();

                    //add it to recent chat
                    addToRencentChatList(text,date);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void addToReceiver(String text,Long date) {
        String unReadKey = mDatabaseRef.child("message").child(friendId).child("unread").push().getKey();
        mDatabaseRef.child("message").child(friendId).child("unread").child(unReadKey).child("text").setValue(text);
        mDatabaseRef.child("message").child(friendId).child("unread").child(unReadKey).child("from").setValue(userId);
        mDatabaseRef.child("message").child(friendId).child("unread").child(unReadKey).child("date").setValue(String.valueOf(date));

        String historyKey = mDatabaseRef.child("message").child(friendId).child("history").child(userId).push().getKey();
        mDatabaseRef.child("message").child(friendId).child("history").child(userId).child(historyKey).child("text").setValue(text);
        mDatabaseRef.child("message").child(friendId).child("history").child(userId).child(historyKey).child("date").setValue(String.valueOf(date));
    }
}