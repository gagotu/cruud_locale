cd ..
mvn clean package -Pprod
copy target\cruud-0.0.1-SNAPSHOT.jar docker\assets\
cd docker\compose