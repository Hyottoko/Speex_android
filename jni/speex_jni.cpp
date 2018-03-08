#include <jni.h>



#include "speex/speex.h"

#include <android/log.h>

#define  LOG_TAG    "Speex JNI :"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)



static int codec_open = 0;



static int dec_frame_size;

static int enc_frame_size;



static SpeexBits ebits, dbits;

void *enc_state;

void *dec_state;



static JavaVM *gJavaVM;



extern "C"

JNIEXPORT jint JNICALL Java_com_trunkbow_speextest_Speex_open
//JNIEXPORT jint JNICALL Java_com_trunkbow_speextest_Speex_open


(JNIEnv *env, jobject obj, jint compression) {

    int tmp;



    if (codec_open++ != 0)

        return (jint)0;



    speex_bits_init(&ebits);

    speex_bits_init(&dbits);



    enc_state = speex_encoder_init(&speex_nb_mode);

    dec_state = speex_decoder_init(&speex_nb_mode);

    tmp = compression;

    speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &tmp);

    speex_encoder_ctl(enc_state, SPEEX_GET_FRAME_SIZE, &enc_frame_size);

    speex_decoder_ctl(dec_state, SPEEX_GET_FRAME_SIZE, &dec_frame_size);



    return (jint)0;

}



extern "C"

JNIEXPORT jint Java_com_trunkbow_speextest_Speex_encode

(JNIEnv *env, jobject obj, jshortArray lin, jint offset, jbyteArray encoded, jint size) {



    jshort buffer[enc_frame_size];

    jbyte output_buffer[enc_frame_size];

    int nsamples = (size-1)/enc_frame_size + 1;

    int i, tot_bytes = 0;

    if (!codec_open)

        return 0;

    speex_bits_reset(&ebits);

    for (i = 0; i < nsamples; i++) {

        env->GetShortArrayRegion(lin, offset + i*enc_frame_size, enc_frame_size, buffer);

        speex_encode_int(enc_state, buffer, &ebits);

    }

    //env->GetShortArrayRegion(lin, offset, enc_frame_size, buffer);

    //speex_encode_int(enc_state, buffer, &ebits);



    tot_bytes = speex_bits_write(&ebits, (char *)output_buffer,

    enc_frame_size);

    env->SetByteArrayRegion(encoded, 0, tot_bytes,

    output_buffer);



    return (jint)tot_bytes;

}



extern "C"

JNIEXPORT jint JNICALL Java_com_trunkbow_speextest_Speex_decode

(JNIEnv *env, jobject obj, jbyteArray encoded, jshortArray lin, jint size) {



    jbyte buffer[dec_frame_size];

    jshort output_buffer[dec_frame_size];

    jsize encoded_length = size;



    if (!codec_open)

        return 0;



    env->GetByteArrayRegion(encoded, 0, encoded_length, buffer);

    speex_bits_read_from(&dbits, (char *)buffer, encoded_length);

    speex_decode_int(dec_state, &dbits, output_buffer);

    env->SetShortArrayRegion(lin, 0, dec_frame_size,

    output_buffer);



    return (jint)dec_frame_size;

}



extern "C"

JNIEXPORT jint JNICALL Java_com_trunkbow_speextest_Speex_getFrameSize

(JNIEnv *env, jobject obj) {



    if (!codec_open)

    return 0;

    return (jint)enc_frame_size;



}



extern "C"

JNIEXPORT void JNICALL Java_com_trunkbow_speextest_Speex_close

(JNIEnv *env, jobject obj) {



    if (--codec_open != 0)

    return;



    speex_bits_destroy(&ebits);

    speex_bits_destroy(&dbits);

    speex_decoder_destroy(dec_state);

    speex_encoder_destroy(enc_state);

}
