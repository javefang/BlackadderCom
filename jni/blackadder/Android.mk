LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := blackadder
LOCAL_SRC_FILES := bitvector.cpp blackadder.cpp nb_blackadder.cpp
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)

include $(BUILD_SHARED_LIBRARY)
