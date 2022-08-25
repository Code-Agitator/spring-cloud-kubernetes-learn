# Getting Start

## 前言

如果是在本地WSL运行，并且本地配置了maven，我建议cp一个配置文件，然后修改仓库路径为/mnt/+windows下的路径

否则他会在当前目录下载依赖并且没法用

mvn [commond] -s "/mnt/d/linux/settings-linux.xml"

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

## 服务注册与发现

由于spring-cloud-kubenetes官方文档用的是maven的fabric8插件打包部署到kubenetes平台，但是这个插件已经过时，不再维护，所以改用jkube插件,jkube可以通过xml配置生成对应的k8s部署yaml配置，并自动apply到kubenetes平台，具体文档

https://github.com/eclipse/jkube/tree/master/kubernetes-maven-plugin

### 新建一个maven项目

```shell
#### 略
```

#### 依赖管理

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
#### 新建一个子模块

#### 引入依赖

```xml

    <dependencies>
          <!--   spring-cloud-kubernetes 服务发现依赖    -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-kubernetes-client-discovery</artifactId>
        </dependency>

        <!--   spring-cloud-kubernetes configMap依赖    -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-kubernetes-client-config</artifactId>
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

#### 新建入口类 App.java

```java
@SpringBootApplication
@EnableDiscoveryClient
@RestController
public class App {
    @Resource
    DiscoveryClient discoveryClient;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @GetMapping("/")
    public String hello() {
        return "Hello World";
    }

    @GetMapping("/services")
    public List<String> getServices() {
        return discoveryClient.getServices();
    }
}

```

#### 指定application name  application.yml

```yaml
spring:
  application:
    name: kubernetes-hello-world
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
                                <!-- 这里指定了一个service account资源，后续需要 -->
                                <serviceAccounts>
                                    <serviceAccount>
                                        <name>spring</name>		 
                                        <deploymentRef>${project.artifactId}</deploymentRef>
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

#### 部署

部分命令介绍：(这里前两个 不说了 玩最后一个)

PS: 由于使用的是本地docker仓库，需要执行以下命令，才能是minikube可以从本地仓库拉取镜像，每次都需要在部署的终端下执行

```shell
eval $(minikube docker-env)
```

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

* 部署成功后

```shell
[INFO] --- kubernetes-maven-plugin:1.8.0:deploy (default-cli) @ spring-cloud-k8s ---
[INFO] k8s: Using Kubernetes at https://127.0.0.1:59152/ in namespace null with manifest /mnt/d/items/spring-cloud-kubernetes-learn/spring-cloud-k8s/target/classes/META-INF/jkube/kubernetes.yml 
[INFO] k8s: Updating a ServiceAccount from kubernetes.yml
[INFO] k8s: Updated ServiceAccount: spring-cloud-k8s/target/jkube/applyJson/default/serviceaccount-spring.json
[INFO] k8s: Creating a Service from kubernetes.yml namespace default name spring-cloud-k8s
[INFO] k8s: Created Service: spring-cloud-k8s/target/jkube/applyJson/default/service-spring-cloud-k8s.json
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name spring-cloud-k8s
[INFO] k8s: Created Deployment: spring-cloud-k8s/target/jkube/applyJson/default/deployment-spring-cloud-k8s.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:02 min
[INFO] Finished at: 2022-08-22T09:55:54+08:00
[INFO] ------------------------------------------------------------------------
```

* 查看pod

```shell
kubectl get pods
#NAME                                READY   STATUS    RESTARTS   AGE
#spring-cloud-k8s-86bc95d68c-c5btk   1/1     Running   0          21s
```

* 转发端口到pod

```shell
# spring-cloud-k8s-86bc95d68c-c5btk 为上述对应pod的NAME
kubectl port-forward spring-cloud-k8s-86bc95d68c-c5btk 8080:8080
```

* 请求本地8080端口 localhost:8080 

