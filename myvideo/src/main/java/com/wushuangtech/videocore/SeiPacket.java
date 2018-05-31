package com.wushuangtech.videocore;

/**
 * Created by Administrator on 2018/2/2.
 */

public class SeiPacket {
    static int UUID_SIZE=16;
    static byte uuid[] = {0x6D, 0x6F, 0x6D, 0x6F, 0x61, 0x39, 0x61, 0x34, 0x32, 0x37, 0x64, 0x31, 0x77, 0x65, 0x69, 0x6C};
    static public int get_sei_nalu_size(int content)
    {
        //SEI payload size
        int sei_payload_size = content+ UUID_SIZE+2;
        //NALU + payload类型 + 数据长度 + 数据
        int sei_size = 1 + 1 + (sei_payload_size / 0xFF + (sei_payload_size % 0xFF != 0 ? 1 : 0)) + sei_payload_size;
        //截止码
        int tail_size = 2;
        if (sei_size % 2 == 1)
        {
            tail_size -= 1;
        }
        sei_size += tail_size;
        return sei_size;
    }

    static public int get_sei_packet_size(int size)
    {
        return get_sei_nalu_size(size) + 4;
    }

    static public int fill_sei_packet(byte[] packet, int isAnnexb, byte[] content, int size)
    {
        int index=0;
        int nalu_size = get_sei_nalu_size(size);
        int sei_size = nalu_size;
        //NALU开始码
        if (isAnnexb==0)
        {
            packet[0]=(byte)((sei_size>>24)&0xFF);
            packet[1]=(byte)((sei_size>>16)&0xFF);
            packet[2]=(byte)((sei_size>>8)&0xFF);
            packet[3]=(byte)((sei_size)&0xFF);
        }
        else
        {
            packet[0]=0;
            packet[1]=0;
            packet[2]=0;
            packet[3]=1;
        }
        index = 4;
        packet[index++]=6;
        packet[index++]=5;
        int sei_payload_size = size + UUID_SIZE+2;
        //数据长度
        while (sei_payload_size >0)
        {
            packet[index++]=(byte)(sei_payload_size >= 0xFF ? 0xFF : sei_payload_size);
            if (sei_payload_size < 0xFF){
                sei_payload_size=0;
            }
            else{
                sei_payload_size -= 0xFF;
            }
        }
        System.arraycopy(uuid,0,packet,index, UUID_SIZE);
        index+=UUID_SIZE;

        packet[index++]=(byte)((size>>8)&0xFF);
        packet[index++]=(byte)(size&0xFF);
        //数据
        System.arraycopy(content,0,packet,index, size);
        index += size;

        sei_size+=4;
        //tail 截止对齐码
        if (sei_size-index==1)
        {
            packet[index] = (byte)0x80;
        }
        else if (sei_size-index == 2)
        {
            packet[index++] = 0x00;
            packet[index++] = (byte)0x80;
        }

        return 1;
    }
}
