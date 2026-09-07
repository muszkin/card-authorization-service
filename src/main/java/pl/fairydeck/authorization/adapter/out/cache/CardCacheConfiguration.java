package pl.fairydeck.authorization.adapter.out.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import pl.fairydeck.authorization.domain.card.Card;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
class CardCacheConfiguration {

    @Bean
    RedisTemplate<String, Card> cardRedisTemplate(RedisConnectionFactory connectionFactory) {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY).build();
        RedisTemplate<String, Card> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(mapper, Card.class));
        return template;
    }
}
