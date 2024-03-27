#FROM nvidia/cuda:10.2-devel-ubuntu18.04
FROM dorowu/ubuntu-desktop-lxde-vnc

copy Docker/Start.sh Start.sh
COPY target/MarioKartWiiAi-latest-jar-with-dependencies.jar mkwiiai.jar
COPY Dolphin Dolphin
copy Data Data

# apt-get update errors for some reason but is still requiered to install jre 
RUN sudo apt-get update; exit 0
RUN sudo apt-get install openjdk-17-jre -y
RUN sudo apt-get install wine -y
#RUN sudo apt-get install novnc python3-websockify python3-numpy  -y
# websockify -D --web=/usr/share/novnc/ 80 localhost:5901 

#ENTRYPOINT ["bash", "/root/Start.sh"]
ENV OPENBOX_ARGS='--startup "bash /root/Start.sh localhost"'

# docker run --gpus all -p 6080:80 minekonst/mkwiiai:latest

#
#  (1) Works with WSL 2 Ubuntu-18.04 (look through commits for other versions)
#

# (1) Install Cuda Host driver for WSL 2: https://docs.nvidia.com/cuda/wsl-user-guide/index.html#cuda-support-for-wsl2
# Short version:
#  wget https://developer.download.nvidia.com/compute/cuda/repos/wsl-ubuntu/x86_64/cuda-wsl-ubuntu.pin
#  sudo mv cuda-wsl-ubuntu.pin /etc/apt/preferences.d/cuda-repository-pin-600
#  wget https://developer.download.nvidia.com/compute/cuda/11.7.0/local_installers/cuda-repo-wsl-ubuntu-11-7-local_11.7.0-1_amd64.deb
#  sudo dpkg -i cuda-repo-wsl-ubuntu-11-7-local_11.7.0-1_amd64.deb
#  sudo apt update
#  sudo apt install cuda

# Some troubleshooting resources:
#  https://github.com/NVIDIA/nvidia-docker/issues/1487

# Build Dolphin on Linux (one step not working)
# RUN sudo apt install -y --no-install-recommends ca-certificates build-essential libgtk2.0-dev libwxgtk3.0-gtk3-dev qtbase5-dev qtbase5-private-dev git cmake make gcc g++ pkg-config libavcodec-dev libavformat-dev libavutil-dev libswscale-dev libxi-dev libxrandr-dev libudev-dev libevdev-dev libsfml-dev libminiupnpc-dev libmbedtls-dev libcurl4-openssl-dev libhidapi-dev libsystemd-dev libbluetooth-dev libasound2-dev libpulse-dev libpugixml-dev libbz2-dev libzstd-dev liblzo2-dev libpng-dev libusb-1.0-0-dev gettext
#RUN cmake ..
#RUN make
#RUN cmake .. -DLINUX_LOCAL_DEV=true
#RUN make -j8
#RUN sudo make install
