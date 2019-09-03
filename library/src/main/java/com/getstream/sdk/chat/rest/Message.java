package com.getstream.sdk.chat.rest;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.getstream.sdk.chat.AttachmentListConverter;
import com.getstream.sdk.chat.CommandInfoConverter;
import com.getstream.sdk.chat.DateConverter;
import com.getstream.sdk.chat.ExtraDataConverter;
import com.getstream.sdk.chat.MessageStatusConverter;
import com.getstream.sdk.chat.ReactionCountConverter;
import com.getstream.sdk.chat.ReactionListConverter;
import com.getstream.sdk.chat.UserListConverter;
import com.getstream.sdk.chat.enums.MessageStatus;
import com.getstream.sdk.chat.interfaces.UserEntity;
import com.getstream.sdk.chat.model.Attachment;
import com.getstream.sdk.chat.model.Reaction;
import com.getstream.sdk.chat.utils.Utils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

/**
 * A message
 */

@Entity(tableName = "stream_message")
public class Message implements UserEntity {
    @SerializedName("id")
    @Expose
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @SerializedName("text")
    @Expose
    private String text;

    @SerializedName("html")
    @Expose
    private String html;

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("user")
    @Expose
    @Embedded(prefix = "user_")
    private User user;

    @SerializedName("attachments")
    @Expose
    @TypeConverters(AttachmentListConverter.class)
    private List<Attachment> attachments;

    @SerializedName("latest_reactions")
    @Expose
    @TypeConverters(ReactionListConverter.class)
    private List<Reaction> latestReactions;

    @SerializedName("own_reactions")
    @Expose
    @TypeConverters(ReactionListConverter.class)
    private List<Reaction> ownReactions;

    @SerializedName("reply_count")
    @Expose
    private int replyCount;

    @SerializedName("created_at")
    @Expose
    @TypeConverters({DateConverter.class})
    private Date createdAt;

    @SerializedName("updated_at")
    @Expose
    @TypeConverters({DateConverter.class})
    private Date updatedAt;

    @SerializedName("deleted_at")
    @Expose
    @TypeConverters({DateConverter.class})
    private Date deletedAt;

    @SerializedName("mentioned_users")
    @Expose
    @TypeConverters(UserListConverter.class)
    private List<User> mentionedUsers;

    @SerializedName("reaction_counts")
    @Expose
    @TypeConverters(ReactionCountConverter.class)
    private Map<String, Integer> reactionCounts;

    @SerializedName("parent_id")
    @Expose
    private String parentId;

    @SerializedName("command")
    @Expose
    private String command;

    @SerializedName("command_info")
    @Expose
    @TypeConverters(CommandInfoConverter.class)
    private Map<String, String> commandInfo;

    public MessageStatus getStatus() {
        return status;
    }

    @TypeConverters({MessageStatusConverter.class})
    private MessageStatus status = MessageStatus.RECEIVED;

