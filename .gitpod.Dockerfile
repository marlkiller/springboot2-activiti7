FROM gitpod/workspace-full
                    
#VOLUME /tmp 
ADD target/SpringBoot2-Activiti7-1.0.0.jar /SpringBoot2-Activiti7-1.0.0.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/SpringBoot2-Activiti7-1.0.0.jar"]

# Install custom tools, runtime, etc. using apt-get
# For example, the command below would install "bastet" - a command line tetris clone:
#
# RUN sudo apt-get -q update && #     sudo apt-get install -yq bastet && #     sudo rm -rf /var/lib/apt/lists/*
#
# More information: https://www.gitpod.io/docs/42_config_docker/