```shell
curl localhost:8080
#Hello World
curl localhost:8080/service
#{"timestamp":"2022-08-22T02:07:00.302+00:00","status":500,"error":"Internal Server Error","message":"","path":"/services"}
```

这里出现了500错误是因为，我们一开始设置的service account 他会在k8s平台自动创建一个spring账号，但是他并没有相应的权限去获取k8s平台的信息，所以没办法获取的服务的信息，此时我们为spring赋予只读权限，具体日志可以通过一下命令查看，不过不建议，因为没有权限的时候他会一直报错，日志无限增长

```shell
kubectl logs spring-cloud-k8s-86bc95d68c-c5btk
```

* 为spring账户添加权限

```shell
#kubectl create serviceaccount spring # 这是创建spring账户的命令 jkube已经可以通过配置创建可以不执行
#创建一个clusterrolebinding 设置clusterrole为view只读 指定serviceaccount 
kubectl create clusterrolebinding spring-api --clusterrole=view --serviceaccount=default:spring --namespace=default
```

* 查询平台服务

```shell
curl localhost:8080/services
# ["hello-world-example","kubernetes","kubernetes-hello-world","spring-cloud-k8s"]
```

到这里就成功将spring Boot部署到k8s平台并且完成服务发现

## ConfigMap 配置管理（k8s原生）

ps: 官方文档使用的工具是Fabric8，本文档用的是jukube 所以这里没有和官方文档的教程走

#### 改写App.java入口类

```java
@Value("${greeting.message}")
String message;

@GetMapping("/config")
public String  getMessage(){
    return message;
}
```

#### 新建文件src/jkube/configmap.yml

```yaml
metadata:
  name: ${project.artifactId}
data:
  application.yml: |-
    greeting:
      message: Say Hello to the World
```

#### 新建文件src/jkube/deployment.yml

```yaml
metadata:
  annotations:
    configmap.jkube.io/update-on-change: ${project.artifactId}
spec:
  replicas: 1
  template:
    spec:
      volumes:
        - name: config
          configMap:
            name: ${project.artifactId}
            items:
              - key: application.yml
                path: application.yml
      containers:
        - volumeMounts:
            - name: config
              mountPath: /deployments/config
      serviceAccount: spring
```

#### 部署到平台后查看新建的configMap

```shell
mvn k8s:deploy -Pkubernetes

kubectl get cm
# NAME               DATA   AGE
# kube-root-ca.crt   1      3d1h
# spring-cloud-k8s   1      95m
```

#### 转发端口 请求查看

```shell
kubectl port-forward spring-cloud-k8s-86bc95d68c-c5btk 8080:8080

curl localhost:8080/config
# Say Hello to the World
```

## ConfigMap热刷新

#### 添加AppProperties.java

亲测如果使用@Value()的方式 即使添加了@RefreshScope 也无法热刷新

```java
@RefreshScope
@ConfigurationProperties(prefix = "greeting")
public class AppProperties {
    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

```

#### 改写App.java

```java
  	@Resource
    AppProperties appProperties;

 	@GetMapping("/config")
    public String getMessage() {
        return appProperties.getMessage();
    }
```

#### 添加applicatoin.yml配置

```yaml
spring:
  application:
    name: spring-cloud-k8s
management:
  endpoint:
    refresh:
      enabled: true
  endpoints:
    web:
      exposure:
        include: "*"
```

#### 添加bootstrap.yml配置

```yaml
spring:
  cloud:
    kubernetes:
      config:
        enabled: true
        sources:
          - namespace: default
          	# 对应的configMap 的name
            name: spring-cloud-k8s
      reload:
        enabled: true
        # 更新策略  
        strategy: refresh
        # 监听 configMap变化
        monitoring-config-maps: true
        mode: event
```

#### 重新部署与验证

```shell
mvn k8s:deploy -Pkubernetes 
# 删除原来的pod 让他重新部署
kubectl delete -n default pod spring-cloud-k8s-85b7b79766-p7h6q
# 转发端口
kubectl port-forward spring-cloud-k8s-85b7b79766-p7h6q 8080:8080 

# 请求
curl localhost:8080/config
# Say Hello to the World
```

