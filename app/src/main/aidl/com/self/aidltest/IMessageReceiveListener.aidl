// IMessageReceiveListener.aidl
package com.self.aidltest;
import com.self.aidltest.enty.Message;
// Declare any non-default types here with import statements
//消息接收 ,不是基本数据类型都要加inout
interface IMessageReceiveListener {
    void onReceiveMessage(in Message message);
}
