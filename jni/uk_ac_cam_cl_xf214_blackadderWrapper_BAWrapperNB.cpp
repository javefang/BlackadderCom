#include "uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB.h"
#include <nb_blackadder.hpp>
#include <string>
#include <stdio.h>

using namespace std;

static JavaVM *baJVM;
//static JNIEnv *jnienv;
static jclass jwrapperClass;
static jmethodID jcallback;
static pthread_key_t worker_thread_key;

void print(string s) {
	cout << "BAWrapperNB: " << s << endl;
}

void detachVMThread(void *cached_jvm) {
	print("Detaching from VM thread...");
	/*
	JNIEnv *env;
	int status = baJVM->GetEnv((void **)&env,  JNI_VERSION_1_6);
	if (status < 0) {
		cout << "Thread not attached to VM, abort..." << endl;
		return;
	}*/
	int status = ((JavaVM *)cached_jvm)->DetachCurrentThread();
	if (status < 0) {
		cerr << "Failed to detach VM thread!" << endl;
	} else {
		cout << "VM thread successfully detached!" << endl;
	}
}

/*
 * callback function, to be called by worker thread of the Blackadder non-blocking api
 * this function will convert the event data into java format and call the "onEventReceived" java method
 */
void onEventReceived(Event *ev) {	
	// TODO: change to use pthread_key_create to define a desturctor function to call "DetachCurrentThread" before exit, use key with pthread_setspecific to store the JNIEnv in the thread-local-storage (passed into the contructor as the argument later)

	// TODO: Since onEventReceived will be called only by the worker thread, it is safe to just AttachCurrentThread once and cache the JNIEnv pointer for later use. (but not very safe if thread changes, keep checking for now)
	JNIEnv *jnienv;
	int status = baJVM->GetEnv((void **)&jnienv, JNI_VERSION_1_6);
	if (status < 0) {
		cerr << "Failed to get JNI environment, assuming native thread" << endl;
		status = baJVM->AttachCurrentThread(&jnienv, NULL);
		if (status < 0) {
			cerr << "Failed to attach current thread." << endl;
			cerr << "Cannot call Java method (JNIEnv failed)." << endl;
			return;
		}
		
		// set thread destructor
		print("Setting thread destructor...");
		pthread_key_create(&worker_thread_key, detachVMThread);
		pthread_setspecific(worker_thread_key, baJVM);
		print("Native thread successfully attached to VM!");
	}

	//cout << "Receiving new event..." << endl;
	// copy id array
	//cout << "Creating event ID..." << endl;
	const char *id_ptr = ev->id.c_str();
	
	jbyteArray idArray = jnienv->NewByteArray(ev->id.length());
	/*
	if (idArray == NULL) {
		cerr << "NewByteArray returns NULL, abort..." << endl;
	}*/
	//cout << "Set event ID value..." << endl;
	jnienv->SetByteArrayRegion(idArray, 0, ev->id.length(), (jbyte *) id_ptr);
	// create ByteBuffer and let Java read c++ memory about the data directly
	//cout << "Creating data buffer..." << endl;
	jobject jbuff = jnienv->NewDirectByteBuffer((void*)ev->data, (jlong) ev->data_len); 
	// calling java method
	//cout << "Calling Java method..." << endl;
	jnienv->CallStaticVoidMethod(jwrapperClass, jcallback, (jlong) ev, (jbyte) ev->type, idArray, jbuff);
	//cout << "Deleting local reference..." << endl;
	// NOTE: MUST delete local reference here, otherwise memory leak until VM crashed!!!!
	jnienv->DeleteLocalRef(idArray);
	jnienv->DeleteLocalRef(jbuff);
	//cout << "Event received" << endl;
}

/* 
 * do the datatype convertion and call specified blackadder function
 */
