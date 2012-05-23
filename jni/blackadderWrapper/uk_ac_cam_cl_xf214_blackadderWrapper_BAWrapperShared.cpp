#include "uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperShared.h"
#include <blackadder.hpp>
#include <string>
#include <stdio.h>

using std::string;
/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperShared
 * Method:    c_hex_to_char
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperShared_c_1hex_1to_1char 
 (JNIEnv *env, jclass c, jstring hex) {
  // convert jstring to local string
  //  jboolean copy = (jboolean)false;
  jboolean copy = (jboolean)false;
  const char *hex_c_str = env->GetStringUTFChars(hex, &copy);
  int hex_size = env->GetStringUTFLength(hex);
  const string hex_str(hex_c_str, hex_size);
  
  // convert to binary
  string char_str = hex_to_chararray(hex_str);
  int char_size = char_str.size();
  char * char_ptr = (char *)char_str.c_str();
  jbyteArray char_array = env->NewByteArray((jsize)char_size);
  env->SetByteArrayRegion(char_array, (jsize)0, (jsize)char_size, (jbyte *)char_ptr);
  
  // release pointers and return
  return char_array;
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperShared
 * Method:    c_char_to_hex
 * Signature: ([B)[B
 */
JNIEXPORT jstring JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperShared_c_1char_1to_1hex
 (JNIEnv *env, jclass c, jbyteArray char_array) {
  // convert jbyteArray to local string
  jboolean copy = (jboolean)false;
  jbyte * char_ptr = env->GetByteArrayElements(char_array, &copy);
  int char_size = env->GetArrayLength(char_array);
  const string char_str((char *)char_ptr, char_size);

  // convert to hex
  string hex_str = chararray_to_hex(char_str);
  int hex_size = hex_str.size();
  char * hex_ptr = (char *)hex_str.c_str();

  // return (TODO: may need to release jbyteArray?)
  return env->NewStringUTF(hex_ptr);
}