#### 修改configmap

```yaml
metadata:
  name: ${project.artifactId}
data:
  application.yml: |-
    greeting:
      message: Say GoodBey to the World
```

#### 应用修改并查看

```shell
kubectl apply -f configmap.yml
curl localhost:8080/config
# Say GoodBey to the World
```

## 查看Pod信息

当SpringBoot部署到K8s平台后，Spring boot 通过spring-actuator 获取一些pod的信息

```shell
curl localhost:8080/actuator/info

# {
#  "kubernetes": {
#    "nodeName": "minikube",
#    "podIp": "172.17.0.4",
#    "hostIp": "192.168.49.2",
#    "namespace": "default",
#    "podName": "spring-cloud-k8s-858d76bc79-z4kkq",
#    "serviceAccount": "spring",
#    "inside": true
#  }
# }
```

上述信息可以通过 **management.info.kubernetes.enabled** 进行关闭(false)

## Leader选举

选举需要依赖 fabric8

而且不能同时出现 spring-cloud-starter-client-discover 因为两个都对服务发现实现了，同时引入会出现bean的重复定义抛出异常

选举机制：只有一个实例成为 leader，当leader挂了之后上一个leader结点会触发 **OnRevokedEvent** 事件，然后所有的节点进行竞争，成功选举为 leader的节点会触发 **OnGrantedEvent**

#### 新建一个新的子模块 spring-cloud-leader

```xml
   <dependencies>
        <!--        引入相关依赖-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-kubernetes-fabric8</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-kubernetes-fabric8-leader</artifactId>
        </dependency>

        <!--        通用依赖-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
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
            <groupId>org.jolokia</groupId>
            <artifactId>jolokia-core</artifactId>
        </dependency>
    </dependencies>
<build>
    ... 参考上文
</build>
<profiles>
     <profile>
         ... 参考上文
         <build>
              <configuration>
                     <resources>
                          <serviceAccounts>
                                    <serviceAccount>
                                        <!-- 重点 不一样的地方 这里指定的service account不一样了 -->
                                        <name>spring-leader</name>
                                        <deploymentRef>${project.artifactId}</deploymentRef>
                                    </serviceAccount>
                                </serviceAccounts>
                  </resources>
         
         	</configuration>
         </build>
        
    </profile>
</profiles>
```

以上pom在配置service account的时候指定了新的，因为原来的sa 只有 view 只读权限，而leader选举是通过K8S的ConfigMap来实现的，所以我们需要给当前项目赋予edit权限，让其有权限去修改configMap

#### 编写LeaderController 已经入口

【App.java(单纯的springboot 不需要其他注解 这里省略)】

```java

@RestController
public class LeaderController {


    private final String host;

    @Value("${spring.cloud.kubernetes.leader.role}")
    private String role;

    private Context context;

    public LeaderController() throws UnknownHostException {
        this.host = InetAddress.getLocalHost().getHostName();
    }

    /**
     * 获取当前节点状态
     *
     * @return info
     */
    @GetMapping
    public String getInfo() {
        if (this.context == null) {
            return String.format("I am '%s' but I am not a leader of the '%s'", this.host, this.role);
        }

        return String.format("I am '%s' and I am the leader of the '%s'", this.host, this.role);
    }

    /**
     * 主动下线
     * @return info about leadership
     */
    @PutMapping
    public ResponseEntity<String> revokeLeadership() {
        if (this.context == null) {
            String message = String.format("Cannot revoke leadership because '%s' is not a leader", this.host);
            return ResponseEntity.badRequest().body(message);
        }

        this.context.yield();

        String message = String.format("Leadership revoked for '%s'", this.host);
        return ResponseEntity.ok(message);
    }

    /**
     * 被选举为leader事件
     *
     * @param event on granted event
     */
    @EventListener
    public void handleEvent(OnGrantedEvent event) {
        System.out.println(String.format("'%s' leadership granted", event.getRole()));
        this.context = event.getContext();
    }

    /**
     * leader下线事件
     *
     * @param event on revoked event
     */
    @EventListener
    public void handleEvent(OnRevokedEvent event) {
        System.out.println(String.format("'%s' leadership revoked", event.getRole()));
        this.context = null;
    }


}
```

