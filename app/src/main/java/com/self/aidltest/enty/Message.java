package com.self.aidltest.enty;

import android.os.Parcel;
import android.os.Parcelable;

public class Message implements Parcelable {

	private String content;
	private boolean isSendSuccess;

	public Message(Parcel in) {
		content = in.readString();
		isSendSuccess = in.readByte() != 0;
	}

	public static final Creator<Message> CREATOR = new Creator<Message>() {
		@Override
		public Message createFromParcel(Parcel in) {
			return new Message(in);
		}

		@Override
		public Message[] newArray(int size) {
			return new Message[size];
		}
	};

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isSendSuccess() {
		return isSendSuccess;
	}

	public void setSendSuccess(boolean sendSuccess) {
		isSendSuccess = sendSuccess;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(content);
		dest.writeByte((byte) (isSendSuccess ? 1 : 0));
	}
}
