# kafka
about me with kafka

# 环境搭建
- [kafka下载地址](https://archive.apache.org/dist/kafka/1.0.0/kafka_2.11-1.0.0.tgz)
- 启动zookeeper服务
  ```
  nohup /opt/kafka_2.11-1.0.0/bin/zookeeper-server-start.sh /opt/kafka_2.11-1.0.0/config/zookeeper.properties > /opt/kafka_2.11-1.0.0/logs/zookeeper.log 2>&1 &
  ```
- 启动kafka服务
  ```
  nohup /opt/kafka_2.11-1.0.0/bin/kafka-server-start.sh /opt/kafka_2.11-1.0.0/config/server.properties > /opt/kafka_2.11-1.0.0/logs/output.log 2>&1 &
  ```
- 验证启动状态
  ```
  /opt/kafka_2.11-1.0.0/bin/kafka-topics.sh --list -zookeeper localhost:2181
  ```












ing...
### [kafka中文文档](https://kafka.apachecn.org/quickstart.html)