#### application.yml

```yaml
spring:
  application:
    name: spring-cloud-leader
  cloud:
    kubernetes:
      leader:
      	# 用于实现leader选举的configMap名称
        config-map-name: leader
        # 当前服务角色
        role: world
```

#### 新建service account 并授予edit权限

```shell
kubectl create serviceaccount spring-leader # 这是创建spring账户的命令 jkube已经可以通过配置创建可以不执行
kubectl create clusterrolebinding spring-api --clusterrole=edit --serviceaccount=default:spring --namespace=default
```

#### 部署项目与验证

```shell
mvn k8s:deploy -Pkubernetes 
# 部署完成后查看pod是否正常运行
kubectl get pod
#NAME                                   READY   STATUS    RESTARTS   AGE 
#spring-cloud-leader-5bdd657d86-qtch5   1/1     Running   0          27m

# 查看他是否触发了OnGrantedEvent 事件成为了leader
kubectl logs spring-cloud-leader-5bdd657d86-qtch5 |grep 'leadership'
#DefaultCandidate{role=world, id=spring-cloud-leader-5bdd657d86-qtch5} has been granted leadership; context: org.springframework.cloud.kubernetes.commons.leader.LeaderContext@128153a0

# 复制一个节点出来
kubectl scale --replicas=2 deployment.apps/spring-cloud-leader
kubectl get pod
#NAME                                   READY   STATUS    RESTARTS   AGE
#spring-cloud-leader-5bdd657d86-hvm47   1/1     Running   0          15m
#spring-cloud-leader-5bdd657d86-qtch5   1/1     Running   0          49m

# 打开两个终端 分别转发两个服务端口
kubectl port-forward spring-cloud-leader-5bdd657d86-hvm47 8081:8080
kubectl port-forward spring-cloud-leader-5bdd657d86-qtch5 8080:8080

# 查看两个节点
curl localhost:8080 
# I am 'spring-cloud-leader-5bdd657d86-qtch5' and I am the leader of the 'world'
curl localhost:8081
# I am 'spring-cloud-leader-5bdd657d86-hvm47' but I am not a leader of the 'world'

# 释放 leader
curl -X PUT localhost:8080
# Leadership revoked for 'spring-cloud-leader-5bdd657d86-qtch5'
# 查看8081 这里是有概率的 我释放了很多次8080 8081才抢到
curl localhost:8081
# I am 'spring-cloud-leader-5bdd657d86-hvm47' and I am the leader of the 'world'

# 查看8080日志多次选举的过程(在我释放了两次后 选举leader失败)
kubectl logs spring-cloud-leader-5bdd657d86-qtch5 | grep leadership
# DefaultCandidate{role=world, id=spring-cloud-leader-5bdd657d86-qtch5} has been granted leadership; context
# DefaultCandidate{role=world, id=spring-cloud-leader-5bdd657d86-qtch5} leadership has been revoked
# DefaultCandidate{role=world, id=spring-cloud-leader-5bdd657d86-qtch5} has been granted leadership; context
# DefaultCandidate{role=world, id=spring-cloud-leader-5bdd657d86-qtch5} leadership has been revoked
# Failure when acquiring leadership for 'DefaultCandidate
```

## loadbalancer and openfeign

#### 新建spring-cloud-balancer模块 

设置为pom 并添加打包插件