void call_blackadder(JNIEnv *env, jlong &ba_ptr, jbyteArray &id, jbyteArray &id_prefix, jbyte &strategy, jbyteArray &jstr_opt, void(NB_Blackadder::*ba_call)(const string &, const string &, unsigned char, void *, unsigned int)) {
	NB_Blackadder *ba;
	ba = (NB_Blackadder *)ba_ptr;
	
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


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
	cout << "JNI load!!!!! (jvm cached)" << endl;
	baJVM = jvm;
	
	JNIEnv *env;
	int status = baJVM->GetEnv((void **)&env, JNI_VERSION_1_6);
	if (status < 0) {
		cerr << "Failed to get JNI environment, assuming native thread" << endl;
		status = baJVM->AttachCurrentThread(&env, NULL);
		if (status < 0) {
			cerr << "Failed to attach current thread." << endl;
			cerr << "Cannot call Java method (JNIEnv failed)." << endl;
			return -1;
		}
	}
	
	// find BAWrapperNB class (only perform this in the "OnLoad" function
	// this ensure correct classloader is used to load non-system class (BAWrapperNB)
	cout << "Finding class..." << endl;
	jclass localClass = env->FindClass("uk/ac/cam/cl/xf214/blackadderWrapper/BAWrapperNB");
	
	if (localClass == NULL) {
		cerr << "Error: Cannot find class!! abort..." << endl;
		return -2;
	}
	
	// make global
	jwrapperClass = (jclass) env->NewGlobalRef((jobject)localClass);
	if (jwrapperClass == NULL) {
		cerr << "Error: Cannot create global reference for class!! abort..." << endl;
		return -2;
	}
	
	// find java callback method id
	cout << "Setting method signature..." << endl;
	jcallback = env->GetStaticMethodID(jwrapperClass, "onEventReceived", "(JB[BLjava/nio/ByteBuffer;)V");
	if (jcallback == NULL) {
		cerr << "Error: Cannot find method signature!! abort..." << endl;
		return -3;
	}
	
	cout << "JNI initialization complete!" << endl;
	return JNI_VERSION_1_6;
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_create_new_ba
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1create_1new_1ba
  (JNIEnv *env, jobject obj, jint userspace) {
  	bool user = userspace ? true : false;
  	printf("create new instance\n");
  	NB_Blackadder *ba_ptr = NB_Blackadder::Instance(user);
  	
  	/*
  	int status = env->GetJavaVM(&baJVM);
  	if (status != 0) {
  		cerr << "Failed to create VM!" << endl;
  		return 0;
  	}*/
  	
  	ba_ptr->setCallback(onEventReceived);
  	return (jlong) ba_ptr;
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_delete_ba
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1delete_1ba
  (JNIEnv *, jobject, jlong ba_ptr) {
  	NB_Blackadder *ba;
  	ba = (NB_Blackadder *) ba_ptr;
  	ba->disconnect();
  	delete ba;
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_publish_scope
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1publish_1scope
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &NB_Blackadder::publish_scope);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_publish_item
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1publish_1item
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &NB_Blackadder::publish_info);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_unpublish_scope
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1unpublish_1scope
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &NB_Blackadder::unpublish_scope);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_unpublish_item
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1unpublish_1item
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &NB_Blackadder::unpublish_info);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_subscribe_scope
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1subscribe_1scope
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &NB_Blackadder::subscribe_scope);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_subscribe_item
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1subscribe_1item
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &NB_Blackadder::subscribe_info);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_unsubscribe_scope
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1unsubscribe_1scope
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &NB_Blackadder::unsubscribe_scope);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_unsubscribe_item
 * Signature: (J[B[BB[B)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1unsubscribe_1item
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray id, jbyteArray id_prefix, jbyte strategy, jbyteArray jstr_opt) {
	call_blackadder(env, ba_ptr, id, id_prefix, strategy, jstr_opt, &NB_Blackadder::unsubscribe_info);
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_publish_data
 * Signature: (J[BB[B[BI)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1publish_1data
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray name, jbyte strategy, jbyteArray jstr_opt, jbyteArray data, jint datalen) {
	/* find Blackadder object by memory address */
	NB_Blackadder *ba;
	ba = (NB_Blackadder *)ba_ptr;
	
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
	char *data_native_ptr = (char *)calloc((int)datalen, sizeof(char *));
	for (int i = 0; i < datalen; i++) {
		data_native_ptr[i] = (char)data_ptr[i];
	}
	
	
	/* call blackadder api to publish data */
	ba->publish_data(name_str, (char)strategy, str_opt, str_opt_len, data_native_ptr, (int)datalen);
	
	/* release native memory */
	(*env).ReleaseByteArrayElements(name, name_ptr, (jint)0);
	(*env).ReleaseByteArrayElements(data, data_ptr, (jint)0);
	
	if(jstr_opt != 0){	
		(*env).ReleaseByteArrayElements(jstr_opt, (jbyte *)str_opt, (jint)0);		
	}
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_publish_data_direct
 * Signature: (J[BB[BLjava/nio/ByteBuffer;I)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1publish_1data_1direct
  (JNIEnv *env, jobject, jlong ba_ptr, jbyteArray name, jbyte strategy, jbyteArray jstr_opt, jobject jbytebuffer, jint length) {
	/* find Blackadder object by memory address */
	NB_Blackadder *ba;
	ba = (NB_Blackadder *)ba_ptr;
	
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

	// TODO: why performing extra array copy? can we just use *data_ptr from ByteBuffer (allocateDirect)? 
	/*
	char *data_native_ptr = (char *)calloc((int)length, sizeof(char *));
	for (int i = 0; i < length; i++) {
		data_native_ptr[i] = data_ptr[i];
	}
	(/

	/* call blackadder api to publish data */
	ba->publish_data(name_str, (char)strategy, str_opt, str_opt_len, data_ptr, (int)length);
	
	/* release native memory */
	(*env).ReleaseByteArrayElements(name, name_ptr, (jint)0);
	if(jstr_opt != 0){	
		(*env).ReleaseByteArrayElements(jstr_opt, (jbyte *)str_opt, (jint)0);		
	}	
}

/*
 * Class:     uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB
 * Method:    c_delete_event
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB_c_1delete_1event
  (JNIEnv *, jobject, jlong ba_ptr, jlong ev_ptr) {
	Event *event = (Event *)ev_ptr;
	delete event;
}



