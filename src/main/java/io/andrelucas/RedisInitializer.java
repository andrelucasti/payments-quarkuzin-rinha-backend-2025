package io.andrelucas;

import io.quarkus.runtime.Startup;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Request;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@ApplicationScoped
public class RedisInitializer {
    private static final Logger log = LoggerFactory.getLogger(RedisInitializer.class);
    private final Redis redis;

    public RedisInitializer(Redis redis) {
        this.redis = redis;
    }

    @Inject
    public void initializeRedis() {
        log.info("Initializing Redis streams and consumer groups...");
        createConsumerGroup();
    }


    private void createConsumerGroup() {
        Request xgroupRequest = Request.cmd(Command.XGROUP)
                .arg("CREATE")
                .arg("payments_stream")
                .arg("payments_group")
                .arg("0")
                .arg("MKSTREAM");
        redis.send(xgroupRequest)
                .onFailure()
                .invoke(error -> {
                    if (error.getMessage().contains("BUSYGROUP")) {
                        log.info("Consumer group 'payments_group' already exists");
                    } else {
                        log.error("Error creating consumer group: {}", error.getMessage());
                    }
                })
                .subscribe()
                .with(success -> log.info("Consumer group 'payments_group' created successfully"));
    }
}