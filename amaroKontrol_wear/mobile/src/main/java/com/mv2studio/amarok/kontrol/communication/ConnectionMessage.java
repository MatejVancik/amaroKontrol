package com.mv2studio.amarok.kontrol.communication;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by matej on 17.11.14.
 */
public class ConnectionMessage implements Parcelable {

    private String topLine;
    private String middleLine;
    private String bottomLine;

    public ConnectionMessage(String topLine, String middleLine, String bottomLine) {
        this.setTopLine(topLine);
        this.setMiddleLine(middleLine);
        this.setBottomLine(bottomLine);
    }


    public String getTopLine() {
        return topLine;
    }

    public void setTopLine(String topLine) {
        this.topLine = topLine;
    }

    public String getMiddleLine() {
        return middleLine;
    }

    public void setMiddleLine(String middleLine) {
        this.middleLine = middleLine;
    }

    public String getBottomLine() {
        return bottomLine;
    }

    public void setBottomLine(String bottomLine) {
        this.bottomLine = bottomLine;
    }

    protected ConnectionMessage(Parcel in) {
        topLine = in.readString();
        middleLine = in.readString();
        bottomLine = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(topLine);
        dest.writeString(middleLine);
        dest.writeString(bottomLine);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ConnectionMessage> CREATOR = new Parcelable.Creator<ConnectionMessage>() {
        @Override
        public ConnectionMessage createFromParcel(Parcel in) {
            return new ConnectionMessage(in);
        }

        @Override
        public ConnectionMessage[] newArray(int size) {
            return new ConnectionMessage[size];
        }
    };
}