```xml
 <packaging>pom</packaging> 
<profiles>
        <profile>
            <id>kubernetes</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.eclipse.jkube</groupId>
                        <artifactId>kubernetes-maven-plugin</artifactId>
                        <version>${kubernetes.maven.plugin.version}</version>
                        <executions>
                            <execution>
                                <id>fmp</id>
                                <goals>
                                    <goal>resource</goal>
                                    <goal>build</goal>
                                </goals>
                            </execution>
                        </executions>
                        <configuration>
                            <enricher>
                                <config>
                                    <jkube-service>
                                        <type>NodePort</type>
                                    </jkube-service>
                                </config>
                            </enricher>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

```

#### 新建生产者模块 name-service 依赖以及插件

```xml
<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
 <build>
        <plugins>
            <plugin>
                <!--skip deploy -->
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-deploy-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>

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

            <plugin>
                <groupId>org.eclipse.jkube</groupId>
                <artifactId>kubernetes-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>fmp</id>
                        <goals>
                            <goal>resource</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

#### App.java springboot即可 略

#### NameController.java

```java

@RestController
public class NameController {

	private static final Logger LOG = LoggerFactory.getLogger(NameController.class);

	private final String hostName = System.getenv("HOSTNAME");

	@GetMapping("/")
	public String ribbonPing() {
		LOG.info("Ribbon ping");
		return hostName;
	}

	/**
	 * 返回hostname  并模拟延时
	 */
	@GetMapping("/name")
	public Mono<String> getName(@RequestParam(value = "delay", defaultValue = "0") int delayValue) {
		LOG.info(String.format("Returning a name '%s' with a delay '%d'", hostName, delayValue));
		delay(delayValue);
		return hostName;
	}

	private void delay(int delayValue) {
		try {
			Thread.sleep(delayValue);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
```

#### 新建生产者name-service-api模块

```xml
  <dependencies>
      <dependency>
          <groupId>org.springframework.cloud</groupId>
          <artifactId>spring-cloud-starter-openfeign</artifactId>
      </dependency>
      <dependency>
          <groupId>io.projectreactor</groupId>
          <artifactId>reactor-core</artifactId>
      </dependency>

  </dependencies>

```

#### 添加feign接口

```java
@FeignClient(name = "name-service")
public interface MsNameService {
    @GetMapping("/name")
    String getName(@RequestParam(value = "delay", defaultValue = "0") int delayValue);
}
```

#### 新建消费者greeting-service  依赖以及插件

```xml

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-kubernetes-client-loadbalancer</artifactId>
        </dependency>
        <!--   spring-cloud-kubernetes 服务发现依赖    -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-kubernetes-client-discovery</artifactId>
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
        <!-- 生产者api模块 -->
         <dependency>
            <groupId>org.example</groupId>
            <artifactId>name-service-api</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

    </dependencies>


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
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                </configuration>
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
                                        <!-- 沿用之前新建的spring账户 具有view权限 -->
                                        <name>spring</name>
                                        <deploymentRef>${project.artifactId}</deploymentRef>
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

#### App.java  

构建loadbalancer

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "example")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

   	@LoadBalanced
    @Bean
    RestTemplate loadBalancedWebClientBuilder() {
        return new RestTemplateBuilder().build();
    }
}

```

#### GreetingController.java  

调用nameService

```java
@RestController
public class GreetingController {


    private final MsNameService msNameService;

    public GreetingController(MsNameService msNameService) {
        this.msNameService = msNameService;
    }

