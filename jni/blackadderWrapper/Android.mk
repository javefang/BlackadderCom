LOCAL_PATH := $(MY_LOCAL_PATH)/blackadderWrapper

# build blackadder wrapper
include $(CLEAR_VARS)

LOCAL_MODULE := uk_ac_cam_cl_xf214_blackadderWrapper
LOCAL_SRC_FILES := uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperShared.cpp uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapper.cpp uk_ac_cam_cl_xf214_blackadderWrapper_BAWrapperNB.cpp
LOCAL_SHARED_LIBRARIES += blackadder
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)

include $(BUILD_SHARED_LIBRARY)
$(call import-module,blackadder)

