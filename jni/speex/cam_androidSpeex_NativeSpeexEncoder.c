#include "cam_androidSpeex_NativeSpeexEncoder.h"
#include <stdlib.h>
#include <stdio.h>
#include <speex/speex.h>

int print(char *msg) {
  printf(msg);
}
/*
 * Class:     cam_androidSpeex_NativeSpeexEncoder
 * Method:    c_init_bits
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_cam_androidSpeex_NativeSpeexEncoder_c_1init_1bits
  (JNIEnv *env, jobject obj) {
  SpeexBits *bits = malloc(sizeof(SpeexBits));
  speex_bits_init(bits);
  return (jlong)bits;
}

/*
 * Class:     cam_androidSpeex_NativeSpeexEncoder
 * Method:    c_init
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_cam_androidSpeex_NativeSpeexEncoder_c_1init
  (JNIEnv *env, jobject obj, jint mode) {
  void *enc_state;
  switch (mode) {
  case SPEEX_MODEID_NB:
  	enc_state = speex_encoder_init(&speex_nb_mode);
  	break;
  case SPEEX_MODEID_WB:
  	enc_state = speex_encoder_init(&speex_wb_mode);
  	break;
  case SPEEX_MODEID_UWB:
  	enc_state = speex_encoder_init(&speex_uwb_mode);
  	break;
  default:
    printf("Unknown mode detected: %d\n", mode);
  	return (jlong)-1;
  }
  printf("NativeSpeexEncoder intiialized!\n");
  return (jlong)enc_state;
}

/*
 * Class:     cam_androidSpeex_NativeSpeexEncoder
 * Method:    c_set
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_cam_androidSpeex_NativeSpeexEncoder_c_1set
  (JNIEnv *env, jobject obj, jlong spx_ptr, jint field_id, jint value) {
  speex_encoder_ctl((void *)spx_ptr, field_id, &value);
}

/*
 * Class:     cam_androidSpeex_NativeSpeexEncoder
 * Method:    c_get
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_cam_androidSpeex_NativeSpeexEncoder_c_1get
  (JNIEnv *env, jobject obj, jlong spx_ptr, jint field_id) {
  int rtnVal;
  speex_encoder_ctl((void *)spx_ptr, field_id, &rtnVal);
  return (jint)rtnVal;
}

/*
 * Class:     cam_androidSpeex_NativeSpeexEncoder
 * Method:    c_mode_query
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_cam_androidSpeex_NativeSpeexEncoder_c_1mode_1query
  (JNIEnv *env, jobject obj, jint mode, jint field) {
  int rtnVal;
  switch (mode) {
  case SPEEX_MODEID_NB:
  	speex_mode_query(&speex_nb_mode, field, &rtnVal);
  	break;
  case SPEEX_MODEID_WB:
  	return speex_mode_query(&speex_wb_mode, field, &rtnVal);
  	break;
  case SPEEX_MODEID_UWB:
  	return speex_mode_query(&speex_uwb_mode, field, &rtnVal);
  	break;
  default:
    printf("Unknown mode detected: %d\n", mode);
  	return (jlong)-1;
  }
  return (jint) rtnVal;
}

/*
 * Class:     cam_androidSpeex_NativeSpeexEncoder
 * Method:    c_encode
 * Signature: (JJ[S)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_cam_androidSpeex_NativeSpeexEncoder_c_1encode
(JNIEnv *env, jobject obj, jlong spx_ptr, jlong bits_ptr, jshortArray input_data, jint frame_count) {
  int nbBytes;
  char *arrayElements;
  void *enc_state = (void *)spx_ptr;
  SpeexBits *bits = (SpeexBits *)bits_ptr;
  jshort * inputArrayElements = (*env)->GetShortArrayElements(env, input_data, 0);
 
  // TODO: modify code to encode the input_data as frame_count chunks
  /* encoding */
  speex_bits_reset(bits);
  speex_encode_int(enc_state, inputArrayElements, bits);
  nbBytes = speex_bits_nbytes(bits);
  arrayElements = malloc(sizeof(char) * nbBytes);
  speex_bits_write(bits, arrayElements, nbBytes);
  
  jobject jbuff = (*env)->NewDirectByteBuffer(env, (void*)arrayElements, (jlong)nbBytes); 
  
  /* release short array */
  (*env)->ReleaseShortArrayElements(env, input_data, inputArrayElements, JNI_ABORT);
  return jbuff;
}

/*
 * Class:     cam_androidSpeex_NativeSpeexEncoder
 * Method:    c_destroy
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_cam_androidSpeex_NativeSpeexEncoder_c_1destroy
  (JNIEnv *env, jobject obj, jlong spx_ptr, jlong bits_ptr) {
  SpeexBits *bits;
  void *enc_state;
  
  bits = (SpeexBits *)bits_ptr;
  enc_state = (void *)spx_ptr;
  
  speex_bits_destroy(bits);	/* does this line also free 'bits'? */
  speex_encoder_destroy(enc_state);
  free(bits);	/* TODO: may have double-free problem */
  print("NativeSpeexEncoder destroyed!");
}