    @GetMapping("/greeting")
    public String getGreeting(@RequestParam(value = "delay", defaultValue = "0") int delay) {
        return String.format("Hello from %s!", msNameService.getName(delay));
    }

}
```

#### 部署验证

进入spring-cloud-loadbalancer目录

```shell
# 构建部署
mvn k8s:deploy -Pkubernetes
#[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
#[INFO] ------------------------------------------------------------------------
#[INFO] Reactor Summary for spring-cloud-loadbalancer 1.0-SNAPSHOT:
#[INFO]
#[INFO] spring-cloud-loadbalancer .......................... SUCCESS [ 14.484 s]
#[INFO] greeting-service ................................... SUCCESS [01:24 min]
#[INFO] name-service ....................................... SUCCESS [01:41 min]
#[INFO] ------------------------------------------------------------------------
#[INFO] BUILD SUCCESS
#[INFO] ------------------------------------------------------------------------
#[INFO] Total time:  03:22 min
#[INFO] Finished at: 2022-08-23T14:54:56+08:00
#[INFO] ------------------------------------------------------------------------

# 查看
kubectl get pod
#NAME                                   READY   STATUS    RESTARTS      AGE
#greeting-service-58d9ff665d-x6svm      1/1     Running   0             10m
#name-service-d577c996c-hnbds           1/1     Running   0             9m49s

# 转发端口
kubectl port-forward greeting-service-58d9ff665d-x6svm 8080:8080
# 请求验证
curl localhost:8080/greeting
# Hello from name-service-d577c996c-hnbds!

# 新增一个name-service 的pod
kubectl scale --replicas=2 deployment name-service
# 查看两个pod
kc get endpoints/name-service
#NAME           ENDPOINTS                          AGE
#name-service   172.17.0.10:8080,172.17.0.9:8080   34m

# 请求并设置延时
curl localhost:8080/gretting?delay=3000
#Hello from name-service-d577c996c-hnbds!
# 同时再次发起
curl localhost:8080/gretting
#Hello from name-service-d577c996c-whxth!
```

## Security Configurations  k8s中的安全配置

#### namespace

K8s 可以通过NAMESPACE去区分命名空间，配置namespace的方式：

* Jkube:

```xml
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
                 			<namespace>my-namespace</namespace>
                        </configuration>