    @Override
    public boolean equals(@Nullable Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }
        Message otherMessage = (Message) obj;
        if (!TextUtils.equals(this.getId(), otherMessage.getId())) {
            return false;
        }
        if (!Objects.equals(updatedAt, otherMessage.updatedAt)) {
            return false;
        }
        if (!Objects.equals(deletedAt, otherMessage.deletedAt)) {
            return false;
        }
        if (replyCount != otherMessage.replyCount) {
            return false;
        }
        return true;
    }

    public Message copy() {
        Message clone = new Message();
        clone.id = id;
        clone.text = text;
        clone.html = html;
        clone.type = type;
        clone.user = user;
        clone.attachments = attachments;
        clone.latestReactions = latestReactions;
        clone.ownReactions = ownReactions;
        clone.replyCount = replyCount;
        clone.updatedAt = new Date(updatedAt.getTime());
        clone.deletedAt = new Date(deletedAt.getTime());
        clone.mentionedUsers = mentionedUsers;
        clone.parentId = parentId;
        clone.command = command;
        clone.commandInfo = commandInfo;
        clone.status = status;
        return clone;
    }

    // Additional Params
    @TypeConverters(ExtraDataConverter.class)
    private Map<String, Object> extraData;
    private boolean isStartDay = false;
    private boolean isYesterday = false;
    private boolean isToday = false;

    private String date, time;

    // region Set Date and Time
    public static void setStartDay(List<Message> messages, @Nullable Message preMessage0) {
        if (messages == null) return;
        if (messages.size() == 0) return;

        Message preMessage = (preMessage0 != null) ? preMessage0 : messages.get(0);
        setFormattedDate(preMessage);
        int startIndex = (preMessage0 != null) ? 0 : 1;
        for (int i = startIndex; i < messages.size(); i++) {
            if (i != startIndex) {
                preMessage = messages.get(i - 1);
            }

            Message message = messages.get(i);
            setFormattedDate(message);
            message.setStartDay(!message.getDate().equals(preMessage.getDate()));
        }
    }

    private static void setFormattedDate(Message message) {
        if (message == null || message.getDate() != null) return;
        Utils.messageDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(message.getCreatedAt().getTime());

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "EEEE";

        DateFormat timeFormat = new SimpleDateFormat(timeFormatString, Utils.locale);
        DateFormat dateFormat1 = new SimpleDateFormat(dateTimeFormatString, Utils.locale);
        DateFormat dateFormat2 = new SimpleDateFormat("MMMM dd yyyy", Utils.locale);

        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            message.setToday(true);
            message.setDate("Today");
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            message.setYesterday(true);
            message.setDate("Yesterday");
        } else if (now.get(Calendar.WEEK_OF_YEAR) == smsTime.get(Calendar.WEEK_OF_YEAR)) {
            message.setDate(dateFormat1.format(message.getCreatedAt()));
        } else {
            message.setDate(dateFormat2.format(message.getCreatedAt()));
        }
        message.setTime(timeFormat.format(message.getCreatedAt()));
    }

    public static String convertDateToString(Date date) {
        Utils.messageDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String timeStr = Utils.messageDateFormat.format(date);
        return timeStr;
    }

    public static boolean isCommandMessage(Message message) {
        return message.getText().startsWith("/");
    }

    public static String differentTime(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) return null;
        Date lastActiveDate = null;
        try {
            lastActiveDate = Utils.messageDateFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        Date dateTwo = new Date();
        long timeDiff = Math.abs(lastActiveDate.getTime() - dateTwo.getTime()) / 1000;
        String timeElapsed = TimeElapsed(timeDiff);
        String differTime = "";
        if (timeElapsed.contains("Just now"))
            differTime = "Active: " + timeElapsed;
        else
            differTime = "Active: " + timeElapsed + " ago";

        return differTime;
    }

    public static String TimeElapsed(long seconds) {
        String elapsed;
        if (seconds < 60) {
            elapsed = "Just now";
        } else if (seconds < 60 * 60) {
            int minutes = (int) (seconds / 60);
            elapsed = String.valueOf(minutes) + " " + ((minutes > 1) ? "mins" : "min");
        } else if (seconds < 24 * 60 * 60) {
            int hours = (int) (seconds / (60 * 60));
            elapsed = String.valueOf(hours) + " " + ((hours > 1) ? "hours" : "hour");
        } else {
            int days = (int) (seconds / (24 * 60 * 60));
            elapsed = String.valueOf(days) + " " + ((days > 1) ? "days" : "day");
        }
        return elapsed;
    }

    public boolean isYesterday() {
        return isYesterday;
    }

    public void setYesterday(boolean yesterday) {
        isYesterday = yesterday;
    }

    public boolean isToday() {
        return isToday;
    }

    public void setToday(boolean today) {
        isToday = today;
    }

    public boolean isStartDay() {
        return isStartDay;
    }

    public void setStartDay(boolean startDay) {
        isStartDay = startDay;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isDelivered() {
        return status == MessageStatus.RECEIVED;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @TypeConverters(AttachmentListConverter.class)
    public List<Attachment> getAttachments() {
        if (attachments == null) {
            return new ArrayList<Attachment>();
        }
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public List<Reaction> getLatestReactions() {
        return latestReactions;
    }

    public void setLatestReactions(List<Reaction> latestReactions) {
        this.latestReactions = latestReactions;
    }

    public List<Reaction> getOwnReactions() {
        return ownReactions;
    }

    public void setOwnReactions(List<Reaction> ownReactions) {
        this.ownReactions = ownReactions;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }


    public List<User> getMentionedUsers() {
        return mentionedUsers;
    }

    public void setMentionedUsers(List<User> mentionedUsers) {
        this.mentionedUsers = mentionedUsers;
    }

    public Map<String, Integer> getReactionCounts() {
        return reactionCounts;
    }

    public void setReactionCounts(Map<String, Integer> reactionCounts) {
        this.reactionCounts = reactionCounts;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParent_id(String parentId) {
        this.parentId = parentId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Map<String, String> getCommandInfo() {
        return commandInfo;
    }

    public void setCommandInfo(Map<String, String> commandInfo) {
        this.commandInfo = commandInfo;
    }

    @Override
    public String getUserId() {
        if (user == null) {
            return null;
        }
        return user.getId();
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }
}
