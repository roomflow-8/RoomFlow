#!/bin/sh

# DockerHub 로그인
echo $DOCKER_PASSWORD | docker login --username $DOCKER_USERNAME --password-stdin

# 최신 이미지 pull
docker pull $DOCKER_USERNAME/roomflow:latest

# 기존 컨테이너가 실행 중이면 중지 및 삭제
if [ $(docker ps -aq -f name=roomflow) ]; then
    docker stop roomflow
    docker rm roomflow
fi

# 컨테이너 실행
docker run -d \
  --name roomflow \
  --restart unless-stopped \
  -p 8080:8080 \
  $DOCKER_USERNAME/roomflow:latest