                    </plugin>
                </plugins>
            </build>
        </profile>
```

#### promession

Spring-cloud-kubenetes 的服务发现以及configMap等都是基于对kubenetes中API来实现的，所以需要为spring-boot程序配置相应的server account 以及 权限

以下是不同依赖对于权限的要求，对于对应的资源，都需要对应的'get','list','watch'权限

| Dependency                                     | Resources                 |
| :--------------------------------------------- | :------------------------ |
| spring-cloud-starter-kubernetes-fabric8        | pods, services, endpoints |
| spring-cloud-starter-kubernetes-fabric8-config | configmaps, secrets       |
| spring-cloud-starter-kubernetes-client         | pods, services, endpoints |
| spring-cloud-starter-kubernetes-client-config  | configmaps, secrets       |

为default用户添加上述权限的示例：

* 创建一个server account

```shell
kubectl create namespace my-namespace
# namespace/my-namespace created
kubectl create serviceaccount spring-security -n my-namespace
# serviceaccount/spring-security created
```

* 添加角色

```yaml
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: my-namespace
  name: namespace-reader
rules:
  - apiGroups: [""]
    resources: ["configmaps", "pods", "services", "endpoints", "secrets"]
    verbs: ["get", "list", "watch"]

```

```shell
kubectl apply -f addRole.yml
# role.rbac.authorization.k8s.io/namespace-reader created
```

* 将default设置为我们刚才添加的角色

```yaml
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: namespace-reader-binding
  namespace: my-namespace
subjects:
- kind: ServiceAccount
  name: spring-security
  apiGroup: ""
roleRef:
  kind: Role
  name: namespace-reader
  apiGroup: ""
```

```shell
kubectl apply -f roleBinding.yml 
# rolebinding.rbac.authorization.k8s.io/namespace-reader-binding created
```

* 查看

```shell
kubectl get sa -n my-namespace
#NAME              SECRETS   AGE
#default           1         9m42s
#spring-security   1         9m27s
```



## 服务注册

`spring.cloud.service-registry.auto-registration.enabled` 和`@EnableDiscoveryClient(autoRegister=false)`都可以控制服务是否自动注册

## 配置中心观察器

在前面的ConfigMap中，我们配置的ConfigMap热更新是旧的实现，官方在2020.*之后的版本已经放弃的更新，并且推荐使用Spring Cloud Kubernetes Configuration Watcher 部署到k8s平台上通知到k8s中的服务进行配置更新，其本质是通过/actuator/refresh来通知服务进行配置刷新

#### 部署Spring Cloud Kubernetes Configuration Watcher到k8s

* 新建deployment.yml (kubectl apply -fdeployment.yml )

这个部署文件做了几件事情：

1. 新建了Service指定端口为8888
2. 新建了一个service account 
3. 新建了一个role和RoleBinding
4. 为新建的role赋予一些资源和权限
5. 使用官方镜像部署服务

```yaml
---
apiVersion: v1
kind: List
items:
  - apiVersion: v1
    kind: Service
    metadata:
      labels:
        app: spring-cloud-kubernetes-configuration-watcher
      name: spring-cloud-kubernetes-configuration-watcher
    spec:
      ports:
        - name: http
          port: 8888
          targetPort: 8888
      selector:
        app: spring-cloud-kubernetes-configuration-watcher
      type: ClusterIP
  - apiVersion: v1
    kind: ServiceAccount
    metadata:
      labels:
        app: spring-cloud-kubernetes-configuration-watcher
      name: spring-cloud-kubernetes-configuration-watcher
  - apiVersion: rbac.authorization.k8s.io/v1
    kind: RoleBinding
    metadata:
      labels:
        app: spring-cloud-kubernetes-configuration-watcher
      name: spring-cloud-kubernetes-configuration-watcher:view
    roleRef:
      kind: Role
      apiGroup: rbac.authorization.k8s.io
      name: namespace-reader
    subjects:
      - kind: ServiceAccount
        name: spring-cloud-kubernetes-configuration-watcher
  - apiVersion: rbac.authorization.k8s.io/v1
    kind: Role
    metadata:
      namespace: default
      name: namespace-reader
    rules:
      - apiGroups: ["", "extensions", "apps"]
        resources: ["configmaps", "pods", "services", "endpoints", "secrets"]
        verbs: ["get", "list", "watch"]
  - apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: spring-cloud-kubernetes-configuration-watcher-deployment
    spec:
      selector:
        matchLabels:
          app: spring-cloud-kubernetes-configuration-watcher
      template:
        metadata:
          labels:
            app: spring-cloud-kubernetes-configuration-watcher
        spec:
          serviceAccount: spring-cloud-kubernetes-configuration-watcher
          containers:
          - name: spring-cloud-kubernetes-configuration-watcher
            image: springcloud/spring-cloud-kubernetes-configuration-watcher:2.0.1-SNAPSHOT
            imagePullPolicy: IfNotPresent
            readinessProbe:
              httpGet:
                port: 8888
                path: /actuator/health/readiness
            livenessProbe:
              httpGet:
                port: 8888
                path: /actuator/health/liveness
            ports:
            - containerPort: 8888
```

#### 修改bootstrap.yml

关闭reload

```yaml
spring:
  cloud:
    kubernetes:
      config:
        enabled: true
        sources:
          - namespace: default
            name: spring-cloud-k8s
      reload:
#        enabled: true
        strategy: refresh
        monitoring-config-maps: true
        mode: event
```

#### 新建configMap.yml

${project.artifactId}是对应的configmap 的name 自行修改

这里的重点是添加了一个label   spring.cloud.kubernetes.config: "true"

configmap watcher就是通过这个label去决定要不要通知其他服务更新

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ${project.artifactId}
  labels:
    spring.cloud.kubernetes.config: "true"
data:
  application.yml: |-
    greeting:
      message: Say Hello from one
```

## 扩展

#### [Spring Cloud Kubernetes Config Server](https://docs.spring.io/spring-cloud-kubernetes/docs/current/reference/html/#spring-cloud-kubernetes-configserver)

spring-cloud-config-server在kubernetes中的实现，hello world部署非常简单，使用官方提供的部署配置即可

#### [Spring Cloud Kubernetes Discovery Server](https://docs.spring.io/spring-cloud-kubernetes/docs/current/reference/html/#spring-cloud-kubernetes-discoveryserver)

可以获取应用已经实例的信息,部署同上

