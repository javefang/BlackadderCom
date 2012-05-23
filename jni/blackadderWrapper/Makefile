CXX=g++
JNI_LOC = /usr/lib/jvm/java-6-openjdk/include
#JNI_LOC = /usr/lib/jvm/java-6-sun/include
JNI_INC+=-I$(JNI_LOC) -I$(JNI_LOC)/linux
TARGET = ../../libs
OBJ = ../../obj

NAME=uk_ac_cam_cl_xf214_blackadderWrapper

.PHONY: all clean

all: $(TARGET)/lib$(NAME).so

$(OBJ)/$(NAME)_BAWrapperShared.o: $(NAME)_BAWrapperShared.cpp
	$(CXX) -c -g -fPIC $(CXXFLAGS) $(JNI_INC) -o $(OBJ)/$(NAME)_BAWrapperShared.o $(NAME)_BAWrapperShared.cpp

$(OBJ)/$(NAME)_BAWrapper.o: $(NAME)_BAWrapper.cpp
	$(CXX) -c -g -fPIC $(CXXFLAGS) $(JNI_INC) -o $(OBJ)/$(NAME)_BAWrapper.o $(NAME)_BAWrapper.cpp

$(OBJ)/$(NAME)_BAWrapperNB.o: $(NAME)_BAWrapperNB.cpp
	$(CXX) -c -g -fPIC $(CXXFLAGS) $(JNI_INC) -o $(OBJ)/$(NAME)_BAWrapperNB.o $(NAME)_BAWrapperNB.cpp

$(TARGET)/lib$(NAME).so: $(OBJ)/$(NAME)_BAWrapper.o $(OBJ)/$(NAME)_BAWrapperNB.o $(OBJ)/$(NAME)_BAWrapperShared.o
	$(CXX) -shared -fPIC -o $(TARGET)/lib$(NAME).so $(OBJ)/$(NAME)_BAWrapper.o $(OBJ)/$(NAME)_BAWrapperShared.o $(OBJ)/$(NAME)_BAWrapperNB.o $(LDFLAGS) -lblackadder

clean:
	rm -f $(OBJ)/$(NAME)_BAWrapper.o $(OBJ)/$(NAME)_BAWrapperNB.o $(OBJ)/$(NAME)_BAWrapperShared.o $(TARGET)/lib$(NAME).so
