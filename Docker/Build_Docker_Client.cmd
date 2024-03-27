cd ..
call mvn clean package
docker build -f Docker/Dockerfile -t minekonst/mkwiiai:latest .