LOCAL_PATH := $(MY_LOCAL_PATH)/pubsub

include $(CLEAR_VARS)
LOCAL_MODULE := nb_publisher
LOCAL_SRC_FILES := nb_publisher.cpp
LOCAL_SHARED_LIBRARIES += blackadder
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE := nb_subscriber
LOCAL_SRC_FILES := nb_subscriber.cpp
LOCAL_SHARED_LIBRARIES += blackadder
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_EXECUTABLE)

$(call import-module,blackadder)
