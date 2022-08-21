# Getting Start

## 前置条件

* docker
* kubectl
* minikube

## 安装/运行 minikube 

### 安装

```shell
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
```

### 运行

```shell
# 一定要指定 1.23.8 最新版会有蜜汁问题 
minikube start  --image-mirror-country='cn' --kubernetes-version=v1.23.8  --driver=docker
```



## 安装kubectl 

#### 安装

```shell
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

#### 验证

```shell
kubectl version
```

## Hello World (简单的服务发现)

由于spring-cloud-kubenetes官方文档用的是maven的fabric8插件打包部署到kubenetes平台，但是这个插件已经过时，不再维护，所以改用jkube插件,jkube可以通过xml配置生成对应的k8s部署yaml配置，并自动apply到kubenetes平台，具体文档

https://github.com/eclipse/jkube/tree/master/kubernetes-maven-plugin

### 新建一个maven项目

```shell
#### 略
```

### 依赖管理

```xml
<!--版本管理-->
<properties>
    <revision>1.0-SNAPSHOT</revision>
    <spring-boot-version>2.4.1</spring-boot-version>
    <spring-cloud-version>2020.0.1</spring-cloud-version>
    <maven-deploy-plugin.version>2.8.2</maven-deploy-plugin.version>
    <maven-surefire-plugin.version>2.22.2</maven-surefire-plugin.version>
    <kubernetes.maven.plugin.version>1.6.0</kubernetes.maven.plugin.version>
    <jkube.version>1.8.0</jkube.version>
</properties>
        <!--依赖版本管理-->
<dependencyManagement>
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud-version}</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot-version}</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
</dependencies>
</dependencyManagement>
<build>
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-deploy-plugin</artifactId>
        <version>${maven-deploy-plugin.version}</version>
        <configuration>
            <skip>true</skip>
        </configuration>
    </plugin>
</plugins>
</build>
```
### 新建一个子模块

#### 引入依赖

```xml

    <dependencies>
        <!--   spring-cloud-kubernetes 服务发现依赖    -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-kubernetes-fabric8-discovery</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-commons</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-bootstrap</artifactId>
        </dependency>


        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>


        <dependency>
            <groupId>org.jolokia</groupId>
            <artifactId>jolokia-core</artifactId>
        </dependency>

    </dependencies>
```

#### 添加打包插件

```xml
  <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot-version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>kubernetes</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.eclipse.jkube</groupId>
                        <artifactId>kubernetes-maven-plugin</artifactId>
                        <version>${jkube.version}</version>

                        <configuration>

                            <resources>
                                <labels>
                                    <all>
                                        <testProject>spring-boot-sample</testProject>
                                    </all>
                                </labels>
                                <serviceAccounts>
                                    <serviceAccount>
                                        <name>spring</name>
                                        <deploymentRef>kubernetes-hello-world</deploymentRef>
                                    </serviceAccount>
                                </serviceAccounts>
                            </resources>

                            <generator>
                                <includes>
                                    <include>spring-boot</include>
                                </includes>
                                <config>
                                    <spring-boot>
                                        <color>always</color>
                                    </spring-boot>
                                </config>
                            </generator>
                            <enricher>
                                <excludes>
                                    <exclude>jkube-expose</exclude>
                                </excludes>
                                <config>
                                    <jkube-service>
                                        <type>NodePort</type>
                                    </jkube-service>
                                </config>
                            </enricher>
                        </configuration>

                        <executions>
                            <execution>
                                <goals>
                                    <goal>resource</goal>
                                    <goal>build</goal>
                                    <goal>helm</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
```

部分命令介绍：(这里前两个 不说了 玩最后一个)

* 生成k8s描述文件

```shell
mvn clean k8s:resource -Pkubernetes
```

* 生成docker镜像

```shell
mvn package k8s:build -Pkubernetes
```

* 部署到k8s平台(在子模块目录下执行这个)

```shell
mvn k8s:deploy -Pkubernetes
```

* 验证

```
```

#### Jkube扩展

 ```xml
 ```









eval $(minikube docker-env)

mvn -s "/mnt/d/linux/settings-linux.xml"