#include "cam_androidSpeex_NativeSpeexDecoder.h"
#include <stdlib.h>
#include <speex/speex.h>

/*
 * Class:     cam_androidSpeex_NativeSpeexDecoder
 * Method:    c_init_bits
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_cam_androidSpeex_NativeSpeexDecoder_c_1init_1bits
  (JNIEnv *env, jobject obj) {
  SpeexBits *bits = (SpeexBits *)malloc(sizeof(SpeexBits));
  speex_bits_init(bits);
  return (jlong) bits;
}
  
/*
 * Class:     cam_androidSpeex_NativeSpeexDecoder
 * Method:    c_init
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_cam_androidSpeex_NativeSpeexDecoder_c_1init
  (JNIEnv *env, jobject obj, jint mode) {
  void *dec_state;
  switch (mode) {
  case SPEEX_MODEID_NB:
  	dec_state = speex_decoder_init(&speex_nb_mode);
  	break;
  case SPEEX_MODEID_WB:
  	dec_state = speex_decoder_init(&speex_wb_mode);
  	break;
  case SPEEX_MODEID_UWB:
  	dec_state = speex_decoder_init(&speex_uwb_mode);
  	break;
  default:
  	return (jlong) -1;
  }
  return (jlong) dec_state;
}

/*
 * Class:     cam_androidSpeex_NativeSpeexDecoder
 * Method:    c_set
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_cam_androidSpeex_NativeSpeexDecoder_c_1set
  (JNIEnv *env, jobject obj, jlong spx_ptr, jint field_id, jint value) {
  speex_decoder_ctl((void *)spx_ptr, field_id, &value);
}

/*
 * Class:     cam_androidSpeex_NativeSpeexDecoder
 * Method:    c_get
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_cam_androidSpeex_NativeSpeexDecoder_c_1get
  (JNIEnv *env, jobject obj, jlong spx_ptr, jint field_id) {
  int rtnVal;
  speex_decoder_ctl((void *)spx_ptr, field_id, &rtnVal);
  return (jint)rtnVal;
}

/*
 * Class:     cam_androidSpeex_NativeSpeexDecoder
 * Method:    c_mode_query
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_cam_androidSpeex_NativeSpeexDecoder_c_1mode_1query
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
 * Class:     cam_androidSpeex_NativeSpeexDecoder
 * Method:    c_decode
 * Signature: (JJLjava/nio/ByteBuffer;II)[S
 */
JNIEXPORT jshortArray JNICALL Java_cam_androidSpeex_NativeSpeexDecoder_c_1decode
(JNIEnv *env, jobject obj, jlong spx_ptr, jlong bits_ptr, jobject jbytebuff, jint data_len, jint frame_size, jint frame_count) {
  void *dec_state = (void *)spx_ptr;;
  SpeexBits *bits = (SpeexBits *)bits_ptr;
  char *input_bytes = (char *)((*env)->GetDirectBufferAddress(env, jbytebuff));
  jshortArray ret = (jshortArray)((*env)->NewShortArray(env, frame_size));
  jshort *output_frame = (*env)->GetShortArrayElements(env, ret, 0);

  // TODO: modify the code to decode the jbytebuff as frame_count chunks
  speex_bits_read_from(bits, input_bytes, data_len);
  speex_decode_int(dec_state, bits, output_frame);
  
  (*env)->ReleaseShortArrayElements(env, ret, output_frame, 0);
  free(input_bytes); /* TODO: should remove free() if other code needs to use the bit-stream later */
  return ret;
}

/*
 * Class:     cam_androidSpeex_NativeSpeexDecoder
 * Method:    c_destroy
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_cam_androidSpeex_NativeSpeexDecoder_c_1destroy
  (JNIEnv *env, jobject obj, jlong spx_ptr, jlong bits_ptr) {
  SpeexBits *bits;
  void *dec_state;
  
  bits = (SpeexBits *)bits_ptr;
  dec_state = (void *)spx_ptr;
  
  speex_bits_destroy(bits);	/* does this line also free 'bits'? */
  speex_decoder_destroy(dec_state);
  free(bits);	/* TODO: may have double-free problem */
}


