package com.example.comp90018.adapter;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp90018.dataBean.ChatItem;
import com.example.comp90018.dataBean.FriendItem;
import com.example.comp90018.dataBean.MessageItem;
import com.example.comp90018.R;
import com.example.comp90018.utils.DataManager;
import com.example.comp90018.utils.RecycleItemTouchHelper;
import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class ChatListAdapter extends RecyclerView.Adapter {
    // The data of the list
    private List<ChatItem> chatItems=new ArrayList<ChatItem>();

    public static final int MARGIN_TEXT_TO_PARENT=180;
    public static final int MARGIN_TEXT_TO_IMAGE=25;

    public static final int VIEW_HOLEDER_TYPE_SELF=0;
    public static final int VIEW_HOLEDER_TYPE_FRIEND=1;

    //The view for each item
    static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView imageAvatar;
        TextView chatText;
        RelativeLayout chatLayout;
        TextView timeText;
        ImageView messagePic;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar=(ImageView)itemView.findViewById(R.id.item_chat_image);
            chatText=(TextView)itemView.findViewById(R.id.item_chat_text);
            chatLayout=(RelativeLayout)itemView.findViewById(R.id.item_chat_text_layout);
            timeText=(TextView)itemView.findViewById(R.id.item_chat_time_text);
            messagePic=(ImageView)itemView.findViewById(R.id.item_chat_message_pic);
        }
    }


    public ChatListAdapter(List<ChatItem> chatItems){
        this.chatItems=chatItems;
        itemSort();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==VIEW_HOLEDER_TYPE_FRIEND){
            View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat,parent,false);
            ViewHolder holder=new ViewHolder(view);
            return holder;
        }else if(viewType==VIEW_HOLEDER_TYPE_SELF){
            View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_self,parent,false);
            ViewHolder holder=new ViewHolder(view);
            return holder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        //Bind the data
        if(position<chatItems.size()){
            final ChatItem item=chatItems.get(position);
            final ViewHolder myHolder=(ViewHolder)holder;
            Picasso.get().load(item.getImage()).into(myHolder.imageAvatar);
            if(item.getMessageType()==ChatItem.TYPE_TEXT){
                myHolder.chatText.setVisibility(View.VISIBLE);
                myHolder.chatText.setText(item.getText());
                myHolder.messagePic.setVisibility(View.GONE);
            }else if(item.getMessageType()==ChatItem.TYPE_PICTURE){
                myHolder.chatText.setVisibility(View.GONE);
                myHolder.messagePic.setVisibility(View.VISIBLE);
                Callback callback=new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        myHolder.messagePic.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                View largeImageView=LayoutInflater.from(view.getContext()).inflate(R.layout.large_image_layout,null);
                                final AlertDialog dialog = new AlertDialog.Builder(view.getContext()).create();
                                ImageView imageView=largeImageView.findViewById(R.id.large_imageView);
                                Picasso.get().load(item.getText()).into(imageView);
                                dialog.setView(largeImageView);
                                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                                dialog.show();
                                largeImageView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        dialog.cancel();
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                };
                if(position<=chatItems.size()/2){
                    Picasso.get().load(item.getText()).placeholder(R.drawable.pic_loading).error(R.drawable.pic_loading_failed).tag("Picture").priority(Picasso.Priority.LOW).into(myHolder.messagePic,callback );
                }else{
                    Picasso.get().load(item.getText()).placeholder(R.drawable.pic_loading).error(R.drawable.pic_loading_failed).tag("Picture").priority(Picasso.Priority.HIGH).into(myHolder.messagePic,callback );
                }

            }

            if(timeNeeded(chatItems,position)){
                myHolder.timeText.setText(transformDate(item.getDate()));
                myHolder.timeText.setVisibility(View.VISIBLE);
            }else{
                myHolder.timeText.setText("");
                myHolder.timeText.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(chatItems.get(position).isSelf()){
            return VIEW_HOLEDER_TYPE_SELF;
        }else{
            return VIEW_HOLEDER_TYPE_FRIEND;
        }
    }

    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    public void addItem(ChatItem item){
        chatItems.add(item);
        notifyDataSetChanged();
    }

    public String transformDate(long date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
        String dateStr = dateFormat.format(date);
        String currentDate = dateFormat.format(new Date(System.currentTimeMillis()));
        String[] dateStrs = dateStr.split("-");
        String[] currentStrs = currentDate.split("-");
        if (dateStrs[0].equals(currentStrs[0]) && dateStrs[1].equals(currentStrs[1]) && dateStrs[2].equals(currentStrs[2])) {
            return dateStrs[3] + ":" + dateStrs[4];
        } else {
            DateFormat newDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm");
            return  newDateFormat.format(date);
        }
    }

    public boolean timeNeeded(List<ChatItem> chatItems, int position){
        if(position==0){
            return true;
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
        String lastDate=dateFormat.format(chatItems.get(position-1).getDate());
        String date=dateFormat.format(chatItems.get(position).getDate());
        String[] lastStrs=lastDate.split("-");
        String[] dateStrs=date.split("-");
        if(lastStrs[0].equals(dateStrs[0]) && lastStrs[1].equals(dateStrs[1]) && lastStrs[2].equals(dateStrs[2]) && lastStrs[3].equals(dateStrs[3]) &&
        Integer.parseInt(dateStrs[4])-Integer.parseInt(lastStrs[4])<=5){
            return false;
        }
        return true;
    }

    /**
     * sort the item by date
     */
    private void itemSort(){
        Collections.sort(chatItems, new Comparator<ChatItem>() {
            @Override
            public int compare(ChatItem chatItem, ChatItem t1) {
                if(chatItem.getDate()==t1.getDate()){
                    return 0;
                }else if(chatItem.getDate()<t1.getDate()){
                    return -1;
                }else{
                    return 1;
                } }
        });
    }
}
