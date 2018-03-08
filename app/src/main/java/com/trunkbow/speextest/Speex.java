package com.trunkbow.speextest;

/**
 * Created by ${fan} on 2018/2/27.
 */

public class Speex {

    private static final int DEFAULT_COMPRESSION = 8;


    static
    {
        System.loadLibrary("speex");
    }

    public void init() {

        open(DEFAULT_COMPRESSION);

    }


    /**
    * SPEEX_SET_QUALITY宏(0x0004)：设置Speex编码器用固定采样频率对音频数据编码时的质量等级。
     * 质量等级越高，音质越好，压缩率越低。ptr参数为spx_int32_t型变量的内存指针，最低为0，最高为10，
     * 默认为8。本参数仅对Speex编码器有效
    */
    public native int open(int compression);

    public native int getFrameSize();


    //encoded要解码的数据（speex）,lin解码后输出数据（pcm）,size :encoded.length  (我的理解)
    public native int decode(byte encoded[], short lin[], int size);

    //lin pcm数据，encoded 编码后speex数据 size:lin.length  (我的理解)
    public native int encode(short lin[], int offset, byte encoded[], int size);

    public native void close();
}
