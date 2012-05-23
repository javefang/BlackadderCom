#include "uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper.h"
#include <blackadder.hpp>
#include <string>
#include <stdio.h>

using std::string;

/* 
 * do the datatype convertion and call specified blackadder function
 */
void call_blackadder(JNIEnv *env, jlong &ba_ptr, jbyteArray &id, jbyteArray &id_prefix, jbyte &strategy, jbyteArray &jstr_opt, void(Blackadder::*ba_call)(const string &, const string &, unsigned char, void *, unsigned int)) {
	Blackadder *ba;
	ba = (Blackadder *)ba_ptr;
	
	jboolean copy = (jboolean)false;
	
	jbyte * id_ptr = env->GetByteArrayElements(id, &copy);
	unsigned int id_length = env->GetArrayLength(id);	
	const string id_str((char *)id_ptr, id_length);
	
	jbyte * id_prefix_ptr = env->GetByteArrayElements(id_prefix, &copy);
	unsigned int id_prefix_length = env->GetArrayLength(id_prefix);	
	const string id_prefix_str((char *)id_prefix_ptr, id_prefix_length);
	
	void *str_opt;
	str_opt = 0;
	unsigned int str_opt_len = 0;
	
	if (jstr_opt != NULL && (str_opt_len = env->GetArrayLength(jstr_opt)) > 0) {
		str_opt = (void *)env->GetByteArrayElements(jstr_opt, &copy);
	}
	
	/* call to blackadder */
	(ba->*ba_call)(id_str, id_prefix_str, (char)strategy, str_opt, str_opt_len);
	
	env->ReleaseByteArrayElements(id, id_ptr, (jint)0);
	env->ReleaseByteArrayElements(id_prefix, id_prefix_ptr, (jint)0);
	
	if (jstr_opt != 0) {
		env->ReleaseByteArrayElements(jstr_opt, (jbyte *)str_opt, (jint)0);
	}
}
/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_create_new_ba
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1create_1new_1ba
  (JNIEnv *, jobject, jint userspace) {
  	bool user = userspace ? true : false;
  	printf("create new instance\n");
  	Blackadder *ba_ptr = Blackadder::Instance(user);
  	return (jlong) ba_ptr;
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_delete_ba
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1delete_1ba
  (JNIEnv *, jobject, jlong ba_ptr) {
  	Blackadder *ba;
  	ba = (Blackadder *) ba_ptr;
  	ba->disconnect();
  	delete ba;
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_publish_scope
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1publish_1scope
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &Blackadder::publish_scope);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_publish_item
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1publish_1item
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &Blackadder::publish_info);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_unpublish_scope
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1unpublish_1scope
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &Blackadder::unpublish_scope);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_unpublish_item
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1unpublish_1item
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &Blackadder::unpublish_info);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_subscribe_scope
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1subscribe_1scope
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &Blackadder::subscribe_scope);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_subscribe_item
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1subscribe_1item
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &Blackadder::subscribe_info);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_unsubscribe_scope
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1unsubscribe_1scope
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &Blackadder::unsubscribe_scope);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_unsubscribe_item
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1unsubscribe_1item
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &Blackadder::unsubscribe_info);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_publish_data
 * Signature: (J[BB[B[BI)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1publish_1data
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray name, jbyte strategy, jbyteArray jstr_opt, jbyteArray data, jint datalen) {
	/* find Blackadder object by memory address */
	Blackadder *ba;
	ba = (Blackadder *)ba_ptr;
	
	jboolean copy = (jboolean)false;
	
	/* get Java object data via JNI */ 
	jbyte *name_ptr = (*env).GetByteArrayElements(name, &copy);
	int name_length = (*env).GetArrayLength(name);
	const string name_str((char *)name_ptr, name_length);
	 
	/* check Blackadder strategy options */
	void *str_opt;
	str_opt = 0;
	unsigned int str_opt_len = 0;
	 
	if(jstr_opt != NULL && (str_opt_len = (*env).GetArrayLength(jstr_opt)) > 0) {
		str_opt = (void *) (*env).GetByteArrayElements(jstr_opt, &copy);
	}
	
	jbyte *data_ptr = (*env).GetByteArrayElements(data, &copy);
	
	/* call blackadder api to publish data */
	ba->publish_data(name_str, (char)strategy, str_opt, str_opt_len, (char *)data_ptr, (int)datalen);
	
	/* release native memory */
	(*env).ReleaseByteArrayElements(name, name_ptr, (jint)0);
	(*env).ReleaseByteArrayElements(data, data_ptr, (jint)0);
	
	if(jstr_opt != 0){	
		(*env).ReleaseByteArrayElements(jstr_opt, (jbyte *)str_opt, (jint)0);		
	}
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_publish_data_direct
 * Signature: (J[BB[BLjava/nio/ByteBuffer;I)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1publish_1data_1direct
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray name, jbyte strategy, jbyteArray jstr_opt, jobject jbytebuffer, jint length) {
	/* find Blackadder object by memory address */
	Blackadder *ba;
	ba = (Blackadder *)ba_ptr;
	
	jboolean copy = (jboolean)false;
	
	/* get Java object data via JNI */ 
	jbyte *name_ptr = (*env).GetByteArrayElements(name, &copy);
	int name_length = (*env).GetArrayLength(name);
	const string name_str ((char *)name_ptr, name_length);
	
	/* check Blackadder strategy options */
	void *str_opt;
	str_opt = 0;
	unsigned int str_opt_len = 0;

	if(jstr_opt != NULL && (str_opt_len = (*env).GetArrayLength(jstr_opt)) > 0) {
		str_opt = (void *) (*env).GetByteArrayElements(jstr_opt, &copy);		
	}

	char *data_ptr = (char *)(*env).GetDirectBufferAddress(jbytebuffer);
	
	/* call blackadder api to publish data */	
	ba->publish_data(name_str, (char)strategy, str_opt, str_opt_len, (char *)data_ptr, (int)length);
	
	/* release native memory */
	(*env).ReleaseByteArrayElements(name, name_ptr, (jint)0);
	if(jstr_opt != 0){	
		(*env).ReleaseByteArrayElements(jstr_opt, (jbyte *)str_opt, (jint)0);		
	}	
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_nextEvent_direct
 * Signature: (JLuk/ac/cam/xf214/blackadderWrapper/BAEvent;)J
 */
JNIEXPORT jlong JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1nextEvent_1direct
  (JNIEnv *env, jobject, jlong ba_ptr, jobject obj_EventInternal) {
	// find Blackadder object by memory address 
	Blackadder *ba;
	ba = (Blackadder *)ba_ptr;
	
	// initialise a c++ "Event" object (on heap, need to delete later)
	Event *ev = new Event();
	// call blackadder api to get new event (given the pointer to allocated Event 
    ba->getEvent(*ev);
    
    // get fields id from the Java "EventInternal" class 
    static jclass cls = (*env).GetObjectClass(obj_EventInternal);
    
    static jfieldID evTypeField = (*env).GetFieldID(cls, "type", "B");
	if (evTypeField == NULL) {
		printf("JNI: error getting field id for Event.type\n");
		return 0;
	}

	static	jfieldID evIdField = (*env).GetFieldID(cls, "id", "[B");
	if (evTypeField == NULL) {
		printf("JNI: error getting field id for Event.id\n");
		return 0;
	}

	static	jfieldID evDataField = (*env).GetFieldID(cls, "data", "Ljava/nio/ByteBuffer;");
	if (evDataField == NULL) {
		printf("JNI: error getting field id for Event.data\n");
		return 0;
	}
	
	/* copy data from c++ "Event" to java "InternalEvent" */
	//copy type byte
	(*env).SetByteField(obj_EventInternal, evTypeField, (jbyte) ev->type);
	
	//copy id array
	const char *id_ptr = ev->id.c_str();	
	jbyteArray idArray = (*env).NewByteArray(ev->id.length());
	(*env).SetByteArrayRegion(idArray, 0, ev->id.length(), (jbyte *) id_ptr);
	(*env).SetObjectField(obj_EventInternal, evIdField, idArray);
	
	// create ByteBuffer and let Java read c++ memory about the data directly
	jobject jbuff = (*env).NewDirectByteBuffer((void*)ev->data, (jlong) ev->data_len); 
	(*env).SetObjectField(obj_EventInternal, evDataField, jbuff);
	
	// return the pointer for c++ "Event" object for memory free later by call to "delete_event"
	return (jlong)ev;
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper
 * Method:    c_delete_event
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper_c_1delete_1event
  (JNIEnv *, jobject, jlong ba_ptr, jlong ev_ptr) {
	Event *event = (Event *)ev_ptr;
	delete event;
}

