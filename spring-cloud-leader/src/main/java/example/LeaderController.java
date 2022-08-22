package example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.leader.Context;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
