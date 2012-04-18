LOCAL_PATH := $(MY_LOCAL_PATH)/videopubsub

include $(CLEAR_VARS)
LOCAL_MODULE := video_publisher
LOCAL_SRC_FILES := video_publisher.cpp
LOCAL_SHARED_LIBRARIES += blackadder
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE :=	video_subscriber
LOCAL_SRC_FILES := video_subscriber.cpp
LOCAL_SHARED_LIBRARIES += blackadder
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_EXECUTABLE)

$(call import-module,blackadder)
